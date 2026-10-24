package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.common.data.RegistriesWithConditions;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import org.jspecify.annotations.Nullable;

@Deprecated
public class RegistriesDatapackGenerator implements DataProvider {
   private final PackOutput output;
   private final String name;
   private final Collection<RegistryDataLoader.RegistryData<?>> registryData;
   private final CompletableFuture<HolderLookup.Provider> registries;
   private final Predicate<String> namespacePredicate;
   private final CompletableFuture<RegistriesWithConditions> registriesWithConditions;

   @Deprecated
   public RegistriesDatapackGenerator(
      PackOutput output, String name, Collection<RegistryDataLoader.RegistryData<?>> registryData, CompletableFuture<HolderLookup.Provider> registryContents
   ) {
      this(output, name, registryData, registryContents.thenApply(RegistriesWithConditions::new), null);
   }

   public RegistriesDatapackGenerator(
      PackOutput output,
      String name,
      Collection<RegistryDataLoader.RegistryData<?>> registryData,
      CompletableFuture<RegistriesWithConditions> registryContents,
      @Nullable Set<String> modIds
   ) {
      this.name = name;
      this.registryData = registryData;
      this.registries = registryContents.thenApply(RegistriesWithConditions::registries);
      this.output = output;
      this.namespacePredicate = modIds == null ? var0 -> true : modIds::contains;
      this.registriesWithConditions = registryContents;
   }

   public static RegistriesDatapackGenerator forWorldLayer(PackOutput output, CompletableFuture<HolderLookup.Provider> registryContents) {
      return new RegistriesDatapackGenerator(output, "world", DataPackRegistriesHooks.getWorldRegistriesWithDimensions().toList(), registryContents);
   }

   public static DataProvider forReloadableLayer(PackOutput output, CompletableFuture<HolderLookup.Provider> registryContents) {
      return new RegistriesDatapackGenerator(output, "reloadable", RegistryDataLoader.RELOADABLE_REGISTRIES, registryContents);
   }

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      return this.registriesWithConditions
         .thenCompose(
            access -> {
               DynamicOps<JsonElement> registryOps = access.createSerializationContext(JsonOps.INSTANCE);
               return CompletableFuture.allOf(
                  this.registryData
                     .stream()
                     .flatMap(v -> this.dumpRegistryCap(cache, access, registryOps, (RegistryDataLoader.RegistryData<?>)v).stream())
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   @Deprecated
   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
      CachedOutput cache, HolderLookup.Provider registries, DynamicOps<JsonElement> writeOps, RegistryDataLoader.RegistryData<T> v
   ) {
      return this.dumpRegistryCap(cache, new RegistriesWithConditions(registries), writeOps, v);
   }

   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
      CachedOutput cache, RegistriesWithConditions registries, DynamicOps<JsonElement> writeOps, RegistryDataLoader.RegistryData<T> v
   ) {
      ResourceKey<? extends Registry<T>> registryKey = v.key();
      Codec<Optional<WithConditions<T>>> conditionalCodec = ConditionalOps.createConditionalCodecWithConditions(v.elementCodec());
      Map<ResourceKey<?>, List<ICondition>> conditions = registries.conditionsByRegistry().getOrDefault(registryKey, Map.of());
      return registries.lookup(registryKey)
         .map(
            registry -> {
               PackOutput.PathProvider pathProvider = this.output.createRegistryElementsPathProvider(registryKey);
               return CompletableFuture.allOf(
                  registry.listElements()
                     .filter(holder -> this.namespacePredicate.test(holder.key().identifier().getNamespace()))
                     .map(
                        e -> dumpValue(
                           pathProvider.json(e.key().identifier()),
                           cache,
                           writeOps,
                           conditionalCodec,
                           Optional.of(new WithConditions(conditions.getOrDefault(e.key(), List.of()), e.value()))
                        )
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(
      Path path, CachedOutput cache, DynamicOps<JsonElement> ops, Encoder<Optional<WithConditions<E>>> codec, Optional<WithConditions<E>> value
   ) {
      return (CompletableFuture<?>)codec.encodeStart(ops, value)
         .mapOrElse(
            result -> DataProvider.saveStable(cache, result, path),
            error -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + path + "': " + error.message()))
         );
   }

   @Override
   public String getName() {
      return "Registries for " + this.name;
   }
}
