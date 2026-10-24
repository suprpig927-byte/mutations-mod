package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;

public abstract class EnchantmentsPredicate implements SingleComponentItemPredicate<ItemEnchantments> {
   private final List<EnchantmentPredicate> enchantments;

   protected EnchantmentsPredicate(List<EnchantmentPredicate> enchantments) {
      this.enchantments = enchantments;
   }

   public static <T extends EnchantmentsPredicate> Codec<T> codec(Function<List<EnchantmentPredicate>, T> constructor) {
      return EnchantmentPredicate.CODEC.listOf().xmap(constructor, EnchantmentsPredicate::enchantments);
   }

   protected List<EnchantmentPredicate> enchantments() {
      return this.enchantments;
   }

   public boolean matches(ItemEnchantments appliedEnchantments) {
      for (EnchantmentPredicate enchantment : this.enchantments) {
         if (!enchantment.containedIn(appliedEnchantments)) {
            return false;
         }
      }

      return true;
   }

   public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> predicates) {
      return new EnchantmentsPredicate.Enchantments(predicates);
   }

   public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> predicates) {
      return new EnchantmentsPredicate.StoredEnchantments(predicates);
   }

   public static class Enchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = codec(EnchantmentsPredicate.Enchantments::new);

      protected Enchantments(List<EnchantmentPredicate> enchantments) {
         super(enchantments);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.ENCHANTMENTS;
      }

      @Override
      public boolean matches(DataComponentGetter getter) {
         if (getter instanceof ItemStack stack) {
            HolderLookup.RegistryLookup<Enchantment> lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
            if (lookup != null) {
               return this.matches(stack.getAllEnchantments(lookup));
            }
         }

         return super.matches(getter);
      }
   }

   public static class StoredEnchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = codec(EnchantmentsPredicate.StoredEnchantments::new);

      protected StoredEnchantments(List<EnchantmentPredicate> enchantments) {
         super(enchantments);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.STORED_ENCHANTMENTS;
      }
   }
}
