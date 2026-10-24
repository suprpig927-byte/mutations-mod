package net.minecraft.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.IShearable;

@Deprecated
public interface Shearable extends IShearable {
   @Deprecated
   void shear(ServerLevel var1, SoundSource var2, ItemStack var3);

   @Deprecated
   boolean readyForShearing();
}
