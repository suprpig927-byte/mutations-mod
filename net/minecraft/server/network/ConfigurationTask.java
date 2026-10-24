package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;

public interface ConfigurationTask {
   void start(Consumer<Packet<?>> var1);

   default boolean tick() {
      return false;
   }

   ConfigurationTask.Type type();

   public record Type(String id) {
      public Type(Identifier location) {
         this(location.toString());
      }

      @Override
      public String toString() {
         return this.id;
      }
   }
}
