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
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Colossus: living steel plating, glowing cracks during Ground Pound, and a bigger body. */
public class ColossusLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier STEEL = tex("steel");
    private static final Identifier CRACKS = tex("cracks");
    private static final Identifier EYES = tex("eyes");
    private static final Identifier PLATES = tex("plates");

    private final ColossusPlatesModel plates;

    public ColossusLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ColossusPlatesModel plates) {
        super(parent);
        this.plates = plates;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/colossus/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        ColossusRenderData data = state.getRenderData(ColossusRenderData.KEY);
        if (data == null || state.isInvisible) return;
        collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.armorCutoutNoCull(STEEL),
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        // 3D plates (hidden under worn armor pieces, see ColossusPlatesModel.setupAnim).
        collector.submitModel(plates, state, poseStack, RenderTypes.armorCutoutNoCull(PLATES),
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        // Eyes always glow.
        collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.armorCutoutNoCull(EYES),
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (data.cracks()) {
            // Cracks glow: drawn at full brightness even in the dark.
            collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.armorCutoutNoCull(CRACKS),
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    // ---- size: scale the whole player up while it's drawn, then undo it ----

    public static void onRenderPre(RenderPlayerEvent.Pre<?> event) {
        if (event.getRenderState().getRenderData(ColossusRenderData.KEY) != null) {
            float s = ColossusRenderData.SIZE;
            event.getPoseStack().scale(s, s, s);
        }
    }

    public static void onRenderPost(RenderPlayerEvent.Post<?> event) {
        if (event.getRenderState().getRenderData(ColossusRenderData.KEY) != null) {
            float s = 1f / ColossusRenderData.SIZE;
            event.getPoseStack().scale(s, s, s);
        }
    }
}
