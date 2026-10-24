package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SoundEventListener {
   void onPlaySound(SoundInstance var1, WeighedSoundEvents var2, float var3);
}
