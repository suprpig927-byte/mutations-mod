package com.mutationsmod.mutations.item;

import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Catalysts: one per base mutation, found in that mutation's Gene Lab. Hold in either hand for the boost. */
public final class Catalysts {
    private Catalysts() {}

    public static final Map<Mutation, Supplier<Item>> BY_MUTATION = new EnumMap<>(Mutation.class);

    static {
        BY_MUTATION.put(Mutation.REGENERATOR, ModContent.TROLL_HEART::get);
        BY_MUTATION.put(Mutation.KINETIC_MIND, ModContent.FOCUS_PRISM::get);
        BY_MUTATION.put(Mutation.STORMCALLER, ModContent.STORM_ROD::get);
        BY_MUTATION.put(Mutation.PHASER, ModContent.KINETIC_SOLES::get);
        BY_MUTATION.put(Mutation.CRYO, ModContent.FROST_CORE::get);
        BY_MUTATION.put(Mutation.BLINKER, ModContent.ENDER_LENS::get);
        BY_MUTATION.put(Mutation.COLOSSUS, ModContent.TITAN_GAUNTLET::get);
        BY_MUTATION.put(Mutation.SIREN, ModContent.RESONANCE_CRYSTAL::get);
        BY_MUTATION.put(Mutation.ABSORBER, ModContent.CAPACITOR_CELL::get);
        BY_MUTATION.put(Mutation.CHAMELEON, ModContent.PRISM_CLOAK::get);
        BY_MUTATION.put(Mutation.PYRO, ModContent.EMBER_HEART::get);
        BY_MUTATION.put(Mutation.HOST, ModContent.SYMBIOTE_JAR::get);
        BY_MUTATION.put(Mutation.BULLET, ModContent.HEAT_SINK::get);
        BY_MUTATION.put(Mutation.FIERCE_WINGS, ModContent.GALE_FEATHER::get);
    }

    /** True if the player holds this mutation's Catalyst in either hand. Works on both client and server. */
    public static boolean holding(Player p, Mutation m) {
        Supplier<Item> item = BY_MUTATION.get(m);
        if (p == null || item == null) return false;
        return p.getOffhandItem().is(item.get()) || p.getMainHandItem().is(item.get());
    }
}
