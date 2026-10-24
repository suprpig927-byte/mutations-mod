package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param time animation clock @param arcs raining on you (lightning arcs) @param flying Rainwalker flight (wind rings) */
public record StormRenderData(float time, boolean arcs, boolean flying) {
    public static final ContextKey<StormRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "storm"));
}
