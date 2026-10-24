package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
   void submit(@Nullable T var1, PoseStack var2, SubmitNodeCollector var3, int var4, int var5, boolean var6, int var7);

   void getExtents(Consumer<Vector3fc> var1);

   @Nullable
   T extractArgument(ItemStack var1);

   public interface BakingContext {
      EntityModelSet entityModelSet();

      SpriteGetter sprites();

      PlayerSkinRenderCache playerSkinRenderCache();

      default PendingAnimations pendingAnimations() {
         return PendingAnimations.EMPTY;
      }
   }

   public interface Unbaked<T> {
      @Nullable
      SpecialModelRenderer<T> bake(SpecialModelRenderer.BakingContext var1);

      MapCodec<? extends SpecialModelRenderer.Unbaked<T>> type();
   }
}
