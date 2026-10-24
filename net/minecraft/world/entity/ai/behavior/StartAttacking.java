package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType;

public class StartAttacking {
   public static <E extends Mob> BehaviorControl<E> create(StartAttacking.TargetFinder<E> targetFinderFunction) {
      return create((level, body) -> true, targetFinderFunction);
   }

   public static <E extends Mob> BehaviorControl<E> create(
      StartAttacking.StartAttackingCondition<E> canAttackPredicate, StartAttacking.TargetFinder<E> targetFinderFunction
   ) {
      return BehaviorBuilder.create(
         i -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(i, (attackTarget, cantReachSince) -> (level, body, timestamp) -> {
               if (!canAttackPredicate.test(level, (E)body)) {
                  return false;
               } else {
                  Optional<? extends LivingEntity> target = targetFinderFunction.get(level, (E)body);
                  if (target.isEmpty()) {
                     return false;
                  } else {
                     LivingEntity targetEntity = target.get();
                     if (!body.canAttack(targetEntity)) {
                        return false;
                     } else {
                        LivingChangeTargetEvent changeTargetEvent = CommonHooks.onLivingChangeTarget(body, targetEntity, LivingTargetType.BEHAVIOR_TARGET);
                        if (!changeTargetEvent.isCanceled() && changeTargetEvent.getNewAboutToBeSetTarget() != null) {
                           attackTarget.set(changeTargetEvent.getNewAboutToBeSetTarget());
                           cantReachSince.erase();
                           return true;
                        } else {
                           return false;
                        }
                     }
                  }
               }
            })
      );
   }

   @FunctionalInterface
   public interface StartAttackingCondition<E> {
      boolean test(ServerLevel var1, E var2);
   }

   @FunctionalInterface
   public interface TargetFinder<E> {
      Optional<? extends LivingEntity> get(ServerLevel var1, E var2);
   }
}
