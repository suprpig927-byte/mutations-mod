package com.mutationsmod.mutations.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mutationsmod.mutations.MutationsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** Omega Awakening: while an ultimate runs, your eyes blaze and a swirling aura wraps you, in your mutation's color. */
public class OmegaLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier EYES = tex("eyes");
    private static final Identifier AURA = tex("aura");
    private final HumanoidModel<HumanoidRenderState> aura;

    public OmegaLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, HumanoidModel<HumanoidRenderState> aura) {
        super(parent);
        this.aura = aura;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/omega/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        OmegaRenderData data = state.getRenderData(OmegaRenderData.KEY);
        if (data == null) return;
        int color = 0xFF000000 | data.rgb();
        collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(EYES),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
        float t = data.time();
        collector.order(1).submitModel(aura, state, poseStack, RenderTypes.energySwirl(AURA, t * 0.012f % 1f, t * 0.008f % 1f),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, darken(color), null, state.outlineColor);
    }

    /** Energy swirl is additive: a dimmer tint keeps it from washing out. */
    private static int darken(int argb) {
        int r = (argb >> 16 & 255) * 3 / 5, g = (argb >> 8 & 255) * 3 / 5, b = (argb & 255) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
