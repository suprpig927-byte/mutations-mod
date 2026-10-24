package com.mojang.renderpearl.backend.opengl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FrameBufferAttachment {
   int glId();

   int fboMipLevel();

   void addAssociatedFbo(FrameBufferCache.CacheKey var1);

   void removeAssociatedFbo(FrameBufferCache.CacheKey var1);

   default boolean hasStencilAspect() {
      return false;
   }
}
