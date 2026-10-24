package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GpuDeviceBackend {
   GpuSurfaceBackend createSurface(long var1, BooleanSupplier var3);

   CommandEncoderBackend createCommandEncoder();

   GpuSampler createSampler(AddressMode var1, AddressMode var2, FilterMode var3, FilterMode var4, int var5, OptionalDouble var6);

   GpuTexture createTexture(@Nullable String var1, @GpuTexture.Usage int var2, GpuFormat var3, int var4, int var5, int var6, int var7);

   GpuTextureView createTextureView(GpuTexture var1, int var2, int var3);

   GpuBuffer createBuffer(@Nullable Supplier<String> var1, @GpuBuffer.Usage int var2, long var3);

   GpuBuffer createBuffer(@Nullable Supplier<String> var1, @GpuBuffer.Usage int var2, ByteBuffer var3);

   List<String> getLastDebugMessages();

   boolean isDebuggingEnabled();

   BackendRenderPipeline.Pending compilePipeline(BackendRenderPipeline.CreateInfo var1);

   void close();

   GpuQueryPool createTimestampQueryPool(int var1);

   long getTimestampCalibrationOffset();

   DeviceInfo getDeviceInfo();
}
