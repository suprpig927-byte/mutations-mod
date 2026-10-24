package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderState extends EntityRenderState {
   public boolean showsBottom = true;
   @Nullable
   public Vec3 beamOffset;
}
