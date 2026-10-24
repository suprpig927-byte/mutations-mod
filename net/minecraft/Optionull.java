package net.minecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Optionull {
   @Deprecated
   public static <T> T orElse(@Nullable T t, T defaultValue) {
      return Objects.requireNonNullElse(t, defaultValue);
   }

   @Nullable
   public static <T, R> R map(@Nullable T t, Function<T, R> map) {
      return t == null ? null : map.apply(t);
   }

   public static <T, R> R mapOrDefault(@Nullable T t, Function<T, R> map, R defaultValue) {
      return t == null ? defaultValue : map.apply(t);
   }

   public static <T, R> R mapOrElse(@Nullable T t, Function<T, R> map, Supplier<R> elseSupplier) {
      return t == null ? elseSupplier.get() : map.apply(t);
   }

   @Nullable
   public static <T> T first(Collection<T> collection) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : null;
   }

   public static <T> T firstOrDefault(Collection<T> collection, T defaultValue) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : defaultValue;
   }

   public static <T> T firstOrElse(Collection<T> collection, Supplier<T> elseSupplier) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : elseSupplier.get();
   }

   public static <T> boolean isNullOrEmpty(@Nullable T[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable boolean[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable byte[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable char[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable short[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable int[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable long[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable float[] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable double[] t) {
      return t == null || t.length == 0;
   }
}
