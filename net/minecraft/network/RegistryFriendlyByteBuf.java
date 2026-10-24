package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;
import net.neoforged.neoforge.network.connection.ConnectionType;

public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
   private final RegistryAccess registryAccess;
   private final ConnectionType connectionType;

   @Deprecated
   public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess) {
      super(source);
      this.registryAccess = registryAccess;
      this.connectionType = ConnectionType.OTHER;
   }

   public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess, ConnectionType connectionType) {
      super(source);
      this.registryAccess = registryAccess;
      this.connectionType = connectionType;
   }

   public ConnectionType getConnectionType() {
      return this.connectionType;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess, ConnectionType connectionType) {
      return buf -> new RegistryFriendlyByteBuf(buf, registryAccess, connectionType);
   }

   @Deprecated
   public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess) {
      return buf -> new RegistryFriendlyByteBuf(buf, registryAccess);
   }
}
