package com.mutationsmod.mutations.world;

import com.mojang.serialization.MapCodec;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Abandoned Gene Lab: a ruined building on the surface and a basement below it with a specimen hall
 * (tanks + Failed Experiment spawners), a research room (notes, loot) and a sealed vault holding the
 * Catalyst of the mutation this lab studied. Each lab is themed for one random mutation.
 */
public record GeneLabFeature() implements Feature {
    public static final MapCodec<GeneLabFeature> CODEC = MapCodec.unit(GeneLabFeature::new);
    private static final ResourceKey<LootTable> RESEARCH_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("mutations", "chests/gene_lab"));

    @Override
    public MapCodec<GeneLabFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource rnd, BlockPos origin) {
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        if (!level.getBlockState(new BlockPos(cx, y, cz)).getFluidState().isEmpty()) return false;
        int lo = y, hi = y;
        for (int[] c : new int[][]{{-6, -5}, {6, -5}, {-6, 5}, {6, 5}}) {
            int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + c[0], cz + c[1]) - 1;
            lo = Math.min(lo, h); hi = Math.max(hi, h);
        }
        if (hi - lo > 6) return false;  // too steep for a building
        Mutation m = Mutation.values()[rnd.nextInt(Mutation.values().length)];
        build(level, rnd, new BlockPos(cx, y, cz), m);
        return true;
    }

    // ------------------------------------------------------------------ building

    private record Ctx(WorldGenLevel level, RandomSource rnd, BlockPos o, LabTheme t) {
        BlockPos at(int x, int y, int z) { return o.offset(x, y, z); }
        void set(int x, int y, int z, BlockState s) {
            BlockPos p = at(x, y, z);
            BlockState here = level.getBlockState(p);
            if (here.is(Blocks.BEDROCK) || here.is(BlockTags.FEATURES_CANNOT_REPLACE)) return;
            level.setBlock(p, s, 2);
        }
        void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }
        BlockState get(int x, int y, int z) { return level.getBlockState(at(x, y, z)); }
        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, s);
        }
    }

    public static void build(WorldGenLevel level, RandomSource rnd, BlockPos surface, Mutation m) {
        Ctx c = new Ctx(level, rnd, surface, LabTheme.of(m));
        int F = -9;  // basement floor, relative to the surface floor
        surfaceRuin(c);
        // Rooms: hollow shells first, then contents.
        room(c, -7, 7, F, F + 6, -11, 0);           // specimen hall
        room(c, -13, -7, F, F + 6, -9, -2);         // research room
        room(c, -3, 3, F, F + 6, -16, -11);         // vault
        hall(c, F, m);
        research(c, F, m);
        vault(c, F, m);
        stairs(c);
    }

    private static BlockState wallBlock(Ctx c) {
        float f = c.rnd.nextFloat();
        return (f < 0.45 ? Blocks.POLISHED_ANDESITE : f < 0.75 ? Blocks.STONE_BRICKS : f < 0.9 ? Blocks.CRACKED_STONE_BRICKS
                : Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
    }

    /** A box with walls, floor and ceiling; the inside is cleared to air. An accent stripe runs around it. */
    private static void room(Ctx c, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, Blocks.AIR); continue; }
            if (y == y0) c.set(x, y, z, c.rnd.nextFloat() < 0.25 ? c.t.floor().defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
            else if (y == y0 + 3 && y != y1) c.set(x, y, z, c.t.accent());
            else c.set(x, y, z, wallBlock(c));
        }
    }

    private static void light(Ctx c, int x, int ceilY, int z) {
        Block b = c.t.light();
        if (b instanceof LanternBlock || b instanceof EndRodBlock) {
            BlockState s = c.t.lightState();
            if (b instanceof EndRodBlock) s = s.setValue(EndRodBlock.FACING, Direction.DOWN);
            c.set(x, ceilY - 1, z, s);
        } else {
            c.set(x, ceilY, z, c.t.lightState());
        }
    }

    // ---- surface ------------------------------------------------------------------------------

    private static void surfaceRuin(Ctx c) {
        for (int x = -6; x <= 6; x++) for (int z = -5; z <= 5; z++) {
            for (int y = 1; y <= 8; y++) c.set(x, y, z, Blocks.AIR);                         // clear the site
            for (int y = -1; y >= -6 && !c.get(x, y, z).isSolid(); y--) c.set(x, y, z, Blocks.STONE_BRICKS);   // foundation
            float f = c.rnd.nextFloat();
            c.set(x, 0, z, f < 0.08 ? Blocks.GRAVEL : f < 0.2 ? Blocks.CRACKED_STONE_BRICKS : Blocks.SMOOTH_STONE);
            boolean wall = x == -6 || x == 6 || z == -5 || z == 5;
            if (wall) {
                for (int h = 1; h <= 4; h++) {
                    if (c.rnd.nextFloat() < 0.08 + 0.17 * (h - 1)) break;                    // crumbling, worse higher up
                    boolean window = (h == 2 || h == 3) && ((x + z) % 3 == 0) && !(Math.abs(x) == 6 && Math.abs(z) == 5);
                    BlockState s;
                    if (window) s = c.rnd.nextFloat() < 0.6 ? Blocks.AIR.defaultBlockState()
                            : (c.rnd.nextBoolean() ? Blocks.IRON_BARS : Blocks.GLASS_PANE).defaultBlockState();
                    else {
                        float g = c.rnd.nextFloat();
                        s = g < 0.04 ? c.t.accent().defaultBlockState() : g < 0.5 ? Blocks.CONCRETE.white().defaultBlockState()
                                : g < 0.8 ? Blocks.CONCRETE.lightGray().defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                    }
                    c.set(x, h, z, s);
                }
            }
        }
        for (int h = 1; h <= 2; h++) { c.set(-1, h, 5, Blocks.AIR); c.set(0, h, 5, Blocks.AIR); }   // doorway
        for (int i = 0; i < 14; i++) {                                                         // debris
            int x = -5 + c.rnd.nextInt(11), z = -4 + c.rnd.nextInt(9);
            if (x >= 3 && z >= -1) continue;                                                   // keep the stairwell clear
            Block b = switch (c.rnd.nextInt(5)) { case 0 -> Blocks.COBWEB; case 1 -> Blocks.MOSS_CARPET; case 2 -> Blocks.GRAVEL;
                case 3 -> c.t.clutter(); default -> Blocks.COBBLESTONE; };
            c.set(x, 1, z, b);
        }
        c.set(-4, 1, -3, Blocks.CRAFTING_TABLE);
        c.set(-3, 1, -3, Blocks.CRAFTING_TABLE);
    }

    private static void stairs(Ctx c) {
        BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        for (int i = 0; i <= 8; i++) {
            int z = 3 - i, y = -i;
            for (int x = 4; x <= 5; x++) {
                c.set(x, y, z, step);
                c.set(x, y - 1, z, Blocks.STONE_BRICKS);
                for (int up = 1; up <= 4; up++) c.set(x, y + up, z, Blocks.AIR);
            }
        }
        for (int x = 4; x <= 5; x++) for (int up = 1; up <= 4; up++) c.set(x, -9 + up, -6, Blocks.AIR);   // landing
    }

    // ---- specimen hall -------------------------------------------------------------------------

    private static void hall(Ctx c, int F, Mutation m) {
        // Specimen chambers line the west and north walls; some are smashed open and empty.
        for (int z : new int[]{-9, -7, -5, -3, -1}) chamber(c, -6, F + 1, z, Direction.EAST, m, c.rnd.nextFloat() < 0.35f);
        for (int x : new int[]{-4, -2, 2, 4}) chamber(c, x, F + 1, -10, Direction.SOUTH, m, c.rnd.nextFloat() < 0.35f);
        for (int[] s : new int[][]{{-2, -5}, {1, -6}}) {
            c.set(s[0], F + 1, s[1], Blocks.SPAWNER);
            if (c.level.getBlockEntity(c.at(s[0], F + 1, s[1])) instanceof SpawnerBlockEntity sp) {
                sp.setEntityId(ModContent.FAILED_EXPERIMENT.get(), c.rnd);
            }
        }
        for (int x = -4; x <= 4; x += 4) for (int z : new int[]{-3, -8}) light(c, x == 4 ? 2 : x, F + 6, z);
        for (int i = 0; i < 10; i++) {
            int x = -6 + c.rnd.nextInt(10), z = -10 + c.rnd.nextInt(10);
            if (c.get(x, F + 1, z).isAir()) c.set(x, F + 1, z, c.rnd.nextFloat() < 0.4 ? Blocks.COBWEB : c.t.clutter());
        }
    }

    private static void chamber(Ctx c, int x, int y, int z, Direction facing, Mutation m, boolean broken) {
        BlockState s = ModContent.SPECIMEN_CHAMBER.get().defaultBlockState()
                .setValue(com.mutationsmod.mutations.block.SpecimenChamberBlock.FACING, facing)
                .setValue(com.mutationsmod.mutations.block.SpecimenChamberBlock.VARIANT, m.ordinal() + 1)
                .setValue(com.mutationsmod.mutations.block.SpecimenChamberBlock.BROKEN, broken);
        com.mutationsmod.mutations.block.SpecimenChamberBlock.placeAt(c.level, s, c.at(x, y, z));
        if (broken) c.set(x + facing.getStepX(), y, z + facing.getStepZ(), Blocks.COBWEB);
    }

    /** A 3x3 specimen tank: accent base, glass ring around the specimen, iron cap. Some are smashed. */
    private static void tank(Ctx c, int cx, int F, int cz) {
        boolean broken = c.rnd.nextFloat() < 0.4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = cx + dx, z = cz + dz;
            c.set(x, F + 1, z, c.t.accent());
            c.set(x, F + 4, z, Blocks.IRON_BLOCK);
            for (int y = F + 2; y <= F + 3; y++) {
                if (dx == 0 && dz == 0) c.set(x, y, z, broken && y == F + 3 ? Blocks.AIR : c.t.tankFill());
                else if (broken && c.rnd.nextFloat() < 0.4) c.set(x, y, z, c.rnd.nextBoolean() ? Blocks.AIR : Blocks.COBWEB);
                else c.set(x, y, z, c.t.glass());
            }
        }
    }

    // ---- research room -------------------------------------------------------------------------

    private static void research(Ctx c, int F, Mutation m) {
        for (int y = F + 1; y <= F + 2; y++) for (int z = -6; z <= -5; z++) c.set(-7, y, z, Blocks.AIR);   // doorway to the hall
        for (int z = -8; z <= -3; z++) {
            if (z == -5) continue;
            for (int y = F + 1; y <= F + 3; y++) c.set(-12, y, z, c.rnd.nextFloat() < 0.8 ? Blocks.BOOKSHELF : Blocks.AIR);
        }
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST);
        c.set(-12, F + 1, -5, chest);
        RandomizableContainer.setBlockEntityLootTable(c.level, c.rnd, c.at(-12, F + 1, -5), RESEARCH_LOOT);
        lectern(c, -9, F + 1, -8, Direction.SOUTH, ResearchNotes.labNote(m));
        lectern(c, -9, F + 1, -3, Direction.NORTH, ResearchNotes.randomGeneral(c.rnd));
        c.set(-11, F + 1, -3, Blocks.CRAFTING_TABLE);
        c.set(-10, F + 1, -3, Blocks.CRAFTING_TABLE);
        if (c.rnd.nextFloat() < 0.5) c.set(-8, F + 1, -8, ModContent.GENE_SPLICER.get());
        light(c, -10, F + 6, -5);
    }

    private static void lectern(Ctx c, int x, int y, int z, Direction facing, ItemStack book) {
        c.set(x, y, z, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true));
        if (c.level.getBlockEntity(c.at(x, y, z)) instanceof LecternBlockEntity lectern) lectern.setBook(book);
    }

    // ---- vault ---------------------------------------------------------------------------------

    private static void vault(Ctx c, int F, Mutation m) {
        for (int x = -3; x <= 3; x++) for (int y = F + 1; y <= F + 6; y++) for (int z = -16; z <= -11; z++) {
            boolean shell = x == -3 || x == 3 || z == -16 || (z == -11 && !(x == 0 && y <= F + 2)) || y == F + 6;
            if (shell && c.rnd.nextFloat() < 0.5) c.set(x, y, z, c.t.accent());
        }
        // Iron door with a button on each side.
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        c.set(0, F + 1, -11, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        c.set(0, F + 2, -11, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockState button = Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL);
        c.set(1, F + 2, -10, button.setValue(ButtonBlock.FACING, Direction.SOUTH));
        c.set(1, F + 2, -12, button.setValue(ButtonBlock.FACING, Direction.NORTH));
        // The Catalyst.
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        c.set(0, F + 1, -15, chest);
        if (c.level.getBlockEntity(c.at(0, F + 1, -15)) instanceof Container box) {
            box.setItem(13, new ItemStack(Catalysts.BY_MUTATION.get(m).get()));
            box.setItem(11, new ItemStack(ModContent.MASTERY_TOME.get()));
            box.setItem(15, new ItemStack(ModContent.MUTAGEN_CRYSTAL.get(), 2 + c.rnd.nextInt(4)));
            if (c.rnd.nextBoolean()) box.setItem(4, new ItemStack(ModContent.CONTAINMENT_ROD.get()));
            box.setItem(22, ResearchNotes.labNote(m));
        }
        chamber(c, -2, F + 1, -15, Direction.SOUTH, m, false);     // the prime subjects, still sealed
        chamber(c, 2, F + 1, -15, Direction.SOUTH, m, false);
        light(c, 0, F + 6, -13);
    }
}
