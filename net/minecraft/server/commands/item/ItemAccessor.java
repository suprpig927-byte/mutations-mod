package net.minecraft.server.commands.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.CommandResponseTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import org.jspecify.annotations.Nullable;

public interface ItemAccessor<Target> {
   void setItems(CommandSourceStack var1, SlotSource var2, ItemAccessor.SetterFunction<Target> var3) throws CommandSyntaxException;

   SlotCollection getSlots(CommandSourceStack var1, SlotSource var2) throws CommandSyntaxException;

   int getReplaceSuccess(CommandSourceStack var1, CommandResponseTracker<Target> var2, @Nullable ItemStack var3) throws CommandSyntaxException;

   int getModifySuccess(CommandSourceStack var1, CommandResponseTracker<Target> var2) throws CommandSyntaxException;

   @FunctionalInterface
   public interface SetterFunction<Target> {
      int apply(Target var1, SlotCollection var2);
   }
}
