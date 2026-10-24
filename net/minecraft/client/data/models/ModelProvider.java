package net.minecraft.client.data.models;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IModelProviderExtension;

@OnlyIn(Dist.CLIENT)
public class ModelProvider implements DataProvider, IModelProviderExtension {
   private final PackOutput.PathProvider blockStatePathProvider;
   private final PackOutput.PathProvider itemInfoPathProvider;
   private final PackOutput.PathProvider modelPathProvider;
   public final String modId;

   @Deprecated
   public ModelProvider(PackOutput output) {
      this(output, "minecraft");
   }

   public ModelProvider(PackOutput output, String modId) {
      this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
      this.itemInfoPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
      this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
      this.modId = modId;
   }

   protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
      blockModels.run();
      itemModels.run();
   }

   protected Stream<? extends Holder<Block>> getKnownBlocks() {
      return BuiltInRegistries.BLOCK.listElements().filter(holder -> holder.key().identifier().getNamespace().equals(this.modId));
   }

   protected Stream<? extends Holder<Item>> getKnownItems() {
      return BuiltInRegistries.ITEM.listElements().filter(holder -> holder.key().identifier().getNamespace().equals(this.modId));
   }

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      ModelProvider.ItemInfoCollector itemModels = new ModelProvider.ItemInfoCollector(this::getKnownItems);
      ModelProvider.BlockStateGeneratorCollector blockStateGenerators = new ModelProvider.BlockStateGeneratorCollector(this::getKnownBlocks);
      ModelProvider.SimpleModelCollector simpleModels = new ModelProvider.SimpleModelCollector();
      this.registerModels(new BlockModelGenerators(blockStateGenerators, itemModels, simpleModels), new ItemModelGenerators(itemModels, simpleModels));
      blockStateGenerators.validate();
      itemModels.finalizeAndValidate();
      return CompletableFuture.allOf(
         blockStateGenerators.save(cache, this.blockStatePathProvider),
         simpleModels.save(cache, this.modelPathProvider),
         itemModels.save(cache, this.itemInfoPathProvider)
      );
   }

   @Override
   public String getName() {
      return "Model Definitions - " + this.modId;
   }

   private static class BlockStateGeneratorCollector implements Consumer<BlockModelDefinitionGenerator> {
      private final Map<Block, BlockModelDefinitionGenerator> generators = new HashMap<>();
      private final Supplier<Stream<? extends Holder<Block>>> knownBlocks;

      public BlockStateGeneratorCollector(Supplier<Stream<? extends Holder<Block>>> knownBlocks) {
         this.knownBlocks = knownBlocks;
      }

      @Deprecated
      public BlockStateGeneratorCollector() {
         this(BuiltInRegistries.BLOCK::listElements);
      }

      public void accept(BlockModelDefinitionGenerator generator) {
         Block block = generator.block();
         BlockModelDefinitionGenerator prev = this.generators.put(block, generator);
         if (prev != null) {
            throw new IllegalStateException("Duplicate blockstate definition for " + block);
         }
      }

      public void validate() {
         Stream<? extends Holder<Block>> holders = this.knownBlocks.get();
         List<Identifier> missingDefinitions = holders.filter(e -> !this.generators.containsKey(e.value()))
            .map(e -> e.unwrapKey().orElseThrow().identifier())
            .toList();
         if (!missingDefinitions.isEmpty()) {
            throw new IllegalStateException("Missing blockstate definitions for: " + missingDefinitions);
         }
      }

      public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
         Map<Block, BlockStateModelDispatcher> definitions = Maps.transformValues(this.generators, BlockModelDefinitionGenerator::create);
         Function<Block, Path> pathGetter = block -> pathProvider.json(block.builtInRegistryHolder().key().identifier());
         return DataProvider.saveAll(cache, BlockStateModelDispatcher.CODEC, pathGetter, definitions);
      }
   }

   private static class ItemInfoCollector implements ItemModelOutput {
      private final Map<Item, ClientItem> itemInfos = new HashMap<>();
      private final Map<Item, Item> copies = new HashMap<>();
      private final Supplier<Stream<? extends Holder<Item>>> knownItems;
      private final Map<Identifier, ClientItem> idItemInfos = new HashMap<>();

      public ItemInfoCollector(Supplier<Stream<? extends Holder<Item>>> knownItems) {
         this.knownItems = knownItems;
      }

      @Deprecated
      public ItemInfoCollector() {
         this(BuiltInRegistries.ITEM::listElements);
      }

      @Override
      public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties) {
         this.register(item, new ClientItem(model, properties));
      }

      @Override
      public void register(Item item, ClientItem itemInfo) {
         ClientItem prev = this.itemInfos.put(item, itemInfo);
         if (prev != null) {
            throw new IllegalStateException("Duplicate item model definition for " + item);
         }
      }

      @Override
      public void register(Identifier identifier, ClientItem clientItem) {
         ClientItem existing = this.idItemInfos.putIfAbsent(identifier, clientItem);
         if (existing != null) {
            throw new IllegalStateException("Duplicate item model definition for " + identifier);
         }
      }

      @Override
      public void copy(Item donor, Item acceptor) {
         this.copies.put(acceptor, donor);
      }

      public void finalizeAndValidate() {
         this.knownItems.get().map(Holder::value).forEach(item -> {
            if (!this.copies.containsKey(item) && item instanceof BlockItem blockItem && !this.itemInfos.containsKey(blockItem)) {
               Identifier targetModel = ModelLocationUtils.getModelLocation(blockItem.getBlock());
               this.accept(blockItem, ItemModelUtils.plainModel(targetModel));
            }
         });
         this.copies.forEach((acceptor, donor) -> {
            ClientItem donorInfo = this.itemInfos.get(donor);
            if (donorInfo == null) {
               throw new IllegalStateException("Missing donor: " + donor + " -> " + acceptor);
            } else {
               this.register(acceptor, donorInfo);
            }
         });
         List<Identifier> missingDefinitions = this.knownItems
            .get()
            .filter(e -> !this.itemInfos.containsKey(e.value()))
            .map(e -> e.unwrapKey().orElseThrow().identifier())
            .toList();
         if (!missingDefinitions.isEmpty()) {
            throw new IllegalStateException("Missing item model definitions for: " + missingDefinitions);
         }
      }

      public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
         return CompletableFuture.allOf(
            DataProvider.saveAll(cache, ClientItem.CODEC, item -> pathProvider.json(item.builtInRegistryHolder().key().identifier()), this.itemInfos),
            DataProvider.saveAll(cache, ClientItem.CODEC, pathProvider::json, this.idItemInfos)
         );
      }
   }

   private static class SimpleModelCollector implements BiConsumer<Identifier, ModelInstance> {
      private final Map<Identifier, ModelInstance> models = new HashMap<>();

      public void accept(Identifier id, ModelInstance contents) {
         Supplier<JsonElement> prev = this.models.put(id, contents);
         if (prev != null) {
            throw new IllegalStateException("Duplicate model definition for " + id);
         }
      }

      public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
         return DataProvider.saveAll(cache, Supplier::get, pathProvider::json, this.models);
      }
   }
}
