package com.mojang.renderpearl.api.commands;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

@OnlyIn(Dist.CLIENT)
public interface RenderPass extends UncheckedAutoCloseable {
   int MAX_VERTEX_BUFFERS = 16;
   int INDIRECT_DRAW_SIZE = 16;
   int INDIRECT_INDEXED_DRAW_SIZE = 20;

   void pushDebugGroup(Supplier<String> var1);

   void popDebugGroup();

   void writeTimestamp(GpuQueryPool var1, int var2);

   void setPipeline(CompiledRenderPipeline var1);

   void setUniform(String var1, @Nullable GpuTextureView var2, @Nullable GpuSampler var3);

   void setUniform(String var1, GpuBuffer var2);

   void setUniform(String var1, GpuBufferSlice var2);

   void pushConstants(ByteBuffer var1);

   void setViewport(int var1, int var2, int var3, int var4);

   void enableScissor(int var1, int var2, int var3, int var4);

   void disableScissor();

   void setVertexBuffer(int var1, @Nullable GpuBufferSlice var2);

   void setIndexBuffer(GpuBuffer var1, IndexType var2);

   void drawIndexed(int var1, int var2, int var3, int var4, int var5);

   void multiDrawIndexed(IntBuffer var1, int var2, int var3, int var4);

   void multiDrawIndexed(PointerBuffer var1, IntBuffer var2, IntBuffer var3, int var4);

   void drawIndexedIndirect(GpuBufferSlice var1, int var2);

   <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> var1, @Nullable GpuBuffer var2, @Nullable IndexType var3, Collection<String> var4, T var5);

   void draw(int var1, int var2, int var3, int var4);

   void multiDraw(IntBuffer var1, int var2, int var3, int var4);

   void multiDraw(IntBuffer var1, IntBuffer var2, int var3);

   void drawIndirect(GpuBufferSlice var1, int var2);

   public record Draw<T>(
      int slot,
      GpuBuffer vertexBuffer,
      @Nullable GpuBuffer indexBuffer,
      @Nullable IndexType indexType,
      int firstIndex,
      int indexCount,
      int baseVertex,
      @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
   ) {
      public Draw(int slot, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, int firstIndex, int indexCount, int baseVertex) {
         this(slot, vertexBuffer, indexBuffer, indexType, firstIndex, indexCount, baseVertex, null);
      }
   }

   public record RenderArea(int x, int y, int width, int height) {
      public boolean fillsTexture(GpuTextureView texture) {
         return this.x == 0 && this.y == 0 && this.width == texture.getWidth(0) && this.height == texture.getHeight(0);
      }
   }

   public interface UniformUploader {
      void setUniform(String var1, GpuBufferSlice var2);

      void pushConstants(ByteBuffer var1);
   }
}
