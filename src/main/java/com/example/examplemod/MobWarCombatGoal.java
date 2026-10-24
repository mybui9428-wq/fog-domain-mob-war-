
package com.example.examplemod;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class MobWarCombatGoal extends Goal {

    private final PathfinderMob mob;
    private int attackCooldown = 0;

    public MobWarCombatGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();

        return MobWarCommands.enabled
                && mob.isAlive()
                && target != null
                && target.isAlive()
                && target != mob;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();

        return MobWarCommands.enabled
                && mob.isAlive()
                && target != null
                && target.isAlive()
                && target != mob
                && target.level() == mob.level();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();

        if (target == null || !target.isAlive()) {
            return;
        }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (mob.distanceToSqr(target) > 2.25D) {
            mob.getNavigation().moveTo(target, 1.1D);
            return;
        }

        mob.getNavigation().stop();

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (attackCooldown <= 0) {
            target.hurt(
                    mob.damageSources().mobAttack(mob),
                    3.0F
            );

            attackCooldown = 20;
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
