package com.mutationsmod.mutations.world;

import com.mutationsmod.mutations.mutation.Mutation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** How each mutation's Gene Lab is decorated: wall accent, floor accent, tank contents, lights and clutter. */
public record LabTheme(Block accent, Block floor, Block tankFill, Block light, Block clutter, Block glass) {
    public static LabTheme of(Mutation m) {
        return switch (m) {
            case REGENERATOR -> new LabTheme(Blocks.CONCRETE.red(), Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK, Blocks.VERDANT_FROGLIGHT, Blocks.MOSS_CARPET, Blocks.GLASS);
            case KINETIC_MIND -> new LabTheme(Blocks.AMETHYST_BLOCK, Blocks.CONCRETE.purple(), Blocks.AMETHYST_BLOCK, Blocks.PEARLESCENT_FROGLIGHT, Blocks.END_ROD, Blocks.STAINED_GLASS.purple());
            case STORMCALLER -> new LabTheme(Blocks.COPPER_BLOCK.waxed().oxidized(), Blocks.COPPER_BLOCK.waxed().weathered(), Blocks.SEA_LANTERN,
                    Blocks.COPPER_BULB.waxed().unaffected(), Blocks.LIGHTNING_ROD.waxed().oxidized(), Blocks.STAINED_GLASS.lightBlue());
            case PHASER -> new LabTheme(Blocks.CONCRETE.yellow(), Blocks.GOLD_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.OCHRE_FROGLIGHT, Blocks.IRON_BARS, Blocks.STAINED_GLASS.yellow());
            case CRYO -> new LabTheme(Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, Blocks.SEA_LANTERN, Blocks.SNOW, Blocks.STAINED_GLASS.lightBlue());
            case BLINKER -> new LabTheme(Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.END_ROD, Blocks.END_ROD, Blocks.STAINED_GLASS.magenta());
            case COLOSSUS -> new LabTheme(Blocks.IRON_BLOCK, Blocks.POLISHED_ANDESITE, Blocks.IRON_BLOCK, Blocks.LANTERN, Blocks.ANVIL, Blocks.GLASS);
            case SIREN -> new LabTheme(Blocks.CONCRETE.pink(), Blocks.CONCRETE.magenta(), Blocks.NOTE_BLOCK, Blocks.PEARLESCENT_FROGLIGHT, Blocks.SCULK_SENSOR, Blocks.STAINED_GLASS.pink());
            case ABSORBER -> new LabTheme(Blocks.REDSTONE_BLOCK, Blocks.CONCRETE.gray(), Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_LAMP, Blocks.LIGHTNING_ROD.waxed().unaffected(), Blocks.STAINED_GLASS.orange());
            case CHAMELEON -> new LabTheme(Blocks.CONCRETE.green(), Blocks.MOSS_BLOCK, Blocks.OAK_LEAVES, Blocks.VERDANT_FROGLIGHT, Blocks.MOSS_CARPET, Blocks.TINTED_GLASS);
            case PYRO -> new LabTheme(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK, Blocks.MAGMA_BLOCK, Blocks.SHROOMLIGHT, Blocks.GILDED_BLACKSTONE, Blocks.STAINED_GLASS.orange());
            case HOST -> new LabTheme(Blocks.CONCRETE.black(), Blocks.SCULK, Blocks.COAL_BLOCK, Blocks.SOUL_LANTERN, Blocks.COBWEB, Blocks.TINTED_GLASS);
            case BULLET -> new LabTheme(Blocks.CONCRETE.lightGray(), Blocks.SMOOTH_STONE, Blocks.RAW_IRON_BLOCK, Blocks.LANTERN, Blocks.TARGET, Blocks.STAINED_GLASS.yellow());
            case FIERCE_WINGS -> new LabTheme(Blocks.CONCRETE.brown(), Blocks.SPRUCE_PLANKS, Blocks.HAY_BLOCK, Blocks.LANTERN, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STAINED_GLASS.brown());
            case SHADOW -> new LabTheme(Blocks.CONCRETE.black(), Blocks.BLACKSTONE, Blocks.OBSIDIAN, Blocks.SOUL_LANTERN, Blocks.COBWEB, Blocks.TINTED_GLASS);
            case RADIATION -> new LabTheme(Blocks.CONCRETE.lime(), Blocks.CONCRETE.gray(), Blocks.SLIME_BLOCK, Blocks.VERDANT_FROGLIGHT, Blocks.IRON_BARS, Blocks.STAINED_GLASS.lime());
            case LUMINARY -> new LabTheme(Blocks.CONCRETE.white(), Blocks.SMOOTH_QUARTZ, Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.END_ROD, Blocks.STAINED_GLASS.white());
            case MAGNETAR -> new LabTheme(Blocks.IRON_BLOCK, Blocks.CONCRETE.red(), Blocks.IRON_BLOCK, Blocks.REDSTONE_LAMP, Blocks.ANVIL, Blocks.STAINED_GLASS.red());
            case VENOM -> new LabTheme(Blocks.CONCRETE.green(), Blocks.MOSS_BLOCK, Blocks.SLIME_BLOCK, Blocks.VERDANT_FROGLIGHT, Blocks.MOSS_CARPET, Blocks.STAINED_GLASS.green());
            case VERDANT -> new LabTheme(Blocks.MOSS_BLOCK, Blocks.ROOTED_DIRT, Blocks.OAK_LEAVES, Blocks.VERDANT_FROGLIGHT, Blocks.FLOWERING_AZALEA, Blocks.GLASS);
            case EARTHSHAPER -> new LabTheme(Blocks.STONE_BRICKS, Blocks.PACKED_MUD, Blocks.GRAVEL, Blocks.LANTERN, Blocks.COBBLESTONE, Blocks.GLASS);
            case BEASTCALLER -> new LabTheme(Blocks.SPRUCE_PLANKS, Blocks.COARSE_DIRT, Blocks.HAY_BLOCK, Blocks.LANTERN, Blocks.HAY_BLOCK, Blocks.GLASS);
            case CHRONO -> new LabTheme(Blocks.CONCRETE.cyan(), Blocks.POLISHED_DIORITE, Blocks.PRISMARINE, Blocks.SEA_LANTERN, Blocks.IRON_BARS, Blocks.STAINED_GLASS.cyan());
            case SWARM -> new LabTheme(Blocks.CONCRETE.yellow(), Blocks.HONEYCOMB_BLOCK, Blocks.HONEY_BLOCK, Blocks.LANTERN, Blocks.BEE_NEST, Blocks.STAINED_GLASS.yellow());
            case GRAVITON -> new LabTheme(Blocks.CONCRETE.purple(), Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.END_ROD, Blocks.IRON_CHAIN, Blocks.STAINED_GLASS.purple());
            case TIDECALLER -> new LabTheme(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN, Blocks.SEA_LANTERN, Blocks.KELP_PLANT, Blocks.STAINED_GLASS.blue());
            case CRYSTALLINE -> new LabTheme(Blocks.AMETHYST_BLOCK, Blocks.CALCITE, Blocks.AMETHYST_BLOCK, Blocks.SEA_LANTERN, Blocks.AMETHYST_CLUSTER, Blocks.STAINED_GLASS.magenta());
            case ELASTIC -> new LabTheme(Blocks.CONCRETE.pink(), Blocks.CONCRETE.magenta(), Blocks.SLIME_BLOCK, Blocks.PEARLESCENT_FROGLIGHT, Blocks.SLIME_BLOCK, Blocks.STAINED_GLASS.pink());
            case SOULBINDER -> new LabTheme(Blocks.SOUL_SOIL, Blocks.SOUL_SAND, Blocks.SOUL_SAND, Blocks.SOUL_LANTERN, Blocks.BONE_BLOCK, Blocks.TINTED_GLASS);
            case MICRO -> new LabTheme(Blocks.CONCRETE.lightBlue(), Blocks.CONCRETE.white(), Blocks.PACKED_ICE, Blocks.SEA_LANTERN, Blocks.IRON_BARS, Blocks.STAINED_GLASS.lightBlue());
            case TAX_MAN -> new LabTheme(Blocks.CONCRETE.green(), Blocks.POLISHED_ANDESITE, Blocks.EMERALD_BLOCK, Blocks.LANTERN, Blocks.LECTERN, Blocks.STAINED_GLASS.lime());
            case LANDLORD -> new LabTheme(Blocks.BRICKS, Blocks.SPRUCE_PLANKS, Blocks.BOOKSHELF, Blocks.LANTERN, Blocks.BARREL, Blocks.GLASS);
        };
    }

    public BlockState lightState() {
        BlockState s = light.defaultBlockState();
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
            s = s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true);
        }
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HANGING)) {
            s = s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HANGING, true);
        }
        return s;
    }
}
