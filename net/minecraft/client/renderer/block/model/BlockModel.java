package net.minecraft.client.renderer.block.model;

import java.util.function.Function;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface BlockModel {
   void update(BlockModelRenderState var1, BlockState var2, BlockDisplayContext var3, long var4);

   public record BakingContext(
      EntityModelSet entityModelSet,
      SpriteGetter sprites,
      PlayerSkinRenderCache playerSkinRenderCache,
      Function<BlockState, BlockStateModel> modelGetter,
      BlockModel missingBlockModel,
      PendingAnimations pendingAnimations
   ) implements SpecialModelRenderer.BakingContext {
      @Deprecated
      public BakingContext(
         EntityModelSet entityModelSet,
         SpriteGetter sprites,
         PlayerSkinRenderCache playerSkinRenderCache,
         Function<BlockState, BlockStateModel> modelGetter,
         BlockModel missingBlockModel
      ) {
         this(entityModelSet, sprites, playerSkinRenderCache, modelGetter, missingBlockModel, PendingAnimations.EMPTY);
      }
   }

   public interface Unbaked {
      BlockModel bake(BlockModel.BakingContext var1, Matrix4fc var2);
   }
}
