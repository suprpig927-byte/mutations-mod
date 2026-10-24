package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param time animation clock (ticks, smooth) @param jets Flame Flight jets showing */
public record PyroRenderData(float time, boolean jets) {
    public static final ContextKey<PyroRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "pyro"));
}
