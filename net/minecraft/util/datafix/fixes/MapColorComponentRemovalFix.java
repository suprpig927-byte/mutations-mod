package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

public class MapColorComponentRemovalFix extends DataComponentRemainderFix {
   public MapColorComponentRemovalFix(Schema outputSchema) {
      super(outputSchema, "MapColorComponentRemovalFix", "minecraft:map_color");
   }

   @Nullable
   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> input) {
      return null;
   }
}
