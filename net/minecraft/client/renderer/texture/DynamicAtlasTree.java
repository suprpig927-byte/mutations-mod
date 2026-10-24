package net.minecraft.client.renderer.texture;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DynamicAtlasTree implements DynamicAtlasTreeSlot {
   private final int x;
   private final int y;
   private final int width;
   private final int height;
   @Nullable
   private DynamicAtlasTree left;
   @Nullable
   private DynamicAtlasTree right;
   private boolean occupied;

   public DynamicAtlasTree(int x, int y, int width, int height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   @Nullable
   public DynamicAtlasTreeSlot insert(int slotWidth, int slotHeight, int spacing) {
      return this.insertInner(slotWidth, slotHeight, spacing);
   }

   @Nullable
   private DynamicAtlasTree insertInner(int slotWidth, int slotHeight, int spacing) {
      if (this.left != null && this.right != null) {
         DynamicAtlasTree newNode = this.left.insertInner(slotWidth, slotHeight, spacing);
         if (newNode == null) {
            newNode = this.right.insertInner(slotWidth, slotHeight, spacing);
         }

         return newNode;
      } else if (this.occupied) {
         return null;
      } else if (slotWidth > this.width || slotHeight > this.height) {
         return null;
      } else if (slotWidth == this.width && slotHeight == this.height) {
         this.occupied = true;
         return this;
      } else {
         int deltaWidth = this.width - slotWidth;
         int deltaHeight = this.height - slotHeight;
         if (deltaWidth > deltaHeight) {
            this.left = new DynamicAtlasTree(this.x, this.y, slotWidth, this.height);
            this.right = new DynamicAtlasTree(this.x + slotWidth + spacing, this.y, this.width - slotWidth - spacing, this.height);
         } else {
            this.left = new DynamicAtlasTree(this.x, this.y, this.width, slotHeight);
            this.right = new DynamicAtlasTree(this.x, this.y + slotHeight + spacing, this.width, this.height - slotHeight - spacing);
         }

         return this.left.insertInner(slotWidth, slotHeight, spacing);
      }
   }

   @Override
   public int x() {
      return this.x;
   }

   @Override
   public int y() {
      return this.y;
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }
}
