package com.mojang.renderpearl.api.device;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GpuDevice {
   GpuSurface createSurface(long var1, BooleanSupplier var3);

   CommandEncoder createCommandEncoder();

   GpuSampler createSampler(AddressMode var1, AddressMode var2, FilterMode var3, FilterMode var4, int var5, OptionalDouble var6);

   GpuTexture createTexture(@Nullable Supplier<String> var1, @GpuTexture.Usage int var2, GpuFormat var3, int var4, int var5, int var6, int var7);

   GpuTexture createTexture(@Nullable String var1, @GpuTexture.Usage int var2, GpuFormat var3, int var4, int var5, int var6, int var7);

   GpuTextureView createTextureView(GpuTexture var1);

   GpuTextureView createTextureView(GpuTexture var1, int var2, int var3);

   GpuBuffer createBuffer(@Nullable Supplier<String> var1, @GpuBuffer.Usage int var2, long var3);

   GpuBuffer createBuffer(@Nullable Supplier<String> var1, @GpuBuffer.Usage int var2, ByteBuffer var3);

   List<String> getLastDebugMessages();

   boolean isDebuggingEnabled();

   CompletableFuture<CompiledRenderPipeline.Pending> compilePipeline(RenderPipeline var1, ShaderSource var2, Executor var3);

   void close();

   GpuQueryPool createTimestampQueryPool(int var1);

   DeviceInfo getDeviceInfo();
}
