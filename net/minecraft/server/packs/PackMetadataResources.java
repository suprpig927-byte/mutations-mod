package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.common.extensions.PackMetadataResourcesExtension;
import org.jspecify.annotations.Nullable;

public interface PackMetadataResources extends AutoCloseable, PackMetadataResourcesExtension {
   PackLocationInfo location();

   @Nullable
   IoSupplier<InputStream> getRootResource(String... var1);

   @Nullable
   <T> T getMetadataSection(MetadataSectionType<T> var1) throws IOException;

   @Override
   void close();
}
