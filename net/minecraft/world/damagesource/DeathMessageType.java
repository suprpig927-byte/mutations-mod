package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;
import net.neoforged.neoforge.common.damagesource.IDeathMessageProvider;

@NetworkedEnum(NetworkCheck.CLIENTBOUND)
@NamedEnum
public enum DeathMessageType implements StringRepresentable, IExtensibleEnum {
   DEFAULT("default"),
   FALL_VARIANTS("fall_variants"),
   INTENTIONAL_GAME_DESIGN("intentional_game_design");

   public static final Codec<DeathMessageType> CODEC = StringRepresentable.fromEnum(DeathMessageType::values);
   private final String id;
   private final IDeathMessageProvider msgFunction;

   @ReservedConstructor
   private DeathMessageType(String id) {
      this(id, IDeathMessageProvider.DEFAULT);
   }

   private DeathMessageType(String id, IDeathMessageProvider msgFunction) {
      this.id = id;
      this.msgFunction = msgFunction;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public IDeathMessageProvider getMessageFunction() {
      return this.msgFunction;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(DeathMessageType.class);
   }
}
