package net.minecraft.network.protocol;

import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jspecify.annotations.Nullable;

public interface BundlerInfo {
   int BUNDLE_SIZE_LIMIT = 4096;

   static <T extends PacketListener, P extends BundlePacket<? super T>> BundlerInfo createForPacket(
      final PacketType<P> bundlePacketType, final Function<Iterable<Packet<? super T>>, P> constructor, final BundleDelimiterPacket<? super T> delimiterPacket
   ) {
      return new BundlerInfo() {
         @Override
         public void unbundlePacket(Packet<?> packet, Consumer<Packet<?>> output) {
            if (packet.type() == bundlePacketType) {
               P bundlerPacket = (P)packet;
               output.accept(delimiterPacket);
               bundlerPacket.subPackets().forEach(output);
               output.accept(delimiterPacket);
            } else {
               output.accept(packet);
            }
         }

         @Override
         public void unbundlePacket(Packet<?> bundlePacket, Consumer<Packet<?>> packetSender, ChannelHandlerContext context) {
            if (bundlePacket.type() == bundlePacketType) {
               P p = (P)bundlePacket;
               List<Packet<?>> packets = NetworkRegistry.filterGameBundlePackets(context, p.subPackets());
               if (packets.isEmpty()) {
                  return;
               }

               if (packets.size() == 1) {
                  packetSender.accept(packets.get(0));
                  return;
               }

               packetSender.accept(delimiterPacket);
               packets.forEach(packetSender);
               packetSender.accept(delimiterPacket);
            } else {
               packetSender.accept(bundlePacket);
            }
         }

         @Nullable
         @Override
         public BundlerInfo.Bundler startPacketBundling(Packet<?> packet) {
            return packet == delimiterPacket ? new BundlerInfo.Bundler() {
               private final List<Packet<? super T>> bundlePackets;

               {
                  Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                  this.bundlePackets = new ArrayList<>();
               }

               @Nullable
               @Override
               public Packet<?> addPacket(Packet<?> packet) {
                  if (packet == delimiterPacket) {
                     return constructor.apply(this.bundlePackets);
                  } else if (this.bundlePackets.size() >= 4096) {
                     throw new IllegalStateException("Too many packets in a bundle");
                  } else {
                     this.bundlePackets.add((Packet<? super T>)packet);
                     return null;
                  }
               }
            } : null;
         }
      };
   }

   @Deprecated
   void unbundlePacket(Packet<?> var1, Consumer<Packet<?>> var2);

   default void unbundlePacket(Packet<?> bundlePacket, Consumer<Packet<?>> packetSender, ChannelHandlerContext context) {
      this.unbundlePacket(bundlePacket, packetSender);
   }

   @Nullable
   BundlerInfo.Bundler startPacketBundling(Packet<?> var1);

   public interface Bundler {
      @Nullable
      Packet<?> addPacket(Packet<?> var1);
   }
}
