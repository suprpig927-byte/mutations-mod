package net.minecraft.world.inventory;

import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;

@NetworkedEnum(NetworkCheck.CLIENTBOUND)
public enum RecipeBookType implements IExtensibleEnum {
   CRAFTING,
   FURNACE,
   BLAST_FURNACE,
   SMOKER;

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(RecipeBookType.class);
   }
}
