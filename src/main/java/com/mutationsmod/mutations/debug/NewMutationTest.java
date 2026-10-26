package com.mutationsmod.mutations.debug;

import com.mojang.authlib.GameProfile;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.power.Bullet;
import com.mutationsmod.mutations.ability.power.Wings;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Ops-only: exercises Bullet and Fierce Wings with fake players and logs the results. */
public final class NewMutationTest {
    private NewMutationTest() {}

    private record Check(String label, FakePlayer p, List<Zombie> zs, long at) {}
    private static final List<Check> CHECKS = new ArrayList<>();

    private static FakePlayer fake(ServerLevel level, String name, Mutation m, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        p.getInventory().clearContent();
        p.snapTo(x + 0.5, y, z + 0.5, 0, 0);
        ModAttachments.set(p, MutationData.EMPTY.withMutations(List.of(m.id())).withRolled(true).withMastery(40, 0, 0));
        return p;
    }

    private static List<Zombie> zombies(ServerLevel level, FakePlayer p, int n, double dist) {
        List<Zombie> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Zombie z = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            z.snapTo(p.getX() + (i - (n - 1) / 2.0) * 1.2, p.getY(), p.getZ() + dist, 180, 0);
            z.setNoAi(true);
            z.setPersistenceRequired();
            level.addFreshEntity(z);
            out.add(z);
        }
        return out;
    }

    public static void start(ServerLevel level, BlockPos o) {
        try {
            // Bullet: three iron rounds, then a scattershot, then fire until overheated.
            FakePlayer b = fake(level, "BulletTest", Mutation.BULLET, o.getX(), o.getZ());
            b.getInventory().add(new ItemStack(Items.IRON_NUGGET, 20));
            b.getInventory().add(new ItemStack(Items.REDSTONE, 4));
            b.getInventory().add(new ItemStack(Items.DIAMOND, 2));
            List<Zombie> bz = zombies(level, b, 1, 6);
            for (int i = 0; i < 3; i++) {
                Bullet.fire(b, ModAttachments.get(b));
            }
            var probe = level.getEntitiesOfClass(com.mutationsmod.mutations.entity.BulletRound.class, b.getBoundingBox().inflate(10));
            for (var r : probe) MutationsMod.LOGGER.info("NEWTEST round at {} vel {} owner={}", r.position(), r.getDeltaMovement(), r.getOwner());
            MutationsMod.LOGGER.info("NEWTEST shooter at {} look {} zombie at {}", b.position(), b.getLookAngle(), bz.get(0).position());
            Bullet.scattershot(b, ModAttachments.get(b));
            boolean switched = Bullet.switchAmmo(b, ModAttachments.get(b));
            MutationsMod.LOGGER.info("NEWTEST bullet switched={} now={} iron={} heat={}", switched, Bullet.selected(ModAttachments.get(b)),
                    Bullet.count(b, Bullet.Ammo.IRON), Bullet.heat(ModAttachments.get(b), level.getGameTime()));
            for (int i = 0; i < 8; i++) Bullet.fire(b, ModAttachments.get(b));
            MutationsMod.LOGGER.info("NEWTEST bullet overheated={} onFire={} heat={}", Bullet.overheated(ModAttachments.get(b), level.getGameTime()),
                    b.getRemainingFireTicks(), Bullet.heat(ModAttachments.get(b), level.getGameTime()));
            CHECKS.add(new Check("bullet", b, bz, level.getGameTime() + 40));

            // Fierce Wings: feather volley at a row of zombies, then a gust; check gliding is granted and chestplates come off.
            FakePlayer w = fake(level, "WingsTest", Mutation.FIERCE_WINGS, o.getX() + 12, o.getZ());
            w.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            Wings.tick(w, ModAttachments.get(w));
            MutationsMod.LOGGER.info("NEWTEST wings gliding={} chest={}", w.getAttributeValue(NeoForgeMod.GLIDING_FLIGHT),
                    w.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem());
            List<Zombie> wz = zombies(level, w, 3, 5);
            Wings.featherVolley(w, ModAttachments.get(w));
            CHECKS.add(new Check("wings-volley", w, wz, level.getGameTime() + 30));
            FakePlayer g = fake(level, "GustTest", Mutation.FIERCE_WINGS, o.getX() + 24, o.getZ());
            List<Zombie> gz = zombies(level, g, 1, 3);
            double before = gz.get(0).getZ();
            Wings.wingGust(g, ModAttachments.get(g));
            MutationsMod.LOGGER.info("NEWTEST wings gust zombieZ {} hp={}", before, gz.get(0).getHealth());
            CHECKS.add(new Check("wings-gust", g, gz, level.getGameTime() + 20));
        } catch (Throwable t) {
            MutationsMod.LOGGER.error("NEWTEST CRASHED", t);
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Check c : List.copyOf(CHECKS)) {
            if (now < c.at()) continue;
            StringBuilder sb = new StringBuilder();
            for (Zombie z : c.zs()) {
                sb.append(z.isAlive() ? String.format("%.1f@z%.1f", z.getHealth(), z.getZ()) : "dead").append(' ');
                z.discard();
            }
            MutationsMod.LOGGER.info("NEWTEST {} zombies=[{}]", c.label(), sb.toString().trim());
            CHECKS.remove(c);
        }
    }
}
