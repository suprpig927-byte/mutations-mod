package net.minecraft.core.component;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface DataComponentGetter {
   @Nullable
   <T> T get(DataComponentType<? extends T> var1);

   default <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
      T value = this.get(type);
      return value != null ? value : defaultValue;
   }

   @Nullable
   default <T> TypedDataComponent<T> getTyped(DataComponentType<T> type) {
      T value = this.get(type);
      return value != null ? new TypedDataComponent<>(type, value) : null;
   }

   @Nullable
   default <T> T get(Supplier<? extends DataComponentType<? extends T>> componentType) {
      return this.get((DataComponentType<? extends T>)componentType.get());
   }

   default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> componentType, T value) {
      return this.getOrDefault((DataComponentType<? extends T>)componentType.get(), value);
   }

   default <T> boolean has(Supplier<? extends DataComponentType<? extends T>> componentType) {
      return this.<T>get(componentType) != null;
   }

   default boolean has(DataComponentType<?> componentType) {
      return this.get(componentType) != null;
   }
}
