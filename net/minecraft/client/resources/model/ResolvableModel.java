package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvableModel {
   void resolveDependencies(ResolvableModel.Resolver var1);

   public interface Resolver {
      void markDependency(Identifier var1);
   }
}
