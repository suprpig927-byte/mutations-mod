package com.mutationsmod.mutations.mastery;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Stat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Turns spent stat points into attribute bonuses. The Mutation stat is read directly by abilities. */
public final class StatEffects {
    private StatEffects() {}

    public static void apply(ServerPlayer p, MutationData d) {
        Powers.add(p, Attributes.ATTACK_DAMAGE, "stat_strength", 0.5 * d.stat(Stat.STRENGTH));
        Powers.add(p, Attributes.MAX_HEALTH, "stat_vitality", 1.0 * d.stat(Stat.VITALITY));
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "stat_agility", 0.01 * d.stat(Stat.AGILITY));
        Powers.mult(p, Attributes.ATTACK_SPEED, "stat_agility", 0.01 * d.stat(Stat.AGILITY));
        Powers.add(p, Attributes.ARMOR, "stat_endurance", 0.5 * d.stat(Stat.ENDURANCE));
        Powers.add(p, Attributes.LUCK, "stat_luck", 0.1 * d.stat(Stat.LUCK));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
