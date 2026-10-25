package com.mutationsmod.mutations.world;

import com.mojang.serialization.MapCodec;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our structures (so /locate and explorer maps work). Each is a single procedural piece; see BuilderPiece.
 */
public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MutationsMod.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MutationsMod.MODID);

    // ------------------------------------------------------------------ shared helpers

    /** Surface block (top solid/liquid) at the chunk's middle, or empty if it's water or too steep. */
    static Optional<BlockPos> dryFlatCenter(Structure.GenerationContext ctx, int maxSlope) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        var gen = ctx.chunkGenerator();
        int surface = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        if (floor < surface || surface <= gen.getSeaLevel()) return Optional.empty();       // water / at sea level
        int lo = surface, hi = surface;
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            int h = gen.getFirstOccupiedHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            lo = Math.min(lo, h); hi = Math.max(hi, h);
        }
        if (hi - lo > maxSlope) return Optional.empty();
        return Optional.of(new BlockPos(x, surface - 1, z));
    }

    static BoundingBox box(BlockPos c, int x0, int y0, int z0, int x1, int y1, int z1) {
        return new BoundingBox(c.getX() + x0, c.getY() + y0, c.getZ() + z0, c.getX() + x1, c.getY() + y1, c.getZ() + z1);
    }

    // ------------------------------------------------------------------ Gene Lab

    public static class GeneLab extends Structure {
        public static final MapCodec<GeneLab> CODEC = simpleCodec(GeneLab::new);
        public GeneLab(StructureSettings s) { super(s); }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            return dryFlatCenter(ctx, 6).map(c -> new GenerationStub(c, b -> b.addPiece(new GeneLabPiece(c,
                    ctx.random().nextInt(Mutation.values().length), ctx.random().nextLong()))));
        }

        @Override
        public StructureType<?> type() { return GENE_LAB.get(); }
    }

    public static class GeneLabPiece extends BuilderPiece {
        public GeneLabPiece(BlockPos c, int variant, long seed) { super(GENE_LAB_PIECE.get(), c, variant, seed, box(c, -14, -11, -17, 8, 9, 6)); }
        public GeneLabPiece(CompoundTag tag) { super(GENE_LAB_PIECE.get(), tag); }

        @Override
        protected void build(WorldGenLevel level, RandomSource rnd, BlockPos center, int variant) {
            GeneLabFeature.build(level, rnd, center, Mutation.values()[Math.floorMod(variant, Mutation.values().length)]);
        }
    }

    // ------------------------------------------------------------------ Meteor Crater

    public static class MeteorCrater extends Structure {
        public static final MapCodec<MeteorCrater> CODEC = simpleCodec(MeteorCrater::new);
        public MeteorCrater(StructureSettings s) { super(s); }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            return dryFlatCenter(ctx, 10).map(c -> new GenerationStub(c, b -> b.addPiece(new CraterPiece(c, 0, ctx.random().nextLong()))));
        }

        @Override
        public StructureType<?> type() { return METEOR_CRATER.get(); }
    }

    public static class CraterPiece extends BuilderPiece {
        public CraterPiece(BlockPos c, int variant, long seed) { super(CRATER_PIECE.get(), c, variant, seed, box(c, -21, -12, -21, 21, 6, 21)); }
        public CraterPiece(CompoundTag tag) { super(CRATER_PIECE.get(), tag); }

        @Override
        protected void build(WorldGenLevel level, RandomSource rnd, BlockPos center, int variant) {
            MeteorCraterFeature.placeAt(level, rnd, center, Heightmap.Types.WORLD_SURFACE_WG);
        }
    }

    // ------------------------------------------------------------------ registration

    private static <S extends Structure> DeferredHolder<StructureType<?>, StructureType<S>> type(String name, MapCodec<S> codec) {
        return TYPES.register(name, () -> () -> codec);
    }

    private static DeferredHolder<StructurePieceType, StructurePieceType> piece(String name, Function<CompoundTag, BuilderPiece> loader) {
        return PIECES.register(name, () -> (StructurePieceType.ContextlessType) loader::apply);
    }

    public static final DeferredHolder<StructureType<?>, StructureType<GeneLab>> GENE_LAB = type("gene_lab", GeneLab.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<MeteorCrater>> METEOR_CRATER = type("meteor_crater", MeteorCrater.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> GENE_LAB_PIECE = piece("gene_lab", GeneLabPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRATER_PIECE = piece("meteor_crater", CraterPiece::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }
}
