package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param spread 0 = folded on the back, 1 = fully spread; @param beat 0..1 wing-beat stroke (0 = none) */
public record WingsRenderData(float spread, float beat, float time) {
    public static final ContextKey<WingsRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "fierce_wings"));
}
