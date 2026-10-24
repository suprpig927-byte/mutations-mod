package com.mojang.renderpearl.api.pipeline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.internal.NonExhaustiveEnum;

@OnlyIn(Dist.CLIENT)
@NonExhaustiveEnum(
   reason = "Further uniform types such as SSBO and StorageTexelBuffer may be added"
)
public enum UniformType {
   COMBINED_IMAGE_SAMPLER,
   UNIFORM_BUFFER,
   TEXEL_BUFFER;
}
