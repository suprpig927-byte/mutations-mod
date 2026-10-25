package com.mutationsmod.mutations.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * A structure piece that runs one of our procedural builders. The builder replays with the same seed
 * for every chunk the structure touches, and Clip keeps each pass to its own chunk.
 */
public abstract class BuilderPiece extends StructurePiece {
    protected final BlockPos center;
    protected final int variant;
    protected final long seed;

    protected BuilderPiece(StructurePieceType type, BlockPos center, int variant, long seed, BoundingBox box) {
        super(type, 0, box);
        this.center = center;
        this.variant = variant;
        this.seed = seed;
    }

    protected BuilderPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 0), tag.getIntOr("CZ", 0));
        this.variant = tag.getIntOr("Variant", 0);
        this.seed = tag.getLongOr("Seed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("Variant", variant);
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        Clip.begin(chunkBB);
        try {
            build(level, RandomSource.create(seed), center, variant);
        } finally {
            Clip.end();
        }
    }

    protected abstract void build(WorldGenLevel level, RandomSource rnd, BlockPos center, int variant);
}
