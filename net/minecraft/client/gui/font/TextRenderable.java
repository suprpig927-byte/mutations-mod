package net.minecraft.client.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface TextRenderable {
   void render(Matrix4fc var1, VertexConsumer var2, int var3, boolean var4);

   RenderType renderType(Font.DisplayMode var1);

   GpuTextureView textureView();

   RenderPipeline guiPipeline();

   float left();

   float top();

   float right();

   float bottom();

   public interface Styled extends TextRenderable, ActiveArea {
      @Override
      default float activeLeft() {
         return this.left();
      }

      @Override
      default float activeTop() {
         return this.top();
      }

      @Override
      default float activeRight() {
         return this.right();
      }

      @Override
      default float activeBottom() {
         return this.bottom();
      }
   }
}
