package com.mojang.renderpearl.api.textures;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuTexture extends UncheckedAutoCloseable {
   int USAGE_COPY_DST = 1;
   int USAGE_COPY_SRC = 2;
   int USAGE_TEXTURE_BINDING = 4;
   int USAGE_RENDER_ATTACHMENT = 8;
   int USAGE_CUBEMAP_COMPATIBLE = 16;

   int getWidth(int var1);

   int getHeight(int var1);

   int getDepthOrLayers();

   int getMipLevels();

   GpuFormat getFormat();

   @GpuTexture.Usage
   int usage();

   String getLabel();

   boolean isClosed();

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.TYPE_USE})
   public @interface Usage {
   }
}
