package net.minecraft.core.component;

import java.util.stream.Stream;
import net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension;
import org.jspecify.annotations.Nullable;

public interface DataComponentHolder extends DataComponentGetter, IDataComponentHolderExtension {
   DataComponentMap getComponents();

   @Nullable
   @Override
   default <T> T get(DataComponentType<? extends T> type) {
      return this.getComponents().get(type);
   }

   default <T> Stream<T> getAllOfType(Class<? extends T> valueClass) {
      return this.getComponents().stream().map(TypedDataComponent::value).filter(value -> valueClass.isAssignableFrom(value.getClass())).map(value -> (T)value);
   }

   @Override
   default <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
      return this.getComponents().getOrDefault(type, defaultValue);
   }

   @Override
   default boolean has(DataComponentType<?> type) {
      return this.getComponents().has(type);
   }
}
