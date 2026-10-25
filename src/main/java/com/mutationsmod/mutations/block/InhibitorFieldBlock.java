package com.mutationsmod.mutations.block;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Suppression;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Inhibitor Field: a Nullite core that suppresses every mutant (and mutated mob) within 16 blocks. */
public class InhibitorFieldBlock extends Block implements EntityBlock {
    public static final int RADIUS = 16;

    public InhibitorFieldBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Field(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModContent.INHIBITOR_FIELD_ENTITY.get()) return null;
        return (lvl, pos, st, be) -> serverTick((ServerLevel) lvl, pos);
    }

    private static void serverTick(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if ((now + pos.hashCode()) % 20 != 0) return;
        AABB area = new AABB(pos).inflate(RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.distanceToSqr(pos.getCenter()) <= RADIUS * RADIUS)) {
            if (e instanceof ServerPlayer p) {
                if (!ModAttachments.get(p).mutations().isEmpty()) Suppression.apply(p, 40, false);
            } else if (!(e instanceof Player)) {
                e.addEffect(new MobEffectInstance(ModContent.SUPPRESSED, 40, 0, true, false, true));
            }
        }
        if (now % 40 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0);
        }
    }

    public static class Field extends BlockEntity {
        public Field(BlockPos pos, BlockState state) {
            super(ModContent.INHIBITOR_FIELD_ENTITY.get(), pos, state);
        }
    }
}
