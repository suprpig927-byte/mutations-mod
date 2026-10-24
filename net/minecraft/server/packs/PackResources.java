package net.minecraft.server.packs;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public interface PackResources extends PackMetadataResources {
   String METADATA_EXTENSION = ".mcmeta";
   String PACK_META = "pack.mcmeta";

   @Nullable
   IoSupplier<InputStream> getResource(PackType var1, Identifier var2);

   void listResources(PackType var1, String var2, String var3, PackResources.ResourceOutput var4);

   Set<String> getNamespaces(PackType var1);

   default String packId() {
      return this.location().id();
   }

   default Optional<KnownPack> knownPackInfo() {
      return this.location().knownPackInfo();
   }

   @FunctionalInterface
   public interface Filter {
      boolean isFiltered(Identifier var1);
   }

   @FunctionalInterface
   public interface ResourceOutput extends BiConsumer<Identifier, IoSupplier<InputStream>> {
   }
}
