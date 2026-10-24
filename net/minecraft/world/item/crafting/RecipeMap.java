package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RecipeMap {
   public static final RecipeMap EMPTY = new RecipeMap(ImmutableMultimap.of(), Map.of());
   private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
   private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

   private RecipeMap(Multimap<RecipeType<?>, RecipeHolder<?>> byType, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey) {
      this.byType = byType;
      this.byKey = byKey;
   }

   public static RecipeMap create(HolderLookup<Recipe<?>> recipes) {
      Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
      com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
      recipes.listElements().forEach(recipe -> {
         RecipeHolder<? extends Recipe<?>> legacyRecipeHolder = new RecipeHolder<>(recipe.key(), recipe.value());
         byType.put(recipe.value().getType(), legacyRecipeHolder);
         byKey.put(recipe.key(), legacyRecipeHolder);
      });
      return new RecipeMap(byType.build(), byKey.build());
   }

   public static RecipeMap createClient(List<RecipeHolder<?>> recipes) {
      Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
      com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();

      for (RecipeHolder<?> recipe : recipes) {
         byType.put(recipe.value().getType(), recipe);
         byKey.put(recipe.id(), recipe);
      }

      return new RecipeMap(byType.build(), byKey.build());
   }

   public void order(Object2IntMap<ResourceKey<Recipe<?>>> recipePriorities) {
      Int2ObjectRBTreeMap<List<RecipeHolder<?>>> priorityBuilder = new Int2ObjectRBTreeMap();
      LinkedListMultimap<RecipeType<?>, RecipeHolder<?>> finalBuilder = LinkedListMultimap.create();

      for (RecipeHolder<?> recipeholder : this.byKey.values()) {
         int priority = recipePriorities.getOrDefault(recipeholder.id(), 0);
         ((List)priorityBuilder.computeIfAbsent(priority, p -> new ArrayList())).add(recipeholder);
      }

      for (List<RecipeHolder<?>> list : priorityBuilder.reversed().values()) {
         for (RecipeHolder<?> recipeHolder : list) {
            finalBuilder.put(recipeHolder.value().getType(), recipeHolder);
         }
      }

      this.byType = ImmutableMultimap.copyOf(finalBuilder);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type) {
      return this.byType.get(type);
   }

   public Collection<RecipeHolder<?>> values() {
      return this.byKey.values();
   }

   @Nullable
   public RecipeHolder<?> byKey(ResourceKey<Recipe<?>> recipeId) {
      return this.byKey.get(recipeId);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> type, I container, Level level) {
      return container.isEmpty() ? Stream.empty() : this.byType(type).stream().filter(r -> r.value().matches(container, level));
   }
}
