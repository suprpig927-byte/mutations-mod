package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VaultRenderState extends BlockEntityRenderState {
   @Nullable
   public ItemClusterRenderState displayItem;
   public float spin;
}
