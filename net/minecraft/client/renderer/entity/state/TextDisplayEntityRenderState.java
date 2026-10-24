package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Display;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TextDisplayEntityRenderState extends DisplayEntityRenderState {
   @Nullable
   public Display.TextDisplay.TextRenderState textRenderState;
   @Nullable
   public Display.TextDisplay.CachedInfo cachedInfo;

   @Override
   public boolean hasSubState() {
      return this.textRenderState != null;
   }
}
