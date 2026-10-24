package net.minecraft.world.entity;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface InterpolationHandler {
   InterpolationHandler NO_OP = new InterpolationHandler.NoOpInterpolationHandler();

   default InterpolationTracker interpolationTracker() {
      return InterpolationTracker.NO_OP;
   }

   @Nullable
   PositionAndRotation target();

   boolean interpolateTo(@Nullable PositionPath var1, float var2, float var3, boolean var4);

   void interpolate();

   void applyPredictedMovement(Vec3 var1);

   boolean hasActiveInterpolation();

   void cancel();

   public static class NoOpInterpolationHandler implements InterpolationHandler {
      @Nullable
      @Override
      public PositionAndRotation target() {
         return null;
      }

      @Override
      public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
         return false;
      }

      @Override
      public void interpolate() {
      }

      @Override
      public void applyPredictedMovement(Vec3 delta) {
      }

      @Override
      public boolean hasActiveInterpolation() {
         return false;
      }

      @Override
      public void cancel() {
      }
   }
}
