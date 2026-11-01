package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Suppression;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Registry and dispatch for the v0.19 mutations. */
public final class ExtraPowers {
    private ExtraPowers() {}

    private static final Map<Mutation, ExtraPower> POWERS = new EnumMap<>(Mutation.class);

    static {
        for (ExtraPower p : List.of(new ShadowPower(), new RadiationPower(), new LuminaryPower(), new MagnetarPower(), new VenomPower(),
                new VerdantPower(), new EarthshaperPower(), new BeastcallerPower(), new ChronoPower(), new SwarmPower(), new GravitonPower(),
                new TidecallerPower(), new CrystallinePower(), new ElasticPower(), new SoulbinderPower(), new MicroPower(),
                new TaxManPower(), new LandlordPower())) {
            POWERS.put(p.mutation(), p);
        }
    }

    public static ExtraPower of(Mutation m) {
        return POWERS.get(m);
    }

    /** This player's extra powers (only while their powers are on). */
    public static List<ExtraPower> active(ServerPlayer p, MutationData d) {
        List<ExtraPower> out = new ArrayList<>();
        if (Suppression.active(d, p.level())) return out;
        for (Mutation m : d.mutationList()) {
            ExtraPower x = POWERS.get(m);
            if (x != null) out.add(x);
        }
        return out;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        for (ExtraPower x : active(p, d)) x.tick(p, ModAttachments.get(p), now);
    }

    public static void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        for (ExtraPower x : active(p, d)) {
            if (e.isCanceled()) return;
            x.onHurt(e, p, ModAttachments.get(p));
        }
    }

    public static void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, LivingEntity target, boolean melee) {
        MutationData d = ModAttachments.get(p);
        for (ExtraPower x : active(p, d)) {
            x.onDealDamage(e, p, d, target);
            if (melee) x.onMelee(e, p, d, target);
        }
        if (melee) {
            var stack = e.getSource().getWeaponItem();
            if (stack != null && stack.getItem() instanceof com.mutationsmod.mutations.item.MutantWeapon w && d.has(w.mutation())
                    && !Suppression.active(d, p.level())) {
                ExtraPower x = POWERS.get(w.mutation());
                if (x != null) x.weaponHit(e, p, d, target);
            }
        }
    }

    public static void onKill(ServerPlayer p, LivingEntity victim) {
        MutationData d = ModAttachments.get(p);
        for (ExtraPower x : active(p, d)) x.onKill(p, ModAttachments.get(p), victim);
    }

    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(e.getEntity()) > 64 * 64) continue;
            MutationData d = ModAttachments.get(p);
            for (ExtraPower x : active(p, d)) x.onDrops(e, p, d);
        }
    }

    public static boolean cheatDeath(LivingDeathEvent e, ServerPlayer p, MutationData d) {
        for (ExtraPower x : active(p, d)) if (x.cheatDeath(e, p, d)) return true;
        return false;
    }

    public static void onDeath(ServerPlayer p, MutationData d) {
        for (Mutation m : d.mutationList()) {
            ExtraPower x = POWERS.get(m);
            if (x != null) x.onDeath(p, d);
        }
    }

    public static float cooldownFactor(ServerPlayer p, MutationData d, ActiveAbility a) {
        float f = 1f;
        for (ExtraPower x : active(p, d)) f *= x.cooldownFactor(p, d, a);
        return f;
    }

    public static String blocked(ServerPlayer p, MutationData d) {
        for (ExtraPower x : active(p, d)) {
            String why = x.blocked(p, d);
            if (why != null) return why;
        }
        return null;
    }

    /** Runs {@code a} if it belongs to one of the extra mutations. */
    public static boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        for (ExtraPower x : active(p, d)) {
            boolean owns = x.mutation().actives().contains(a);
            if (!owns) for (var s : x.mutation().secondaries()) if (s.active() == a && d.has(s)) owns = true;
            if (owns) return x.use(p, d, a);
        }
        return false;
    }

    public static boolean ultimate(ServerPlayer p, MutationData d, Mutation m) {
        ExtraPower x = POWERS.get(m);
        return x != null && x.ultimate(p, d);
    }

    public static boolean weaponUse(ServerPlayer p, MutationData d, Mutation m, net.minecraft.world.item.ItemStack stack) {
        ExtraPower x = POWERS.get(m);
        return x != null && x.weaponUse(p, d, stack);
    }

    public static void afterAbility(ServerPlayer p, MutationData d) {
        for (ExtraPower x : active(p, d)) x.afterAbility(p, ModAttachments.get(p));
    }

    public static boolean blocksEffect(ServerPlayer p, MutationData d, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        for (ExtraPower x : active(p, d)) if (x.blocksEffect(p, d, effect)) return true;
        return false;
    }

    /** Targeting rules: bribed mobs, minions, and players some mobs ignore. */
    public static void onChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent e) {
        Minions.onChangeTarget(e);
        if (e.isCanceled()) return;
        LivingEntity mob = e.getEntity();
        if (e.getNewAboutToBeSetTarget() != null && Marks.has(mob, "bribed")) {
            e.setCanceled(true);
            return;
        }
        if (e.getNewAboutToBeSetTarget() instanceof ServerPlayer p && mob instanceof net.minecraft.world.entity.Mob m) {
            MutationData d = ModAttachments.get(p);
            for (ExtraPower x : active(p, d)) {
                if (x.ignoredBy(p, d, m)) {
                    e.setCanceled(true);
                    return;
                }
            }
        }
    }

    public static void reset(ServerPlayer p) {
        for (ExtraPower x : POWERS.values()) x.reset(p);
        Minions.forget(p);
    }

    public static void tickLevel(ServerLevel level) {
        Zones.tick(level);
        Minions.tick(level);
        if (level.getGameTime() % 200 == 0) Marks.sweep(level.getGameTime());
        RadiationPower.tickSickness(level);
        ChronoPower.tickFrozen(level);
    }
}
