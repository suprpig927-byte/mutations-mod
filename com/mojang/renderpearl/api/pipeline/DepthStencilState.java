package com.mojang.renderpearl.api.pipeline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.stencil.StencilTest;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant, @Nullable StencilTest stencilTest) {
   public static final DepthStencilState DEFAULT = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true);

   public DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant) {
      this(depthTest, writeDepth, depthBiasScaleFactor, depthBiasConstant, null);
   }

   public DepthStencilState(CompareOp depthTest, boolean depthWrite) {
      this(depthTest, depthWrite, 0.0F, 0.0F);
   }
}
