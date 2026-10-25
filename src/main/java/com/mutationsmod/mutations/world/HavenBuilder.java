package com.mutationsmod.mutations.world;

import com.mutationsmod.mutations.entity.HavenMutant;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Mutant Haven: a hidden cave village. A mossy rock outcrop on the surface hides a spiral stair that
 * winds down into a huge cavern lit by Mutagen crystal, with five huts around a plaza.
 */
public final class HavenBuilder {
    private HavenBuilder() {}

    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("mutations", "chests/haven"));
    public static final int DEPTH = 28, RX = 19, RY = 8, RZ = 15;

    private record Ctx(WorldGenLevel level, BlockPos o) {
        BlockPos at(int x, int y, int z) { return o.offset(x, y, z); }
        boolean here(int x, int y, int z) { return Clip.inside(at(x, y, z)); }
        void set(int x, int y, int z, BlockState s) {
            BlockPos p = at(x, y, z);
            if (!Clip.inside(p)) return;
            BlockState cur = level.getBlockState(p);
            if (cur.is(Blocks.BEDROCK) || cur.is(BlockTags.FEATURES_CANNOT_REPLACE)) return;
            level.setBlock(p, s, 2);
        }
        void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
        BlockState get(int x, int y, int z) { BlockPos p = at(x, y, z); return Clip.inside(p) ? level.getBlockState(p) : Blocks.STONE.defaultBlockState(); }
    }

    /** {@code surface} is the top block at the entrance. */
    public static void build(WorldGenLevel level, long seed, BlockPos surface) {
        Ctx c = new Ctx(level, surface);
        int fy = -DEPTH;                                   // cavern floor, relative to the surface
        if (surface.getY() + fy < level.getMinY() + 10) fy = level.getMinY() + 10 - surface.getY();
        cavern(c, seed, fy);
        plaza(c, seed, fy);
        int[][] huts = {{-12, -7}, {12, -7}, {-12, 7}, {12, 7}, {0, -11}};
        HavenMutant.Role[] roles = HavenMutant.Role.values();
        for (int i = 0; i < huts.length; i++) hut(c, seed, huts[i][0], fy, huts[i][1], roles[i]);
        stairwell(c, fy);
    }

    // ---- the cavern ----------------------------------------------------------------------------

    private static void cavern(Ctx c, long seed, int fy) {
        int cy = fy + RY - 2;
        for (int x = -RX - 2; x <= RX + 2; x++) for (int z = -RZ - 2; z <= RZ + 2; z++) {
            if (!Clip.insideXZ(c.o.getX() + x, c.o.getZ() + z)) continue;
            for (int y = fy - 1; y <= cy + RY + 2; y++) {
                double n = sq((double) x / RX) + sq((double) (y - cy) / RY) + sq((double) z / RZ);
                RandomSource r = Clip.at(seed, x, y, z);
                if (y == fy && n <= 1.05) {
                    float f = r.nextFloat();
                    c.set(x, y, z, f < 0.5 ? Blocks.MOSS_BLOCK : f < 0.75 ? Blocks.COARSE_DIRT : f < 0.9 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL);
                } else if (y > fy && n <= 1.0) {
                    c.set(x, y, z, Blocks.CAVE_AIR);
                } else if (n <= 1.2 && y >= fy) {
                    BlockState cur = c.get(x, y, z);
                    if (cur.isAir() || !cur.getFluidState().isEmpty()) c.set(x, y, z, Blocks.STONE);   // seal the shell
                }
            }
            // Crystal on the ceiling, hanging down.
            RandomSource r = Clip.at(seed, x, 999, z);
            if (r.nextFloat() < 0.06) {
                for (int y = cy + RY + 1; y > fy + 4; y--) {
                    if (c.get(x, y, z).isAir() && !c.get(x, y + 1, z).isAir()) {
                        c.set(x, y + 1, z, ModContent.MUTAGEN_CRYSTAL_BLOCK.get());
                        c.set(x, y, z, ModContent.MUTAGEN_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN));
                        break;
                    }
                }
            } else if (r.nextFloat() < 0.02) {
                for (int y = cy + RY + 1; y > fy + 4; y--) {
                    if (c.get(x, y, z).isAir() && !c.get(x, y + 1, z).isAir()) { c.set(x, y + 1, z, Blocks.SHROOMLIGHT); break; }
                }
            }
        }
    }

    private static double sq(double v) { return v * v; }

    // ---- plaza: campfire, benches, Gene Splicer, glowcap patch, pond ----------------------------

    private static void plaza(Ctx c, long seed, int fy) {
        int y = fy + 1;
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) if (x * x + z * z <= 45) c.set(x, fy, z, Blocks.STONE_BRICKS);
        // Campfire with benches, beside the stair column.
        c.set(5, y, -5, Blocks.CAMPFIRE);
        c.set(4, y, -5, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        c.set(6, y, -5, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        c.set(5, y, -6, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        c.set(-5, y, -1, ModContent.GENE_SPLICER.get());
        // Lantern posts.
        for (int[] p : new int[][]{{-7, 0}, {7, 0}, {0, 7}, {-5, -5}}) {
            c.set(p[0], y, p[1], Blocks.SPRUCE_FENCE); c.set(p[0], y + 1, p[1], Blocks.SPRUCE_FENCE);
            c.set(p[0], y + 2, p[1], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        // Glowcap patch.
        for (int x = 5; x <= 8; x++) for (int z = 1; z <= 4; z++) {
            c.set(x, fy, z, Blocks.MYCELIUM);
            RandomSource r = Clip.at(seed, x, 3, z);
            if (r.nextFloat() < 0.6) c.set(x, y, z, r.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM);
        }
        // Pond (blindfish live here).
        for (int x = -9; x <= -6; x++) for (int z = 1; z <= 4; z++) {
            if ((x == -9 || x == -6) && (z == 1 || z == 4)) continue;
            c.set(x, fy, z, Blocks.WATER); c.set(x, fy - 1, z, Blocks.CLAY);
        }
    }

    // ---- huts ----------------------------------------------------------------------------------

    private static void hut(Ctx c, long seed, int hx, int fy, int hz, HavenMutant.Role role) {
        int y0 = fy + 1;
        Direction door = Math.abs(hx) > Math.abs(hz) ? (hx > 0 ? Direction.WEST : Direction.EAST) : (hz > 0 ? Direction.NORTH : Direction.SOUTH);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            c.set(hx + dx, fy, hz + dz, Blocks.SPRUCE_PLANKS);
            for (int h = 0; h < 3; h++) {
                BlockState s = !wall ? Blocks.AIR.defaultBlockState()
                        : corner ? Blocks.SPRUCE_LOG.defaultBlockState()
                        : h == 1 && (dx == 0 || dz == 0) ? Blocks.GLASS_PANE.defaultBlockState()
                        : (Clip.at(seed, hx + dx, h, hz + dz).nextFloat() < 0.5 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState();
                c.set(hx + dx, y0 + h, hz + dz, s);
            }
            c.set(hx + dx, y0 + 3, hz + dz, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        for (int h = 0; h < 2; h++) c.set(hx + door.getStepX() * 2, y0 + h, hz + door.getStepZ() * 2, Blocks.AIR);    // doorway
        c.set(hx, y0 + 2, hz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        // Role furniture against the back wall.
        int bx = hx - door.getStepX(), bz = hz - door.getStepZ();
        switch (role) {
            case QUARTERMASTER -> { chest(c, bx, y0, bz, door); c.set(bx + door.getClockWise().getStepX(), y0, bz + door.getClockWise().getStepZ(), Blocks.BARREL); }
            case ARCHIVIST -> { c.set(bx, y0, bz, Blocks.LECTERN); c.set(bx + door.getClockWise().getStepX(), y0, bz + door.getClockWise().getStepZ(), Blocks.BOOKSHELF); }
            case KEEPER -> com.mutationsmod.mutations.block.SpecimenChamberBlock.placeAt(c.level, ModContent.SPECIMEN_CHAMBER.get().defaultBlockState()
                    .setValue(com.mutationsmod.mutations.block.SpecimenChamberBlock.FACING, door), c.at(bx, y0, bz));
            case CARTOGRAPHER -> c.set(bx, y0, bz, Blocks.CARTOGRAPHY_TABLE);
            case COOK -> { c.set(bx, y0, bz, Blocks.SMOKER); chest(c, bx + door.getClockWise().getStepX(), y0, bz + door.getClockWise().getStepZ(), door); }
        }
        // The resident.
        if (c.here(hx, y0, hz)) {
            HavenMutant m = ModContent.HAVEN_MUTANT.get().create(c.level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (m != null) {
                Mutation mut = Mutation.values()[Math.floorMod((int) (seed ^ hx * 31 ^ hz), Mutation.values().length)];
                m.snapTo(c.o.getX() + hx + 0.5, c.o.getY() + y0, c.o.getZ() + hz + 0.5, 0, 0);
                m.setup(role, mut, c.at(hx, y0, hz));
                c.level.addFreshEntityWithPassengers(m);
            }
        }
    }

    private static void chest(Ctx c, int x, int y, int z, Direction facing) {
        c.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (c.here(x, y, z)) RandomizableContainer.setBlockEntityLootTable(c.level, RandomSource.create(c.at(x, y, z).asLong()), c.at(x, y, z), LOOT);
    }

    // ---- the hidden entrance: a mossy outcrop over a spiral stair ------------------------------

    private static void stairwell(Ctx c, int fy) {
        // Surface: a mossy rock outcrop hiding the shaft, with a low doorway on the south side.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int d = Math.max(Math.abs(x), Math.abs(z));
            int h = 4 - Math.max(0, d - 2);
            for (int y = 1; y <= h; y++) {
                boolean inside = Math.abs(x) <= 2 && Math.abs(z) <= 2 && y <= 3;
                c.set(x, y, z, inside ? Blocks.CAVE_AIR.defaultBlockState()
                        : ((x * 7 + z * 3 + y) % 3 == 0 ? Blocks.MOSS_BLOCK : (x + z) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE).defaultBlockState());
            }
            if (d <= 2) c.set(x, 0, z, Math.abs(x) <= 2 && Math.abs(z) <= 2 && !(x == 0 && z == 0) ? Blocks.CAVE_AIR : Blocks.MOSSY_STONE_BRICKS);
        }
        c.set(0, 0, 0, Blocks.MOSSY_STONE_BRICKS);
        for (int y = 1; y <= 2; y++) for (int x = -1; x <= 0; x++) c.set(x, y, 3, Blocks.CAVE_AIR);      // doorway
        for (int x = -1; x <= 0; x++) c.set(x, 3, 4, Blocks.MOSS_CARPET);
        c.set(-2, 1, 4, Blocks.AZALEA); c.set(1, 1, 4, Blocks.FLOWERING_AZALEA);                    // a bit of cover
        c.set(-1, 0, -1, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        // Shaft walls (7x7) from the cavern ceiling up to the surface; spiral steps around a 1x1 pillar.
        for (int y = fy + 1; y <= 0; y++) {
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (wall) {
                    if (!c.get(x, y, z).isAir() || y > fy + 13) c.set(x, y, z, (x + y + z) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
                } else if (x == 0 && z == 0) {
                    c.set(x, y, z, Blocks.MOSSY_STONE_BRICKS);
                } else {
                    c.set(x, y, z, Blocks.CAVE_AIR);
                }
            }
            if (y % 6 == 0) c.set(0, y, 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        // Steps: walk the ring of 8 cells around the pillar, one block down per step.
        int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        Direction[] up = {Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH};
        int i = 0;
        for (int y = 0; y >= fy + 1; y--, i++) {
            int[] cell = ring[i % 8];
            for (int below = y - 1; below >= fy + 1 && below >= y - 1; below--) c.set(cell[0], below, cell[1], Blocks.STONE_BRICKS);
            c.set(cell[0], y, cell[1], Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up[i % 8]));
        }
    }
}
