package net.minecraft.data.loot;

import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContextAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.extensions.LootTableSubProviderContextExtension;

@FunctionalInterface
public interface LootTableSubProvider {
   void run();

   public interface Context extends BootstrapContextAccess, LootTableSubProviderContextExtension {
      Holder.Reference<LootTable> accept(ResourceKey<LootTable> var1, LootTable.Builder var2);
   }

   public interface Factory {
      LootTableSubProvider create(LootTableSubProvider.Context var1);
   }
}
