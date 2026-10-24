package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DynamicOps<JsonElement> ops;
   private final Codec<T> codec;
   private final FileToIdConverter lister;

   protected SimpleJsonResourceReloadListener(Codec<T> codec, FileToIdConverter lister) {
      this(JsonOps.INSTANCE, codec, lister);
   }

   private SimpleJsonResourceReloadListener(DynamicOps<JsonElement> ops, Codec<T> codec, FileToIdConverter lister) {
      this.ops = ops;
      this.codec = codec;
      this.lister = lister;
   }

   protected Map<Identifier, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<Identifier, T> result = new HashMap<>();
      ConditionalOps<JsonElement> conditionalOps = this.makeConditionalOps(this.ops);
      return scanDirectory(manager, this.lister, conditionalOps, this.codec, result);
   }

   public static <T> Map<Identifier, T> scanDirectory(
      ResourceManager manager, FileToIdConverter lister, DynamicOps<JsonElement> ops, Codec<T> codec, Map<Identifier, T> result
   ) {
      Codec<Optional<T>> conditionalCodec = ConditionalOps.createConditionalCodec(codec);

      for (Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
         Identifier location = entry.getKey();
         Identifier id = lister.fileToId(location);

         try (Reader reader = entry.getValue().openAsReader()) {
            conditionalCodec.parse(ops, StrictJsonParser.parse(reader)).ifSuccess(parsed -> {
               if (parsed.isEmpty()) {
                  LOGGER.debug("Skipping loading data file '{}' from '{}' as its conditions were not met", id, location);
               } else if (result.putIfAbsent(id, (T)parsed.get()) != null) {
                  throw new IllegalStateException("Duplicate data file ignored with ID " + id);
               }
            }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", new Object[]{id, location, error}));
         } catch (IllegalArgumentException | IOException | JsonParseException var15) {
            LOGGER.error("Couldn't parse data file '{}' from '{}'", new Object[]{id, location, var15});
         }
      }

      return result;
   }

   protected Identifier getPreparedPath(Identifier rl) {
      return this.lister.idToFile(rl);
   }
}
