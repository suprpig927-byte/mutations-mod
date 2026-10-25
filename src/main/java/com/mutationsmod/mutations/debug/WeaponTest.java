package com.mutationsmod.mutations.debug;

import com.mojang.authlib.GameProfile;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.Weapons;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.EchoBowItem;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModWeapons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Ops-only: every mutation weapon swung (and right-clicked) by a fake player at a zombie, plus the hold-lock check. */
public final class WeaponTest {
    private WeaponTest() {}

    private record Run(Mutation m, FakePlayer p, Zombie z, long start) {}
    private static final List<Run> RUNS = new ArrayList<>();

    public static int start(ServerLevel level, BlockPos origin) {
        int n = 0;
        for (Mutation m : Mutation.values()) {
            int x = origin.getX() + n * 8, z = origin.getZ();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("weapon-" + m).getBytes()), "W" + m.ordinal()));
            p.snapTo(x + 0.5, y, z + 0.5, 0, 0);                     // facing south (+z)
            MutationData d = MutationData.EMPTY.withMutations(List.of(m.id())).withRolled(true).withMastery(40, 0, 0);
            if (m == Mutation.ABSORBER) d = d.withCharge(30);
            if (m == Mutation.COLOSSUS) d = d.withTimer("steel", 1);
            ModAttachments.set(p, d);
            ItemStack weapon = new ItemStack(ModWeapons.BY_MUTATION.get(m).get());
            p.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            Zombie zb = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            zb.snapTo(x + 0.5, y, z + 2.5, 180, 0);
            zb.setPersistenceRequired();
            zb.setNoAi(true);
            level.addFreshEntity(zb);
            try {
                if (m == Mutation.SIREN) {
                    Arrow arrow = new Arrow(level, p, new ItemStack(Items.ARROW), weapon);
                    arrow.addTag(EchoBowItem.TAG);
                    arrow.shoot(0, 0.1, 1, 1.5f, 0);
                    level.addFreshEntity(arrow);
                } else {
                    float before = zb.getHealth();
                    zb.hurtServer(level, p.damageSources().playerAttack(p), 6f);   // a melee hit with the weapon
                    MutationsMod.LOGGER.info("WEAPONTEST {} melee {} -> {}", m, before, zb.getHealth());
                    var r = weapon.getItem().use(level, p, InteractionHand.MAIN_HAND);
                    MutationsMod.LOGGER.info("WEAPONTEST {} use -> {} hand={}", m, r, p.getMainHandItem().getItem());
                }
            } catch (Throwable t) {
                MutationsMod.LOGGER.error("WEAPONTEST {} CRASHED", m, t);
            }
            RUNS.add(new Run(m, p, zb, level.getGameTime()));
            n++;
        }
        // Hold-lock: a Cryo holding the Thunderhead Maul loses it from their hand.
        FakePlayer wrong = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("weapon-wrong".getBytes()), "Wrong"));
        ModAttachments.set(wrong, MutationData.EMPTY.withMutations(List.of(Mutation.CRYO.id())).withRolled(true));
        wrong.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModWeapons.THUNDERHEAD_MAUL.get()));
        Weapons.tick(wrong, ModAttachments.get(wrong));
        MutationsMod.LOGGER.info("WEAPONTEST holdlock mainhand={} inventorySlot9={}", wrong.getMainHandItem().getItem(), wrong.getInventory().getItem(9).getItem());
        return n;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Run r : List.copyOf(RUNS)) {
            long age = now - r.start();
            if (age == 60) {
                MutationsMod.LOGGER.info("WEAPONTEST {} t=60 zombie={} effects={} fire={} player={},{},{} inv={}", r.m(),
                        r.z().isAlive() ? r.z().getHealth() : "dead", r.z().getActiveEffects().stream().map(e -> e.getEffect().getRegisteredName()).toList(),
                        r.z().getRemainingFireTicks(), (int) r.p().getX(), (int) r.p().getY(), (int) r.p().getZ(),
                        r.p().getInventory().contains(new ItemStack(ModWeapons.BY_MUTATION.get(r.m()).get())));
                r.z().discard();
                RUNS.remove(r);
            }
        }
    }
}
