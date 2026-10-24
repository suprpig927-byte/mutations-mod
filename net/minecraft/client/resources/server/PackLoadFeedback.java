package net.minecraft.client.resources.server;

import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PackLoadFeedback {
   void reportUpdate(UUID var1, PackLoadFeedback.Update var2);

   void reportFinalResult(UUID var1, PackLoadFeedback.FinalResult var2);

   public static enum FinalResult {
      DECLINED,
      APPLIED,
      DISCARDED,
      DOWNLOAD_FAILED,
      ACTIVATION_FAILED;
   }

   public static enum Update {
      ACCEPTED,
      DOWNLOADED;
   }
}
