package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.SpaceProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;

@OnlyIn(Dist.CLIENT)
@NamedEnum
public enum GlyphProviderType implements StringRepresentable, IExtensibleEnum {
   BITMAP("bitmap", BitmapProvider.Definition.CODEC),
   TTF("ttf", TrueTypeGlyphProviderDefinition.CODEC),
   SPACE("space", SpaceProvider.Definition.CODEC),
   UNIHEX("unihex", UnihexProvider.Definition.CODEC),
   REFERENCE("reference", ProviderReferenceDefinition.CODEC);

   public static final Codec<GlyphProviderType> CODEC = StringRepresentable.fromEnum(GlyphProviderType::values);
   private final String name;
   private final MapCodec<? extends GlyphProviderDefinition> codec;

   private GlyphProviderType(String name, MapCodec<? extends GlyphProviderDefinition> codec) {
      this.name = name;
      this.codec = codec;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public MapCodec<? extends GlyphProviderDefinition> mapCodec() {
      return this.codec;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(GlyphProviderType.class);
   }
}
