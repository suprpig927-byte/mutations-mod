package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.nio.ByteBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
public interface CommandEncoderBackend {
   void submit();

   TransientMemory transientMemory();

   RenderPassBackend createRenderPass(RenderPassDescriptor var1);

   void submitRenderPass();

   void clearColorTexture(GpuTexture var1, Vector4fc var2);

   void clearColorAndDepthTextures(GpuTexture var1, Vector4fc var2, GpuTexture var3, double var4);

   void clearColorAndDepthTextures(GpuTexture var1, Vector4fc var2, GpuTexture var3, double var4, int var6, int var7, int var8, int var9, int var10);

   void clearDepthTexture(GpuTexture var1, double var2);

   void clearStencilTexture(GpuTexture var1, int var2);

   void writeToBuffer(GpuBufferSlice var1, ByteBuffer var2);

   void copyToBuffer(GpuBufferSlice var1, GpuBufferSlice var2);

   void writeToTexture(GpuTexture var1, ByteBuffer var2, int var3, int var4, int var5, int var6, int var7, int var8);

   void copyBufferToTexture(
      GpuBufferSlice var1, int var2, int var3, int var4, int var5, GpuTexture var6, int var7, int var8, int var9, int var10, int var11, int var12
   );

   void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6);

   void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6, int var7, int var8, int var9, int var10);

   void copyTextureToTexture(GpuTexture var1, GpuTexture var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9);

   GpuFence createFence();

   void writeTimestamp(GpuQueryPool var1, int var2);
}
