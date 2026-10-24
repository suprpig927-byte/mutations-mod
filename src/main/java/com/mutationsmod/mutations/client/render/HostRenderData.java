package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * What the Host layer needs to draw a player, copied onto their render state each frame.
 *
 * @param bonded whether the organism is fully bonded
 * @param strike Tendril ability animation, 1 = just used, fading to 0
 * @param lash   Lash animation, 1 = just used, fading to 0
 */
public record HostRenderData(boolean bonded, float strike, float lash) {
    public static final ContextKey<HostRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "host"));
}
