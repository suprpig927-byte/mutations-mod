package net.minecraft.world.level.levelgen.densityfunction.op;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.generator.ConstantFunction;
import org.slf4j.Logger;

public record BinaryFunction(BinaryFunction.Type type, DensityFunction left, DensityFunction right) implements DensityFunction {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   public DensitySampler compileSampler(DensityFunction.CompileContext context) {
      DensitySampler left = this.left.compileSampler(context);
      DensitySampler right = this.right.compileSampler(context);

      return (DensitySampler)(switch (this.type) {
         case ADD -> {
            if (this.left instanceof ConstantFunction(float var80)) {
               float var42 = var80;
               if (true) {
                  yield new BinaryFunction.ConstAddSampler(right, var42);
               }
            }

            if (this.right instanceof ConstantFunction(float var82)) {
               float var50 = var82;
               if (true) {
                  yield new BinaryFunction.ConstAddSampler(left, var50);
               }
            }

            yield new BinaryFunction.AddSampler(left, right);
         }
         case SUB -> {
            if (this.left instanceof ConstantFunction(float var76)) {
               float var41 = var76;
               if (true) {
                  yield new BinaryFunction.ConstSubSampler(var41, right);
               }
            }

            if (this.right instanceof ConstantFunction(float var78)) {
               float var49 = var78;
               if (true) {
                  yield new BinaryFunction.ConstAddSampler(left, -var49);
               }
            }

            yield new BinaryFunction.SubSampler(left, right);
         }
         case MUL -> {
            if (this.left instanceof ConstantFunction(float var72)) {
               float var40 = var72;
               if (true) {
                  yield new BinaryFunction.ConstMulSampler(right, var40);
               }
            }

            if (this.right instanceof ConstantFunction(float var74)) {
               float var48 = var74;
               if (true) {
                  yield new BinaryFunction.ConstMulSampler(left, var48);
               }
            }

            yield new BinaryFunction.MulSampler(left, right);
         }
         case DIV -> {
            if (this.left instanceof ConstantFunction(float var68)) {
               float var39 = var68;
               if (true) {
                  yield new BinaryFunction.ConstDivSampler(var39, right);
               }
            }

            if (this.right instanceof ConstantFunction(float var70)) {
               float var47 = var70;
               if (true) {
                  yield new BinaryFunction.ConstMulSampler(left, 1.0F / var47);
               }
            }

            yield new BinaryFunction.DivSampler(left, right);
         }
         case MIN -> {
            Interval leftRange = this.left.range();
            Interval rightRange = this.right.range();
            if (leftRange.max() < rightRange.min()) {
               this.warnNonIntersecting();
               yield left;
            } else if (rightRange.max() < leftRange.min()) {
               this.warnNonIntersecting();
               yield right;
            } else {
               Object var66;
               label201: {
                  if (this.left instanceof ConstantFunction(float var63)) {
                     float var55 = var63;
                     if (true) {
                        var66 = new BinaryFunction.ConstMinSampler(right, var55);
                        break label201;
                     }
                  }

                  if (this.right instanceof ConstantFunction(float var65)) {
                     float var56 = var65;
                     if (true) {
                        var66 = new BinaryFunction.ConstMinSampler(left, var56);
                        break label201;
                     }
                  }

                  var66 = new BinaryFunction.MinSampler(left, right, rightRange.min());
               }

               yield (DensitySampler)var66;
            }
         }
         case MAX -> {
            Interval leftRange = this.left.range();
            Interval rightRange = this.right.range();
            if (leftRange.min() > rightRange.max()) {
               this.warnNonIntersecting();
               yield left;
            } else if (rightRange.min() > leftRange.max()) {
               this.warnNonIntersecting();
               yield right;
            } else {
               Object var61;
               label198: {
                  if (this.left instanceof ConstantFunction(float var58)) {
                     float var11 = var58;
                     if (true) {
                        var61 = new BinaryFunction.ConstMaxSampler(right, var11);
                        break label198;
                     }
                  }

                  if (this.right instanceof ConstantFunction(float var60)) {
                     float var54 = var60;
                     if (true) {
                        var61 = new BinaryFunction.ConstMaxSampler(left, var54);
                        break label198;
                     }
                  }

                  var61 = new BinaryFunction.MaxSampler(left, right, rightRange.max());
               }

               yield (DensitySampler)var61;
            }
         }
      });
   }

   private void warnNonIntersecting() {
      LOGGER.warn(
         "Compiling a {} function between two non-overlapping inputs: {} ({}) and {} ({})",
         new Object[]{this.type, this.left, this.left.range(), this.right, this.right.range()}
      );
   }

   @Override
   public DensityFunction rewriteChildren(DfRewriteRule rule) {
      DensityFunction left = rule.rewrite(this.left);
      DensityFunction right = rule.rewrite(this.right);
      return left == this.left && this.right == right ? this : new BinaryFunction(this.type, left, right);
   }

