package com.mutationsmod.mutations.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Structures are built one chunk at a time: while a piece is being placed, writes are clipped to the
 * chunk currently generating, so a big build spread over many chunks never touches ungenerated ones.
 * Outside world gen (e.g. /mutation lab) there's no clip and everything is written.
 */
public final class Clip {
    private Clip() {}

    private static final ThreadLocal<BoundingBox> BOX = new ThreadLocal<>();

    public static void begin(BoundingBox box) { BOX.set(box); }
    public static void end() { BOX.remove(); }

    public static boolean inside(BlockPos pos) {
        BoundingBox b = BOX.get();
        return b == null || b.isInside(pos);
    }

    /** Column test: is (x, z) in the chunk being generated? */
    public static boolean insideXZ(int x, int z) {
        BoundingBox b = BOX.get();
        return b == null || (x >= b.minX() && x <= b.maxX() && z >= b.minZ() && z <= b.maxZ());
    }

    /** A random source that only depends on the seed and a position, so every chunk agrees on it. */
    public static net.minecraft.util.RandomSource at(long seed, int x, int y, int z) {
        long h = seed ^ (x * 3129871L) ^ (z * 116129781L) ^ (y * 42317861L);
        h = h * h * 42317861L + h * 11L;
        return net.minecraft.util.RandomSource.create(h);
    }

    public static void set(LevelAccessor level, BlockPos pos, BlockState state) {
        if (inside(pos)) level.setBlock(pos, state, 2);
    }
}
