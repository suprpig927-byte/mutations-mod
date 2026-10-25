package com.mutationsmod.mutations.registry;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.block.GeneSplicerBlock;
import com.mutationsmod.mutations.item.MasteryTomeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ToolMaterial;
import com.mutationsmod.mutations.effect.SuppressedEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks, items and the creative tab. (World gen is pure data: data/mutations/worldgen.) */
public final class ModContent {
    private ModContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MutationsMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MutationsMod.MODID);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, MutationsMod.MODID);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature>,
            com.mojang.serialization.MapCodec<com.mutationsmod.mutations.world.MeteorCraterFeature>> METEOR_CRATER =
            FEATURE_TYPES.register("meteor_crater", () -> com.mutationsmod.mutations.world.MeteorCraterFeature.CODEC);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MutationsMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MutationsMod.MODID);

    // ---------------------------------------------------------------- blocks

    public static final DeferredBlock<Block> MUTAGEN_ORE = BLOCKS.registerSimpleBlock("mutagen_ore",
            p -> p.mapColor(MapColor.STONE).strength(3f, 3f).requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 3));
    public static final DeferredBlock<Block> DEEPSLATE_MUTAGEN_ORE = BLOCKS.registerSimpleBlock("deepslate_mutagen_ore",
            p -> p.mapColor(MapColor.DEEPSLATE).strength(4.5f, 3f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 3));
    public static final DeferredBlock<Block> MUTAGEN_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("mutagen_crystal_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.5f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 5));
    public static final DeferredBlock<AmethystClusterBlock> MUTAGEN_CLUSTER = BLOCKS.registerBlock("mutagen_cluster",
            p -> new AmethystClusterBlock(7f, 3f, p),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER)
                    .strength(1.5f).lightLevel(s -> 6).pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<GeneSplicerBlock> GENE_SPLICER = BLOCKS.registerBlock("gene_splicer",
            GeneSplicerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.5f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 7).noOcclusion());

    public static final DeferredBlock<Block> METEORITE = BLOCKS.registerSimpleBlock("meteorite",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5f, 9f).requiresCorrectToolForDrops().sound(SoundType.ANCIENT_DEBRIS));
    public static final DeferredBlock<AmethystClusterBlock> NULLITE_CLUSTER = BLOCKS.registerBlock("nullite_cluster",
            p -> new AmethystClusterBlock(7f, 3f, p),
            p -> p.mapColor(MapColor.COLOR_GREEN).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER)
                    .strength(2.5f).requiresCorrectToolForDrops().lightLevel(s -> 9).pushReaction(PushReaction.POPPED));

    // ---------------------------------------------------------------- effects

    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, SuppressedEffect> SUPPRESSED =
            EFFECTS.register("suppressed", SuppressedEffect::new);

    // ---------------------------------------------------------------- items

    /** Nullite: between iron and diamond. Repaired with Nullite Shards. */
    public static final ToolMaterial NULLITE_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 7.0f, 2.5f, 12,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MutationsMod.MODID, "nullite_tool_materials")));

    public static final DeferredItem<Item> NULLITE_SHARD = ITEMS.registerSimpleItem("nullite_shard", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> CONTAINMENT_ROD = ITEMS.registerSimpleItem("containment_rod", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> NULLITE_SPEAR = ITEMS.registerItem("nullite_spear", Item::new,
            p -> p.spear(NULLITE_MATERIAL, 1.0f, 1.0f, 0.55f, 2.75f, 10.5f, 6.6f, 5.1f, 10.6f, 4.6f).rarity(Rarity.RARE));

    public static final DeferredItem<Item> MUTAGEN_INGOT = ITEMS.registerSimpleItem("mutagen_ingot");
    public static final DeferredItem<Item> MUTAGEN_CRYSTAL = ITEMS.registerSimpleItem("mutagen_crystal");
    public static final DeferredItem<MasteryTomeItem> MASTERY_TOME = ITEMS.registerItem("mastery_tome", MasteryTomeItem::new,
            p -> p.stacksTo(16).rarity(Rarity.RARE));


    // ---------------------------------------------------------------- catalysts (one per mutation, found in Gene Labs)
    public static final DeferredItem<Item> TROLL_HEART = ITEMS.registerSimpleItem("troll_heart", p -> catalyst(p, "Regenerator", "Regenerate twice as fast"));
    public static final DeferredItem<Item> FOCUS_PRISM = ITEMS.registerSimpleItem("focus_prism", p -> catalyst(p, "Kinetic Mind", "+50% telekinesis range, hold 1 extra block"));
    public static final DeferredItem<Item> STORM_ROD = ITEMS.registerSimpleItem("storm_rod", p -> catalyst(p, "Stormcaller", "Counts as rain for you, even in clear weather"));
    public static final DeferredItem<Item> KINETIC_SOLES = ITEMS.registerSimpleItem("kinetic_soles", p -> catalyst(p, "Phaser", "Unlocks one extra speed tier"));
    public static final DeferredItem<Item> FROST_CORE = ITEMS.registerSimpleItem("frost_core", p -> catalyst(p, "Cryo", "Heat no longer weakens you"));
    public static final DeferredItem<Item> ENDER_LENS = ITEMS.registerSimpleItem("ender_lens", p -> catalyst(p, "Blinker", "+50% Blink and Displace range"));
    public static final DeferredItem<Item> TITAN_GAUNTLET = ITEMS.registerSimpleItem("titan_gauntlet", p -> catalyst(p, "Colossus", "Ground Pound hits 50% harder and farther"));
    public static final DeferredItem<Item> RESONANCE_CRYSTAL = ITEMS.registerSimpleItem("resonance_crystal", p -> catalyst(p, "Siren", "Sonic Scream reaches 50% farther and stuns"));
    public static final DeferredItem<Item> CAPACITOR_CELL = ITEMS.registerSimpleItem("capacitor_cell", p -> catalyst(p, "Absorber", "Absorb charge 50% faster"));
    public static final DeferredItem<Item> PRISM_CLOAK = ITEMS.registerSimpleItem("prism_cloak", p -> catalyst(p, "Chameleon", "Mimic lasts twice as long"));
    public static final DeferredItem<Item> EMBER_HEART = ITEMS.registerSimpleItem("ember_heart", p -> catalyst(p, "Pyro", "Rain no longer hurts you (water still does)"));
    public static final DeferredItem<Item> SYMBIOTE_JAR = ITEMS.registerSimpleItem("symbiote_jar", p -> catalyst(p, "Host", "The organism starves half as fast"));

    public static final DeferredItem<BlockItem> MUTAGEN_ORE_ITEM = ITEMS.registerSimpleBlockItem("mutagen_ore", MUTAGEN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MUTAGEN_ORE_ITEM = ITEMS.registerSimpleBlockItem("deepslate_mutagen_ore", DEEPSLATE_MUTAGEN_ORE);
    public static final DeferredItem<BlockItem> MUTAGEN_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mutagen_crystal_block", MUTAGEN_CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> MUTAGEN_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("mutagen_cluster", MUTAGEN_CLUSTER);
    public static final DeferredItem<BlockItem> METEORITE_ITEM = ITEMS.registerSimpleBlockItem("meteorite", METEORITE);
    public static final DeferredItem<BlockItem> NULLITE_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("nullite_cluster", NULLITE_CLUSTER);
    public static final DeferredItem<BlockItem> GENE_SPLICER_ITEM = ITEMS.registerSimpleBlockItem("gene_splicer", GENE_SPLICER);

    // ---------------------------------------------------------------- creative tab

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("mutations", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mutations"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> MUTAGEN_CRYSTAL.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(MUTAGEN_ORE_ITEM.get());
                out.accept(DEEPSLATE_MUTAGEN_ORE_ITEM.get());
                out.accept(MUTAGEN_INGOT.get());
                out.accept(MUTAGEN_CLUSTER_ITEM.get());
                out.accept(MUTAGEN_CRYSTAL.get());
                out.accept(MUTAGEN_CRYSTAL_BLOCK_ITEM.get());
                out.accept(GENE_SPLICER_ITEM.get());
                out.accept(MASTERY_TOME.get());
                out.accept(METEORITE_ITEM.get());
                out.accept(NULLITE_CLUSTER_ITEM.get());
                out.accept(NULLITE_SHARD.get());
                out.accept(CONTAINMENT_ROD.get());
                out.accept(NULLITE_SPEAR.get());
                out.accept(TROLL_HEART.get());
                out.accept(FOCUS_PRISM.get());
                out.accept(STORM_ROD.get());
                out.accept(KINETIC_SOLES.get());
                out.accept(FROST_CORE.get());
                out.accept(ENDER_LENS.get());
                out.accept(TITAN_GAUNTLET.get());
                out.accept(RESONANCE_CRYSTAL.get());
                out.accept(CAPACITOR_CELL.get());
                out.accept(PRISM_CLOAK.get());
                out.accept(EMBER_HEART.get());
                out.accept(SYMBIOTE_JAR.get());
            }).build());

    private static Item.Properties catalyst(Item.Properties p, String mutation, String effect) {
        return p.stacksTo(1).rarity(Rarity.EPIC).component(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        Component.literal(mutation + " Catalyst").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                        Component.literal(effect).withStyle(net.minecraft.ChatFormatting.GRAY),
                        Component.literal("Hold in either hand").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        EFFECTS.register(modBus);
        FEATURE_TYPES.register(modBus);
    }
}
