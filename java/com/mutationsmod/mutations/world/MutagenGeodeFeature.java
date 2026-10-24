package com.mutationsmod.mutations.world;

import com.mojang.serialization.Codec;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A small hollow geode deep underground: smooth basalt shell, calcite, a lining of Mutagen Crystal
 * blocks, and Mutagen Clusters growing inward from the lining.
 */
public class MutagenGeodeFeature extends Feature<NoneFeatureConfiguration> {
    public MutagenGeodeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos center = ctx.origin();
        double r = 3.2 + rnd.nextDouble() * 1.3;      // radius of the hollow + lining
        int reach = (int) Math.ceil(r + 2);

        // Only in solid rock: skip if the spot is too open (caves, lakes) or touches the surface.
        int open = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            if (!level.getBlockState(p).isSolid()) open++;
        }
        if (open > 30 || !level.getBlockState(center.above(reach + 2)).isSolid()) return false;

        BlockState basalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState lining = ModContent.MUTAGEN_CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        for (BlockPos p : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            double d = Math.sqrt(p.distSqr(center)) + rnd.nextDouble() * 0.35;
            BlockState here = level.getBlockState(p);
            if (here.is(BlockTags.FEATURES_CANNOT_REPLACE) || here.is(Blocks.BEDROCK)) continue;
            if (d <= r - 1.0) setBlock(level, p, air);
            else if (d <= r) setBlock(level, p, lining);
            else if (d <= r + 0.9) setBlock(level, p, calcite);
            else if (d <= r + 1.8) setBlock(level, p, basalt);
        }
        // Clusters grow off the lining into the hollow.
        int ir = (int) Math.ceil(r);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-ir, -ir, -ir), center.offset(ir, ir, ir))) {
            if (!level.getBlockState(p).isAir() || rnd.nextFloat() > 0.3f) continue;
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(p.relative(dir)).is(ModContent.MUTAGEN_CRYSTAL_BLOCK.get())) {
                    setBlock(level, p, ModContent.MUTAGEN_CLUSTER.get().defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, dir.getOpposite()));
                    break;
                }
            }
        }
        return true;
    }
}
