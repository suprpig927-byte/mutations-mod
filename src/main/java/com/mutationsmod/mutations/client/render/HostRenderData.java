package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * What the Host layer needs to draw a player, computed each frame.
 *
 * @param bonded    whether the organism is fully bonded
 * @param lash      Lash animation, 1 = just used, fading to 0
 * @param reachYaw  per tendril: model-space yaw to point at a target (when reachLen > 0)
 * @param reachPitch per tendril: model-space pitch to point at a target
 * @param reachLen  per tendril: how long it must stretch, in model pixels (0 = not reaching)
 */
public record HostRenderData(boolean bonded, float lash, float[] reachYaw, float[] reachPitch, float[] reachLen) {
    public static final ContextKey<HostRenderData> KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "host"));
}
