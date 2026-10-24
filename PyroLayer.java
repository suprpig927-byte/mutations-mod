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

/** Pyro's Flame On form: a flickering body of living flame plus 3D fire, all drawn at full brightness. */
public class PyroLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier[] BODY = {tex("flame_0"), tex("flame_1"), tex("flame_2")};
    private static final Identifier TONGUE = tex("tongue");

    private final PyroFlamesModel flames;

    public PyroLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, PyroFlamesModel flames) {
        super(parent);
        this.flames = flames;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/pyro/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        PyroRenderData data = state.getRenderData(PyroRenderData.KEY);
        if (data == null || state.isInvisible) return;
        int frame = Math.floorMod((int) (data.time() / 3f), BODY.length);
        collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.armorCutoutNoCull(BODY[frame]),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        collector.submitModel(this.flames, state, poseStack, RenderTypes.armorCutoutNoCull(TONGUE),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
