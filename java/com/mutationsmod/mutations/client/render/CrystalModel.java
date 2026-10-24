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

/**
 * Cryo's ice crystals. Built on the humanoid skeleton so the crystals ride along with the arms
 * as they swing; the body/arm boxes themselves are empty, only the crystals are drawn.
 */
public class CrystalModel extends HumanoidModel<HumanoidRenderState> {
    /** x, y, z pivot (arm space), length, width, xRot, zRot (for the right arm; mirrored on the left). */
    private static final float[][] CRYSTALS = {
            // shoulder cluster
            {-1.5f, -2f, 0f, 7.0f, 1.8f, 0.10f, -0.35f},
            {-2.5f, -2f, 1.0f, 5.0f, 1.5f, 0.35f, -0.90f},
            {0.0f, -2f, -0.5f, 4.0f, 1.3f, -0.20f, 0.15f},
            {-3.0f, -1f, -1.0f, 3.5f, 1.2f, -0.30f, -1.30f},
            // forearm spikes, pointing out and back
            {-3.0f, 6f, 0.0f, 3.6f, 1.2f, 0.30f, -1.60f},
            {-3.0f, 8f, 0.5f, 2.8f, 1.0f, 0.45f, -1.75f},
    };

    private final List<ModelPart> crystals = new ArrayList<>();

    public CrystalModel(ModelPart root) {
        super(root);
        this.head.visible = false;
        this.hat.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        for (int i = 0; i < CRYSTALS.length; i++) {
            crystals.add(this.rightArm.getChild("c" + i));
            crystals.add(this.leftArm.getChild("c" + i));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createArmorMeshSet(CubeDeformation.NONE, CubeDeformation.NONE).chest();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition right = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5f, 2f, 0f));
        PartDefinition left = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5f, 2f, 0f));
        for (int i = 0; i < CRYSTALS.length; i++) {
            float[] c = CRYSTALS[i];
            float w = c[4];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2, -c[3], -w / 2, w, c[3], w);
            right.addOrReplaceChild("c" + i, cube, PartPose.offsetAndRotation(c[0], c[1], c[2], c[5], 0f, c[6]));
            left.addOrReplaceChild("c" + i, cube, PartPose.offsetAndRotation(-c[0], c[1], c[2], c[5], 0f, -c[6]));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        CryoRenderData data = state.getRenderData(CryoRenderData.KEY);
        float s = data == null ? 1f : data.crystalScale();
        for (ModelPart c : crystals) {
            c.xScale = s;
            c.yScale = s;
            c.zScale = s;
        }
    }
}
