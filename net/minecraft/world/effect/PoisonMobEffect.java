package net.minecraft.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForgeMod;

public class PoisonMobEffect extends MobEffect {
   public static final int DAMAGE_INTERVAL = 25;

   protected PoisonMobEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   @Override
   public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
      if (mob.getHealth() > 1.0F) {
         Registry<DamageType> dTypeReg = mob.damageSources().damageTypes;
         Holder.Reference<DamageType> dType = dTypeReg.get(NeoForgeMod.POISON_DAMAGE).orElse(dTypeReg.getOrThrow(DamageTypes.MAGIC));
         mob.hurtServer(level, new DamageSource(dType), 1.0F);
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
      int interval = 25 >> amplification;
      return interval > 0 ? tickCount % interval == 0 : true;
   }
}
