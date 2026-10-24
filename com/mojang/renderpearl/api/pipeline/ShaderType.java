package com.mojang.renderpearl.api.pipeline;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.internal.NonExhaustiveEnum;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@NonExhaustiveEnum(
   reason = "Further shader types such as Compute, Task, and Mesh may be added"
)
public enum ShaderType {
   VERTEX("vertex", ".vsh"),
   FRAGMENT("fragment", ".fsh");

   private static final ShaderType[] TYPES = values();
   private final String name;
   private final String extension;

   private ShaderType(String name, String extension) {
      this.name = name;
      this.extension = extension;
   }

   @Nullable
   public static ShaderType byLocation(Identifier location) {
      for (ShaderType type : TYPES) {
         if (location.getPath().endsWith(type.extension)) {
            return type;
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   public FileToIdConverter idConverter() {
      return new FileToIdConverter("shaders", this.extension);
   }
}
