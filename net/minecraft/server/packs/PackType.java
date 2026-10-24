package net.minecraft.server.packs;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum PackType implements StringRepresentable {
   CLIENT_RESOURCES("assets"),
   SERVER_DATA("data");

   private final String directory;

   private PackType(String directory) {
      this.directory = directory;
   }

   public String getDirectory() {
      return this.directory;
   }

   @Override
   public String getSerializedName() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}
