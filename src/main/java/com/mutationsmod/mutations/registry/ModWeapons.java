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
