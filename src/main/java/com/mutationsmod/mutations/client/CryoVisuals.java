package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

/** Client-side Cryo touches: whether a player is in the heat, and meltwater drips. */
public final class CryoVisuals {
    private CryoVisuals() {}

    /** Same rule as the server's hot-biome weakness. */
    public static boolean hot(Player p) {
        if (com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.CRYO)) return false;
        if (Powers.isNether(p.level())) return true;
        return Powers.temperature(p) > 1.0f && !Powers.isNight(p.level());
    }

    /** Meltwater dripping off the crystals of every Cryo player in the heat. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.level.getGameTime() % 6 != 0) return;
        for (Player p : mc.level.players()) {
            if (!p.getData(ModAttachments.DATA).has(Mutation.CRYO) || !hot(p) || p.isInvisible()) continue;
            double side = p.getRandom().nextBoolean() ? 0.4 : -0.4;
            double yaw = Math.toRadians(p.yBodyRot);
            double x = p.getX() + Math.cos(yaw) * side;
            double z = p.getZ() + Math.sin(yaw) * side;
            mc.level.addParticle(ParticleTypes.DRIPPING_DRIPSTONE_WATER, x, p.getY() + 1.45, z, 0, 0, 0);
        }
    }
}
