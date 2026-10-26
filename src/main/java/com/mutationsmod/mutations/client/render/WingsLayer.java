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

/** Draws the Fierce Wings on the player's back. */
public class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/wings/hawk.png");
    private final WingsModel wings;

    public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, WingsModel wings) {
        super(parent);
        this.wings = wings;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.getRenderData(WingsRenderData.KEY) == null || state.isInvisible) return;
        collector.submitModel(wings, state, poseStack, RenderTypes.armorCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
