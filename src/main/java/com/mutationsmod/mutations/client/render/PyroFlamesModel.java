package com.mutationsmod.mutations.client.render;

import java.util.ArrayList;
import java.util.List;
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
 * Pyro's 3D fire, built from flat flame "cards" (zero-thickness cutout planes) set in crosses so they
 * look full from every angle: a crown of fire on the head, flames on the shoulders, and flared jets
 * from the fists and feet during Flame Flight. Each card flickers by stretching from its base.
 */
public class PyroFlamesModel extends HumanoidModel<HumanoidRenderState> {
    private static final float QUARTER = (float) (Math.PI / 2);

    private final List<ModelPart> flames = new ArrayList<>();
    private final List<ModelPart> jets = new ArrayList<>();

    public PyroFlamesModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 6; i++) flames.add(this.head.getChild("f" + i));
        for (int i = 0; i < 2; i++) {
            flames.add(this.rightArm.getChild("f" + i));
            flames.add(this.leftArm.getChild("f" + i));
        }
        for (ModelPart limb : List.of(this.rightArm, this.leftArm, this.rightLeg, this.leftLeg)) {
            jets.add(limb.getChild("j0"));
            jets.add(limb.getChild("j1"));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createArmorMeshSet(CubeDeformation.NONE, CubeDeformation.NONE).chest();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder crown = CubeListBuilder.create().texOffs(0, 0).addBox(-5f, -9f, 0f, 10, 9, 0);
        CubeListBuilder ring = CubeListBuilder.create().texOffs(20, 0).addBox(-4.5f, -6f, 0f, 9, 6, 0);
        CubeListBuilder shoulder = CubeListBuilder.create().texOffs(38, 0).addBox(-2.5f, -5f, 0f, 5, 5, 0);
        CubeListBuilder jet = CubeListBuilder.create().texOffs(0, 12).addBox(-3f, 0f, 0f, 6, 12, 0);

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        // Crown: a tall cross in the middle, ringed by a card along each edge of the head.
        head.addOrReplaceChild("f0", crown, PartPose.offsetAndRotation(0f, -8f, 0f, 0f, 0f, 0f));
        head.addOrReplaceChild("f1", crown, PartPose.offsetAndRotation(0f, -8f, 0f, 0f, QUARTER, 0f));
        head.addOrReplaceChild("f2", ring, PartPose.offsetAndRotation(0f, -8f, -4f, -0.15f, 0f, 0f));
        head.addOrReplaceChild("f3", ring, PartPose.offsetAndRotation(0f, -8f, 4f, 0.15f, 0f, 0f));
        head.addOrReplaceChild("f4", ring, PartPose.offsetAndRotation(-4f, -8f, 0f, 0f, QUARTER, -0.15f));
        head.addOrReplaceChild("f5", ring, PartPose.offsetAndRotation(4f, -8f, 0f, 0f, QUARTER, 0.15f));

        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition right = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5f, 2f, 0f));
        PartDefinition left = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5f, 2f, 0f));
        right.addOrReplaceChild("f0", shoulder, PartPose.offsetAndRotation(-1f, -2f, 0f, 0f, 0f, -0.2f));
        right.addOrReplaceChild("f1", shoulder, PartPose.offsetAndRotation(-1f, -2f, 0f, 0f, QUARTER, 0f));
        left.addOrReplaceChild("f0", shoulder, PartPose.offsetAndRotation(1f, -2f, 0f, 0f, 0f, 0.2f));
        left.addOrReplaceChild("f1", shoulder, PartPose.offsetAndRotation(1f, -2f, 0f, 0f, QUARTER, 0f));
        cross(right, jet, -1f, 10f);
        cross(left, jet, 1f, 10f);
        cross(root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12f, 0f)), jet, 0f, 12f);
        cross(root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12f, 0f)), jet, 0f, 12f);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void cross(PartDefinition limb, CubeListBuilder card, float x, float y) {
        limb.addOrReplaceChild("j0", card, PartPose.offsetAndRotation(x, y, 0f, 0f, 0f, 0f));
        limb.addOrReplaceChild("j1", card, PartPose.offsetAndRotation(x, y, 0f, 0f, QUARTER, 0f));
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        PyroRenderData data = state.getRenderData(PyroRenderData.KEY);
        float t = data == null ? 0f : data.time();
        boolean flying = data != null && data.jets();

        this.head.visible = state.headEquipment.isEmpty();
        this.hat.visible = false;
        for (int i = 0; i < flames.size(); i++) {
            ModelPart f = flames.get(i);
            f.yScale = 0.85f + 0.15f * Mth.sin(t * 0.9f + i * 1.7f) + 0.08f * Mth.sin(t * 2.3f + i * 0.6f);
            f.xScale = 1f + 0.05f * Mth.sin(t * 1.3f + i);
        }
        for (int i = 0; i < jets.size(); i++) {
            ModelPart jet = jets.get(i);
            jet.visible = flying;
            jet.yScale = 1.0f + 0.2f * Mth.sin(t * 2.7f + i * 2.1f);
            jet.xScale = 1.0f + 0.1f * Mth.sin(t * 3.1f + i);
        }
    }
}
