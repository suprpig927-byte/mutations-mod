package net.minecraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String SCREENSHOT_DIR = "screenshots";

   public static void grab(File workDir, RenderTarget target, Consumer<Component> callback) {
      grab(workDir, null, target, 1, callback);
   }

   public static void grab(Minecraft minecraft, boolean debugPanoramaRequested) {
      if (debugPanoramaRequested && SharedConstants.DEBUG_PANORAMA_SCREENSHOT) {
         minecraft.showDebugChat(minecraft.grabPanoramixScreenshot(minecraft.gameDirectory));
      } else {
         grab(minecraft.gameDirectory, minecraft.gameRenderer.mainRenderTarget(), message -> minecraft.execute(() -> minecraft.showDebugChat(message)));
      }
   }

   public static void grab(File workDir, @Nullable String forceName, RenderTarget target, int downscaleFactor, Consumer<Component> callback) {
      takeScreenshot(
         target,
         downscaleFactor,
         image -> {
            File picDir = new File(workDir, "screenshots");
            picDir.mkdir();
            File file;
            if (forceName == null) {
               file = getFile(picDir);
            } else {
               file = new File(picDir, forceName);
            }

            ScreenshotEvent event = ClientHooks.onScreenshot(image, file);
            if (event.isCanceled()) {
               callback.accept(event.getCancelMessage());
            } else {
               File screenshotFile = event.getScreenshotFile();
               Util.ioPool()
                  .execute(
                     () -> {
                        try {
                           NativeImage twrVar0$ = image;

                           try {
                              image.writeToFile(screenshotFile);
                              if (event.getResultMessage() != null) {
                                 callback.accept(event.getResultMessage());
                              } else {
                                 Component component = Component.literal(screenshotFile.getName())
                                    .withStyle(ChatFormatting.UNDERLINE)
                                    .withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(screenshotFile.getAbsoluteFile())));
                                 callback.accept(Component.translatable("screenshot.success", component));
                              }
                           } catch (Throwable var8) {
                              if (image != null) {
                                 try {
                                    twrVar0$.close();
                                 } catch (Throwable var7x) {
                                    var8.addSuppressed(var7x);
                                 }
                              }

                              throw var8;
                           }

                           if (image != null) {
                              image.close();
                           }
                        } catch (Exception var9) {
                           LOGGER.warn("Couldn't save screenshot", var9);
                           callback.accept(Component.translatable("screenshot.failure", var9.getMessage()));
                        }
                     }
                  );
            }
         }
      );
   }

   public static void takeScreenshot(RenderTarget target, Consumer<NativeImage> callback) {
      takeScreenshot(target, 1, callback);
   }

   public static void takeScreenshot(RenderTarget target, int downscaleFactor, Consumer<NativeImage> callback) {
      int width = target.width;
      int height = target.height;
      GpuTexture sourceTexture = target.getColorTexture();
      if (sourceTexture == null) {
         throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
      } else if (width % downscaleFactor == 0 && height % downscaleFactor == 0) {
         GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)width * height * sourceTexture.getFormat().blockSize());
         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToBuffer(
               sourceTexture,
               buffer,
               0L,
               () -> {
                  try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
                     int outputHeight = height / downscaleFactor;
                     int outputWidth = width / downscaleFactor;
                     NativeImage image = new NativeImage(outputWidth, outputHeight, false);

                     for (int y = 0; y < outputHeight; y++) {
                        for (int x = 0; x < outputWidth; x++) {
                           if (downscaleFactor == 1) {
                              int argb = read.data().getInt((x + y * width) * sourceTexture.getFormat().blockSize());
                              image.setPixelABGR(x, height - y - 1, argb | 0xFF000000);
                           } else {
                              int red = 0;
                              int green = 0;
                              int blue = 0;

                              for (int i = 0; i < downscaleFactor; i++) {
                                 for (int j = 0; j < downscaleFactor; j++) {
                                    int argb = read.data()
                                       .getInt((x * downscaleFactor + i + (y * downscaleFactor + j) * width) * sourceTexture.getFormat().blockSize());
                                    red += ARGB.red(argb);
                                    green += ARGB.green(argb);
                                    blue += ARGB.blue(argb);
                                 }
                              }

                              int sampleCount = downscaleFactor * downscaleFactor;
                              image.setPixelABGR(x, outputHeight - y - 1, ARGB.color(255, red / sampleCount, green / sampleCount, blue / sampleCount));
                           }
                        }
                     }

                     callback.accept(image);
                  }

                  buffer.close();
               },
               0
            );
      } else {
         throw new IllegalArgumentException("Image size is not divisible by downscale factor");
      }
   }

   private static File getFile(File picDir) {
      String name = Util.getFilenameFormattedDateTime();
      int count = 1;

      while (true) {
         File file = new File(picDir, name + (count == 1 ? "" : "_" + count) + ".png");
         if (!file.exists()) {
            return file;
         }

         count++;
      }
   }
}
