package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param crystalScale 1 = full ice crystals, 0.5 = melting in the heat; pulses up on Ice Blast */
public record CryoRenderData(float crystalScale) {
    public static final ContextKey<CryoRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "cryo"));
}
