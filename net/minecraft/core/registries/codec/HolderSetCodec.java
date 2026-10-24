package net.minecraft.core.registries.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> tagKeyOrValuesCodec;
   private final Codec<ICustomHolderSet<E>> customDispatchCodec;
   private final Codec<Either<ICustomHolderSet<E>, Either<TagKey<E>, List<Holder<E>>>>> combinedCodec;

   private static <E> Codec<List<Holder<E>>> directCodec(Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
      Codec<List<Holder<E>>> listCodec = elementCodec.listOf();
      return alwaysUseList ? listCodec : ExtraCodecs.compactListCodec(elementCodec, listCodec);
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
      return new HolderSetCodec<>(registryKey, elementCodec, alwaysUseList);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
      this.registryKey = registryKey;
      this.tagKeyOrValuesCodec = Codec.either(TagKey.hashedCodec(registryKey), directCodec(elementCodec, alwaysUseList));
      this.customDispatchCodec = NeoForgeRegistries.HOLDER_SET_TYPES
         .byNameCodec()
         .dispatch(ICustomHolderSet::type, type -> type.makeCodec(registryKey, elementCodec, alwaysUseList));
      this.combinedCodec = Codec.either(this.customDispatchCodec, this.tagKeyOrValuesCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> ops, T input) {
      return this.combinedCodec
         .decode(ops, input)
         .flatMap(
            tagKeyOrValues -> {
               DataResult<HolderSet<E>> result = (DataResult<HolderSet<E>>)((Either)tagKeyOrValues.getFirst())
                  .map(
                     DataResult::success,
                     tagOrList -> (DataResult)tagOrList.map(
                        tagKey -> {
                           if (ops instanceof RegistryOps<T> registryOps) {
                              Optional<HolderGetter<E>> maybeRegistry = registryOps.getter(this.registryKey);
                              return maybeRegistry.isPresent()
                                 ? lookupTag(maybeRegistry.get(), tagKey)
                                 : DataResult.error(() -> "Registry " + this.registryKey.identifier() + " is not available in this context");
                           } else {
                              return DataResult.error(() -> "Registries are not available in this context");
                           }
                        },
                        values -> DataResult.success(HolderSet.direct(values))
                     )
                  );
               return result.map(holders -> Pair.of(holders, tagKeyOrValues.getSecond()));
            }
         );
   }

   private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> registry, TagKey<E> key) {
      return registry.get(key)
         .<DataResult<HolderSet<E>>>map(DataResult::success)
         .orElseGet(() -> DataResult.error(() -> "Missing tag: '" + key.location() + "' in '" + key.registry().identifier() + "'"));
   }

   public <T> DataResult<T> encode(HolderSet<E> input, DynamicOps<T> ops, T prefix) {
      if (input instanceof HolderSet.Named<E> named && ops instanceof RegistryOps<T> registryOps) {
         Optional<? extends HolderOwner<E>> maybeOwner = registryOps.getter(this.registryKey);
         if (!maybeOwner.isPresent()) {
            return DataResult.error(() -> "Registry " + this.registryKey.identifier() + " is not available in this context");
         } else {
            return !named.canSerializeIn((HolderOwner<T>)maybeOwner.get())
               ? DataResult.error(() -> "HolderSet " + named + " is not valid in current registry set")
               : this.tagKeyOrValuesCodec.encode(Either.left(named.key()), ops, prefix);
         }
      } else {
         return input instanceof ICustomHolderSet<E> customHolderSet
            ? this.customDispatchCodec.encode(customHolderSet, ops, prefix)
            : this.tagKeyOrValuesCodec.encode(Either.right(input.stream().toList()), ops, prefix);
      }
   }
}
