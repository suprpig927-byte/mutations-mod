package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;

@NetworkedEnum(NetworkCheck.CLIENTBOUND)
@NamedEnum
public enum DamageScaling implements StringRepresentable, IExtensibleEnum {
   NEVER("never"),
   WHEN_CAUSED_BY_LIVING_NON_PLAYER("when_caused_by_living_non_player"),
   ALWAYS("always");

   public static final Codec<DamageScaling> CODEC = StringRepresentable.fromEnum(DamageScaling::values);
   private final String id;
   private final IScalingFunction scaling;

   @ReservedConstructor
   private DamageScaling(String id) {
      this(id, IScalingFunction.DEFAULT);
   }

   private DamageScaling(String id, IScalingFunction scaling) {
      this.id = id;
      this.scaling = scaling;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public IScalingFunction getScalingFunction() {
      return this.scaling;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(DamageScaling.class);
   }
}
