package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BellRenderState extends BlockEntityRenderState {
   @Nullable
   public Direction shakeDirection;
   public float ticks;
}
