package com.mojang.renderpearl.api.device;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GpuBackend {
   String getName();

   void loadLibrary() throws BackendCreationException;

   void unloadLibrary();

   long createWindow(@Nullable String var1, int var2, int var3, long var4);

   GpuDevice createDevice(GpuDebugOptions var1) throws BackendCreationException;
}
