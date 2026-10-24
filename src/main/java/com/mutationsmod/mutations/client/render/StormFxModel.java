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
 * Stormcaller's effects, built from flat cards: storm-cloud wisps streaming off the shoulders,
 * lightning arcs that crackle around the hands and head in the rain, and spinning wind rings
 * around the legs during Rainwalker flight.
 */
public class StormFxModel extends HumanoidModel<HumanoidRenderState> {
    private static final float QUARTER = (float) (Math.PI / 2);

    private final ModelPart[] wisps;
    private final List<ModelPart> arcs = new ArrayList<>();
    private final ModelPart[] rings;

    public StormFxModel(ModelPart root) {
        super(root);
        this.wisps = new ModelPart[]{this.body.getChild("w0"), this.body.getChild("w1")};
        this.rings = new ModelPart[]{this.body.getChild("r0"), this.body.getChild("r1"), this.body.getChild("r2")};
        for (ModelPart limb : List.of(this.rightArm, this.leftArm, this.head)) {
            arcs.add(limb.getChild("a0"));
            arcs.add(limb.getChild("a1"));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createArmorMeshSet(CubeDeformation.NONE, CubeDeformation.NONE).chest();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder wisp = CubeListBuilder.create().texOffs(0, 0).addBox(0f, -4f, 0f, 0, 8, 12);
        CubeListBuilder arcA = CubeListBuilder.create().texOffs(24, 0).addBox(-4f, -4f, 0f, 8, 8, 0);
        CubeListBuilder arcB = CubeListBuilder.create().texOffs(40, 0).addBox(-4f, -4f, 0f, 8, 8, 0);
        CubeListBuilder ring = CubeListBuilder.create().texOffs(0, 32).addBox(-6f, 0f, -6f, 12, 0, 12);

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("a0", arcA, PartPose.offsetAndRotation(-2.5f, -10f, 0f, 0f, 0.4f, 0f));
        head.addOrReplaceChild("a1", arcB, PartPose.offsetAndRotation(2.5f, -10f, 0f, 0f, QUARTER + 0.4f, 0f));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("w0", wisp, PartPose.offset(-3.5f, 1.5f, 2f));
        body.addOrReplaceChild("w1", wisp, PartPose.offset(3.5f, 1.5f, 2f));
        body.addOrReplaceChild("r0", ring, PartPose.offset(0f, 14f, 0f));
        body.addOrReplaceChild("r1", ring, PartPose.offset(0f, 18f, 0f));
        body.addOrReplaceChild("r2", ring, PartPose.offset(0f, 22f, 0f));

        PartDefinition right = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5f, 2f, 0f));
        PartDefinition left = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5f, 2f, 0f));
        right.addOrReplaceChild("a0", arcA, PartPose.offsetAndRotation(-1f, 10f, 0f, 0f, 0f, 0f));
        right.addOrReplaceChild("a1", arcB, PartPose.offsetAndRotation(-1f, 10f, 0f, 0f, QUARTER, 0f));
        left.addOrReplaceChild("a0", arcA, PartPose.offsetAndRotation(1f, 10f, 0f, 0f, 0f, 0f));
        left.addOrReplaceChild("a1", arcB, PartPose.offsetAndRotation(1f, 10f, 0f, 0f, QUARTER, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        StormRenderData data = state.getRenderData(StormRenderData.KEY);
        float t = data == null ? 0f : data.time();
        boolean raining = data != null && data.arcs();
        boolean flying = data != null && data.flying();
        this.hat.visible = false;

        // Wisps stream back off the shoulders and sway; in flight they stretch out behind you.
        boolean chestFree = state.chestEquipment.isEmpty();
        for (int i = 0; i < wisps.length; i++) {
            ModelPart w = wisps[i];
            float side = i == 0 ? -1f : 1f;
            w.visible = chestFree;
            w.yRot = side * (0.5f + 0.12f * Mth.sin(t * 0.25f + i * 2f));
            w.xRot = flying ? -0.05f : -0.35f + 0.08f * Mth.sin(t * 0.3f + i);
            w.zScale = flying ? 1.5f : 1f;
        }
        // Arcs flicker on and off at random-looking moments, flipping to look like new bolts.
        for (int i = 0; i < arcs.size(); i++) {
            ModelPart a = arcs.get(i);
            int beat = (int) (t * 0.7f + i * 2.3f);
            a.visible = raining && Math.floorMod(beat * 7 + i, 5) < 2;
            float s = i >= 4 ? 0.45f : 0.6f;
            a.xScale = (beat % 2 == 0 ? s : -s);
            a.yScale = s;
        }
        // Wind rings spin around your legs while you fly.
        for (int i = 0; i < rings.length; i++) {
            ModelPart r = rings[i];
            r.visible = flying;
            r.yRot = t * (0.5f + 0.15f * i) * (i % 2 == 0 ? 1 : -1);
            float s = 1.15f - i * 0.2f;
            r.xScale = s;
            r.zScale = s;
        }
    }
}
