package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import org.jspecify.annotations.Nullable;

public record SavedDataType<T extends SavedData>(
   Identifier id, SavedDataType.Factory<T> factory, SavedDataType.Factory<Codec<T>> codecFactory, @Nullable DataFixTypes dataFixType
) {
   public SavedDataType(Identifier id, Supplier<T> constructor, Codec<T> codec, @Nullable DataFixTypes dataFixType) {
      this(id, ignored -> constructor.get(), ignored -> codec, dataFixType);
   }

   public SavedDataType(Identifier id, Supplier<T> constructor, Codec<T> codec) {
      this(id, ignored -> constructor.get(), ignored -> codec, null);
   }

   public SavedDataType(Identifier id, SavedDataType.Factory<T> constructor, SavedDataType.Factory<Codec<T>> codec) {
      this(id, constructor, codec, null);
   }

   @Deprecated
   public Supplier<T> constructor() {
      return () -> this.factory.create(null);
   }

   @Deprecated
   public Codec<T> codec() {
      return this.codecFactory.create(null);
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof SavedDataType<?> type && this.id.equals(type.id);
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return "SavedDataType[" + this.id + "]";
   }

   @FunctionalInterface
   public interface Factory<T> {
      T create(@Nullable ServerLevel var1);
   }
}
