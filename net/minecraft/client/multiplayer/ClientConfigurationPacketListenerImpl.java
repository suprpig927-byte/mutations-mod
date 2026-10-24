package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent.ClientPacketReceived;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements ClientConfigurationPacketListener, TickablePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Component DISCONNECTED_MESSAGE = Component.translatable("multiplayer.disconnect.code_of_conduct");
   private final LevelLoadTracker levelLoadTracker;
   private final GameProfile localGameProfile;
   private FeatureFlagSet enabledFeatures;
   private final RegistryAccess.Frozen receivedRegistries;
   private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
   @Nullable
   private KnownPacksManager knownPacks;
   @Nullable
   protected ChatComponent.State chatState;
   private boolean seenCodeOfConduct;
   private ConnectionType connectionType = ConnectionType.OTHER;
   private Map<Identifier, Component> failureReasons = new HashMap<>();

   public ClientConfigurationPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie cookie) {
      super(minecraft, connection, cookie);
      this.levelLoadTracker = cookie.levelLoadTracker();
      this.localGameProfile = cookie.localGameProfile();
      this.receivedRegistries = cookie.receivedRegistries();
      this.enabledFeatures = cookie.enabledFeatures();
      this.chatState = cookie.chatState();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   protected void handleCustomPayload(CustomPacketPayload payload) {
      this.handleUnknownCustomPayload(payload);
   }

   private void handleUnknownCustomPayload(CustomPacketPayload payload) {
      LOGGER.warn("Unknown custom packet payload: {}", payload.type().id());
   }

   @Override
   public void handleRegistryData(ClientboundRegistryDataPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendContents(packet.registry(), packet.entries());
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      this.registryDataCollector.appendTags(packet.tags());
   }

   @Override
   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket packet) {
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(packet.features());
      ClientNetworkRegistry.initializeOtherConnection(this);
   }

   @Override
   public void handleSelectKnownPacks(ClientboundSelectKnownPacks packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.knownPacks == null) {
         this.knownPacks = new KnownPacksManager();
      }

      List<KnownPack> selected = this.knownPacks.trySelectingPacks(packet.knownPacks());
      this.send(new ServerboundSelectKnownPacks(selected));
   }

   @Override
   public void handleResetChat(ClientboundResetChatPacket packet) {
      this.chatState = null;
   }

   private <T> T runWithResources(Function<ResourceProvider, T> operation) {
      if (this.knownPacks == null) {
         return operation.apply(ResourceProvider.EMPTY);
      } else {
         Object var3;
         try (CloseableResourceManager manager = this.knownPacks.createResourceManager()) {
            var3 = operation.apply(manager);
         }

         return (T)var3;
      }
   }

   @Override
   public void handleCodeOfConduct(ClientboundCodeOfConductPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      if (this.seenCodeOfConduct) {
         throw new IllegalStateException("Server sent duplicate Code of Conduct");
      } else {
         this.seenCodeOfConduct = true;
         String codeOfConduct = packet.codeOfConduct();
         if (this.serverData != null && this.serverData.hasAcceptedCodeOfConduct(codeOfConduct)) {
            this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
         } else {
            Screen lastScreen = this.minecraft.gui.screen();
            this.minecraft.gui.setScreen(new CodeOfConductScreen(this.serverData, lastScreen, codeOfConduct, accepted -> {
               if (accepted) {
                  this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
                  this.minecraft.gui.setScreen(lastScreen);
               } else {
                  this.createDialogAccess().disconnect(DISCONNECTED_MESSAGE);
               }
            }));
         }
      }
   }

   @Override
   public void handleConfigurationFinished(ClientboundFinishConfigurationPacket packet) {
      PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
      RegistryAccess.Frozen registries = this.runWithResources(
         knownPacksProvider -> this.registryDataCollector
            .collectGameRegistries(knownPacksProvider, this.receivedRegistries, this.connection.isMemoryConnection())
      );
      IntegratedServer localServer = this.minecraft.getSingleplayerServer();
      if (localServer != null) {
         registries = filterRegistries(localServer.registryAccess(), registries.listRegistryKeys());
      }

      this.connection
         .setupInboundProtocol(
            GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries, this.connectionType)),
            new ClientPacketListener(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  this.levelLoadTracker,
                  this.localGameProfile,
                  this.telemetryManager,
                  registries,
                  this.enabledFeatures,
                  this.serverBrand,
                  this.serverData,
                  this.postDisconnectScreen,
                  this.serverCookies,
                  this.chatState,
                  this.customReportDetails,
                  this.serverLinks(),
                  this.seenPlayers,
                  this.seenInsecureChatWarning,
                  this.connectionType
               )
            )
         );
      NeoForge.EVENT_BUS.post(new ClientPacketReceived(registries, this.connection.isMemoryConnection()));
      if (this.connectionType.isOther()) {
         ClientNetworkRegistry.initializeOtherConnection(this);
      }

      NetworkRegistry.onConfigurationFinished(this);
      this.connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
      this.connection
         .setupOutboundProtocol(
            GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries, this.connectionType), new GameProtocols.Context() {
               {
                  Objects.requireNonNull(ClientConfigurationPacketListenerImpl.this);
               }

               @Override
               public boolean hasInfiniteMaterials() {
                  return true;
               }

               @Override
               public boolean canUseCommandBlocks() {
                  return true;
               }
            })
         );
   }

   private static RegistryAccess.Frozen filterRegistries(RegistryAccess.Frozen original, Stream<ResourceKey<? extends Registry<?>>> keysToInclude) {
      List<? extends Registry<?>> filteredRegistries = keysToInclude.map(original::lookupOrThrow).toList();
      return new RegistryAccess.ImmutableRegistryAccess(filteredRegistries).freeze();
   }

   @Override
   public void tick() {
      this.sendDeferredPackets();
   }

   @Override
   public void onDisconnect(DisconnectionDetails reason) {
      super.onDisconnect(reason);
      this.minecraft.clearDownloadedResourcePacks();
   }

   @Override
   public void handleCustomPayload(ClientboundCustomPayloadPacket packet) {
      if (!ClientNetworkRegistry.isConnectionInitialized(this.connection) && packet.payload() instanceof MinecraftRegisterPayload) {
         super.handleCustomPayload(packet);
         ClientNetworkRegistry.sendInitialListeningChannels(this);
      } else if (packet.payload() instanceof ModdedNetworkQueryPayload) {
         this.connectionType = ConnectionType.NEOFORGE;
         ClientNetworkRegistry.onNetworkQuery(this);
      } else if (packet.payload() instanceof ModdedNetworkPayload moddedNetworkPayload) {
         ClientNetworkRegistry.initializeNeoForgeConnection(this, moddedNetworkPayload.setup());
      } else if (packet.payload() instanceof ModdedNetworkSetupFailedPayload setupFailedPayload) {
         this.failureReasons = setupFailedPayload.failureReasons();
      } else {
         if (this.connectionType.isOther() && packet.payload() instanceof BrandPayload) {
            ClientNetworkRegistry.initializeOtherConnection(this);
         }

         super.handleCustomPayload(packet);
      }
   }

   @Override
   protected Screen createDisconnectScreen(DisconnectionDetails details) {
      Screen superScreen = super.createDisconnectScreen(details);
      return (Screen)(this.failureReasons.isEmpty()
         ? superScreen
         : new ModMismatchDisconnectedScreen(superScreen, Component.translatable("disconnect.lost"), this.failureReasons));
   }

   public ConnectionType getConnectionType() {
      return this.connectionType;
   }

   @Override
   protected DialogConnectionAccess createDialogAccess() {
      return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
         {
            Objects.requireNonNull(ClientConfigurationPacketListenerImpl.this);
         }

         @Override
         public void runCommand(String command, @Nullable Screen activeScreen) {
            ClientConfigurationPacketListenerImpl.LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", command);
         }
      };
   }
}
