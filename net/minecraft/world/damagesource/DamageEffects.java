package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;

@NetworkedEnum(NetworkCheck.CLIENTBOUND)
@NamedEnum
public enum DamageEffects implements StringRepresentable, IExtensibleEnum {
   HURT("hurt", SoundEvents.PLAYER_HURT),
   THORNS("thorns", SoundEvents.PLAYER_HURT),
   DROWNING("drowning", SoundEvents.PLAYER_HURT_DROWN),
   BURNING("burning", SoundEvents.PLAYER_HURT_ON_FIRE),
   POKING("poking", SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH),
   FREEZING("freezing", SoundEvents.PLAYER_HURT_FREEZE);

   public static final Codec<DamageEffects> CODEC = StringRepresentable.fromEnum(DamageEffects::values);
   private final String id;
   @Deprecated
   private final SoundEvent sound;
   private final Supplier<SoundEvent> soundSupplier;

   @ReservedConstructor
   private DamageEffects(String id, SoundEvent sound) {
      this(id, () -> sound);
   }

   private DamageEffects(String id, Supplier<SoundEvent> sound) {
      this.id = id;
      this.soundSupplier = sound;
      this.sound = null;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public SoundEvent sound() {
      return this.soundSupplier.get();
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(DamageEffects.class);
   }
}
