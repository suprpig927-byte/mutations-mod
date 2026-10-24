package net.minecraft.client.renderer.block.dispatch;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.BlockStateModelExtension;
import net.neoforged.neoforge.client.model.block.BlockStateModelHooks;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.ApiStatus.Internal;

@OnlyIn(Dist.CLIENT)
public interface BlockStateModel extends BlockStateModelExtension {
   @Deprecated
   void collectParts(RandomSource var1, List<BlockStateModelPart> var2);

   @Deprecated
   Material.Baked particleMaterial();

   @Deprecated
   @BakedQuad.MaterialFlags
   int materialFlags();

   @Deprecated
   default boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag) {
      return (this.materialFlags() & flag) != 0;
   }

   public static class SimpleCachedUnbakedRoot implements BlockStateModel.UnbakedRoot {
      private final BlockStateModel.Unbaked contents;
      private final ModelBaker.SharedOperationKey<BlockStateModel> bakingKey = new ModelBaker.SharedOperationKey<BlockStateModel>() {
         {
            Objects.requireNonNull(SimpleCachedUnbakedRoot.this);
         }

         public BlockStateModel compute(ModelBaker modelBakery) {
            return SimpleCachedUnbakedRoot.this.contents.bake(modelBakery);
         }
      };

      public SimpleCachedUnbakedRoot(BlockStateModel.Unbaked contents) {
         this.contents = contents;
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
         this.contents.resolveDependencies(resolver);
      }

      @Override
      public BlockStateModel bake(BlockState blockState, ModelBaker modelBakery) {
         return modelBakery.compute(this.bakingKey);
      }

      @Override
      public Object visualEqualityGroup(BlockState blockState) {
         return this;
      }
   }

   public interface Unbaked extends ResolvableModel {
      Codec<Weighted<Variant>> ELEMENT_CODEC = RecordCodecBuilder.create(
         i -> i.group(Variant.MAP_CODEC.forGetter(Weighted::value), ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight))
            .apply(i, Weighted::new)
      );
      @Internal
      Codec<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>> SINGLE_MODEL_CODEC = BlockStateModelHooks.makeSingleModelCodec().codec();
      @Internal
      Codec<Weighted<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>>> WEIGHTED_MODEL_CODEC = BlockStateModelHooks.makeElementCodec();
      Codec<WeightedVariants.Unbaked> HARDCODED_WEIGHTED_CODEC = ExtraCodecs.nonEmptyList(WEIGHTED_MODEL_CODEC.listOf())
         .flatComapMap(
            w -> new WeightedVariants.Unbaked(WeightedList.of(Lists.transform(w, e -> e.map(either -> (BlockStateModel.Unbaked)either.map(m -> m, m -> m))))),
            unbaked -> {
               List<Weighted<BlockStateModel.Unbaked>> entries = unbaked.entries().unwrap();
               List<Weighted<Either<CustomUnbakedBlockStateModel, SingleVariant.Unbaked>>> result = new ArrayList<>(entries.size());

               for (Weighted<BlockStateModel.Unbaked> entry : entries) {
                  switch ((BlockStateModel.Unbaked)entry.value()) {
                     case CustomUnbakedBlockStateModel customModel:
                        result.add(new Weighted<>(Either.left(customModel), entry.weight()));
                        break;
                     case SingleVariant.Unbaked singlevariant$unbaked:
                        result.add(new Weighted<>(Either.right(new SingleVariant.Unbaked(singlevariant$unbaked.variant())), entry.weight()));
                        break;
                     default:
                        return DataResult.error(() -> "Only custom models or single variants are supported");
                  }
               }

               return DataResult.success(result);
            }
         );
      Codec<BlockStateModel.Unbaked> CODEC = Codec.either(HARDCODED_WEIGHTED_CODEC, SINGLE_MODEL_CODEC)
         .flatComapMap(v -> (BlockStateModel.Unbaked)v.map(l -> l, r -> (BlockStateModel.Unbaked)r.map(m -> m, m -> m)), o -> {
            return switch (o) {
               case CustomUnbakedBlockStateModel customModel -> DataResult.success(Either.right(Either.left(customModel)));
               case SingleVariant.Unbaked single -> DataResult.success(Either.right(Either.right(single)));
               case WeightedVariants.Unbaked multiple -> DataResult.success(Either.left(multiple));
               default -> DataResult.error(() -> "Only a custom model or a single variant or a list of variants are supported");
            };
         });

      BlockStateModel bake(ModelBaker var1);

      default BlockStateModel.UnbakedRoot asRoot() {
         return new BlockStateModel.SimpleCachedUnbakedRoot(this);
      }
   }

   public interface UnbakedRoot extends ResolvableModel {
      BlockStateModel bake(BlockState var1, ModelBaker var2);

      Object visualEqualityGroup(BlockState var1);
   }
}
