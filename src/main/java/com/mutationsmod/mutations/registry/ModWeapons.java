package com.mutationsmod.mutations.registry;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.entity.ThrownChakram;
import com.mutationsmod.mutations.entity.ThrownRiftKnife;
import com.mutationsmod.mutations.item.EchoBowItem;
import com.mutationsmod.mutations.item.MutantWeapon;
import com.mutationsmod.mutations.item.MutantWeapon.Kind;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** One specialized weapon per mutation. Only that mutation can hold it; its power works for them alone. */
public final class ModWeapons {
    private ModWeapons() {}

    /** Between iron and diamond; repaired with Mutagen Ingots. */
    public static final ToolMaterial MUTANT_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 1000, 7.0f, 2.5f, 14,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MutationsMod.MODID, "mutant_weapon_materials")));

    public static final Map<Mutation, Supplier<Item>> BY_MUTATION = new EnumMap<>(Mutation.class);
    public static final List<DeferredItem<? extends Item>> ALL = new ArrayList<>();

    private static DeferredItem<MutantWeapon> melee(String name, Mutation m, Kind kind, float damage, float speed) {
        DeferredItem<MutantWeapon> item = ModContent.ITEMS.registerItem(name, p -> new MutantWeapon(p, m, kind),
                p -> p.sword(MUTANT_MATERIAL, damage, speed).rarity(Rarity.RARE));
        BY_MUTATION.put(m, item::get);
        ALL.add(item);
        return item;
    }

    private static DeferredItem<MutantWeapon> tool(String name, Mutation m, Kind kind, float damage, float speed) {
        DeferredItem<MutantWeapon> item = ModContent.ITEMS.registerItem(name, p -> new MutantWeapon(p, m, kind),
                p -> p.pickaxe(MUTANT_MATERIAL, damage, speed).rarity(Rarity.RARE));
        BY_MUTATION.put(m, item::get);
        ALL.add(item);
        return item;
    }

    public static final DeferredItem<MutantWeapon> BLOODTHORN_BLADE = melee("bloodthorn_blade", Mutation.REGENERATOR, Kind.BLOODTHORN, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> ORBIT_CHAKRAM = melee("orbit_chakram", Mutation.KINETIC_MIND, Kind.CHAKRAM, 2.0f, -2.0f);
    public static final DeferredItem<MutantWeapon> THUNDERHEAD_MAUL = melee("thunderhead_maul", Mutation.STORMCALLER, Kind.MAUL, 5.5f, -3.1f);
    public static final DeferredItem<MutantWeapon> BLUR_DAGGERS = melee("blur_daggers", Mutation.PHASER, Kind.DAGGERS, 1.5f, -1.6f);
    public static final DeferredItem<MutantWeapon> GLACIER_HALBERD = melee("glacier_halberd", Mutation.CRYO, Kind.HALBERD, 4.0f, -2.9f);
    public static final DeferredItem<MutantWeapon> RIFT_KNIFE = melee("rift_knife", Mutation.BLINKER, Kind.RIFT_KNIFE, 2.0f, -1.8f);
    public static final DeferredItem<MutantWeapon> EARTHBREAKER = melee("earthbreaker", Mutation.COLOSSUS, Kind.EARTHBREAKER, 6.0f, -3.2f);
    public static final DeferredItem<EchoBowItem> ECHO_BOW = registerBow();
    public static final DeferredItem<MutantWeapon> CAPACITOR_BLADE = melee("capacitor_blade", Mutation.ABSORBER, Kind.CAPACITOR, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> SHADE_FANG = melee("shade_fang", Mutation.CHAMELEON, Kind.SHADE_FANG, 2.0f, -1.8f);
    public static final DeferredItem<MutantWeapon> EMBER_SCYTHE = melee("ember_scythe", Mutation.PYRO, Kind.SCYTHE, 4.0f, -2.8f);
    public static final DeferredItem<MutantWeapon> SYMBIOTE_WHIP = melee("symbiote_whip", Mutation.HOST, Kind.WHIP, 2.5f, -2.4f);
    public static final DeferredItem<MutantWeapon> GUNBLADE = melee("gunblade", Mutation.BULLET, Kind.GUNBLADE, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> TALON_GLAIVE = melee("talon_glaive", Mutation.FIERCE_WINGS, Kind.GLAIVE, 3.5f, -2.6f);

    // ---- v0.19 ----
    public static final DeferredItem<MutantWeapon> DUSKBLADE = melee("duskblade", Mutation.SHADOW, Kind.DUSKBLADE, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> ISOTOPE_CLEAVER = melee("isotope_cleaver", Mutation.RADIATION, Kind.CLEAVER, 4.0f, -2.9f);
    public static final DeferredItem<MutantWeapon> DAWN_SABER = melee("dawn_saber", Mutation.LUMINARY, Kind.DAWN_SABER, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> LODESTONE_FLAIL = melee("lodestone_flail", Mutation.MAGNETAR, Kind.FLAIL, 3.5f, -2.7f);
    public static final DeferredItem<MutantWeapon> VIPER_KUKRI = melee("viper_kukri", Mutation.VENOM, Kind.KUKRI, 2.0f, -2.0f);
    public static final DeferredItem<MutantWeapon> THORNBRANCH_STAFF = melee("thornbranch_staff", Mutation.VERDANT, Kind.STAFF, 3.0f, -2.6f);
    public static final DeferredItem<MutantWeapon> QUARRY_MAUL = tool("quarry_maul", Mutation.EARTHSHAPER, Kind.QUARRY_MAUL, 5.0f, -3.1f);
    public static final DeferredItem<MutantWeapon> PACKLEADERS_SPEAR = melee("packleaders_spear", Mutation.BEASTCALLER, Kind.PACK_SPEAR, 3.5f, -2.6f);
    public static final DeferredItem<MutantWeapon> HOURGLASS_BLADE = melee("hourglass_blade", Mutation.CHRONO, Kind.HOURGLASS, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> STINGER_RAPIER = melee("stinger_rapier", Mutation.SWARM, Kind.RAPIER, 2.0f, -1.8f);
    public static final DeferredItem<MutantWeapon> EVENT_HORIZON = melee("event_horizon", Mutation.GRAVITON, Kind.EVENT_HORIZON, 5.0f, -3.0f);
    public static final DeferredItem<MutantWeapon> CORAL_TRIDENT = melee("coral_trident", Mutation.TIDECALLER, Kind.CORAL_TRIDENT, 4.0f, -2.8f);
    public static final DeferredItem<MutantWeapon> GEODE_BLADE = melee("geode_blade", Mutation.CRYSTALLINE, Kind.GEODE_BLADE, 3.0f, -2.4f);
    public static final DeferredItem<MutantWeapon> RECOIL_HAMMER = melee("recoil_hammer", Mutation.ELASTIC, Kind.RECOIL_HAMMER, 4.0f, -2.8f);
    public static final DeferredItem<MutantWeapon> REAPERS_SICKLE = melee("reapers_sickle", Mutation.SOULBINDER, Kind.SICKLE, 3.5f, -2.6f);
    public static final DeferredItem<MutantWeapon> SCALE_HAMMER = melee("scale_hammer", Mutation.MICRO, Kind.SCALE_HAMMER, 4.0f, -2.8f);
    public static final DeferredItem<MutantWeapon> GAVEL_OF_COLLECTIONS = melee("gavel_of_collections", Mutation.TAX_MAN, Kind.GAVEL, 4.5f, -3.0f);
    public static final DeferredItem<MutantWeapon> MASTER_KEY = melee("master_key", Mutation.LANDLORD, Kind.MASTER_KEY, 4.0f, -2.8f);

    private static DeferredItem<EchoBowItem> registerBow() {
        DeferredItem<EchoBowItem> item = ModContent.ITEMS.registerItem("echo_bow", EchoBowItem::new,
                p -> p.durability(600).enchantable(14).repairable(MUTANT_MATERIAL.repairItems()).rarity(Rarity.RARE));
        BY_MUTATION.put(Mutation.SIREN, item::get);
        ALL.add(item);
        return item;
    }

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownChakram>> CHAKRAM = ModContent.ENTITIES.registerEntityType(
            "orbit_chakram", ThrownChakram::new, MobCategory.MISC, b -> b.sized(0.5f, 0.25f).clientTrackingRange(8).updateInterval(1).noLootTable());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRiftKnife>> RIFT_KNIFE_ENTITY = ModContent.ENTITIES.registerEntityType(
            "rift_knife", ThrownRiftKnife::new, MobCategory.MISC, b -> b.sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(1).noLootTable());

    /** The weapon for {@code m}'s mutation, if any. */
    public static Mutation owner(Item item) {
        if (item instanceof MutantWeapon w) return w.mutation();
        if (item instanceof EchoBowItem) return Mutation.SIREN;
        return null;
    }

    /** Loads the class (and so registers everything above) before the registries fire. */
    public static void init() {}
}