   @Override
   public Interval range() {
      Interval left = this.left.range();
      Interval right = this.right.range();

      return switch (this.type) {
         case ADD -> Interval.add(left, right);
         case SUB -> Interval.sub(left, right);
         case MUL -> Interval.mul(left, right);
         case DIV -> Interval.div(left, right);
         case MIN -> Interval.min(left, right);
         case MAX -> Interval.max(left, right);
      };
   }

   @DensityFunction.Axes
   @Override
   public int domainAxes() {
      return this.left.domainAxes() | this.right.domainAxes();
   }

   @Override
   public MapCodec<BinaryFunction> codec() {
      return this.type().codec;
   }

   public record AddSampler(DensitySampler left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               outputBuffer.addTo(i, rightBuffer.get(i));
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left.sampleValue(context, blockX, blockY, blockZ) + this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public record ConstAddSampler(DensitySampler left, float right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            outputBuffer.addTo(i, this.right);
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left.sampleValue(context, blockX, blockY, blockZ) + this.right;
      }
   }

   public record ConstDivSampler(float left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.right.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            outputBuffer.set(i, this.left / outputBuffer.get(i));
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left / this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public record ConstMaxSampler(DensitySampler left, float right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            if (this.right > outputBuffer.get(i)) {
               outputBuffer.set(i, this.right);
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return Math.max(this.left.sampleValue(context, blockX, blockY, blockZ), this.right);
      }
   }

   public record ConstMinSampler(DensitySampler left, float right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            if (this.right < outputBuffer.get(i)) {
               outputBuffer.set(i, this.right);
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return Math.min(this.left.sampleValue(context, blockX, blockY, blockZ), this.right);
      }
   }

   public record ConstMulSampler(DensitySampler left, float right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            outputBuffer.set(i, outputBuffer.get(i) * this.right);
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left.sampleValue(context, blockX, blockY, blockZ) * this.right;
      }
   }

   public record ConstSubSampler(float left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.right.sampleVolume(context, outputBuffer, volume);

         for (int i = 0; i < outputBuffer.size(); i++) {
            outputBuffer.set(i, this.left - outputBuffer.get(i));
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left - this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public record DivSampler(DensitySampler left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               outputBuffer.set(i, outputBuffer.get(i) / rightBuffer.get(i));
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         float left = this.left.sampleValue(context, blockX, blockY, blockZ);
         return left == 0.0F ? 0.0F : left / this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public record MaxSampler(DensitySampler left, DensitySampler right, float rightMaxValue) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               float rightValue = rightBuffer.get(i);
               if (rightValue > outputBuffer.get(i)) {
                  outputBuffer.set(i, rightValue);
               }
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         float left = this.left.sampleValue(context, blockX, blockY, blockZ);
         return left >= this.rightMaxValue ? left : Math.max(left, this.right.sampleValue(context, blockX, blockY, blockZ));
      }
   }

   public record MinSampler(DensitySampler left, DensitySampler right, float rightMinValue) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               float rightValue = rightBuffer.get(i);
               if (rightValue < outputBuffer.get(i)) {
                  outputBuffer.set(i, rightValue);
               }
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         float left = this.left.sampleValue(context, blockX, blockY, blockZ);
         return left <= this.rightMinValue ? left : Math.min(left, this.right.sampleValue(context, blockX, blockY, blockZ));
      }
   }

   public record MulSampler(DensitySampler left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               outputBuffer.set(i, outputBuffer.get(i) * rightBuffer.get(i));
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         float left = this.left.sampleValue(context, blockX, blockY, blockZ);
         return left == 0.0F ? 0.0F : left * this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public record SubSampler(DensitySampler left, DensitySampler right) implements DensitySampler {
      @Override
      public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
         this.left.sampleVolume(context, outputBuffer, volume);

         try (ScopedDensityBuffer rightBuffer = context.acquireBuffer(volume)) {
            this.right.sampleVolume(context, rightBuffer, volume);

            for (int i = 0; i < outputBuffer.size(); i++) {
               outputBuffer.addTo(i, -rightBuffer.get(i));
            }
         }
      }

      @Override
      public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
         return this.left.sampleValue(context, blockX, blockY, blockZ) - this.right.sampleValue(context, blockX, blockY, blockZ);
      }
   }

   public static enum Type {
      ADD("add"),
      SUB("sub"),
      MUL("mul"),
      DIV("div"),
      MIN("min"),
      MAX("max");

      public final String id;
      public final MapCodec<BinaryFunction> codec = RecordCodecBuilder.mapCodec(
         i -> i.group(
               DensityFunction.CODEC.fieldOf("left").forGetter(BinaryFunction::left), DensityFunction.CODEC.fieldOf("right").forGetter(BinaryFunction::right)
            )
            .apply(i, (left, right) -> new BinaryFunction(this, left, right))
      );

      private Type(String id) {
         this.id = id;
      }
   }
}
