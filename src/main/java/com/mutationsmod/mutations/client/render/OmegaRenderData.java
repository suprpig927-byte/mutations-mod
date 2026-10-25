package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param rgb the mutation's color, for the Omega glow; @param time animation clock */
public record OmegaRenderData(int rgb, float time) {
    public static final ContextKey<OmegaRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "omega"));
}
