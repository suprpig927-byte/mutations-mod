package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ParallelMapTransform;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.common.conditions.ConditionContext;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

public class ResourceManagerRegistryLoadTask<T> extends RegistryLoadTask<T> {
   private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize(knownPack -> {
      Lifecycle lifecycle = knownPack.map(KnownPack::isVanilla).map(info -> Lifecycle.stable()).orElse(Lifecycle.experimental());
      return new RegistrationInfo(knownPack, lifecycle);
   });
   private final ResourceManager resourceManager;
   private final List<Registry.PendingTags<?>> pendingTags;

   @Deprecated
   public ResourceManagerRegistryLoadTask(
      RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors, ResourceManager resourceManager
   ) {
      this(data, lifecycle, loadingErrors, resourceManager, List.of());
   }

   public ResourceManagerRegistryLoadTask(
      RegistryDataLoader.RegistryData<T> data,
      Lifecycle lifecycle,
      Map<ResourceKey<?>, Exception> loadingErrors,
      ResourceManager resourceManager,
      List<Registry.PendingTags<?>> pendingTags
   ) {
      super(data, lifecycle, loadingErrors);
      this.resourceManager = resourceManager;
      this.pendingTags = pendingTags;
   }

   @Override
   public CompletableFuture<?> load(RegistryOps.RegistryInfoLookup context, Executor executor) {
      FileToIdConverter lister = FileToIdConverter.registry(this.registryKey());
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> lister.listMatchingResources(this.resourceManager), executor)
         .thenCompose(
            registryResources -> {
               ConditionContext conditionContext = new ConditionContext(this.pendingTags, context, FeatureFlags.VANILLA_SET);
               RegistryOps<JsonElement> ops = new ConditionalOps(RegistryOps.create(JsonOps.INSTANCE, context), conditionContext);
               return ParallelMapTransform.schedule(
                  (Map<Identifier, Resource>)registryResources,
                  (resourceId, thunk) -> {
                     ResourceKey<T> elementKey = ResourceKey.create(this.registryKey(), lister.fileToId(resourceId));
                     RegistrationInfo registrationInfo = REGISTRATION_INFO_CACHE.apply(thunk.knownPackInfo());
                     return new RegistryLoadTask.PendingRegistration<>(
                        elementKey, RegistryLoadTask.PendingRegistration.loadFromResource(this.data.elementCodec(), ops, elementKey, thunk), registrationInfo
                     );
                  },
                  executor
               );
            }
         )
         .thenAcceptAsync(
            loadedEntries -> {
               this.registerElements(loadedEntries.entrySet().stream().sorted(Entry.comparingByKey()).map(Entry::getValue));
               TagLoader.ElementLookup<Holder<T>> tagElementLookup = TagLoader.ElementLookup.fromGetters(
                  this.registryKey(), this.concurrentRegistrationGetter, this.readOnlyRegistry()
               );
               Map<TagKey<T>, List<Holder<T>>> pendingTags = TagLoader.loadTagsForRegistry(this.resourceManager, this.registryKey(), tagElementLookup);
               this.registerTags(pendingTags);
            },
            executor
         );
   }
}
