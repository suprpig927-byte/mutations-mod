package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public class CreativeModeTab {
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
   private static final Identifier DEFAULT_BACKGROUND = createTextureLocation("items");
   private final Component displayName;
   private Identifier backgroundTexture = DEFAULT_BACKGROUND;
   private boolean canScroll = true;
   private boolean showTitle = true;
   private boolean alignedRight = false;
   private final CreativeModeTab.Row row;
   private final int column;
   private final CreativeModeTab.Type type;
   @Nullable
   private ItemStack iconItemStack;
   private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
   private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();
   private final Supplier<ItemStack> iconGenerator;
   private final CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;
   @Nullable
   private final Identifier scrollerSpriteLocation;
   private final boolean hasSearchBar;
   private final int searchBarWidth;
   private final int labelColor;
   public final List<Identifier> tabsBefore;
   public final List<Identifier> tabsAfter;

   private CreativeModeTab(
      CreativeModeTab.Row row,
      int column,
      CreativeModeTab.Type type,
      Component displayName,
      Supplier<ItemStack> iconGenerator,
      CreativeModeTab.DisplayItemsGenerator displayItemsGenerator,
      Identifier scrollerSpriteLocation,
      boolean hasSearchBar,
      int searchBarWidth,
      int labelColor,
      List<Identifier> tabsBefore,
      List<Identifier> tabsAfter
   ) {
      this.row = row;
      this.column = column;
      this.displayName = displayName;
      this.iconGenerator = iconGenerator;
      this.displayItemsGenerator = displayItemsGenerator;
      this.type = type;
      this.scrollerSpriteLocation = scrollerSpriteLocation;
      this.hasSearchBar = hasSearchBar;
      this.searchBarWidth = searchBarWidth;
      this.labelColor = labelColor;
      this.tabsBefore = List.copyOf(tabsBefore);
      this.tabsAfter = List.copyOf(tabsAfter);
   }

   protected CreativeModeTab(CreativeModeTab.Builder builder) {
      this(
         builder.row,
         builder.column,
         builder.type,
         builder.displayName,
         builder.iconGenerator,
         builder.displayItemsGenerator,
         builder.spriteScrollerLocation,
         builder.hasSearchBar,
         builder.searchBarWidth,
         builder.labelColor,
         builder.tabsBefore,
         builder.tabsAfter
      );
   }

   public static CreativeModeTab.Builder builder() {
      return new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0);
   }

   public static Identifier createTextureLocation(String name) {
      return Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + name + ".png");
   }

   @Deprecated
   public static CreativeModeTab.Builder builder(CreativeModeTab.Row row, int column) {
      return new CreativeModeTab.Builder(row, column);
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public ItemStack getIconItem() {
      if (this.iconItemStack == null) {
         this.iconItemStack = this.iconGenerator.get();
      }

      return this.iconItemStack;
   }

   public Identifier getBackgroundTexture() {
      return this.backgroundTexture;
   }

   public boolean showTitle() {
      return this.showTitle;
   }

   public boolean canScroll() {
      return this.canScroll;
   }

   public int column() {
      return this.column;
   }

   public CreativeModeTab.Row row() {
      return this.row;
   }

   public boolean hasAnyItems() {
      return !this.displayItems.isEmpty();
   }

   public boolean shouldDisplay() {
      return this.type != CreativeModeTab.Type.CATEGORY || this.hasAnyItems();
   }

   public boolean isAlignedRight() {
      return this.alignedRight;
   }

   public CreativeModeTab.Type getType() {
      return this.type;
   }

   public void buildContents(CreativeModeTab.ItemDisplayParameters parameters) {
      CreativeModeTab.ItemDisplayBuilder displayList = new CreativeModeTab.ItemDisplayBuilder(this, parameters.enabledFeatures);
      EventHooks.onCreativeModeTabBuildContents(this, this.displayItemsGenerator, parameters, displayList);
      this.displayItems = displayList.tabContents;
      this.displayItemsSearchTab = displayList.searchTabContents;
   }

   public Collection<ItemStack> getDisplayItems() {
      return this.displayItems;
   }

   public Collection<ItemStack> getSearchTabDisplayItems() {
      return this.displayItemsSearchTab;
   }

   public boolean contains(ItemStack stack) {
      return this.displayItemsSearchTab.contains(stack);
   }

   public boolean hasSearchBar() {
      return this.hasSearchBar;
   }

   public int getSearchBarWidth() {
      return this.searchBarWidth;
   }

   public int getLabelColor() {
      return this.labelColor;
   }

   public Identifier getScrollerSprite() {
      if (this.scrollerSpriteLocation == null) {
         return this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      } else {
         return this.scrollerSpriteLocation;
      }
   }

   public static class Builder {
      private static final CreativeModeTab.DisplayItemsGenerator EMPTY_GENERATOR = (parameters, output) -> {};
      private static final Identifier CREATIVE_ITEM_SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
      private final CreativeModeTab.Row row;
      private final int column;
      private Component displayName = Component.empty();
      private Supplier<ItemStack> iconGenerator = () -> ItemStack.EMPTY;
      private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator = EMPTY_GENERATOR;
      private boolean canScroll = true;
      private boolean showTitle = true;
      private boolean alignedRight = false;
      private CreativeModeTab.Type type = CreativeModeTab.Type.CATEGORY;
      private Identifier backgroundTexture = CreativeModeTab.DEFAULT_BACKGROUND;
      @Nullable
      private Identifier spriteScrollerLocation;
      private boolean hasSearchBar = false;
      private int searchBarWidth = 89;
      private int labelColor = -12566464;
      private Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory = CreativeModeTab::new;
      private final List<Identifier> tabsBefore = new ArrayList<>();
      private final List<Identifier> tabsAfter = new ArrayList<>();

      public Builder(CreativeModeTab.Row row, int column) {
         this.row = row;
         this.column = column;
      }

      public CreativeModeTab.Builder title(Component displayName) {
         this.displayName = displayName;
         return this;
      }

      public CreativeModeTab.Builder icon(Supplier<ItemStack> iconGenerator) {
         this.iconGenerator = iconGenerator;
         return this;
      }

      public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator displayItemsGenerator) {
         this.displayItemsGenerator = displayItemsGenerator;
         return this;
      }

      public CreativeModeTab.Builder alignedRight() {
         this.alignedRight = true;
         return this;
      }

      public CreativeModeTab.Builder hideTitle() {
         this.showTitle = false;
         return this;
      }

      public CreativeModeTab.Builder noScrollBar() {
         this.canScroll = false;
         return this;
      }

      protected CreativeModeTab.Builder type(CreativeModeTab.Type type) {
         this.type = type;
         return type == CreativeModeTab.Type.SEARCH ? this.withSearchBar() : this;
      }

      public CreativeModeTab.Builder backgroundTexture(Identifier backgroundTexture) {
         this.backgroundTexture = backgroundTexture;
         return this;
      }

      public CreativeModeTab.Builder withSearchBar() {
         this.hasSearchBar = true;
         return this.backgroundTexture == CreativeModeTab.DEFAULT_BACKGROUND ? this.backgroundTexture(CREATIVE_ITEM_SEARCH_BACKGROUND) : this;
      }

      public CreativeModeTab.Builder withSearchBar(int searchBarWidth) {
         this.searchBarWidth = searchBarWidth;
         return this.withSearchBar();
      }

      public CreativeModeTab.Builder withScrollBarSpriteLocation(Identifier scrollBarSpriteLocation) {
         this.spriteScrollerLocation = scrollBarSpriteLocation;
         return this;
      }

      public CreativeModeTab.Builder withLabelColor(int labelColor) {
         this.labelColor = labelColor;
         return this;
      }

      public CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory) {
         this.tabFactory = tabFactory;
         return this;
      }

      public CreativeModeTab.Builder withTabsBefore(Identifier... tabs) {
         this.tabsBefore.addAll(List.of(tabs));
         return this;
      }

      public CreativeModeTab.Builder withTabsAfter(Identifier... tabs) {
         this.tabsAfter.addAll(List.of(tabs));
         return this;
      }

      @SafeVarargs
      public final CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
         Stream.of(tabs).map(ResourceKey::identifier).forEach(this.tabsBefore::add);
         return this;
      }

      @SafeVarargs
      public final CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
         Stream.of(tabs).map(ResourceKey::identifier).forEach(this.tabsAfter::add);
         return this;
      }

      public CreativeModeTab.Builder displayItems(Collection<? extends Holder<? extends ItemLike>> collection) {
         return this.displayItems(
            (p, o) -> collection.stream()
               .map(Holder::value)
               .map(ItemLike::asItem)
               .filter(i -> i != Items.AIR)
               .filter(i -> i.isEnabled(p.enabledFeatures()))
               .forEach(o::accept)
         );
      }

      public CreativeModeTab build() {
         if ((this.type == CreativeModeTab.Type.HOTBAR || this.type == CreativeModeTab.Type.INVENTORY) && this.displayItemsGenerator != EMPTY_GENERATOR) {
            throw new IllegalStateException("Special tabs can't have display items");
         } else {
            CreativeModeTab tab = this.tabFactory.apply(this);
            tab.alignedRight = this.alignedRight;
            tab.showTitle = this.showTitle;
            tab.canScroll = this.canScroll;
            tab.backgroundTexture = this.backgroundTexture;
            return tab;
         }
      }
   }

   @FunctionalInterface
   public interface DisplayItemsGenerator {
      void accept(CreativeModeTab.ItemDisplayParameters var1, CreativeModeTab.Output var2);
   }

   private static class ItemDisplayBuilder implements CreativeModeTab.Output {
      public final Collection<ItemStack> tabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
      public final Set<ItemStack> searchTabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
      private final CreativeModeTab tab;
      private final FeatureFlagSet featureFlagSet;

      public ItemDisplayBuilder(CreativeModeTab tab, FeatureFlagSet featureFlagSet) {
         this.tab = tab;
         this.featureFlagSet = featureFlagSet;
      }

      @Override
      public void accept(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility) {
         if (stack.getCount() != 1) {
            throw new IllegalArgumentException("Stack size must be exactly 1");
         } else {
            boolean foundDuplicateStack = this.tabContents.contains(stack) && tabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
            if (foundDuplicateStack) {
               throw new IllegalStateException(
                  "Accidentally adding the same item stack twice "
                     + stack.getDisplayName().getString()
                     + " to a Creative Mode Tab: "
                     + this.tab.getDisplayName().getString()
               );
            } else {
               if (stack.getItem().isEnabled(this.featureFlagSet)) {
                  switch (tabVisibility) {
                     case PARENT_AND_SEARCH_TABS:
                        this.tabContents.add(stack);
                        this.searchTabContents.add(stack);
                        break;
                     case PARENT_TAB_ONLY:
                        this.tabContents.add(stack);
                        break;
                     case SEARCH_TAB_ONLY:
                        this.searchTabContents.add(stack);
                  }
               }
            }
         }
      }
   }

   public record ItemDisplayParameters(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
      public boolean needsUpdate(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
         return !this.enabledFeatures.equals(enabledFeatures) || this.hasPermissions != hasPermissions || this.holders != holders;
      }
   }

   public interface Output {
      void accept(ItemStack var1, CreativeModeTab.TabVisibility var2);

      default void accept(ItemStack stack) {
         this.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }

      default void accept(ItemLike item, CreativeModeTab.TabVisibility tabVisibility) {
         this.accept(new ItemStack(item), tabVisibility);
      }

      default void accept(ItemLike item) {
         this.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }

      default void acceptAll(Collection<ItemStack> stacks, CreativeModeTab.TabVisibility tabVisibility) {
         stacks.forEach(stack -> this.accept(stack, tabVisibility));
      }

      default void acceptAll(Collection<ItemStack> stacks) {
         this.acceptAll(stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }
   }

   public static enum Row {
      TOP,
      BOTTOM;
   }

   public static enum TabVisibility {
      PARENT_AND_SEARCH_TABS,
      PARENT_TAB_ONLY,
      SEARCH_TAB_ONLY;
   }

   public static enum Type {
      CATEGORY,
      INVENTORY,
      HOTBAR,
      SEARCH;
   }
}
