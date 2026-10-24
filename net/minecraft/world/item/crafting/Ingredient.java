package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.HolderSetCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientCodecs;
import org.jspecify.annotations.Nullable;

public final class Ingredient implements Predicate<ItemStack>, StackedContents.IngredientInfo<Holder<Item>> {
   public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = IngredientCodecs.streamCodec(
      ByteBufCodecs.holderSet(Registries.ITEM).map(Ingredient::new, i -> i.getValuesForSync())
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = IngredientCodecs.optionalStreamCodec(
      ByteBufCodecs.holderSet(Registries.ITEM)
         .map(
            ingredient -> ingredient.size() == 0 ? Optional.empty() : Optional.of(new Ingredient((HolderSet<Item>)ingredient)),
            ingredient -> ingredient.<HolderSet.Direct<Item>>map(i -> i.getValuesForSync()).orElse(HolderSet.direct())
         )
   );
   public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
   public static final Codec<Ingredient> CODEC = IngredientCodecs.codec(
      ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, i -> i.values)
   );
   private final HolderSet<Item> values;
   @Nullable
   private ICustomIngredient customIngredient = null;
   @Nullable
   private List<Holder<Item>> customIngredientValues;

   private Ingredient(HolderSet<Item> values) {
      if (values.isImmediatelyResolvable()) {
         values.unwrap().ifRight(directValues -> {
            if (directValues.isEmpty()) {
               throw new UnsupportedOperationException("Ingredients can't be empty");
            } else if (directValues.contains(Items.AIR.builtInRegistryHolder())) {
               throw new UnsupportedOperationException("Ingredient can't contain air");
            }
         });
      }

      this.values = values;
   }

   public Ingredient(ICustomIngredient customIngredient) {
      this.values = HolderSet.empty();
      this.customIngredient = customIngredient;
   }

   public static boolean testOptionalIngredient(Optional<Ingredient> ingredient, ItemStack stack) {
      return ingredient.<Boolean>map(value -> value.test(stack)).orElseGet(stack::isEmpty);
   }

   @Deprecated
   public Stream<Holder<Item>> items() {
      return this.customIngredient != null ? this.updateCustomIngredientValues().stream() : this.values.stream();
   }

   public boolean isEmpty() {
      return this.customIngredient != null ? this.updateCustomIngredientValues().isEmpty() : this.values.size() == 0;
   }

   public boolean test(ItemStack input) {
      return this.customIngredient != null ? this.customIngredient.test(input) : input.is(this.values);
   }

   public boolean acceptsItem(Holder<Item> item) {
      return this.customIngredient != null ? this.updateCustomIngredientValues().contains(item) : this.values.contains(item);
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Ingredient ingredient
         ? Objects.equals(this.customIngredient, ingredient.customIngredient) && Objects.equals(this.values, ingredient.values)
         : false;
   }

   @Override
   public int hashCode() {
      return this.customIngredient != null ? this.customIngredient.hashCode() : this.values.hashCode();
   }

   public HolderSet<Item> getValues() {
      if (this.isCustom()) {
         throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
      } else {
         return this.values;
      }
   }

   private HolderSet<Item> getValuesForSync() {
      return (HolderSet<Item>)(this.isCustom() ? HolderSet.direct(this.items().toList()) : this.values);
   }

   public boolean isSimple() {
      return this.customIngredient == null || this.customIngredient.isSimple();
   }

   @Nullable
   public ICustomIngredient getCustomIngredient() {
      return this.customIngredient;
   }

   public boolean isCustom() {
      return this.customIngredient != null;
   }

   private List<Holder<Item>> updateCustomIngredientValues() {
      if (this.customIngredientValues == null) {
         this.customIngredientValues = this.customIngredient.items().toList();
      }

      return this.customIngredientValues;
   }

   public static Ingredient of(ItemLike itemLike) {
      return new Ingredient(HolderSet.direct(itemLike.asItem().builtInRegistryHolder()));
   }

   public static Ingredient of(ItemLike... items) {
      return of(Arrays.stream(items));
   }

   public static Ingredient of(Stream<? extends ItemLike> stream) {
      return new Ingredient(HolderSet.direct(stream.map(e -> e.asItem().builtInRegistryHolder()).toList()));
   }

   public static Ingredient of(HolderSet<Item> tag) {
      return new Ingredient(tag);
   }

   public SlotDisplay display() {
      return (SlotDisplay)(this.customIngredient != null ? this.customIngredient.display() : new SlotDisplay.TagSlotDisplay(this.values));
   }

   public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> ingredient) {
      return ingredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
   }

   public static SlotDisplay displayForSingleItem(Holder<Item> item) {
      SlotDisplay inputDisplay = new SlotDisplay.ItemSlotDisplay(item);
      ItemStackTemplate remainderStack = item.value().getCraftingRemainder();
      if (remainderStack != null) {
         SlotDisplay remainderDisplay = new SlotDisplay.ItemStackSlotDisplay(remainderStack);
         return new SlotDisplay.WithRemainder(inputDisplay, remainderDisplay);
      } else {
         return inputDisplay;
      }
   }

   public Optional<Holder<Item>> getSingleItem() {
      return this.values.size() != 1 ? Optional.empty() : Optional.of(this.values.get(0));
   }
}
