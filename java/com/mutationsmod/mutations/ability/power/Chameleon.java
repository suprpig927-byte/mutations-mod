package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Chameleon {
    private Chameleon() {}

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Map<UUID, Long> LAST_HIDDEN = new HashMap<>();
    private static final Map<UUID, String> LAST_KILL = new HashMap<>();
    /** Mobs hit by an ambush recently: mob uuid -> time (for Silent Kill). */
    private static final Map<UUID, Long> AMBUSHED = new HashMap<>();

    /** Copycat traits and the mobs they come from. */
    public enum Trait {
        CLIMB("Spider: wall climbing", Set.of("spider", "cave_spider")),
        FIRE("Blaze: fire immunity", Set.of("blaze", "magma_cube", "strider", "ghast", "wither_skeleton")),
        BLAST("Creeper: your next hit explodes", Set.of("creeper")),
        BLINK("Enderman: blink away when hit", Set.of("enderman", "endermite", "shulker")),
        UNDYING("Zombie: regeneration", Set.of("zombie", "husk", "drowned", "zombie_villager", "zombified_piglin")),
        JUMP("Rabbit: huge jumps", Set.of("rabbit", "goat", "frog", "slime")),
        FEATHER("Chicken: slow falling", Set.of("chicken", "parrot", "bat", "phantom", "bee", "allay")),
        GILLS("Fish: water breathing and swimming", Set.of("squid", "glow_squid", "dolphin", "cod", "salmon", "tropical_fish",
                "pufferfish", "guardian", "elder_guardian", "axolotl", "turtle")),
        SWIFT("Wolf: speed", Set.of("wolf", "fox", "cat", "ocelot", "horse", "donkey", "camel")),
        BRUTE("Golem: strength and resistance", Set.of("iron_golem", "ravager", "hoglin", "zoglin", "polar_bear", "warden")),
        HUNTER("Strength", Set.of());

        public final String label;
        final Set<String> mobs;

        Trait(String label, Set<String> mobs) {
            this.label = label;
            this.mobs = mobs;
        }

        public String timer() {
            return "copy_" + name().toLowerCase(java.util.Locale.ROOT);
        }

        static Trait of(String mobPath) {
            for (Trait t : values()) if (t.mobs.contains(mobPath)) return t;
            return HUNTER;
        }
    }

    public static boolean camouflaged(ServerPlayer p, MutationData d) {
        Long t = LAST_HIDDEN.get(p.getUUID());
        return t != null && p.level().getGameTime() - t <= 1;
    }

    private static boolean wearsArmor(ServerPlayer p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!p.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Hidden from mobs: camouflaged and not betrayed by armor. */
    public static boolean hiddenFromMobs(ServerPlayer p, MutationData d) {
        return (camouflaged(p, d) || Powers.timerActive(d, p.level(), "smoke"))
                && (!wearsArmor(p) || d.has(Secondary.MIRROR_SKIN));
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        Vec3 last = LAST_POS.put(p.getUUID(), p.position());
        boolean still = last != null && last.distanceToSqr(p.position()) < 0.0004;
        boolean camo = p.isShiftKeyDown() && (still || d.has(Secondary.CAMOUFLAGE));
        if (camo) {
            LAST_HIDDEN.put(p.getUUID(), now);
            if (now % 5 == 0) Powers.effect(p, MobEffects.INVISIBILITY, 15, 0);
            if (d.has(Secondary.SHADOW_STEP) && now % 10 == 0) Powers.effect(p, MobEffects.SPEED, 20, 2);
        }
        if (now % 10 == 0 && (hiddenFromMobs(p, d) || Powers.timerActive(d, p.level(), "mimic"))) {
            for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) {
                if (mob.distanceTo(p) > 2 || Powers.timerActive(d, p.level(), "mimic")) mob.setTarget(null);
            }
        }
        // Copycat traits that are status effects.
        if (now % 20 == 0) {
            if (Powers.timerActive(d, p.level(), Trait.FIRE.timer())) Powers.effect(p, MobEffects.FIRE_RESISTANCE, 40, 0);
            if (Powers.timerActive(d, p.level(), Trait.UNDYING.timer())) Powers.effect(p, MobEffects.REGENERATION, 40, 0);
            if (Powers.timerActive(d, p.level(), Trait.JUMP.timer())) Powers.effect(p, MobEffects.JUMP_BOOST, 40, 2);
            if (Powers.timerActive(d, p.level(), Trait.FEATHER.timer())) Powers.effect(p, MobEffects.SLOW_FALLING, 40, 0);
            if (Powers.timerActive(d, p.level(), Trait.GILLS.timer())) {
                Powers.effect(p, MobEffects.WATER_BREATHING, 40, 0);
                Powers.effect(p, MobEffects.DOLPHINS_GRACE, 40, 0);
            }
            if (Powers.timerActive(d, p.level(), Trait.SWIFT.timer())) Powers.effect(p, MobEffects.SPEED, 40, 1);
            if (Powers.timerActive(d, p.level(), Trait.BRUTE.timer())) {
                Powers.effect(p, MobEffects.STRENGTH, 40, 0);
                Powers.effect(p, MobEffects.RESISTANCE, 40, 0);
            }
            if (Powers.timerActive(d, p.level(), Trait.HUNTER.timer())) Powers.effect(p, MobEffects.STRENGTH, 40, 0);
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (Powers.timerActive(d, p.level(), Trait.BLINK.timer()) && p.getRandom().nextFloat() < 0.5f) {
            double angle = p.getRandom().nextDouble() * Math.PI * 2;
            Vec3 spot = Powers.safeSpot(p, p.position().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6));
            if (spot != null) {
                Powers.particles(p, ParticleTypes.PORTAL, 20, 0.4, 0.3);
                Powers.teleport(p, spot);
                event.setCanceled(true);
            }
        }
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        long now = p.level().getGameTime();
        Long hidden = LAST_HIDDEN.get(p.getUUID());
        boolean fromHiding = (hidden != null && now - hidden <= 10) || Powers.timerActive(d, p.level(), "smoke");
        if (fromHiding) {
            event.setAmount(event.getAmount() * 2.5f);
            AMBUSHED.put(target.getUUID(), now);
            LAST_HIDDEN.remove(p.getUUID());
            Powers.clearTimer(p, "smoke");
            p.removeEffect(MobEffects.INVISIBILITY);
            Powers.particles(target, ParticleTypes.CRIT, 20, 0.4, 0.3);
            p.sendOverlayMessage(Component.literal("Ambush!").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        }
        if (d.has(Secondary.BACKSTAB)) {
            Vec3 facing = target.getLookAngle().multiply(1, 0, 1);
            Vec3 toAttacker = p.position().subtract(target.position()).multiply(1, 0, 1);
            if (facing.lengthSqr() > 0 && toAttacker.lengthSqr() > 0 && facing.normalize().dot(toAttacker.normalize()) < -0.3) {
                event.setAmount(event.getAmount() * 1.5f);
            }
        }
        if (Powers.timerActive(d, p.level(), Trait.BLAST.timer())) {
            Powers.clearTimer(p, Trait.BLAST.timer());
            for (LivingEntity e : Powers.around(p, 3.5)) {
                Powers.hurt(p, e, 6f * Powers.power(d));
                Powers.knockAway(e, target.position(), 1.2, 0.5);
            }
            Powers.particles(target, ParticleTypes.EXPLOSION, 3, 0.8, 0);
            Powers.sound(target, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.2f);
        }
    }

    /** Called when a Chameleon kills something: remembered for Copycat. */
    public static void onKill(ServerPlayer p, LivingEntity victim) {
        LAST_KILL.put(p.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath());
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.CHAMELEON)) return;
        LivingEntity mob = event.getEntity();
        if (Powers.timerActive(d, p.level(), "mimic") && Powers.isHostile(mob)) {
            event.setCanceled(true);
        } else if (hiddenFromMobs(p, d) && mob.distanceTo(p) > 2) {
            event.setCanceled(true);
        } else if (d.has(Secondary.SCENTLESS) && mob.getType().getCategory() != MobCategory.MONSTER) {
            event.setCanceled(true);
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        Long t = AMBUSHED.remove(victim.getUUID());
        if (t == null || victim.level().getGameTime() - t > 60) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Secondary.SILENT_KILL)) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            extra.add(new ItemEntity(victim.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
        event.getDrops().addAll(extra);
    }

    // ------------------------------------------------------------------ actives

    public static boolean mimic(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "mimic", (long) (8 * 20 * Powers.power(d)));
        for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) {
            mob.setTarget(null);
        }
        Powers.particles(p, ParticleTypes.HAPPY_VILLAGER, 15, 0.5, 0.05);
        Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f, 1.2f);
        return true;
    }

    public static boolean copycat(ServerPlayer p, MutationData d) {
        String last = LAST_KILL.get(p.getUUID());
        if (last == null) {
            p.sendOverlayMessage(Component.literal("Kill a mob first to copy it.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Trait trait = Trait.of(last);
        long duration = (long) (60 * 20 * Powers.power(d)) * (d.has(Secondary.COPYCAT_MASTERY) ? 2 : 1);
        for (Trait t : Trait.values()) Powers.clearTimer(p, t.timer());
        Powers.setTimer(p, trait.timer(), duration);
        p.sendOverlayMessage(Component.literal("Copycat: " + trait.label).withStyle(ChatFormatting.GREEN));
        Powers.particles(p, ParticleTypes.HAPPY_VILLAGER, 20, 0.5, 0.1);
        return true;
    }

    public static boolean smokeBurst(ServerPlayer p, MutationData d) {
        for (LivingEntity e : Powers.around(p, 8)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            if (e instanceof Mob mob && mob.getTarget() == p) mob.setTarget(null);
        }
        Powers.effect(p, MobEffects.INVISIBILITY, 100, 0);
        Powers.setTimer(p, "smoke", 100);
        Powers.particles(p, ParticleTypes.LARGE_SMOKE, 80, 1.5, 0.05);
        Powers.sound(p, SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.6f);
        return true;
    }

    public static void forget(UUID id) {
        LAST_POS.remove(id);
        LAST_HIDDEN.remove(id);
    }
}
