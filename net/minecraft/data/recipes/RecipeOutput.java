package net.minecraft.data.recipes;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.worldgen.BootstrapContextAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IRecipeOutputExtension;
import org.jspecify.annotations.Nullable;

public interface RecipeOutput extends BootstrapContextAccess, IRecipeOutputExtension {
   default void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
      this.accept(id, recipe, advancement, new ICondition[0]);
   }

   Advancement.Builder advancement();
}
