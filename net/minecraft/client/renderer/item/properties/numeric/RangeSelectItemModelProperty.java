package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface RangeSelectItemModelProperty {
   float get(ItemStack var1, @Nullable ClientLevel var2, @Nullable ItemOwner var3, int var4);

   MapCodec<? extends RangeSelectItemModelProperty> type();
}
