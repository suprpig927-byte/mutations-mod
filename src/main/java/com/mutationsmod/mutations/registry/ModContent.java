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
            }).build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        EFFECTS.register(modBus);
        FEATURE_TYPES.register(modBus);
    }
}
