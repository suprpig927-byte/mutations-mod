package com.mutationsmod.mutations.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * The Host's four back tendrils. Two sets are built: full-size (bonded) and small (dormant);
 * only one is visible at a time. Each tendril is a chain of segments, each a child of the last,
 * so bending every joint a little makes the whole tendril curl.
 */
public class TendrilModel extends EntityModel<AvatarRenderState> {
    public static final int SEGMENTS = 8;
    /** Anchor x/y on the back, and which side (+1 right, -1 left) and row (upper/lower). */
    private static final float[][] ANCHORS = {{-2.5f, 2.5f}, {2.5f, 2.5f}, {-2.0f, 7.0f}, {2.0f, 7.0f}};

    private final ModelPart big;
    private final ModelPart small;
    private final ModelPart[][] bigSegs = new ModelPart[4][SEGMENTS];
    private final ModelPart[][] smallSegs = new ModelPart[4][SEGMENTS];

    public TendrilModel(ModelPart root) {
        super(root);
        this.big = root.getChild("big");
        this.small = root.getChild("small");
        for (int t = 0; t < 4; t++) {
            collect(big.getChild("t" + t), bigSegs[t]);
            collect(small.getChild("t" + t), smallSegs[t]);
        }
    }

    private static void collect(ModelPart first, ModelPart[] out) {
        ModelPart part = first;
        for (int i = 0; i < SEGMENTS; i++) {
            out[i] = part;
            if (i + 1 < SEGMENTS) part = part.getChild("s" + (i + 1));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        build(root.addOrReplaceChild("big", CubeListBuilder.create(), PartPose.ZERO), 3.0f, 3.0f);
        build(root.addOrReplaceChild("small", CubeListBuilder.create(), PartPose.ZERO), 1.6f, 1.6f);
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** One set of 4 tendrils. Segments shrink toward the tip. */
    private static void build(PartDefinition set, float width, float length) {
        for (int t = 0; t < 4; t++) {
            float ax = ANCHORS[t][0];
            float ay = ANCHORS[t][1];
            PartDefinition parent = set;
            String name = "t" + t;
            float w = width;
            for (int i = 0; i < SEGMENTS; i++) {
                float half = w / 2f;
                CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0).addBox(-half, -half, 0, w, w, length);
                if (i == SEGMENTS - 1) {
                    // Claw tip: three thin prongs.
                    cube.texOffs(0, 8).addBox(-half - 0.6f, -0.3f, length - 0.2f, 0.6f, 0.6f, 2.0f)
                            .texOffs(0, 8).addBox(half, -0.3f, length - 0.2f, 0.6f, 0.6f, 2.0f)
                            .texOffs(0, 8).addBox(-0.3f, -half - 0.6f, length - 0.2f, 0.6f, 0.6f, 2.0f);
                }
                PartPose pose = i == 0 ? PartPose.offset(ax, ay, 2.0f) : PartPose.offset(0, 0, length);
                parent = parent.addOrReplaceChild(name, cube, pose);
                name = "s" + (i + 1);
                w = Math.max(0.8f, w * 0.88f);
            }
        }
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        HostRenderData data = state.getRenderData(HostRenderData.KEY);
        if (data == null) {
            big.visible = false;
            small.visible = false;
            return;
        }
        big.visible = data.bonded();
        small.visible = !data.bonded();
        ModelPart[][] segs = data.bonded() ? bigSegs : smallSegs;

        // Follow the body when crouching.
        ModelPart set = data.bonded() ? big : small;
        set.xRot = state.isCrouching ? 0.5f : 0f;
        set.y = state.isCrouching ? 3.2f : 0f;
        // Lash: the whole set whips around once.
        set.yRot = data.lash() > 0 ? (1f - data.lash()) * (float) (Math.PI * 2) : 0f;

        float age = state.ageInTicks;
        for (int t = 0; t < 4; t++) {
            boolean right = t % 2 == 1;
            boolean upper = t < 2;
            float side = right ? 1f : -1f;
            for (int i = 0; i < SEGMENTS; i++) {
                ModelPart seg = segs[t][i];
                float sway = Mth.sin(age * 0.08f + t * 1.7f + i * 0.6f) * 0.07f;
                if (i == 0) {
                    // Root joint: upper pair reaches up and out over the shoulders, lower pair out and down.
                    seg.yRot = side * (upper ? 0.55f : 0.9f);
                    seg.xRot = upper ? 0.75f : -0.35f;
                } else {
                    seg.yRot = side * 0.08f + sway;
                    seg.xRot = (upper ? 0.16f : -0.06f) + sway * 0.5f;
                }
                seg.zRot = 0f;
            }
            // Tendril ability: every tendril snaps straight forward over the shoulders.
            if (data.strike() > 0) {
                float s = data.strike();
                ModelPart first = segs[t][0];
                first.xRot = Mth.lerp(s, first.xRot, upper ? 2.6f : 2.2f);
                first.yRot = Mth.lerp(s, first.yRot, side * 0.25f);
                for (int i = 1; i < SEGMENTS; i++) {
                    segs[t][i].xRot = Mth.lerp(s, segs[t][i].xRot, 0.02f);
                    segs[t][i].yRot = Mth.lerp(s, segs[t][i].yRot, 0f);
                }
            }
        }
    }
}
