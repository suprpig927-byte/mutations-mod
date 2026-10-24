package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface OrderedSubmitNodeCollector extends OrderedSubmitNodeCollectorExtension {
   void submitShadow(PoseStack var1, float var2, List<EntityRenderState.ShadowPiece> var3);

   void submitNameTag(PoseStack var1, @Nullable Vec3 var2, int var3, Component var4, boolean var5, int var6, CameraRenderState var7);

   void submitText(
      PoseStack var1, float var2, float var3, FormattedCharSequence var4, boolean var5, Font.DisplayMode var6, int var7, int var8, int var9, int var10
   );

   void submitTextBackground(PoseStack var1, float var2, float var3, float var4, float var5, int var6, Font.DisplayMode var7, int var8);

   void submitFlame(PoseStack var1, EntityRenderState var2, Quaternionf var3);

   void submitLeash(PoseStack var1, EntityRenderState.LeashState var2);

   <S> void submitModel(Model<? super S> var1, S var2, PoseStack var3, RenderType var4, int var5, int var6, int var7, @Nullable UvMapping var8, int var9);

   default <S> void submitModel(
      Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int outlineColor
   ) {
      this.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, -1, null, outlineColor);
   }

   default <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, Identifier texture, int lightCoords, int overlayCoords, int outlineColor) {
      this.submitModel(model, state, poseStack, model.renderType(texture), lightCoords, overlayCoords, -1, null, outlineColor);
   }

   default <S> void submitModel(
      Model<S> model,
      S state,
      PoseStack poseStack,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      SpriteId sprite,
      SpriteGetter sprites,
      int outlineColor
   ) {
      this.submitModel(
         model, state, poseStack, sprite.renderType(model.renderType()), lightCoords, overlayCoords, tintedColor, sprites.get(sprite), outlineColor
      );
   }

   <S> void submitCrumblingOverlay(
      Model<? super S> var1, S var2, PoseStack var3, RenderType var4, int var5, int var6, int var7, ModelFeatureRenderer.CrumblingOverlay var8
   );

   default void submitCrumblingOverlay(
      ModelPart modelPart,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      this.submitCrumblingOverlay(
         new Model.Simple(modelPart, var1x -> renderType), Unit.INSTANCE, poseStack, renderType, lightCoords, overlayCoords, tintedColor, crumblingOverlay
      );
   }

   default void submitModelPart(
      ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable UvMapping uvMapping
   ) {
      this.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, -1, 0);
   }

   default void submitModelPart(
      ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable UvMapping uvMapping, int tintedColor
   ) {
      this.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, tintedColor, 0);
   }

   default void submitModelPart(
      ModelPart modelPart,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      @Nullable UvMapping uvMapping,
      int tintedColor,
      int outlineColor
   ) {
      Model.Simple model = new Model.Simple(modelPart, var1x -> renderType);
      this.submitModel(model, Unit.INSTANCE, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor);
   }

   void submitMovingBlock(PoseStack var1, MovingBlockRenderState var2, int var3);

   void submitBlockModel(PoseStack var1, RenderType var2, List<BlockStateModelPart> var3, int[] var4, int var5, int var6, int var7);

   void submitBreakingBlockModel(PoseStack var1, List<BlockStateModelPart> var2, int var3, boolean var4);

   void submitShapeOutline(PoseStack var1, VoxelShape var2, RenderType var3, int var4, float var5, boolean var6);

   void submitItem(PoseStack var1, ItemDisplayContext var2, int var3, int var4, int var5, int[] var6, ItemQuads var7, ItemStackRenderState.FoilType var8);

   void submitCustomGeometry(PoseStack var1, RenderType var2, SubmitNodeCollector.CustomGeometryRenderer var3);

   void submitQuadParticleGroup(QuadParticleRenderState var1);

   void submitGizmoPrimitives(DrawableGizmoPrimitives.Group var1, CameraRenderState var2, boolean var3);
}
