package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BuiltInBlockModels;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.LoadedBlockModels;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations;
import net.neoforged.neoforge.client.model.UnbakedModelParser;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelManager implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
   private Map<Identifier, ItemModel> bakedItemStackModels = Map.of();
   private Map<Identifier, ClientItem.Properties> itemProperties = Map.of();
   private final AtlasManager atlasManager;
   private final PlayerSkinRenderCache playerSkinRenderCache;
   private final BlockColors blockColors;
   private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
   private ModelBakery.MissingModels missingModels;
   @Nullable
   private BlockStateModelSet blockStateModelSet;
   @Nullable
   private BlockModelSet blockModelSet;
   @Nullable
   private FluidStateModelSet fluidStateModelSet;
   private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
   private final AtomicReference<ModelBakery> modelBakery = new AtomicReference<>(null);
   private BakedModels bakedStandaloneModels;
   private Set<Identifier> reportedMissingItemModels = new HashSet<>();

   public ModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinRenderCache playerSkinRenderCache) {
      this.blockColors = blockColors;
      this.atlasManager = atlasManager;
      this.playerSkinRenderCache = playerSkinRenderCache;
   }

   public ItemModel getItemModel(Identifier id) {
      ItemModel model = this.bakedItemStackModels.get(id);
      if (model == null) {
         if (this.reportedMissingItemModels.add(id)) {
            LOGGER.warn("Missing item model for location {}", id);
         }

         return this.missingModels.item();
      } else {
         return model;
      }
   }

   public ClientItem.Properties getItemProperties(Identifier id) {
      return this.itemProperties.getOrDefault(id, ClientItem.Properties.DEFAULT);
   }

   public BlockStateModelSet getBlockStateModelSet() {
      return Objects.requireNonNull(this.blockStateModelSet, "Block models not yet initialized");
   }

   public BlockModelSet getBlockModelSet() {
      return Objects.requireNonNull(this.blockModelSet, "Block models not yet initialized");
   }

   public FluidStateModelSet getFluidStateModelSet() {
      return Objects.requireNonNull(this.fluidStateModelSet, "Fluid models not yet initialized");
   }

   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState currentReload,
      Executor taskExecutor,
      PreparableReloadListener.PreparationBarrier preparationBarrier,
      Executor reloadExecutor
   ) {
      ResourceManager manager = currentReload.resourceManager();
      CompletableFuture<EntityModelSet> entityModelSet = CompletableFuture.supplyAsync(EntityModelSet::vanilla, taskExecutor);
      CompletableFuture<Map<Identifier, UnbakedModel>> modelCache = loadBlockModels(manager, taskExecutor);
      CompletableFuture<BlockStateModelLoader.LoadedModels> blockStateModels = BlockStateModelLoader.loadBlockStates(manager, taskExecutor);
      CompletableFuture<Map<BlockState, BlockModel.Unbaked>> blockModelContents = CompletableFuture.supplyAsync(
         () -> BuiltInBlockModels.createBlockModels(this.blockColors), taskExecutor
      );
      CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> itemStackModels = ClientItemInfoLoader.scheduleLoad(manager, taskExecutor);
      CompletableFuture<LoadedModels> standaloneModelsFuture = StandaloneModelLoader.load(taskExecutor);
      CompletableFuture<ModelManager.ResolvedModels> modelDiscovery = CompletableFuture.allOf(
            modelCache, blockStateModels, itemStackModels, standaloneModelsFuture
         )
         .thenApplyAsync(
            var3x -> discoverModelDependencies(modelCache.join(), blockStateModels.join(), itemStackModels.join(), standaloneModelsFuture.join()), taskExecutor
         );
      CompletableFuture<Object2IntMap<BlockState>> modelGroups = blockStateModels.thenApplyAsync(
         models -> buildModelGroups(this.blockColors, models), taskExecutor
      );
      AtlasManager.PendingStitchResults pendingStitches = currentReload.get(AtlasManager.PENDING_STITCH);
      CompletableFuture<SpriteLoader.Preparations> pendingBlockAtlasSprites = pendingStitches.get(AtlasIds.BLOCKS);
      CompletableFuture<SpriteLoader.Preparations> pendingItemAtlasSprites = pendingStitches.get(AtlasIds.ITEMS);
      CompletableFuture<LoadedBlockModels> blockModels = CompletableFuture.allOf(blockModelContents, entityModelSet)
         .thenApply(var3x -> new LoadedBlockModels(blockModelContents.join(), entityModelSet.join(), this.atlasManager, this.playerSkinRenderCache));
      PendingAnimations pendingAnimations = currentReload.get(AnimationLoader.STATE_KEY);
      return CompletableFuture.allOf(
            pendingBlockAtlasSprites,
            pendingItemAtlasSprites,
            modelDiscovery,
            modelGroups,
            blockStateModels,
            itemStackModels,
            entityModelSet,
            blockModels,
            modelCache,
            standaloneModelsFuture
         )
         .thenComposeAsync(
            var11x -> {
               SpriteLoader.Preparations blockAtlasSprites = pendingBlockAtlasSprites.join();
               SpriteLoader.Preparations itemAtlasSprites = pendingItemAtlasSprites.join();
               ModelManager.ResolvedModels resolvedModels = modelDiscovery.join();
               Object2IntMap<BlockState> groups = modelGroups.join();
               Set<Identifier> unreferencedModels = Sets.difference(modelCache.join().keySet(), resolvedModels.models.keySet());
               if (!unreferencedModels.isEmpty()) {
                  LOGGER.debug(
                     "Unreferenced models: \n{}", unreferencedModels.stream().sorted().map(modelId -> "\t" + modelId + "\n").collect(Collectors.joining())
                  );
               }

               ModelBakery bakery = new ModelBakery(
                  entityModelSet.join(),
                  this.atlasManager,
                  this.playerSkinRenderCache,
                  blockStateModels.join().models(),
                  itemStackModels.join().contents(),
                  resolvedModels.models(),
                  resolvedModels.missing(),
                  standaloneModelsFuture.join(),
                  pendingAnimations
               );
               this.modelBakery.set(bakery);
               return loadModels(
                  blockAtlasSprites, itemAtlasSprites, bakery, blockModels.join(), groups, entityModelSet.join(), taskExecutor, pendingAnimations
               );
            },
            taskExecutor
         )
         .thenCompose(preparationBarrier::wait)
         .thenAcceptAsync(this::apply, reloadExecutor);
   }

   private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(ResourceManager manager, Executor executor) {
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(manager), executor)
         .thenCompose(
            resources -> {
               List<CompletableFuture<Pair<Identifier, UnbakedModel>>> result = new ArrayList<>(resources.size());

               for (Entry<Identifier, Resource> resource : resources.entrySet()) {
                  result.add(CompletableFuture.supplyAsync(() -> {
                     Identifier modelId = MODEL_LISTER.fileToId(resource.getKey());

                     try {
                        Pair t$;
                        try (Reader reader = resource.getValue().openAsReader()) {
                           t$ = Pair.of(modelId, UnbakedModelParser.parse(reader));
                        }

                        return t$;
                     } catch (Exception var7) {
                        LOGGER.error("Failed to load model {}", resource.getKey(), var7);
                        return null;
                     }
                  }, executor));
               }

               return Util.sequence(result)
                  .thenApply(pairs -> pairs.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
            }
         );
   }

   @Deprecated
   private static ModelManager.ResolvedModels discoverModelDependencies(
      Map<Identifier, UnbakedModel> allModels, BlockStateModelLoader.LoadedModels blockStateModels, ClientItemInfoLoader.LoadedClientInfos itemInfos
   ) {
      return discoverModelDependencies(allModels, blockStateModels, itemInfos, LoadedModels.EMPTY);
   }

   private static ModelManager.ResolvedModels discoverModelDependencies(
      Map<Identifier, UnbakedModel> allModels,
      BlockStateModelLoader.LoadedModels blockStateModels,
      ClientItemInfoLoader.LoadedClientInfos itemInfos,
      LoadedModels standaloneModels
   ) {
      ModelManager.ResolvedModels var6;
      try (Zone ignored = Profiler.get().zone("dependencies")) {
         ModelDiscovery result = new ModelDiscovery(allModels, MissingCuboidModel.missingModel());
         result.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
         blockStateModels.models().values().forEach(result::addRoot);
         itemInfos.contents().values().forEach(info -> result.addRoot(info.model()));
         standaloneModels.models().values().forEach(result::addRoot);
         var6 = new ModelManager.ResolvedModels(result.missingModel(), result.resolve());
      }

      return var6;
   }

   @Deprecated
   private static CompletableFuture<ModelManager.ReloadState> loadModels(
      SpriteLoader.Preparations blockAtlas,
      SpriteLoader.Preparations itemAtlas,
      ModelBakery bakery,
      LoadedBlockModels blockModels,
      Object2IntMap<BlockState> modelGroups,
      EntityModelSet entityModelSet,
      Executor taskExecutor
   ) {
      return loadModels(blockAtlas, itemAtlas, bakery, blockModels, modelGroups, entityModelSet, taskExecutor, PendingAnimations.EMPTY);
   }

   private static CompletableFuture<ModelManager.ReloadState> loadModels(
      SpriteLoader.Preparations blockAtlas,
      SpriteLoader.Preparations itemAtlas,
      ModelBakery bakery,
      LoadedBlockModels blockModels,
      Object2IntMap<BlockState> modelGroups,
      EntityModelSet entityModelSet,
      Executor taskExecutor,
      PendingAnimations pendingAnimations
   ) {
      MaterialBaker materialBaker = new MaterialBaker(blockAtlas, itemAtlas);
      CompletableFuture<ModelBakery.BakingResult> bakedStateResults = bakery.bakeModels(materialBaker, taskExecutor);
      CompletableFuture<Map<BlockState, BlockModel>> bakedModelsFuture = bakedStateResults.thenCompose(
         bakingResult -> blockModels.bake(bakingResult::getBlockStateModel, bakingResult.missingModels().block(), taskExecutor, pendingAnimations)
      );
      return bakedStateResults.thenCombine(
         bakedModelsFuture,
         (bakingResult, bakedModels) -> {
            Map<Fluid, FluidModel> fluidModels = FluidStateModelSet.bake(materialBaker);

            try (Zone ignored = Profiler.get().zone("neoforge_modify_baking_result")) {
               ClientHooks.onModifyBakingResult(bakingResult, materialBaker, blockAtlas, itemAtlas, bakery);
            }

            materialBaker.logMissingTextures();
            Map<BlockState, BlockStateModel> modelByStateCache = createBlockStateToModelDispatch(
               bakingResult.blockStateModels(), bakingResult.missingModels().block()
            );
            return new ModelManager.ReloadState(
               bakingResult, modelGroups, modelByStateCache, (Map<BlockState, BlockModel>)bakedModels, fluidModels, entityModelSet
            );
         }
      );
   }

   private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> bakedModels, BlockStateModel missingModel) {
      Object var8;
      try (Zone ignored = Profiler.get().zone("block state dispatch")) {
         Map<BlockState, BlockStateModel> modelByStateCache = new IdentityHashMap<>(bakedModels);

         for (Block block : BuiltInRegistries.BLOCK) {
            block.getStateDefinition().getPossibleStates().forEach(state -> {
               if (bakedModels.putIfAbsent(state, missingModel) == null) {
                  LOGGER.warn("Missing model for variant: '{}'", state);
               }
            });
         }

         var8 = modelByStateCache;
      }

      return (Map<BlockState, BlockStateModel>)var8;
   }

   private static Object2IntMap<BlockState> buildModelGroups(BlockColors blockColors, BlockStateModelLoader.LoadedModels blockStateModels) {
      Object2IntMap var3;
      try (Zone ignored = Profiler.get().zone("block groups")) {
         var3 = ModelGroupCollector.build(blockColors, blockStateModels);
      }

      return var3;
   }

   private void apply(ModelManager.ReloadState preparations) {
      ModelBakery.BakingResult bakedModels = preparations.bakedModels;
      this.bakedItemStackModels = bakedModels.itemStackModels();
      this.itemProperties = bakedModels.itemProperties();
      this.modelGroups = preparations.modelGroups;
      this.missingModels = bakedModels.missingModels();
      this.blockStateModelSet = new BlockStateModelSet(preparations.blockStateModels, this.missingModels.block());
      this.blockModelSet = new BlockModelSet(this.blockStateModelSet, preparations.blockModels, this.blockColors);
      this.fluidStateModelSet = new FluidStateModelSet(preparations.fluidModels, this.missingModels.fluid());
      this.entityModelSet = preparations.entityModelSet;
      this.bakedStandaloneModels = bakedModels.standaloneModels();
      ClientHooks.onModelBake(this, bakedModels, this.modelBakery.get());
      this.reportedMissingItemModels = new HashSet<>();
   }

   public boolean requiresRender(BlockState oldState, BlockState newState) {
      if (oldState == newState) {
         return false;
      } else {
         int oldModelGroup = this.modelGroups.getInt(oldState);
         if (oldModelGroup != -1) {
            int newModelGroup = this.modelGroups.getInt(newState);
            if (oldModelGroup == newModelGroup) {
               FluidState oldFluidState = oldState.getFluidState();
               FluidState newFluidState = newState.getFluidState();
               return oldFluidState != newFluidState;
            }
         }

         return true;
      }
   }

   public Supplier<EntityModelSet> entityModels() {
      return () -> this.entityModelSet;
   }

   public ModelBakery getModelBakery() {
      return this.modelBakery.get();
   }

   @Nullable
   public <T> T getStandaloneModel(StandaloneModelKey<T> modelKey) {
      return (T)this.bakedStandaloneModels.get(modelKey);
   }

   public ModelBakery.MissingModels getMissingModels() {
      return this.missingModels;
   }

   private record ReloadState(
      ModelBakery.BakingResult bakedModels,
      Object2IntMap<BlockState> modelGroups,
      Map<BlockState, BlockStateModel> blockStateModels,
      Map<BlockState, BlockModel> blockModels,
      Map<Fluid, FluidModel> fluidModels,
      EntityModelSet entityModelSet
   ) {
   }

   private record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
   }
}
