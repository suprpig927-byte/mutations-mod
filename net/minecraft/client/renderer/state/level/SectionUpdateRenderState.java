package net.minecraft.client.renderer.state.level;

import java.util.List;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer;

@OnlyIn(Dist.CLIENT)
public record SectionUpdateRenderState(long sectionNode, boolean playerChanged, RenderSectionRegion region, List<AdditionalSectionRenderer> additionalRenderers) {
   @Deprecated
   public SectionUpdateRenderState(long sectionNode, boolean playerChanged, RenderSectionRegion region) {
      this(sectionNode, playerChanged, region, List.of());
   }
}
