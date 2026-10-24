package com.mutationsmod.mutations.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** Stormcaller: lightning arcs in the rain and wind rings in flight (the player keeps their own skin). */
public class StormLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier FX = tex("fx");

    private final StormFxModel fx;

    public StormLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, StormFxModel fx) {
        super(parent);
        this.fx = fx;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/storm/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.getRenderData(StormRenderData.KEY) == null || state.isInvisible) return;
        collector.submitModel(this.fx, state, poseStack, RenderTypes.armorCutoutNoCull(FX),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
