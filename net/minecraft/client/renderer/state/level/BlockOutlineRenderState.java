package net.minecraft.client.renderer.state.level;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockOutlineRenderState(
   BlockPos pos,
   boolean isTranslucent,
   boolean highContrast,
   VoxelShape shape,
   @Nullable VoxelShape collisionShape,
   @Nullable VoxelShape occlusionShape,
   @Nullable VoxelShape interactionShape,
   List<CustomBlockOutlineRenderer> customRenderers
) {
   @Deprecated
   public BlockOutlineRenderState(
      BlockPos pos,
      boolean isTranslucent,
      boolean highContrast,
      VoxelShape shape,
      @Nullable VoxelShape collisionShape,
      @Nullable VoxelShape occlusionShape,
      @Nullable VoxelShape interactionShape
   ) {
      this(pos, isTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, List.of());
   }

   @Deprecated
   public BlockOutlineRenderState(BlockPos pos, boolean isTranslucent, boolean highContrast, VoxelShape shape) {
      this(pos, isTranslucent, highContrast, shape, List.of());
   }

   public BlockOutlineRenderState(BlockPos pos, boolean isTranslucent, boolean highContrast, VoxelShape shape, List<CustomBlockOutlineRenderer> customRenderers) {
      this(pos, isTranslucent, highContrast, shape, null, null, null, customRenderers);
   }
}
