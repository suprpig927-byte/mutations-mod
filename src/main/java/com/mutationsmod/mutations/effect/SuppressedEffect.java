package com.mutationsmod.mutations.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Suppressed (from Nullite): a mutant's powers are switched off. The logic lives in Suppression. */
public class SuppressedEffect extends MobEffect {
    public SuppressedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6CFF4A);
    }
}
