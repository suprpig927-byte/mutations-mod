package com.mojang.renderpearl.backend.vulkan.checkpoints;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.util.List;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.vulkan.VkCommandBuffer;

@OnlyIn(Dist.CLIENT)
public interface CheckpointExtension extends UncheckedAutoCloseable {
   CheckpointExtension.CheckpointStorage createStorage(VulkanDevice var1, VulkanQueue var2, int var3);

   List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(boolean var1);

   public interface CheckpointStorage {
      void rotate();

      void recordCheckpoint(VkCommandBuffer var1, CheckpointExtension.CheckpointType var2, Supplier<String> var3);
   }

   public static enum CheckpointType {
      BEGIN_RENDER_PASS,
      END_RENDER_PASS;
   }

   public record QueueCheckpoints(long queue, List<CheckpointExtension.StageCheckpoint> checkpoints) {
   }

   public record StageCheckpoint(long stage, CheckpointExtension.CheckpointType type, String label) {
   }
}
