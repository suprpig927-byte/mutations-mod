package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.pipeline.IndexType;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

@OnlyIn(Dist.CLIENT)
public interface RenderPassBackend {
   void pushDebugGroup(Supplier<String> var1);

   void popDebugGroup();

   void setPipeline(BackendRenderPipeline var1);

   void setUniform(int var1, @Nullable Object var2);

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

   void draw(int var1, int var2, int var3, int var4);

   void multiDraw(IntBuffer var1, int var2, int var3, int var4);

   void multiDraw(IntBuffer var1, IntBuffer var2, int var3);

   void drawIndirect(GpuBufferSlice var1, int var2);

   void writeTimestamp(GpuQueryPool var1, int var2);
}
