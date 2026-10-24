package net.minecraft.client.renderer.feature;

import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.List;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface FeatureRenderer<Submit extends SubmitNode> extends AutoCloseable {
   default void beginPrepare(FeatureFrameContext context) {
   }

   void prepareGroup(FeatureFrameContext var1, List<Submit> var2, boolean var3);

   default void finishPrepare(FeatureFrameContext context) {
   }

   void executeGroup(FeatureFrameContext var1, @Nullable OitStage var2, RenderPass var3, int var4, List<Submit> var5, boolean var6);

   default void finishExecute(FeatureFrameContext context) {
   }

   @Override
   default void close() {
   }
}
