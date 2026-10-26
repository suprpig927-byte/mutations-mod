package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.BulletRound;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Bullet: the body turns ore into ammunition. Each material is a different round. Every shot builds heat;
 * at 100 you overheat: you catch fire and can't shoot for 5 seconds.
 */
public final class Bullet {
    private Bullet() {}

    public enum Ammo {
        IRON("Iron", Items.IRON_NUGGET, 6f, 3.2f, 0xC8C8D0),
        COAL("Incendiary", Items.COAL, 4f, 3.0f, 0x3A3A3A),
        COPPER("Shock", Items.COPPER_INGOT, 5f, 3.2f, 0xE0804A),
        GOLD("Gold", Items.GOLD_NUGGET, 4f, 4.6f, 0xFFD84A),
        REDSTONE("Blast", Items.REDSTONE, 4f, 2.6f, 0xD02020),
        LAPIS("Heavy", Items.LAPIS_LAZULI, 7f, 2.6f, 0x2A4AC0),
        DIAMOND("Piercing", Items.DIAMOND, 10f, 3.8f, 0x6AE8E0),
        NETHERITE("Netherite", Items.NETHERITE_SCRAP, 16f, 3.4f, 0x5A4448);

        public final String label;
        public final Item item;
        public final float damage;
        public final float speed;
        public final int color;

        Ammo(String label, Item item, float damage, float speed, int color) {
            this.label = label;
            this.item = item;
            this.damage = damage;
            this.speed = speed;
            this.color = color;
        }

        public static Ammo byId(long i) {
            return values()[(int) Math.floorMod(i, values().length)];
        }
    }

    public static final int MAX_HEAT = 100;

    // ------------------------------------------------------------------ heat

    /** Current heat (it cools by 1 per tick since the last shot). */
    public static float heat(MutationData d, long now) {
        return Math.max(0f, d.timer("heat") - (now - d.timer("heat_t")) * coolRate(d));
    }

    private static float coolRate(MutationData d) {
        return 1.0f;
    }

    public static float heatCap(MutationData d, Player p) {
        return com.mutationsmod.mutations.item.Catalysts.holding(p, Mutation.BULLET) ? MAX_HEAT * 2 : MAX_HEAT;   // Heat Sink
    }

    public static boolean overheated(MutationData d, long now) {
        return d.timer("overheat") > now;
    }

    private static void addHeat(ServerPlayer p, float amount) {
        MutationData d = ModAttachments.get(p);
        long now = p.level().getGameTime();
        if (d.has(Secondary.COOLANT)) amount *= 0.6f;
        float h = heat(d, now) + amount;
        if (h >= heatCap(d, p)) {
            ModAttachments.set(p, d.withTimer("heat", 0).withTimer("heat_t", now).withTimer("overheat", now + 100));
            p.igniteForSeconds(3);
            Powers.hurtSelf(p, 2f);
            Powers.particles(p, ParticleTypes.LARGE_SMOKE, 25, 0.4, 0.05);
            Powers.sound(p, SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.6f);
            p.sendOverlayMessage(Component.literal("OVERHEATED!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        } else {
            ModAttachments.set(p, d.withTimer("heat", (long) Math.ceil(h)).withTimer("heat_t", now));
        }
    }

    // ------------------------------------------------------------------ ammo

    public static Ammo selected(MutationData d) {
        return Ammo.byId(d.timer("ammo"));
    }

    public static int count(Player p, Ammo a) {
        if (p.isCreative()) return 999;
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(a.item)) n += s.getCount();
        }
        return n;
    }

    /** The selected ammo, or the next kind the player actually has. Null if they have none at all. */
    private static Ammo available(ServerPlayer p, MutationData d) {
        Ammo cur = selected(d);
        for (int k = 0; k < Ammo.values().length; k++) {
            Ammo a = Ammo.byId(cur.ordinal() + k);
            if (count(p, a) > 0) {
                if (a != cur) ModAttachments.set(p, ModAttachments.get(p).withTimer("ammo", a.ordinal()));
                return a;
            }
        }
        return null;
    }

