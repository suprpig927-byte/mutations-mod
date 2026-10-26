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
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MutationsMod.MODID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.FailedExperiment>>
            FAILED_EXPERIMENT = ENTITIES.registerEntityType("failed_experiment", com.mutationsmod.mutations.entity.FailedExperiment::new,
            net.minecraft.world.entity.MobCategory.MONSTER, b -> b.sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MutationsMod.MODID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.HavenMutant>>
            HAVEN_MUTANT = ENTITIES.registerEntityType("haven_mutant", com.mutationsmod.mutations.entity.HavenMutant::new,
            net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.WardenDirector>>
            WARDEN_DIRECTOR = ENTITIES.registerEntityType("warden_director", com.mutationsmod.mutations.entity.WardenDirector::new,
            net.minecraft.world.entity.MobCategory.MONSTER, b -> b.sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.SubjectZero>>
            SUBJECT_ZERO = ENTITIES.registerEntityType("subject_zero", com.mutationsmod.mutations.entity.SubjectZero::new,
            net.minecraft.world.entity.MobCategory.MONSTER, b -> b.sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.BulletRound>>
            BULLET_ROUND = ENTITIES.registerEntityType("bullet_round", com.mutationsmod.mutations.entity.BulletRound::new,
            net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.2f, 0.2f).clientTrackingRange(8).updateInterval(1).noLootTable());
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.mutationsmod.mutations.entity.FeatherDart>>
            FEATHER_DART = ENTITIES.registerEntityType("feather_dart", com.mutationsmod.mutations.entity.FeatherDart::new,
            net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(1).noLootTable());
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

    public static final DeferredBlock<com.mutationsmod.mutations.block.SpecimenChamberBlock> SPECIMEN_CHAMBER = BLOCKS.registerBlock("specimen_chamber",
            com.mutationsmod.mutations.block.SpecimenChamberBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 9).noOcclusion());
    public static final DeferredBlock<com.mutationsmod.mutations.block.InhibitorFieldBlock> INHIBITOR_FIELD = BLOCKS.registerBlock("inhibitor_field",
            com.mutationsmod.mutations.block.InhibitorFieldBlock::new,
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(5f, 9f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 10));
    public static final DeferredBlock<com.mutationsmod.mutations.block.PsiBeaconBlock> PSI_BEACON = BLOCKS.registerBlock("psi_beacon",
            com.mutationsmod.mutations.block.PsiBeaconBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.5f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 8).noOcclusion());
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.mutationsmod.mutations.block.InhibitorFieldBlock.Field>> INHIBITOR_FIELD_ENTITY =
            BLOCK_ENTITIES.register("inhibitor_field", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                    com.mutationsmod.mutations.block.InhibitorFieldBlock.Field::new, INHIBITOR_FIELD.get()));
    public static final DeferredBlock<com.mutationsmod.mutations.block.ContainmentGateBlock> CONTAINMENT_GATE = BLOCKS.registerBlock("containment_gate",
            com.mutationsmod.mutations.block.ContainmentGateBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(-1f, 3600000f).noLootTable().sound(SoundType.METAL).lightLevel(s -> 6));
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
    public static final DeferredItem<Item> HEAT_SINK = ITEMS.registerSimpleItem("heat_sink", p -> catalyst(p, "Bullet", "Fire twice as much before you overheat"));
    public static final DeferredItem<Item> GALE_FEATHER = ITEMS.registerSimpleItem("gale_feather", p -> catalyst(p, "Fierce Wings", "Flapping costs no hunger"));
    /** A Fierce Wings razor feather (the projectile's look; also a plain crafting feather). */
    public static final DeferredItem<Item> HAWK_FEATHER = ITEMS.registerSimpleItem("hawk_feather");

    public static final DeferredItem<Item> FAILED_EXPERIMENT_SPAWN_EGG = ITEMS.registerItem("failed_experiment_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(FAILED_EXPERIMENT.get()));

    public static final DeferredItem<Item> HAVEN_MUTANT_SPAWN_EGG = ITEMS.registerItem("haven_mutant_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(HAVEN_MUTANT.get()));

    // ---- Haven food: ordinary food, only made in the Mutant Haven ----
    private static net.minecraft.world.food.FoodProperties food(int nutrition, float saturation) {
        return new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }
    public static final DeferredItem<Item> GLOWCAP_STEW = ITEMS.registerSimpleItem("glowcap_stew",
            p -> p.stacksTo(1).food(food(8, 0.6f)).usingConvertsTo(net.minecraft.world.item.Items.BOWL));
    public static final DeferredItem<Item> CAVE_LOAF = ITEMS.registerSimpleItem("cave_loaf", p -> p.food(food(5, 0.6f)));
    public static final DeferredItem<Item> CRYSTAL_CANDY = ITEMS.registerSimpleItem("crystal_candy", p -> p.food(food(2, 0.3f)));
    public static final DeferredItem<Item> SMOKED_BLINDFISH = ITEMS.registerSimpleItem("smoked_blindfish", p -> p.food(food(6, 0.8f)));

    public static final DeferredItem<BlockItem> MUTAGEN_ORE_ITEM = ITEMS.registerSimpleBlockItem("mutagen_ore", MUTAGEN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MUTAGEN_ORE_ITEM = ITEMS.registerSimpleBlockItem("deepslate_mutagen_ore", DEEPSLATE_MUTAGEN_ORE);
    public static final DeferredItem<BlockItem> MUTAGEN_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("mutagen_crystal_block", MUTAGEN_CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> MUTAGEN_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem("mutagen_cluster", MUTAGEN_CLUSTER);
    public static final DeferredItem<BlockItem> SPECIMEN_CHAMBER_ITEM = ITEMS.registerSimpleBlockItem("specimen_chamber", SPECIMEN_CHAMBER);
    public static final DeferredItem<BlockItem> INHIBITOR_FIELD_ITEM = ITEMS.registerSimpleBlockItem("inhibitor_field", INHIBITOR_FIELD);
    public static final DeferredItem<BlockItem> PSI_BEACON_ITEM = ITEMS.registerSimpleBlockItem("psi_beacon", PSI_BEACON);
    public static final DeferredItem<com.mutationsmod.mutations.item.InhibitorCollarItem> INHIBITOR_COLLAR = ITEMS.registerItem("inhibitor_collar",
            com.mutationsmod.mutations.item.InhibitorCollarItem::new, p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    // ---- Containment Suit: anti-mutant armor; each piece blocks 15% of ability damage ----
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> CONTAINMENT_ASSET =
            net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(MutationsMod.MODID, "containment"));
    public static final net.minecraft.world.item.equipment.ArmorMaterial CONTAINMENT_MATERIAL = new net.minecraft.world.item.equipment.ArmorMaterial(
            25, java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.BOOTS, 2, net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 5,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 6, net.minecraft.world.item.equipment.ArmorType.HELMET, 2,
                    net.minecraft.world.item.equipment.ArmorType.BODY, 5),
            12, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0f, 0.0f,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MutationsMod.MODID, "repairs_containment_armor")), CONTAINMENT_ASSET);
    public static final DeferredItem<Item> CONTAINMENT_HELMET = ITEMS.registerSimpleItem("containment_helmet",
            p -> p.humanoidArmor(CONTAINMENT_MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final DeferredItem<Item> CONTAINMENT_CHESTPLATE = ITEMS.registerSimpleItem("containment_chestplate",
            p -> p.humanoidArmor(CONTAINMENT_MATERIAL, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> CONTAINMENT_LEGGINGS = ITEMS.registerSimpleItem("containment_leggings",
            p -> p.humanoidArmor(CONTAINMENT_MATERIAL, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final DeferredItem<Item> CONTAINMENT_BOOTS = ITEMS.registerSimpleItem("containment_boots",
            p -> p.humanoidArmor(CONTAINMENT_MATERIAL, net.minecraft.world.item.equipment.ArmorType.BOOTS));

    /** How many Containment Suit pieces this entity is wearing (each blocks 15% of ability damage). */
    public static int containmentPieces(net.minecraft.world.entity.LivingEntity e) {
        int n = 0;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var s = e.getItemBySlot(slot);
            if (s.is(CONTAINMENT_HELMET.get()) || s.is(CONTAINMENT_CHESTPLATE.get()) || s.is(CONTAINMENT_LEGGINGS.get()) || s.is(CONTAINMENT_BOOTS.get())) n++;
        }
        return n;
    }

    public static final DeferredItem<BlockItem> CONTAINMENT_GATE_ITEM = ITEMS.registerSimpleBlockItem("containment_gate", CONTAINMENT_GATE);
    public static final DeferredItem<Item> DIRECTORS_KEYCARD = ITEMS.registerSimpleItem("directors_keycard", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<com.mutationsmod.mutations.item.HeartOfZeroItem> HEART_OF_ZERO = ITEMS.registerItem("heart_of_zero",
            com.mutationsmod.mutations.item.HeartOfZeroItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    /** The Warden-Director's spear: diamond-plus, and its suppression lasts twice as long. */
    public static final ToolMaterial OVERSEER_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5f, 3.5f, 15,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MutationsMod.MODID, "nullite_tool_materials")));
    public static final DeferredItem<Item> OVERSEERS_SPEAR = ITEMS.registerItem("overseers_spear", Item::new,
            p -> p.spear(OVERSEER_MATERIAL, 1.1f, 1.15f, 0.45f, 2.75f, 9.5f, 6.0f, 5.1f, 9.5f, 4.6f).rarity(Rarity.EPIC).fireResistant());
    /** The Warden-Director's weapon: an iron-tier club whose hits shock (slow) the target for a moment. */
    public static final DeferredItem<Item> SHOCK_BATON = ITEMS.registerItem("shock_baton", Item::new,
            p -> p.sword(ToolMaterial.IRON, 3.0f, -2.2f).rarity(Rarity.RARE));
    public static final DeferredItem<Item> WARDEN_DIRECTOR_SPAWN_EGG = ITEMS.registerItem("warden_director_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(WARDEN_DIRECTOR.get()));
    public static final DeferredItem<Item> SUBJECT_ZERO_SPAWN_EGG = ITEMS.registerItem("subject_zero_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(SUBJECT_ZERO.get()));
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
                out.accept(SPECIMEN_CHAMBER_ITEM.get());
                out.accept(INHIBITOR_FIELD_ITEM.get());
                out.accept(PSI_BEACON_ITEM.get());
                out.accept(INHIBITOR_COLLAR.get());
                out.accept(CONTAINMENT_HELMET.get());
                out.accept(CONTAINMENT_CHESTPLATE.get());
                out.accept(CONTAINMENT_LEGGINGS.get());
                out.accept(CONTAINMENT_BOOTS.get());
                out.accept(MASTERY_TOME.get());
                out.accept(METEORITE_ITEM.get());
                out.accept(NULLITE_CLUSTER_ITEM.get());
                out.accept(NULLITE_SHARD.get());
                out.accept(CONTAINMENT_ROD.get());
                out.accept(NULLITE_SPEAR.get());
                out.accept(FAILED_EXPERIMENT_SPAWN_EGG.get());
                out.accept(HAVEN_MUTANT_SPAWN_EGG.get());
                out.accept(WARDEN_DIRECTOR_SPAWN_EGG.get());
                out.accept(SUBJECT_ZERO_SPAWN_EGG.get());
                out.accept(CONTAINMENT_GATE_ITEM.get());
                out.accept(DIRECTORS_KEYCARD.get());
                out.accept(OVERSEERS_SPEAR.get());
                out.accept(SHOCK_BATON.get());
                for (var w : ModWeapons.ALL) out.accept(w.get());
                out.accept(HEART_OF_ZERO.get());
                out.accept(GLOWCAP_STEW.get());
                out.accept(CAVE_LOAF.get());
                out.accept(CRYSTAL_CANDY.get());
                out.accept(SMOKED_BLINDFISH.get());
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
                out.accept(HEAT_SINK.get());
                out.accept(GALE_FEATHER.get());
                out.accept(HAWK_FEATHER.get());
            }).build());

    private static Item.Properties catalyst(Item.Properties p, String mutation, String effect) {
        return p.stacksTo(1).rarity(Rarity.EPIC).component(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        Component.literal(mutation + " Catalyst").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                        Component.literal(effect).withStyle(net.minecraft.ChatFormatting.GRAY),
                        Component.literal("Hold in either hand").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))));
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(FAILED_EXPERIMENT.get(), com.mutationsmod.mutations.entity.FailedExperiment.createAttributes().build()));
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(HAVEN_MUTANT.get(), com.mutationsmod.mutations.entity.HavenMutant.createAttributes().build()));
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
            e.put(WARDEN_DIRECTOR.get(), com.mutationsmod.mutations.entity.WardenDirector.createAttributes().build());
            e.put(SUBJECT_ZERO.get(), com.mutationsmod.mutations.entity.SubjectZero.createAttributes().build());
        });
        ModWeapons.init();
        ITEMS.register(modBus);
        TABS.register(modBus);
        EFFECTS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        FEATURE_TYPES.register(modBus);
        com.mutationsmod.mutations.world.ModStructures.register(modBus);
    }
}
