package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
   default void accept(Item item, ItemModel.Unbaked generator) {
      this.accept(item, generator, ClientItem.Properties.DEFAULT);
   }

   void accept(Item var1, ItemModel.Unbaked var2, ClientItem.Properties var3);

   void copy(Item var1, Item var2);

   default void register(Item item, ClientItem clientItem) {
   }

   default void register(Identifier identifier, ClientItem clientItem) {
   }
}
