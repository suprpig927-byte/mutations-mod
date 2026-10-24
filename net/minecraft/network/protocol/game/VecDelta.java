package net.minecraft.network.protocol.game;

import io.netty.handler.codec.DecoderException;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.PositionStep;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public sealed interface VecDelta permits VecDelta.Linear, VecDelta.Stepped {
   VecDelta ZERO = new VecDelta.Linear((short)0, (short)0, (short)0);

   int stepCount();

   boolean hasDeltaX();

   boolean hasDeltaZ();

   PositionPath decode(VecDeltaCodec var1);

   static VecDelta read(FriendlyByteBuf input, int stepCount) {
      if (stepCount <= 0) {
         short xa = input.readShort();
         short ya = input.readShort();
         short za = input.readShort();
         return new VecDelta.Linear(xa, ya, za);
      } else {
         int maxSteps = input.readableBytes() / 7;
         if (stepCount > maxSteps) {
            throw new DecoderException("VecDelta with size " + stepCount + " is bigger than allowed " + maxSteps);
         } else {
            List<VecDelta.Stepped.DeltaStep> steps = new ArrayList<>(stepCount);

            for (int i = 0; i < stepCount; i++) {
               int ticks = input.readVarInt();
               short xa = input.readShort();
               short ya = input.readShort();
               short za = input.readShort();
               steps.add(new VecDelta.Stepped.DeltaStep(xa, ya, za, ticks));
            }

            return new VecDelta.Stepped(steps);
         }
      }
   }

   static void write(FriendlyByteBuf output, VecDelta delta) {
      Objects.requireNonNull(delta);
      VecDelta var3 = delta;
      byte var4 = 0;

      while (true) {
         switch (SwitchBootstraps.typeSwitch<"typeSwitch",VecDelta.Linear,VecDelta.Stepped>(var3, var4)) {
            case 0:
               VecDelta.Linear var5 = (VecDelta.Linear)var3;
               VecDelta.Linear var22 = var5;

               try {
                  var23 = var22.xa();
               } catch (Throwable var16) {
                  throw new MatchException(var16.toString(), var16);
               }

               short var17 = var23;
               if (true) {
                  var22 = var5;

                  try {
                     var25 = var22.ya();
                  } catch (Throwable var15) {
                     throw new MatchException(var15.toString(), var15);
                  }

                  var17 = var25;
                  if (true) {
                     var22 = var5;

                     try {
                        var27 = var22.za();
                     } catch (Throwable var14) {
                        throw new MatchException(var14.toString(), var14);
                     }

                     var17 = var27;
                     if (true) {
                        output.writeShort(var17);
                        output.writeShort(var17);
                        output.writeShort(var17);
                        break;
                     }
                  }
               }

               var4 = 1;
               continue;
            case 1:
               VecDelta.Stepped var9 = (VecDelta.Stepped)var3;
               VecDelta.Stepped var10000 = var9;

               try {
                  var21 = var10000.steps();
               } catch (Throwable var13) {
                  throw new MatchException(var13.toString(), var13);
               }

               for (VecDelta.Stepped.DeltaStep step : var21) {
                  output.writeVarInt(step.ticks);
                  output.writeShort(step.xa);
                  output.writeShort(step.ya);
                  output.writeShort(step.za);
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return;
      }
   }

   public record Linear(short xa, short ya, short za) implements VecDelta {
      @Override
      public int stepCount() {
         return 0;
      }

      @Override
      public boolean hasDeltaX() {
         return this.xa != 0;
      }

      @Override
      public boolean hasDeltaZ() {
         return this.za != 0;
      }

      @Override
      public PositionPath decode(VecDeltaCodec positionCodec) {
         Vec3 pos = positionCodec.decode(this.xa, this.ya, this.za);
         return PositionPath.of(pos);
      }
   }

   public record Stepped(List<VecDelta.Stepped.DeltaStep> steps) implements VecDelta {
      private static final int MIN_BYTES_PER_STEP = 7;

      @Override
      public int stepCount() {
         return this.steps.size();
      }

      @Override
      public boolean hasDeltaX() {
         for (VecDelta.Stepped.DeltaStep step : this.steps) {
            if (step.xa != 0) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean hasDeltaZ() {
         for (VecDelta.Stepped.DeltaStep step : this.steps) {
            if (step.za != 0) {
               return true;
            }
         }

         return false;
      }

      @Override
      public PositionPath decode(VecDeltaCodec positionCodec) {
         if (this.steps.isEmpty()) {
            return PositionPath.of(positionCodec.getBase());
         } else {
            List<PositionStep> output = new ArrayList<>(this.steps.size());
            Vec3 originalBase = positionCodec.getBase();

            for (VecDelta.Stepped.DeltaStep e : this.steps) {
               Vec3 pos = positionCodec.decode(e.xa, e.ya, e.za);
               output.add(new PositionStep(pos, e.ticks));
               positionCodec.setBase(pos);
            }

            positionCodec.setBase(originalBase);
            return PositionPath.stepped(output);
         }
      }

      @Nullable
      public static VecDelta.Stepped tryEncode(VecDeltaCodec positionCodec, List<PositionStep> steps) {
         if (steps.isEmpty()) {
            return new VecDelta.Stepped(List.of());
         } else {
            List<VecDelta.Stepped.DeltaStep> output = new ArrayList<>(steps.size());
            Vec3 originalBase = positionCodec.getBase();

            for (PositionStep step : steps) {
               Vec3 pos = step.position();
               long xa = positionCodec.encodeX(pos);
               long ya = positionCodec.encodeY(pos);
               long za = positionCodec.encodeZ(pos);
               if (VecDeltaCodec.isDeltaTooBig(xa, ya, za)) {
                  positionCodec.setBase(originalBase);
                  return null;
               }

               output.add(new VecDelta.Stepped.DeltaStep((short)xa, (short)ya, (short)za, step.tickOffset()));
               positionCodec.setBase(pos);
            }

            positionCodec.setBase(originalBase);
            return new VecDelta.Stepped(output);
         }
      }

      public record DeltaStep(short xa, short ya, short za, int ticks) {
      }
   }
}
