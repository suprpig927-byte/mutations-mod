package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.npc.villager.VillagerData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderState extends HoldingEntityRenderState implements VillagerDataHolderRenderState {
   public boolean isUnhappy;
   @Nullable
   public VillagerData villagerData;

   @Nullable
   @Override
   public VillagerData getVillagerData() {
      return this.villagerData;
   }
}
