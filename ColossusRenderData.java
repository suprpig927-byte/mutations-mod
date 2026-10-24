package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** @param cracks whether the glowing Ground Pound fissures are showing */
public record ColossusRenderData(boolean cracks) {
    public static final ContextKey<ColossusRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "colossus"));
    /** Colossus players are drawn this much bigger (the hitbox stays normal). */
    public static final float SIZE = 1.15f;
}
