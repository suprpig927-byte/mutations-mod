package com.mutationsmod.mutations.world;

import com.mutationsmod.mutations.entity.PrisonBoss;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Containment Prison. A guard tower on the surface; a spiral stair down to the cell block (kept under
 * Inhibitor Fields); the Warden-Director's office at the end; then a stair down to the deep cell, sealed by
 * a Containment Gate, where Subject Zero waits.
 */
public final class PrisonBuilder {
    private PrisonBuilder() {}

    public static final int F1 = -20, F2 = F1 - 12;
    private static final ResourceKey<LootTable> CELL = key("chests/prison_cell"), OFFICE = key("chests/prison_office"), VAULT = key("chests/prison_vault");

    private static ResourceKey<LootTable> key(String p) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("mutations", p));
    }

    private record Ctx(WorldGenLevel level, BlockPos o, long seed) {
        BlockPos at(int x, int y, int z) { return o.offset(x, y, z); }
        boolean here(int x, int y, int z) { return Clip.inside(at(x, y, z)); }
        RandomSource r(int x, int y, int z) { return Clip.at(seed, x, y, z); }
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

    public static void build(WorldGenLevel level, long seed, BlockPos surface) {
        Ctx c = new Ctx(level, surface, seed);
        tower(c);
        spiral(c, 0, F1 + 1);
        cellBlock(c);
        office(c);
        descent(c);
        deepCell(c);
    }

    private static BlockState wall(Ctx c, int x, int y, int z) {
        float f = c.r(x, y, z).nextFloat();
        return (f < 0.55 ? Blocks.DEEPSLATE_BRICKS : f < 0.8 ? Blocks.CRACKED_DEEPSLATE_BRICKS : f < 0.92 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE).defaultBlockState();
    }

    /** Hollow box: walls/floor/ceiling of prison stone, inside cleared. */
    private static void room(Ctx c, int x0, int x1, int y0, int y1, int z0, int z1, Block floor) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
            if (!edge) c.set(x, y, z, Blocks.CAVE_AIR);
            else if (y == y0) c.set(x, y, z, floor);
            else c.set(x, y, z, wall(c, x, y, z));
        }
    }

    private static void chest(Ctx c, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        c.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (c.here(x, y, z)) RandomizableContainer.setBlockEntityLootTable(c.level, RandomSource.create(c.at(x, y, z).asLong()), c.at(x, y, z), loot);
    }

    private static void spawner(Ctx c, int x, int y, int z) {
        c.set(x, y, z, Blocks.SPAWNER);
        if (c.here(x, y, z) && c.level.getBlockEntity(c.at(x, y, z)) instanceof SpawnerBlockEntity sp) {
            sp.setEntityId(ModContent.FAILED_EXPERIMENT.get(), RandomSource.create(x * 31L + z));
        }
    }

    private static <T extends PrisonBoss> void boss(Ctx c, EntityType<T> type, int x, int y, int z, int home) {
        if (!c.here(x, y, z)) return;
        T b = type.create(c.level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (b == null) return;
        b.snapTo(c.o.getX() + x + 0.5, c.o.getY() + y, c.o.getZ() + z + 0.5, 270, 0);
        b.setHomeTo(c.at(x, y, z), home);
        c.level.addFreshEntityWithPassengers(b);
    }

    // ---- surface guard tower -------------------------------------------------------------------

    private static void tower(Ctx c) {
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            for (int y = -1; y >= -6 && !c.get(x, y, z).isSolid(); y--) c.set(x, y, z, Blocks.DEEPSLATE_BRICKS);  // foundation
            c.set(x, 0, z, Blocks.POLISHED_DEEPSLATE);
            boolean wallXZ = Math.abs(x) == 5 || Math.abs(z) == 5;
            for (int y = 1; y <= 12; y++) {
                if (!wallXZ) { c.set(x, y, z, y == 12 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState()); continue; }
                boolean window = (y == 5 || y == 9) && (x == 0 || z == 0);
                c.set(x, y, z, window ? Blocks.IRON_BARS.defaultBlockState() : wall(c, x, y, z));
            }
            if (wallXZ && (x + z) % 2 == 0) c.set(x, 13, z, Blocks.DEEPSLATE_BRICK_WALL);                 // crenellations
        }
        for (int y = 1; y <= 3; y++) for (int x = -1; x <= 0; x++) c.set(x, y, 5, Blocks.CAVE_AIR);   // doorway
        c.set(-2, 3, 6, Blocks.SOUL_LANTERN); c.set(1, 3, 6, Blocks.SOUL_LANTERN);
        c.set(0, 13, 0, ModContent.INHIBITOR_FIELD.get());                                               // the warning light
    }

    /** 7x7 shaft with steps winding around a central pillar, from {@code top} down to {@code bottom}. */
    private static void spiral(Ctx c, int top, int bottom) {
        for (int y = bottom; y <= top; y++) {
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                boolean wallXZ = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (wallXZ) { if (y < 0) c.set(x, y, z, wall(c, x, y, z)); }
                else if (x == 0 && z == 0) c.set(x, y, z, Blocks.POLISHED_DEEPSLATE);
                else c.set(x, y, z, Blocks.CAVE_AIR);
            }
            if (y % 6 == 0 && y < 0) c.set(0, y, -1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        Direction[] up = {Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH};
        int i = 0;
        for (int y = top; y >= bottom; y--, i++) {
            int[] cell = ring[i % 8];
            if (y - 1 >= bottom) c.set(cell[0], y - 1, cell[1], Blocks.DEEPSLATE_BRICKS);
            c.set(cell[0], y, cell[1], Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up[i % 8]));
        }
        c.set(0, top, 0, Blocks.POLISHED_DEEPSLATE);
    }

    // ---- level 1: cell block -------------------------------------------------------------------

    private static void cellBlock(Ctx c) {
        int y0 = F1;
        room(c, 3, 36, y0, y0 + 6, -9, 9, Blocks.POLISHED_DEEPSLATE);                 // whole block shell
        for (int y = y0 + 1; y <= y0 + 3; y++) for (int z = -1; z <= 1; z++) c.set(3, y, z, Blocks.CAVE_AIR);   // from the stair shaft
        // Corridor floor stripe + lights.
        for (int x = 4; x <= 35; x++) for (int z = -2; z <= 2; z++) c.set(x, y0, z, Math.abs(z) == 2 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE);
        for (int x = 6; x <= 34; x += 6) c.set(x, y0 + 6, 0, Blocks.SEA_LANTERN);
        c.set(10, y0 + 6, 0, ModContent.INHIBITOR_FIELD.get());                         // powers off in here
        c.set(22, y0 + 6, 0, ModContent.INHIBITOR_FIELD.get());
        // Cells: four on each side, iron bars facing the corridor.
        int n = 0;
        for (int side : new int[]{-1, 1}) {
            for (int cx = 6; cx <= 30; cx += 8) {
                n++;
                int zNear = side * 3, zFar = side * 8;
                for (int x = cx - 3; x <= cx + 3; x++) for (int y = y0 + 1; y <= y0 + 5; y++) for (int z = Math.min(zNear, zFar); z <= Math.max(zNear, zFar); z++) {
                    boolean sideWall = x == cx - 3 || x == cx + 3 || z == zFar;
                    boolean front = z == zNear;
                    if (sideWall) c.set(x, y, z, wall(c, x, y, z));
                    else if (front) c.set(x, y, z, y <= y0 + 3 ? Blocks.IRON_BARS.defaultBlockState() : wall(c, x, y, z));
                    else c.set(x, y, z, Blocks.CAVE_AIR);
                }
                RandomSource r = c.r(cx, 77, side);
                if (r.nextFloat() < 0.4) for (int y = y0 + 1; y <= y0 + 2; y++) c.set(cx, y, zNear, Blocks.CAVE_AIR);   // broken out
                int inZ = side * 6;
                c.set(cx - 1, y0 + 1, inZ, Blocks.BONE_BLOCK);
                c.set(cx + 2, y0 + 5, zFar - side, Blocks.IRON_CHAIN);
                c.set(cx + 2, y0 + 4, zFar - side, Blocks.IRON_CHAIN);
                c.set(cx + 1, y0 + 1, inZ + side, Blocks.COBWEB);
                if (n == 2 || n == 5 || n == 7) spawner(c, cx, y0 + 1, inZ);
                if (n == 3 || n == 8) chest(c, cx + 1, y0 + 1, zFar - side, side > 0 ? Direction.NORTH : Direction.SOUTH, CELL);
            }
        }
    }

    // ---- the Warden-Director's office ----------------------------------------------------------

    private static void office(Ctx c) {
        int y0 = F1;
        room(c, 36, 56, y0, y0 + 9, -9, 9, Blocks.POLISHED_BLACKSTONE_BRICKS);
        for (int y = y0 + 1; y <= y0 + 4; y++) for (int z = -1; z <= 1; z++) c.set(36, y, z, Blocks.CAVE_AIR);   // doorway
        for (int x = 37; x <= 55; x++) for (int z = -8; z <= 8; z++) if ((x + z) % 2 == 0) c.set(x, y0, z, Blocks.POLISHED_BLACKSTONE);
        for (int z = -6; z <= 6; z += 4) for (int x = 40; x <= 52; x += 6) c.set(x, y0 + 9, z, Blocks.SEA_LANTERN);
        // Observation windows into the cell block, desk, monitors, files.
        for (int z = -6; z <= 6; z++) if (Math.abs(z) > 2) for (int y = y0 + 3; y <= y0 + 5; y++) c.set(36, y, z, Blocks.TINTED_GLASS);
        for (int z = -2; z <= 2; z++) c.set(52, y0 + 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        for (int z = -2; z <= 2; z += 2) c.set(55, y0 + 3, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true));
        c.set(55, y0 + 1, -6, Blocks.LECTERN); c.set(55, y0 + 1, 6, Blocks.LECTERN);
        chest(c, 54, y0 + 1, -8, Direction.SOUTH, OFFICE);
        chest(c, 54, y0 + 1, 8, Direction.NORTH, OFFICE);
        c.set(40, y0 + 1, -8, Blocks.IRON_BLOCK); c.set(41, y0 + 1, -8, Blocks.BARREL);
        boss(c, ModContent.WARDEN_DIRECTOR.get(), 46, y0 + 1, 0, 10);
    }

    // ---- stair down to the deep cell -----------------------------------------------------------

    private static void descent(Ctx c) {
        for (int i = 0; i <= 12; i++) {
            int x = 56 + i, y = F1 - i;
            for (int z = -2; z <= 2; z++) {
                for (int h = 0; h <= 5; h++) c.set(x, y + h, z, (Math.abs(z) == 2 || h == 5) ? wall(c, x, y + h, z) : Blocks.CAVE_AIR.defaultBlockState());
                c.set(x, y - 1, z, Blocks.DEEPSLATE_BRICKS);
            }
            for (int z = -1; z <= 1; z++) c.set(x, y, z, i == 12 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            if (i % 4 == 0) c.set(x, y + 4, 0, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        for (int y = F1 + 1; y <= F1 + 4; y++) for (int z = -1; z <= 1; z++) c.set(56, y, z, Blocks.CAVE_AIR);   // out of the office
    }

    // ---- level 2: Subject Zero's cell -----------------------------------------------------------

    private static void deepCell(Ctx c) {
        int y0 = F2, cx = 80, R = 11;
        // Antechamber and the sealed gate.
        room(c, 68, 70, y0, y0 + 6, -3, 3, Blocks.POLISHED_DEEPSLATE);
        for (int y = y0 + 1; y <= y0 + 4; y++) for (int z = -1; z <= 1; z++) c.set(68, y, z, Blocks.CAVE_AIR);
        for (int y = y0 + 1; y <= y0 + 5; y++) for (int z = -2; z <= 2; z++) c.set(70, y, z, ModContent.CONTAINMENT_GATE.get());
        // The round cell.
        for (int x = cx - R - 1; x <= cx + R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) for (int y = y0; y <= y0 + 10; y++) {
            double d = Math.sqrt((x - cx) * (x - cx) + z * z);
            if (d > R + 0.9 || x < 70) continue;
            boolean shell = d > R - 0.1 || y == y0 || y == y0 + 10;
            if (!shell) c.set(x, y, z, Blocks.CAVE_AIR);
            else if (y == y0) c.set(x, y, z, d < 4 ? Blocks.CRYING_OBSIDIAN : (int) d % 3 == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_DEEPSLATE);
            else c.set(x, y, z, (y - y0) % 4 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : wall(c, x, y, z));
        }
        for (int y = y0 + 1; y <= y0 + 5; y++) for (int z = -2; z <= 2; z++) c.set(70, y, z, ModContent.CONTAINMENT_GATE.get());
        // Restraints and broken chambers around the wall, lights in the ceiling.
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int x = cx + (int) Math.round(Math.cos(a) * (R - 2)), z = (int) Math.round(Math.sin(a) * (R - 2));
            if (x < 74) continue;
            if (k % 2 == 0) {
                com.mutationsmod.mutations.block.SpecimenChamberBlock.placeAt(c.level, ModContent.SPECIMEN_CHAMBER.get().defaultBlockState()
                        .setValue(com.mutationsmod.mutations.block.SpecimenChamberBlock.BROKEN, true), c.at(x, y0 + 1, z));
            } else {
                for (int y = y0 + 6; y <= y0 + 9; y++) c.set(x, y, z, Blocks.IRON_CHAIN);
            }
            c.set(cx + (int) Math.round(Math.cos(a) * 5), y0 + 10, (int) Math.round(Math.sin(a) * 5), Blocks.SEA_LANTERN);
        }
        chest(c, cx + R - 1, y0 + 1, 0, Direction.WEST, VAULT);
        chest(c, cx + R - 2, y0 + 1, 2, Direction.WEST, VAULT);
        boss(c, ModContent.SUBJECT_ZERO.get(), cx, y0 + 1, 0, 9);
    }
}
