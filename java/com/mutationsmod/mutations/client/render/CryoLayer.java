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

/** Cryo: frost on the skin, frost over worn armor, and ice crystals on the shoulders and forearms. */
public class CryoLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier FROST = tex("frost");
    private static final Identifier FROST_ARMOR = tex("frost_armor");
    private static final Identifier CRYSTAL = tex("crystal");

    private final CrystalModel crystals;
    private final HumanoidModel<HumanoidRenderState> head, chest, legs, feet;

    public CryoLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, CrystalModel crystals,
                     HumanoidModel<HumanoidRenderState> head, HumanoidModel<HumanoidRenderState> chest,
                     HumanoidModel<HumanoidRenderState> legs, HumanoidModel<HumanoidRenderState> feet) {
        super(parent);
        this.crystals = crystals;
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/cryo/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.getRenderData(CryoRenderData.KEY) == null || state.isInvisible) return;
        collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.armorCutoutNoCull(FROST),
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (!state.headEquipment.isEmpty()) frost(collector, head, state, poseStack, light);
        if (!state.chestEquipment.isEmpty()) frost(collector, chest, state, poseStack, light);
        if (!state.legsEquipment.isEmpty()) frost(collector, legs, state, poseStack, light);
        if (!state.feetEquipment.isEmpty()) frost(collector, feet, state, poseStack, light);
        collector.submitModel(this.crystals, state, poseStack, RenderTypes.armorCutoutNoCull(CRYSTAL),
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }

    private static void frost(SubmitNodeCollector collector, HumanoidModel<HumanoidRenderState> model, AvatarRenderState state,
                              PoseStack poseStack, int light) {
        collector.submitModel(model, state, poseStack, RenderTypes.armorCutoutNoCull(FROST_ARMOR),
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
