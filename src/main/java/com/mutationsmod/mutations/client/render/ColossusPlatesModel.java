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

/**
 * Colossus' 3D stone-steel plates: a crown and brow on the head, chest/back plates and a belt,
 * big shoulder pauldrons, heavy gauntlets, knee guards and boots. Built on the humanoid skeleton,
 * so every plate moves with its limb; the limb boxes themselves are empty.
 */
public class ColossusPlatesModel extends HumanoidModel<HumanoidRenderState> {
    public ColossusPlatesModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        // Hide plates under worn armor pieces so they don't clip through it.
        this.head.visible = state.headEquipment.isEmpty();
        this.hat.visible = false;
        boolean chest = state.chestEquipment.isEmpty();
        this.body.visible = chest;
        this.rightArm.visible = chest;
        this.leftArm.visible = chest;
        boolean legs = state.legsEquipment.isEmpty() && state.feetEquipment.isEmpty();
        this.rightLeg.visible = legs;
        this.leftLeg.visible = legs;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createArmorMeshSet(CubeDeformation.NONE, CubeDeformation.NONE).chest();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5f, -8.75f, -4.5f, 9, 1, 9)      // crown
                .texOffs(36, 0).addBox(-4.5f, -6.25f, -5.0f, 9, 1, 1),    // brow ridge
                PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-4.5f, 0.5f, -3.0f, 9, 7, 1)       // chest plate
                .texOffs(20, 10).addBox(-4.5f, 8.0f, -2.75f, 9, 3, 5)     // belt
                .texOffs(0, 18).addBox(-4.0f, 1.0f, 1.75f, 8, 6, 1),      // back plate
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-4.0f, -3.0f, -3.0f, 6, 4, 6)      // pauldron
                .texOffs(24, 26).addBox(-3.5f, 5.5f, -2.5f, 5, 5, 5),     // gauntlet
                PartPose.offset(-5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                .texOffs(0, 26).addBox(-2.0f, -3.0f, -3.0f, 6, 4, 6)
                .texOffs(24, 26).addBox(-1.5f, 5.5f, -2.5f, 5, 5, 5),
                PartPose.offset(5f, 2f, 0f));

        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(44, 26).addBox(-2.5f, 3.5f, -3.0f, 5, 3, 1)      // knee guard
                .texOffs(0, 36).addBox(-2.5f, 9.0f, -2.5f, 5, 3, 5);      // boot
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.9f, 12f, 0f));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.9f, 12f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
