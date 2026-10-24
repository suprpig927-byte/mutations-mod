package net.minecraft.client.renderer.block.dispatch.multipart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MultiPartModel implements BlockStateModel, DynamicBlockStateModel {
   private final MultiPartModel.SharedBakedState shared;
   private final BlockState blockState;
   @Nullable
   private List<BlockStateModel> models;

   private MultiPartModel(MultiPartModel.SharedBakedState shared, BlockState blockState) {
      this.shared = shared;
      this.blockState = blockState;
   }

   @Override
   public Material.Baked particleMaterial() {
      return this.shared.particleMaterial;
   }

   @BakedQuad.MaterialFlags
   @Override
   public int materialFlags() {
      return this.shared.materialFlags;
   }

   public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      long seed = random.nextLong();

      for (BlockStateModel model : this.models) {
         random.setSeed(seed);
         model.collectParts(level, pos, state, random, output);
      }
   }

   @Nullable
   public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      long seed = random.nextLong();
      if (this.models.size() == 1) {
         random.setSeed(seed);
         return this.models.getFirst().createGeometryKey(level, pos, state, random);
      } else {
         List<Object> subKeys = new ArrayList<>(this.models.size());

         for (BlockStateModel model : this.models) {
            random.setSeed(seed);
            Object subKey = model.createGeometryKey(level, pos, state, random);
            if (subKey == null) {
               return null;
            }

            subKeys.add(subKey);
         }

         return new MultiPartModel.GeometryKey(subKeys, this);
      }
   }

   public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return ((BlockStateModel)this.shared.selectors.getFirst().model).particleMaterial(level, pos, state);
   }

   @BakedQuad.MaterialFlags
   public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      if (this.models == null) {
         this.models = this.shared.selectModels(this.blockState);
      }

      int flags = 0;

      for (BlockStateModel model : this.models) {
         flags |= model.materialFlags(level, pos, state);
      }

      return flags;
   }

   private record GeometryKey(List<Object> subKeys, MultiPartModel multiPart) {
   }

   public record Selector<T>(Predicate<BlockState> condition, T model) {
      public <S> MultiPartModel.Selector<S> with(S newModel) {
         return new MultiPartModel.Selector<>(this.condition, newModel);
      }
   }

   private static final class SharedBakedState {
      private final List<MultiPartModel.Selector<BlockStateModel>> selectors;
      private final Material.Baked particleMaterial;
      @BakedQuad.MaterialFlags
      private final int materialFlags;
      private final Map<BitSet, List<BlockStateModel>> subsets = new ConcurrentHashMap<>();

      private static BlockStateModel getFirstModel(List<MultiPartModel.Selector<BlockStateModel>> selectors) {
         if (selectors.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one selector");
         } else {
            return selectors.getFirst().model();
         }
      }

      @BakedQuad.MaterialFlags
      private static int computeMaterialFlags(List<MultiPartModel.Selector<BlockStateModel>> selectors) {
         int flags = 0;

         for (MultiPartModel.Selector<BlockStateModel> selector : selectors) {
            flags |= selector.model.materialFlags();
         }

         return flags;
      }

      public SharedBakedState(List<MultiPartModel.Selector<BlockStateModel>> selectors) {
         this.selectors = selectors;
         BlockStateModel firstModel = getFirstModel(selectors);
         this.particleMaterial = firstModel.particleMaterial();
         this.materialFlags = computeMaterialFlags(selectors);
      }

      public List<BlockStateModel> selectModels(BlockState state) {
         BitSet selectedModels = new BitSet();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(state)) {
               selectedModels.set(i);
            }
         }

         return this.subsets.computeIfAbsent(selectedModels, selected -> {
            Builder<BlockStateModel> result = ImmutableList.builder();

            for (int ix = 0; ix < this.selectors.size(); ix++) {
               if (selected.get(ix)) {
                  result.add(this.selectors.get(ix).model);
               }
            }

            return result.build();
         });
      }
   }

   public static class Unbaked implements BlockStateModel.UnbakedRoot {
      private final List<MultiPartModel.Selector<BlockStateModel.Unbaked>> selectors;
      private final ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState> sharedStateKey = new ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState>(
         
      ) {
         {
            Objects.requireNonNull(Unbaked.this);
         }

         public MultiPartModel.SharedBakedState compute(ModelBaker modelBakery) {
            Builder<MultiPartModel.Selector<BlockStateModel>> selectors = ImmutableList.builderWithExpectedSize(Unbaked.this.selectors.size());

            for (MultiPartModel.Selector<BlockStateModel.Unbaked> selector : Unbaked.this.selectors) {
               selectors.add(selector.with(selector.model.bake(modelBakery)));
            }

            return new MultiPartModel.SharedBakedState(selectors.build());
         }
      };

      public Unbaked(List<MultiPartModel.Selector<BlockStateModel.Unbaked>> selectors) {
         this.selectors = selectors;
      }

      @Override
      public Object visualEqualityGroup(BlockState blockState) {
         IntList triggeredSelectors = new IntArrayList();

         for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).condition.test(blockState)) {
               triggeredSelectors.add(i);
            }
         }

         record Key(MultiPartModel.Unbaked model, IntList selectors) {
         }

         return new Key(this, triggeredSelectors);
      }

      @Override
      public void resolveDependencies(ResolvableModel.Resolver resolver) {
         this.selectors.forEach(s -> s.model.resolveDependencies(resolver));
      }

      @Override
      public BlockStateModel bake(BlockState blockState, ModelBaker modelBakery) {
         MultiPartModel.SharedBakedState shared = modelBakery.compute(this.sharedStateKey);
         return new MultiPartModel(shared, blockState);
      }
   }
}
