package net.minecraft.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteSet {
   TextureAtlasSprite get(int var1, int var2);

   TextureAtlasSprite get(RandomSource var1);

   TextureAtlasSprite first();
}
