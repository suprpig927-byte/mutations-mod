package com.mutationsmod.mutations.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/**
 * Fierce Wings: two hawk wings on the shoulder blades, each an inner "arm" card plus an outer card of long
 * flight feathers that bends at the wrist. Folded down along the back on the ground, swept out when gliding,
 * and they beat on every flap.
 */
public class WingsModel extends HumanoidModel<HumanoidRenderState> {
    private final ModelPart rightWing, leftWing, rightTip, leftTip;

    public WingsModel(ModelPart root) {
        super(root);
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
        this.rightTip = this.rightWing.getChild("tip");
        this.leftTip = this.leftWing.getChild("tip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        // Inner wing: 12 wide x 13 tall card. Outer wing (tip): 14 wide x 16 tall card, hinged at the wrist.
        PartDefinition left = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 0).addBox(0f, -2f, 0f, 12, 13, 0), PartPose.offset(1.5f, 2f, 2.1f));
        left.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(24, 0).addBox(0f, -2f, 0f, 14, 16, 0), PartPose.offset(12f, 0f, 0f));
        PartDefinition right = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-12f, -2f, 0f, 12, 13, 0), PartPose.offset(-1.5f, 2f, 2.1f));
        right.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-14f, -2f, 0f, 14, 16, 0), PartPose.offset(-12f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        this.head.visible = false;
        this.hat.visible = false;
        this.rightArm.visible = false;
        this.leftArm.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        WingsRenderData data = state.getRenderData(WingsRenderData.KEY);
        float s = data == null ? 0f : data.spread();
        float beat = data == null ? 0f : data.beat();
        float t = data == null ? 0f : data.time();
        // Folded: hanging down along the back, crossing at the tail, tips tucked short.
        // Spread: straight out to the sides; each flap is a down-stroke.
        float idle = Mth.sin(t * 0.08f) * 0.03f;
        float stroke = Mth.sin(beat * Mth.PI) * 0.9f;
        float zDown = Mth.lerp(s, 1.25f, -0.15f) + idle + stroke;
        leftWing.yRot = -0.12f;
        leftWing.zRot = zDown;
        leftWing.xRot = Mth.lerp(s, 0.12f, 0.05f);
        rightWing.yRot = 0.12f;
        rightWing.zRot = -zDown;
        rightWing.xRot = leftWing.xRot;
        float tipOut = Mth.lerp(s, 0f, 0.08f) + stroke * 0.4f;
        float tipIn = Mth.lerp(s, 0.45f, 0f);
        leftTip.yRot = tipOut;
        rightTip.yRot = -tipOut;
        leftTip.zRot = tipIn;
        rightTip.zRot = -tipIn;
        leftTip.xScale = rightTip.xScale = Mth.lerp(s, 0.55f, 1f);
    }
}
