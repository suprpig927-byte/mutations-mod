package net.minecraft.client.data.models;

import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

@OnlyIn(Dist.CLIENT)
public record MultiVariant(WeightedList<Variant> variants, WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
   public MultiVariant(WeightedList<Variant> variants, WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
      if (!customBlockStateModels.isEmpty()) {
         if (!variants.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify both a variant list and a custom blockstate model");
         }
      } else if (variants.isEmpty()) {
         throw new IllegalArgumentException("Variant list must contain at least one element");
      }

      this.variants = variants;
      this.customBlockStateModels = customBlockStateModels;
   }

   public MultiVariant(WeightedList<Variant> variants) {
      this(variants, WeightedList.of());
   }

   public static MultiVariant of(WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
      return new MultiVariant(WeightedList.of(), customBlockStateModels);
   }

   public static MultiVariant of(CustomBlockStateModelBuilder customBlockStateModel) {
      return new MultiVariant(WeightedList.of(), WeightedList.of(customBlockStateModel));
   }

   public MultiVariant with(VariantMutator mutator) {
      return !this.customBlockStateModels.isEmpty()
         ? new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(mutator)))
         : new MultiVariant(this.variants.map(mutator));
   }

   public MultiVariant with(UnbakedMutator mutator) {
      return !this.customBlockStateModels.isEmpty()
         ? new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(mutator)))
         : new MultiVariant(this.variants.map(v -> ((SingleVariant.Unbaked)mutator.apply(new SingleVariant.Unbaked(v))).variant()));
   }

   public BlockStateModel.Unbaked toUnbaked() {
      if (!this.customBlockStateModels.isEmpty()) {
         List<Weighted<CustomBlockStateModelBuilder>> builders = this.customBlockStateModels.unwrap();
         return (BlockStateModel.Unbaked)(builders.size() == 1
            ? builders.getFirst().value().toUnbaked()
            : new WeightedVariants.Unbaked(this.customBlockStateModels.map(CustomBlockStateModelBuilder::toUnbaked)));
      } else {
         List<Weighted<Variant>> entries = this.variants.unwrap();
         return (BlockStateModel.Unbaked)(entries.size() == 1
            ? new SingleVariant.Unbaked(entries.getFirst().value())
            : new WeightedVariants.Unbaked(this.variants.map(SingleVariant.Unbaked::new)));
      }
   }
}
