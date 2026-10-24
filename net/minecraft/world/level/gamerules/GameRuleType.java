package net.minecraft.world.level.gamerules;

import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;

@NamedEnum
public enum GameRuleType implements StringRepresentable, IExtensibleEnum {
   INT("integer"),
   BOOL("boolean");

   private final String name;

   private GameRuleType(String name) {
      this.name = name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(GameRuleType.class);
   }
}