    private static void consume(ServerPlayer p, MutationData d, Ammo a) {
        if (p.isCreative()) return;
        if (d.has(Secondary.RECYCLER) && p.getRandom().nextInt(3) == 0) return;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(a.item)) {
                s.shrink(1);
                return;
            }
        }
    }

    private static boolean ready(ServerPlayer p, MutationData d) {
        if (overheated(d, p.level().getGameTime())) {
            p.sendOverlayMessage(Component.literal("Too hot to fire! Cooling down...").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static Ammo ammoOrComplain(ServerPlayer p, MutationData d) {
        Ammo a = available(p, d);
        if (a == null) {
            p.sendOverlayMessage(Component.literal("Out of ammo: carry iron nuggets, coal, copper, gold nuggets, redstone, lapis, diamonds or netherite scrap.")
                    .withStyle(ChatFormatting.GRAY));
        }
        return a;
    }

    // ------------------------------------------------------------------ firing

    /** Spawns one round from the player's eyes. */
    public static BulletRound shoot(ServerPlayer p, MutationData d, Ammo a, float spread, float damageMult) {
        ServerLevel level = (ServerLevel) p.level();
        BulletRound round = new BulletRound(level, p, a);
        float dmg = a.damage * Powers.power(d) * damageMult;
        if (d.has(Secondary.HOLLOW_POINTS)) dmg *= 1.25f;
        if (d.has(Secondary.STEADY_AIM) && p.isShiftKeyDown()) dmg *= 1.5f;
        round.setup(dmg, d.has(Secondary.RICOCHET), d.has(Secondary.TRACER_ROUNDS), d.has(Secondary.SCRAP_COLLECTOR));
        round.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, a.speed, spread);
        level.addFreshEntity(round);
        Vec3 muzzle = p.getEyePosition().add(p.getLookAngle().scale(0.8)).subtract(0, 0.2, 0);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.01);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.02, 0.02, 0.02, 0.01);
        return round;
    }

    public static boolean fire(ServerPlayer p, MutationData d) {
        if (!ready(p, d)) return false;
        Ammo a = ammoOrComplain(p, d);
        if (a == null) return false;
        d = ModAttachments.get(p);
        shoot(p, d, a, d.has(Secondary.STEADY_AIM) && p.isShiftKeyDown() ? 0f : 0.6f, 1f);
        consume(p, d, a);
        addHeat(p, 12);
        Powers.sound(p, a == Ammo.NETHERITE || a == Ammo.LAPIS ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.6f);
        return true;
    }

    public static boolean scattershot(ServerPlayer p, MutationData d) {
        if (!ready(p, d)) return false;
        Ammo a = ammoOrComplain(p, d);
        if (a == null) return false;
        d = ModAttachments.get(p);
        float spread = d.has(Secondary.STEADY_AIM) && p.isShiftKeyDown() ? 4f : 8f;
        for (int i = 0; i < 5; i++) shoot(p, d, a, spread, 0.7f);
        consume(p, d, a);
        addHeat(p, 35);
        Powers.impulse(p, p.getLookAngle().scale(-0.4), false);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.4f);
        return true;
    }

    public static boolean overpressure(ServerPlayer p, MutationData d) {
        if (!ready(p, d)) return false;
        Ammo a = ammoOrComplain(p, d);
        if (a == null) return false;
        d = ModAttachments.get(p);
        BulletRound r = shoot(p, d, a, 0f, 3f);
        r.setHeavy(true);
        consume(p, d, a);
        addHeat(p, 40);
        Powers.impulse(p, p.getLookAngle().scale(-1.1).add(0, 0.2, 0), false);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.8f);
        return true;
    }

    public static boolean switchAmmo(ServerPlayer p, MutationData d) {
        Ammo cur = selected(d);
        for (int k = 1; k <= Ammo.values().length; k++) {
            Ammo a = Ammo.byId(cur.ordinal() + k);
            if (count(p, a) > 0) {
                ModAttachments.set(p, d.withTimer("ammo", a.ordinal()));
                p.sendOverlayMessage(Component.literal("Ammo: " + a.label + " (" + count(p, a) + ")").withStyle(s -> s.withColor(a.color)));
                Powers.sound(p, SoundEvents.CROSSBOW_LOADING_END.value(), 0.6f, 1.4f);
                return true;
            }
        }
        p.sendOverlayMessage(Component.literal("No other ammo to switch to.").withStyle(ChatFormatting.GRAY));
        return false;
    }

    /** Bullet Storm: a free round of the selected ammo (or iron) every other tick. */
    public static void stormShot(ServerPlayer p, MutationData d) {
        Ammo a = selected(d);
        shoot(p, d, a, 2.5f, 0.8f);
        if (p.level().getGameTime() % 4 == 0) Powers.sound(p, SoundEvents.CROSSBOW_SHOOT, 0.5f, 1.8f);
    }

    /** Gunblade right-click: a round of your ammo that builds no heat. */
    public static boolean gunbladeShot(ServerPlayer p, MutationData d) {
        Ammo a = ammoOrComplain(p, d);
        if (a == null) return false;
        d = ModAttachments.get(p);
        shoot(p, d, a, 0.2f, 1f);
        consume(p, d, a);
        Powers.sound(p, SoundEvents.CROSSBOW_SHOOT, 0.7f, 1.2f);
        return true;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        if (overheated(d, now) && now % 4 == 0) {
            Powers.particles(p, ParticleTypes.SMOKE, 3, 0.3, 0.02);
        } else if (heat(d, now) > heatCap(d, p) * 0.7f && now % 6 == 0) {
            Powers.particles(p, ParticleTypes.WHITE_SMOKE, 2, 0.3, 0.02);
        }
    }
}
