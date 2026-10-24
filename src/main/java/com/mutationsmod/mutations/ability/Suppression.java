package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

/**
 * Nullite suppression: while it lasts, a mutant's powers are completely off (no actives, no passives,
 * no mutation looks). Stored as the synced timer "suppressed" so every client can see it, plus a
 * Suppressed status effect so the player sees an icon and countdown.
 */
public final class Suppression {
    private Suppression() {}

    /** Hit durations (ticks). */
    public static final int JAB = 5 * 20, CHARGE = 15 * 20;

    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static boolean active(MutationData d, Level level) {
        return d.timer("suppressed") > level.getGameTime();
    }

    /** Suppresses for at least {@code ticks} from now (never shortens a longer suppression). */
    public static void apply(ServerPlayer p, int ticks, boolean announce) {
        MutationData d = ModAttachments.get(p);
        long until = p.level().getGameTime() + ticks;
        if (d.timer("suppressed") < until) Powers.setTimer(p, "suppressed", ticks);
        MobEffectInstance cur = p.getEffect(ModContent.SUPPRESSED);
        if (cur == null || cur.getDuration() < ticks) {
            p.addEffect(new MobEffectInstance(ModContent.SUPPRESSED, ticks, 0, false, true, true));
        }
        if (announce && !d.mutations().isEmpty()) {
            Powers.particles(p, ParticleTypes.HAPPY_VILLAGER, 12, 0.4, 0.05);
            Powers.sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);
        }
    }

    /**
     * Called every player tick. Handles holding a Nullite Spear (dampens the holder), and turns
     * powers off/on at the start/end of a suppression. Returns true while suppressed.
     */
    public static boolean tick(ServerPlayer p) {
        if (p.getMainHandItem().is(ModContent.NULLITE_SPEAR.get()) || p.getOffhandItem().is(ModContent.NULLITE_SPEAR.get())) {
            if (p.level().getGameTime() % 10 == 0) apply(p, 40, false);
        }
        MutationData d = ModAttachments.get(p);
        boolean on = active(d, p.level());
        if (on && ACTIVE.add(p.getUUID())) {
            AbilityHandler.resetState(p);
            if (!d.mutations().isEmpty()) {
                p.sendOverlayMessage(Component.literal("Nullite: your powers are suppressed!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            }
        } else if (!on && ACTIVE.remove(p.getUUID())) {
            if (!d.mutations().isEmpty()) {
                p.sendOverlayMessage(Component.literal("Your powers return.").withStyle(ChatFormatting.AQUA));
            }
        }
        return on;
    }

    public static void forget(UUID id) {
        ACTIVE.remove(id);
    }
}
