package com.mutationsmod.mutations.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * In first person the player model isn't drawn, so the Host's tendrils are drawn into the world
 * here instead, posed exactly as they would be on the body.
 */
public final class FirstPersonTendrils {
    private FirstPersonTendrils() {}

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/host/tendril.png");
    public static TendrilModel model;
    private static final AvatarRenderState STATE = new AvatarRenderState();

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (model == null || player == null || mc.level == null) return;
        if (!mc.options.getCameraType().isFirstPerson() || player.isInvisible()) return;
        MutationData d = player.getData(ModAttachments.DATA);
        if (!d.has(Mutation.HOST)) return;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float bodyRot = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        STATE.ageInTicks = player.tickCount + pt;
        STATE.isCrouching = player.isCrouching();
        STATE.bodyRot = bodyRot;
        STATE.setRenderData(HostRenderData.KEY, HostRenderLogic.compute(player, d, bodyRot, pt));

        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        Vec3 pos = player.getPosition(pt);
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        stack.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        // Same transform the entity renderer applies to the player model.
        stack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRot));
        stack.scale(-1.0f, -1.0f, 1.0f);
        stack.translate(0.0f, -1.501f, 0.0f);
        int light = LightCoordsUtil.getLightCoords(mc.level, BlockPos.containing(player.getEyePosition(pt)));
        event.getSubmitNodeCollector().submitModel(model, STATE, stack,
                RenderTypes.armorCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, 0);
        stack.popPose();
    }
}
