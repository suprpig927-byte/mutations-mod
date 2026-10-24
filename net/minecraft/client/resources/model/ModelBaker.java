package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ModelBakerExtension;
import net.neoforged.neoforge.client.extensions.ModelBakerInternerExtension;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends ModelBakerExtension {
   ResolvedModel getModel(Identifier var1);

   BlockStateModelPart missingBlockModelPart();

   MaterialBaker materials();

   ModelBaker.Interner interner();

   <T> T compute(ModelBaker.SharedOperationKey<T> var1);

   public interface Interner extends ModelBakerInternerExtension {
      Vector3fc vector(Vector3fc var1);

      BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo var1);
   }

   @FunctionalInterface
   public interface SharedOperationKey<T> {
      T compute(ModelBaker var1);
   }
}
