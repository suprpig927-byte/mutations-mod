package net.minecraft.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class KeyMapping implements Comparable<KeyMapping>, IKeyMappingExtension {
   private static final Map<String, KeyMapping> ALL = Maps.newConcurrentMap();
   private static final KeyMappingLookup MAP = new KeyMappingLookup();
   private final String name;
   private final InputConstants.Key defaultKey;
   private final KeyMapping.Category category;
   protected InputConstants.Key key;
   boolean isDown;
   private int clickCount;
   private final int order;
   private KeyModifier keyModifierDefault = KeyModifier.NONE;
   private KeyModifier keyModifier = KeyModifier.NONE;
   private IKeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;

   public static void click(InputConstants.Key key) {
      forAllKeyMappings(key, keyMapping -> keyMapping.clickCount++);
   }

   public static void set(InputConstants.Key key, boolean state) {
      forAllKeyMappings(key, keyMapping -> keyMapping.setDown(state), !state);
   }

   private static void forAllKeyMappings(InputConstants.Key key, Consumer<KeyMapping> operation) {
      forAllKeyMappings(key, operation, false);
   }

   private static void forAllKeyMappings(InputConstants.Key key, Consumer<KeyMapping> operation, boolean releasing) {
      List<KeyMapping> keyMappings = MAP.getAll(key, releasing);
      if (keyMappings != null && !keyMappings.isEmpty()) {
         for (KeyMapping keyMapping : keyMappings) {
            operation.accept(keyMapping);
         }
      }
   }

   public static void setAll() {
      Window window = Minecraft.getInstance().getWindow();

      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping.shouldSetOnIngameFocus()) {
            keyMapping.setDown(InputConstants.isKeyDown(keyMapping.key.getValue()));
         }
      }
   }

   public static void releaseAll() {
      for (KeyMapping keyMapping : ALL.values()) {
         keyMapping.release();
      }
   }

   public static void restoreToggleStatesOnScreenClosed() {
      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping instanceof ToggleKeyMapping toggleKeyMapping && toggleKeyMapping.shouldRestoreStateOnScreenClosed()) {
            toggleKeyMapping.setDown(true);
         }
      }
   }

   public static void resetToggleKeys() {
      for (KeyMapping keyMapping : ALL.values()) {
         if (keyMapping instanceof ToggleKeyMapping toggleKeyMapping) {
            toggleKeyMapping.reset();
         }
      }
   }

   public static void resetMapping() {
      MAP.clear();

      for (KeyMapping keyMapping : ALL.values()) {
         keyMapping.registerMapping(keyMapping.key);
      }
   }

   public KeyMapping(String name, int keysym, KeyMapping.Category category) {
      this(name, InputConstants.Type.KEYBOARD, keysym, category);
   }

   public KeyMapping(String name, InputConstants.Type type, int value, KeyMapping.Category category) {
      this(name, type, value, category, 0);
   }

   public KeyMapping(String name, InputConstants.Type type, int value, KeyMapping.Category category, int order) {
      this.name = name;
      this.key = type.getOrCreate(value);
      this.defaultKey = this.key;
      this.category = category;
      this.order = order;
      ALL.put(name, this);
      this.registerMapping(this.key);
   }

   public KeyMapping(String name, IKeyConflictContext keyConflictContext, InputConstants.Type inputType, int keyCode, KeyMapping.Category category) {
      this(name, keyConflictContext, inputType.getOrCreate(keyCode), category);
   }

   public KeyMapping(String name, IKeyConflictContext keyConflictContext, InputConstants.Key keyCode, KeyMapping.Category category) {
      this(name, keyConflictContext, KeyModifier.NONE, keyCode, category);
   }

   public KeyMapping(
      String name, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, InputConstants.Type inputType, int keyCode, KeyMapping.Category category
   ) {
      this(name, keyConflictContext, keyModifier, inputType.getOrCreate(keyCode), category);
   }

   public KeyMapping(String name, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, InputConstants.Key keyCode, KeyMapping.Category category) {
      this.name = name;
      this.key = keyCode;
      this.defaultKey = keyCode;
      this.category = category;
      this.order = 0;
      this.keyConflictContext = keyConflictContext;
      this.keyModifier = keyModifier;
      this.keyModifierDefault = keyModifier;
      if (this.keyModifier.matches(keyCode)) {
         this.keyModifier = KeyModifier.NONE;
      }

      ALL.put(name, this);
      MAP.put(keyCode, this);
   }

   public InputConstants.Key getKey() {
      return this.key;
   }

   public void setKeyConflictContext(IKeyConflictContext keyConflictContext) {
      this.keyConflictContext = keyConflictContext;
   }

   public IKeyConflictContext getKeyConflictContext() {
      return this.keyConflictContext;
   }

   public KeyModifier getDefaultKeyModifier() {
      return this.keyModifierDefault;
   }

   public KeyModifier getKeyModifier() {
      return this.keyModifier;
   }

   public void setKeyModifierAndCode(KeyModifier keyModifier, InputConstants.Key keyCode) {
      this.key = keyCode;
      if (keyModifier.matches(keyCode)) {
         keyModifier = KeyModifier.NONE;
      }

      MAP.remove(this);
      this.keyModifier = keyModifier;
      MAP.put(keyCode, this);
   }

   public boolean isDown() {
      return this.isDown && this.isConflictContextAndModifierActive();
   }

   public KeyMapping.Category getCategory() {
      return this.category;
   }

   public boolean consumeClick() {
      if (this.clickCount == 0) {
         return false;
      } else {
         this.clickCount--;
         return true;
      }
   }

   protected void release() {
      this.clickCount = 0;
      this.setDown(false);
   }

   protected boolean shouldSetOnIngameFocus() {
      return this.key.getType() == InputConstants.Type.KEYBOARD && this.key.getValue() != InputConstants.UNKNOWN.getValue();
   }

   public String getName() {
      return this.name;
   }

   public InputConstants.Key getDefaultKey() {
      return this.defaultKey;
   }

   public void setKey(InputConstants.Key key) {
      this.key = key;
   }

   public int compareTo(KeyMapping o) {
      if (this.category == o.category) {
         return this.order == o.order ? I18n.get(this.name).compareTo(I18n.get(o.name)) : Integer.compare(this.order, o.order);
      } else {
         return Integer.compare(KeyMapping.Category.SORT_ORDER.indexOf(this.category), KeyMapping.Category.SORT_ORDER.indexOf(o.category));
      }
   }

   public static Supplier<Component> createNameSupplier(String key) {
      KeyMapping map = ALL.get(key);
      return map == null ? () -> Component.translatable(key) : map::getTranslatedKeyMessage;
   }

   public boolean same(KeyMapping that) {
      if (this.getKeyConflictContext().conflicts(that.getKeyConflictContext()) || that.getKeyConflictContext().conflicts(this.getKeyConflictContext())) {
         KeyModifier keyModifier = this.getKeyModifier();
         KeyModifier otherKeyModifier = that.getKeyModifier();
         if (keyModifier.matches(that.getKey()) || otherKeyModifier.matches(this.getKey())) {
            return true;
         }

         if (this.getKey().equals(that.getKey())) {
            return keyModifier == otherKeyModifier
               || this.getKeyConflictContext().conflicts(KeyConflictContext.IN_GAME)
                  && (keyModifier == KeyModifier.NONE || otherKeyModifier == KeyModifier.NONE);
         }
      }

      return this.key.equals(that.key);
   }

   public boolean isUnbound() {
      return this.key.equals(InputConstants.UNKNOWN);
   }

   public boolean matches(KeyEvent event) {
      return this.key.getType() == InputConstants.Type.KEYBOARD && this.key.getValue() == event.key() && !this.isUnbound();
   }

   public boolean matchesMouse(MouseButtonEvent event) {
      return this.key.getType() == InputConstants.Type.MOUSE && this.key.getValue() == event.button() && !this.isUnbound();
   }

   public boolean matches(InputConstants.Key key) {
      return this.key.equals(key) && !this.isUnbound();
   }

   public Component getTranslatedKeyMessage() {
      return this.getKeyModifier().getCombinedName(this.key, () -> this.key.getDisplayName());
   }

   public boolean isDefault() {
      return this.key.equals(this.defaultKey) && this.getKeyModifier() == this.getDefaultKeyModifier();
   }

   public String saveString() {
      return this.key.getName();
   }

   public void setDown(boolean down) {
      this.isDown = down;
   }

   private void registerMapping(InputConstants.Key key) {
      MAP.put(key, this);
   }

   @Nullable
   public static KeyMapping get(String name) {
      return ALL.get(name);
   }

   public record Category(Identifier id) {
      static final List<KeyMapping.Category> SORT_ORDER = new ArrayList<>();
      public static final KeyMapping.Category MOVEMENT = register("movement");
      public static final KeyMapping.Category MISC = register("misc");
      public static final KeyMapping.Category MULTIPLAYER = register("multiplayer");
      public static final KeyMapping.Category GAMEPLAY = register("gameplay");
      public static final KeyMapping.Category INVENTORY = register("inventory");
      public static final KeyMapping.Category CREATIVE = register("creative");
      public static final KeyMapping.Category SPECTATOR = register("spectator");
      public static final KeyMapping.Category DEBUG = register("debug");

      private static KeyMapping.Category register(String name) {
         return register(Identifier.withDefaultNamespace(name));
      }

      @Deprecated
      public static KeyMapping.Category register(Identifier id) {
         KeyMapping.Category category = new KeyMapping.Category(id);
         if (SORT_ORDER.contains(category)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", id));
         } else {
            SORT_ORDER.add(category);
            return category;
         }
      }

      public Component label() {
         return Component.translatable(this.id.toLanguageKey("key.category"));
      }
   }
}
