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

/** Draws the Host organism over the player's skin (faint or bonded), plus the back tendrils. */
public class HostLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier FAINT = tex("organism_faint");
    private static final Identifier FULL = tex("organism_full");
    private static final Identifier TENDRIL = tex("tendril");

    private final TendrilModel tendrils;

    public HostLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, TendrilModel tendrils) {
        super(parent);
        this.tendrils = tendrils;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/host/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        HostRenderData data = state.getRenderData(HostRenderData.KEY);
        if (data == null || state.isInvisible) return;
        // The armor render type draws on top of the same model without flickering (like armor trims).
        collector.submitModel(this.getParentModel(), state, poseStack,
                RenderTypes.armorCutoutNoCull(data.bonded() ? FULL : FAINT), light, OverlayTexture.NO_OVERLAY, -1);
        collector.submitModel(this.tendrils, state, poseStack,
                RenderTypes.armorCutoutNoCull(TENDRIL), light, OverlayTexture.NO_OVERLAY, -1);
    }
}
