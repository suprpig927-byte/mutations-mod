package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;

public record BiomeSpecialEffects(
   int waterColor,
   Optional<Integer> foliageColorOverride,
   Optional<Integer> dryFoliageColorOverride,
   Optional<Integer> grassColorOverride,
   BiomeSpecialEffects.GrassColorModifier grassColorModifier
) {
   public static final Codec<BiomeSpecialEffects> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.STRING_RGB_COLOR.fieldOf("water_color").forGetter(BiomeSpecialEffects::waterColor),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("foliage_color").forGetter(BiomeSpecialEffects::foliageColorOverride),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("dry_foliage_color").forGetter(BiomeSpecialEffects::dryFoliageColorOverride),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("grass_color").forGetter(BiomeSpecialEffects::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC
               .optionalFieldOf("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE)
               .forGetter(BiomeSpecialEffects::grassColorModifier)
         )
         .apply(i, BiomeSpecialEffects::new)
   );

   public static class Builder {
      protected OptionalInt waterColor = OptionalInt.empty();
      protected Optional<Integer> foliageColorOverride = Optional.empty();
      protected Optional<Integer> dryFoliageColorOverride = Optional.empty();
      protected Optional<Integer> grassColorOverride = Optional.empty();
      protected BiomeSpecialEffects.GrassColorModifier grassColorModifier = BiomeSpecialEffects.GrassColorModifier.NONE;

      public BiomeSpecialEffects.Builder waterColor(int waterColor) {
         this.waterColor = OptionalInt.of(waterColor);
         return this;
      }

      public BiomeSpecialEffects.Builder foliageColorOverride(int foliageColor) {
         this.foliageColorOverride = Optional.of(foliageColor);
         return this;
      }

      public BiomeSpecialEffects.Builder dryFoliageColorOverride(int dryFoliageColor) {
         this.dryFoliageColorOverride = Optional.of(dryFoliageColor);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorOverride(int grassColor) {
         this.grassColorOverride = Optional.of(grassColor);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorModifier(BiomeSpecialEffects.GrassColorModifier grassModifier) {
         this.grassColorModifier = grassModifier;
         return this;
      }

      public BiomeSpecialEffects build() {
         return new BiomeSpecialEffects(
            this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
            this.foliageColorOverride,
            this.dryFoliageColorOverride,
            this.grassColorOverride,
            this.grassColorModifier
         );
      }
   }

   @NetworkedEnum(NetworkCheck.CLIENTBOUND)
   @NamedEnum
   public static enum GrassColorModifier implements StringRepresentable, IExtensibleEnum {
      NONE("none") {
         @Override
         public int modifyColor(double x, double z, int baseColor) {
            return baseColor;
         }
      },
      DARK_FOREST("dark_forest") {
         @Override
         public int modifyColor(double x, double z, int baseColor) {
            return ARGB.opaque((baseColor & 16711422) + 2634762 >> 1);
         }
      },
      SWAMP("swamp") {
         @Override
         public int modifyColor(double x, double z, int baseColor) {
            double groundValue = Biome.BIOME_INFO_NOISE.get(x * 0.0225, z * 0.0225);
            return groundValue < -0.1 ? -11766212 : -9801671;
         }
      };

      private final String name;
      private final BiomeSpecialEffects.GrassColorModifier.ColorModifier delegate;
      public static final Codec<BiomeSpecialEffects.GrassColorModifier> CODEC = StringRepresentable.fromEnum(BiomeSpecialEffects.GrassColorModifier::values);

      public int modifyColor(double x, double z, int baseColor) {
         return this.delegate.modifyGrassColor(x, z, baseColor);
      }

      @ReservedConstructor
      private GrassColorModifier(String name) {
         this.name = name;
         this.delegate = null;
      }

      private GrassColorModifier(String name, BiomeSpecialEffects.GrassColorModifier.ColorModifier delegate) {
         this.name = name;
         this.delegate = delegate;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static ExtensionInfo getExtensionInfo() {
         return ExtensionInfo.nonExtended(BiomeSpecialEffects.GrassColorModifier.class);
      }

      @FunctionalInterface
      public interface ColorModifier {
         int modifyGrassColor(double var1, double var3, int var5);
      }
   }
}
