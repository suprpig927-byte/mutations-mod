package net.minecraft.network.protocol.common.custom;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public interface CustomPacketPayload {
   CustomPacketPayload.Type<? extends CustomPacketPayload> type();

   static <B extends ByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(StreamMemberEncoder<B, T> writer, StreamDecoder<B, T> reader) {
      return StreamCodec.ofMember(writer, reader);
   }

   static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String id) {
      return new CustomPacketPayload.Type<>(Identifier.withDefaultNamespace(id));
   }

   static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(
      final CustomPacketPayload.FallbackProvider<B> fallback,
      List<CustomPacketPayload.TypeAndCodec<? super B, ?>> types,
      final ConnectionProtocol protocol,
      final PacketFlow packetFlow
   ) {
      final Map<Identifier, StreamCodec<? super B, ? extends CustomPacketPayload>> idToType = types.stream()
         .collect(Collectors.toUnmodifiableMap(t -> t.type().id(), CustomPacketPayload.TypeAndCodec::codec));
      return new StreamCodec<B, CustomPacketPayload>() {
         private StreamCodec<? super B, ? extends CustomPacketPayload> findCodec(Identifier typeId) {
            StreamCodec<? super B, ? extends CustomPacketPayload> codec = idToType.get(typeId);
            if (codec == null) {
               codec = NetworkRegistry.getCodec(typeId, protocol, packetFlow);
            }

            return codec != null ? codec : fallback.create(typeId);
         }

         private <T extends CustomPacketPayload> void writeCap(B output, CustomPacketPayload.Type<T> type, CustomPacketPayload payload) {
            output.writeIdentifier(type.id());
            StreamCodec<B, T> codec = this.findCodec(type.id);

            try {
               codec.encode(output, (T)payload);
            } catch (RuntimeException var6) {
               throw new RuntimeException("Failed encoding custom payload " + type.id() + ": " + var6, var6);
            }
         }

         public void encode(B output, CustomPacketPayload value) {
            this.writeCap(output, value.type(), value);
         }

         public CustomPacketPayload decode(B input) {
            Identifier identifier = input.readIdentifier();

            try {
               return (CustomPacketPayload)this.findCodec(identifier).decode(input);
            } catch (RuntimeException var4) {
               throw new RuntimeException("Failed decoding custom payload " + identifier + ": " + var4, var4);
            }
         }
      };
   }

   default ClientboundCustomPayloadPacket toVanillaClientbound() {
      return new ClientboundCustomPayloadPacket(this);
   }

   default ServerboundCustomPayloadPacket toVanillaServerbound() {
      return new ServerboundCustomPayloadPacket(this);
   }

   public interface FallbackProvider<B extends FriendlyByteBuf> {
      StreamCodec<B, ? extends CustomPacketPayload> create(Identifier var1);
   }

   public record Type<T extends CustomPacketPayload>(Identifier id) {
   }

   public record TypeAndCodec<B extends FriendlyByteBuf, T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<B, T> codec) {
   }
}
