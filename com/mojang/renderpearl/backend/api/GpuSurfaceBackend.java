package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.util.Collection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuSurfaceBackend extends UncheckedAutoCloseable {
   void configure(GpuSurface.Configuration var1) throws SurfaceException;

   boolean isSuboptimal();

   void acquireNextTexture() throws SurfaceException;

   void blitFromTexture(CommandEncoderBackend var1, GpuTextureView var2);

   void present();

   Collection<GpuSurface.PresentMode> supportedPresentModes();
}
