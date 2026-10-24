package net.minecraft.commands;

import java.util.Map;
import net.minecraft.network.chat.PlayerChatMessage;
import org.jspecify.annotations.Nullable;

public interface CommandSigningContext {
   CommandSigningContext ANONYMOUS = new CommandSigningContext() {
      @Nullable
      @Override
      public PlayerChatMessage getArgument(String name) {
         return null;
      }
   };

   @Nullable
   PlayerChatMessage getArgument(String var1);

   public record SignedArguments(Map<String, PlayerChatMessage> arguments) implements CommandSigningContext {
      @Nullable
      @Override
      public PlayerChatMessage getArgument(String name) {
         return this.arguments.get(name);
      }
   }
}
