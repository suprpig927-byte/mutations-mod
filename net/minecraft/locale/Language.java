package net.minecraft.locale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringDecomposer;
import net.neoforged.neoforge.server.LanguageHook;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();
   private static final Pattern UNSUPPORTED_FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
   public static final String DEFAULT = "en_us";
   public static final Language DEFAULT_INSTANCE = loadDefault();
   private static volatile Language instance = DEFAULT_INSTANCE;

   private static Language loadDefault() {
      DeprecatedTranslationsInfo deprecatedInfo = DeprecatedTranslationsInfo.loadFromDefaultResource();
      final Map<String, String> loadedData = new HashMap<>();
      BiConsumer<String, String> output = loadedData::put;
      final Map<String, Component> componentMap = new HashMap<>();
      parseTranslations(output, componentMap::put, "/assets/minecraft/lang/en_us.json");
      deprecatedInfo.applyToMap(loadedData);
      LanguageHook.captureLanguageMap(loadedData, componentMap);
      return new Language() {
         @Override
         public String getOrDefault(String elementId, String defaultValue) {
            return loadedData.getOrDefault(elementId, defaultValue);
         }

         @Override
         public boolean has(String elementId) {
            return loadedData.containsKey(elementId);
         }

         @Override
         public boolean isDefaultRightToLeft() {
            return false;
         }

         @Override
         public FormattedCharSequence getVisualOrder(FormattedText logicalOrderText) {
            return outputx -> logicalOrderText.visit(
                  (style, contents) -> StringDecomposer.iterateFormatted(contents, style, output) ? Optional.empty() : FormattedText.STOP_ITERATION,
                  Style.EMPTY
               )
               .isPresent();
         }

         @Override
         public Map<String, String> getLanguageData() {
            return loadedData;
         }

         @Nullable
         @Override
         public Component getComponent(String key) {
            return componentMap.get(key);
         }
      };
   }

   @Deprecated
   private static void parseTranslations(BiConsumer<String, String> output, String path) {
      parseTranslations(output, (key, value) -> {}, path);
   }

   private static void parseTranslations(BiConsumer<String, String> output, BiConsumer<String, Component> componentConsumer, String path) {
      try (InputStream stream = Language.class.getResourceAsStream(path)) {
         loadFromJson(stream, output, componentConsumer);
      } catch (JsonParseException | IOException var8) {
         LOGGER.error("Couldn't read strings from {}", path, var8);
      }
   }

   public static void loadFromJson(InputStream stream, BiConsumer<String, String> output) {
      loadFromJson(stream, output, (key, value) -> {});
   }

   public static void loadFromJson(InputStream stream, BiConsumer<String, String> output, BiConsumer<String, Component> componentConsumer) {
      JsonObject entries = (JsonObject)GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);

      for (Entry<String, JsonElement> entry : entries.entrySet()) {
         if (!entry.getValue().isJsonArray() && !entry.getValue().isJsonObject()) {
            String text = UNSUPPORTED_FORMAT_PATTERN.matcher(GsonHelper.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            output.accept(entry.getKey(), text);
         } else {
            Component component = (Component)ComponentSerialization.CODEC
               .parse(JsonOps.INSTANCE, entry.getValue())
               .getOrThrow(msg -> new JsonParseException("Error parsing translation for " + entry.getKey() + ": " + msg));
            output.accept(entry.getKey(), component.getString());
            componentConsumer.accept(entry.getKey(), component);
         }
      }
   }

   public static Language getInstance() {
      return instance;
   }

   public static void inject(Language language) {
      instance = language;
   }

   public Map<String, String> getLanguageData() {
      return ImmutableMap.of();
   }

   @Nullable
   public Component getComponent(String key) {
      return null;
   }

   public String getOrDefault(String elementId) {
      return this.getOrDefault(elementId, elementId);
   }

   public abstract String getOrDefault(String var1, String var2);

   public abstract boolean has(String var1);

   public abstract boolean isDefaultRightToLeft();

   public abstract FormattedCharSequence getVisualOrder(FormattedText var1);

   public List<FormattedCharSequence> getVisualOrder(List<FormattedText> lines) {
      return lines.stream().map(this::getVisualOrder).collect(ImmutableList.toImmutableList());
   }
}
