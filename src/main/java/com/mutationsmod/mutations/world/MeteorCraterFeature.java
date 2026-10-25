package com.mutationsmod.mutations.world;

import com.mojang.serialization.MapCodec;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A large, very rare Meteor Crater: a bowl blasted into the surface with a raised rim, a half-buried
 * meteorite core in the middle covered in Nullite Clusters, and Mutagen Crystal scattered across the bowl.
 * Always centered in its chunk so the whole crater stays inside the area world gen may edit.
 */
public record MeteorCraterFeature() implements Feature {
    public static final MapCodec<MeteorCraterFeature> CODEC = MapCodec.unit(MeteorCraterFeature::new);

    @Override
    public MapCodec<MeteorCraterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource rnd, BlockPos origin) {
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        int baseY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        if (!level.getBlockState(new BlockPos(cx, baseY, cz)).getFluidState().isEmpty()) return false; // no craters in water
        return placeAt(level, rnd, new BlockPos(cx, baseY, cz));
    }

    /** Builds the crater around {@code center} (a surface block). Also used by /mutation crater. */
    public static boolean placeAt(WorldGenLevel level, RandomSource rnd, BlockPos center) {
        double radius = 13 + rnd.nextDouble() * 3;      // ~30 blocks across
        double depth = radius * 0.42;
        double rim = 1.3;                                  // rim reaches 30% beyond the bowl
        int reach = (int) Math.ceil(radius * rim);
        int baseY = center.getY();

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / radius + (rnd.nextDouble() - 0.5) * 0.04;
                if (d > rim) continue;
                int x = center.getX() + dx, z = center.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (d <= 1.0) {
                    int floor = baseY - (int) Math.round(depth * (1 - d * d));
                    for (int y = Math.max(floor + 1, level.getMinY()); y <= Math.max(top, baseY) + 2; y++) {
                        set(level, x, y, z, Blocks.AIR.defaultBlockState());
                    }
                    set(level, x, floor, z, floorBlock(rnd, d));
                    set(level, x, floor - 1, z, rnd.nextFloat() < 0.5 ? Blocks.STONE.defaultBlockState() : Blocks.TUFF.defaultBlockState());
                    // Mutagen Crystal scattered over the bowl, thickest toward the rim.
                    if (d > 0.35 && rnd.nextFloat() < 0.035 + 0.04 * d) {
                        set(level, x, floor, z, ModContent.MUTAGEN_CRYSTAL_BLOCK.get().defaultBlockState());
                        if (rnd.nextFloat() < 0.7) set(level, x, floor + 1, z, cluster(ModContent.MUTAGEN_CLUSTER.get().defaultBlockState(), Direction.UP));
                    }
                } else {
                    // Rim: ejected rock piled up around the edge, tapering outward.
                    int h = (int) Math.round(2.5 * (1 - (d - 1) / (rim - 1)));
                    int ground = Math.max(top, baseY - 1);
                    for (int y = ground + 1; y <= ground + h; y++) set(level, x, y, z, rimBlock(rnd));
                    if (h > 0 && rnd.nextFloat() < 0.02) {
                        set(level, x, ground + h, z, ModContent.MUTAGEN_CRYSTAL_BLOCK.get().defaultBlockState());
                    }
                }
            }
        }

        // The meteorite core: a lumpy, half-buried rock studded with Nullite.
        int floorCenter = baseY - (int) Math.round(depth);
        BlockPos core = new BlockPos(center.getX(), floorCenter + 1, center.getZ());
        double coreR = 2.8 + rnd.nextDouble();
        int cr = (int) Math.ceil(coreR) + 1;
        BlockState rock = ModContent.METEORITE.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-cr, -cr, -cr), core.offset(cr, cr, cr))) {
            double dist = Math.sqrt(p.distSqr(core)) + rnd.nextDouble() * 0.7;
            if (dist <= coreR) level.setBlock(p, rock, 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-cr - 1, -1, -cr - 1), core.offset(cr + 1, cr + 1, cr + 1))) {
            if (!level.getBlockState(p).isAir() || rnd.nextFloat() > 0.45) continue;
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;               // clusters grow out of the rock, never hang
                if (level.getBlockState(p.relative(dir)).is(ModContent.METEORITE.get())) {
                    level.setBlock(p, cluster(ModContent.NULLITE_CLUSTER.get().defaultBlockState(), dir.getOpposite()), 2);
                    break;
                }
            }
        }
        return true;
    }

    private static BlockState cluster(BlockState state, Direction facing) {
        return state.setValue(AmethystClusterBlock.FACING, facing);
    }

    private static BlockState floorBlock(RandomSource rnd, double d) {
        float f = rnd.nextFloat();
        if (d < 0.3 && f < 0.35) return ModContent.METEORITE.get().defaultBlockState();   // fragments near the impact
        if (f < 0.35) return Blocks.STONE.defaultBlockState();
        if (f < 0.6) return Blocks.COBBLESTONE.defaultBlockState();
        if (f < 0.8) return Blocks.TUFF.defaultBlockState();
        return Blocks.GRAVEL.defaultBlockState();
    }

    private static BlockState rimBlock(RandomSource rnd) {
        float f = rnd.nextFloat();
        return f < 0.4 ? Blocks.COBBLESTONE.defaultBlockState() : f < 0.7 ? Blocks.STONE.defaultBlockState()
                : f < 0.85 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
    }

    private static void set(WorldGenLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState here = level.getBlockState(pos);
        if (here.is(Blocks.BEDROCK) || here.is(BlockTags.FEATURES_CANNOT_REPLACE)) return;
        level.setBlock(pos, state, 2);
    }
}
