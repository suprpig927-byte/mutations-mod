package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Verdant: a plant mutant. Roots, thorns, fast growth; meat gives no food. */
public class VerdantPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.VERDANT; }

    /** Speeds up plants near {@code center} by giving a few of them extra random ticks. */
    public static void growNear(ServerLevel level, BlockPos center, int radius, int tries) {
        var r = level.getRandom();
        for (int i = 0; i < tries; i++) {
            BlockPos pos = center.offset(r.nextInt(radius * 2 + 1) - radius, r.nextInt(7) - 3, r.nextInt(radius * 2 + 1) - radius);
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof CropBlock || s.getBlock() instanceof SaplingBlock || s.getBlock() instanceof StemBlock
                    || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.CACTUS) || s.is(Blocks.BAMBOO)) {
                s.randomTick(level, pos, r);
            }
        }
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        if (now % 10 == 0) growNear(level, p.blockPosition(), 8, 6);
        BlockState under = level.getBlockState(p.blockPosition().below());
        boolean green = under.is(Blocks.GRASS_BLOCK) || under.is(Blocks.MOSS_BLOCK) || under.getBlock() instanceof LeavesBlock || under.is(Blocks.MOSS_CARPET);
        int healEvery = Catalysts.holding(p, Mutation.VERDANT) ? 30 : 60;
        if (green && now % healEvery == 0) p.heal(1f);
        boolean still = p.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
        Powers.add(p, Attributes.ARMOR, "barkskin", d.has(Secondary.BARKSKIN) && still ? 4 : 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "deep_roots", d.has(Secondary.DEEP_ROOTS) && p.isShiftKeyDown() ? 1 : 0);
        if (d.has(Secondary.SUN_FED) && level.dimension() == Level.OVERWORLD && !Powers.isNight(level) && level.canSeeSky(p.blockPosition().above())) {
            p.getFoodData().setExhaustion(0f);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (!(e.getSource().getEntity() instanceof LivingEntity att) || att == p) return;
        if (d.has(Secondary.POLLEN_CLOUD) && p.getRandom().nextFloat() < 0.3f) att.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
        if (d.has(Secondary.THORNMAIL) && e.getSource().getDirectEntity() == att) Powers.hurt(p, att, 2f);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case ROOT_SNARE -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                Kit.root(t, 80);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), t.getX(), t.getY() + 0.3, t.getZ(), 30, 0.4, 0.3, 0.4, 0.05);
                Powers.sound(p, SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.6f);
                return true;
            }
            case THORN_BURST -> {
                for (LivingEntity e : Kit.foes(p, 5)) {
                    Powers.hurt(p, e, 5f * Powers.power(d));
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), p.getX(), p.getY() + 0.2, p.getZ(), 80, 3, 0.2, 3, 0.1);
                Powers.sound(p, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.5f, 0.6f);
                return true;
            }
            case OVERGROWTH -> {
                wall(p, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 5, 3, 2, 600);
                Powers.sound(p, SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, 0.8f);
                return true;
            }
            case VINE_SWING -> {
                BlockHitResult hit = Kit.lookBlock(p, 20);
                if (hit.getType() != HitResult.Type.BLOCK) return false;
                Vec3 to = hit.getLocation().subtract(p.position());
                Powers.impulse(p, to.normalize().scale(Math.min(2.0, to.length() * 0.18 + 0.6)).add(0, 0.4, 0), true);
                Powers.line(level, p.getEyePosition(), hit.getLocation(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState()), 0.5);
                return true;
            }
            case HEALING_BLOOM -> {
                BlockPos at = p.blockPosition();
                if (level.getBlockState(at).isAir()) Powers.placeTemporary(level, at, Blocks.ALLIUM.defaultBlockState(), 200);
                Vec3 c = p.position();
                Zones.add(level, c, 5, 200, p.getUUID(), z -> {
                    if (z.age() % 20 != 0) return;
                    for (Player pl : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(5))) pl.heal(2f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.5, c.z, 15, 3, 0.5, 3, 0);
                }, null);
                return true;
            }
            default -> { return false; }
        }
    }

    /** A temporary wall of {@code state} in front of the player. */
    static void wall(ServerPlayer p, BlockState state, int width, int height, int dist, int ticks) {
        ServerLevel level = (ServerLevel) p.level();
        Direction f = p.getDirection();
        Direction side = f.getClockWise();
        BlockPos base = p.blockPosition().relative(f, dist);
        for (int w = -width / 2; w <= width / 2; w++) {
            BlockPos col = base.relative(side, w);
            for (int i = 0; i < 3 && level.getBlockState(col.below()).canBeReplaced(); i++) col = col.below();
            for (int h = 0; h < height; h++) Powers.placeTemporary(level, col.above(h), state, ticks);
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Kit.foes(p, 20)) {
            Kit.root(e, 120);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), e.getX(), e.getY() + 0.3, e.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        }
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
        var r = p.getRandom();
        BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.ALLIUM.defaultBlockState()};
        for (int i = 0; i < 120; i++) {
            int x = p.getBlockX() + r.nextInt(41) - 20, z = p.getBlockZ() + r.nextInt(41) - 20;
            BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
            BlockState s = level.getBlockState(top);
            if (s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)) level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            else if (s.is(Blocks.GRASS_BLOCK) && level.getBlockState(top.above()).isAir() && r.nextFloat() < 0.4f) {
                level.setBlock(top.above(), flowers[r.nextInt(flowers.length)], 3);
            }
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 200, 12, 1, 12, 0);
        Powers.sound(p, SoundEvents.ROOTED_DIRT_PLACE, 2f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        ServerLevel level = (ServerLevel) p.level();
        BlockHitResult hit = Kit.lookBlock(p, 6);
        BlockPos c = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : p.blockPosition().below();
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
            if (level.getBlockState(q).getBlock() instanceof BonemealableBlock && BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, q.immutable(), p)) n++;
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 30, 2, 0.5, 2, 0);
        p.getCooldowns().addCooldown(stack, 200);
        return n > 0 || true;
    }

    // ---------------------------------------------------------------- events

    /** Empty-handed right-click on a plant: bonemeal it. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.getMainHandItem().isEmpty() || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.VERDANT) || com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) return;
        if (Powers.timerActive(d, p.level(), "hand_bonemeal")) return;
        ServerLevel level = (ServerLevel) p.level();
        if (!(level.getBlockState(e.getPos()).getBlock() instanceof BonemealableBlock)) return;
        if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, e.getPos(), p)) {
            level.levelEvent(1505, e.getPos(), 15);
            Powers.setTimer(p, "hand_bonemeal", 100);
            e.setCanceled(true);
        }
    }

    private static final Map<UUID, int[]> FOOD_BEFORE = new HashMap<>();

    static boolean isMeat(ItemStack s) {
        return s.is(ItemTags.MEAT) || s.is(ItemTags.FISHES) || s.is(Items.RABBIT_STEW);
    }

    public static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof ServerPlayer p && isMeat(e.getItem())) {
            FOOD_BEFORE.put(p.getUUID(), new int[]{p.getFoodData().getFoodLevel(), Float.floatToIntBits(p.getFoodData().getSaturationLevel())});
        }
    }

    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        int[] before = FOOD_BEFORE.remove(p.getUUID());
        if (before != null && d.has(Mutation.VERDANT) && !Catalysts.holding(p, Mutation.VERDANT)
                && !com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) {
            p.getFoodData().setFoodLevel(before[0]);
            p.getFoodData().setSaturation(Float.intBitsToFloat(before[1]));
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Your plant body can't digest meat.").withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        // Swarm: Honey Body
        ItemStack s = e.getItem();
        if (d.has(Secondary.HONEY_BODY) && (s.is(Items.HONEY_BOTTLE) || s.is(Items.COOKIE) || s.is(Items.CAKE) || s.is(Items.PUMPKIN_PIE))) p.heal(4f);
    }

    /** Green Thumb: grown crops drop 50% more. */
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel level)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Secondary.GREEN_THUMB)) return;
        BlockState s = e.getState();
        if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
            for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(s, level, e.getPos(), null)) {
                if (p.getRandom().nextBoolean()) net.minecraft.world.level.block.Block.popResource(level, e.getPos(), drop);
            }
        }
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.ARMOR, "barkskin", 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "deep_roots", 0);
    }
}
