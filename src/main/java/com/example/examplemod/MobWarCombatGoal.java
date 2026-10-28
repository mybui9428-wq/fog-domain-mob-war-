
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class MobWarCombatGoal extends Goal {

    private static final double ATTACK_DISTANCE_SQR = 4.0D;
    private static final double SEARCH_RADIUS = 30.0D;
    private static final double MOVE_SPEED = 1.1D;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int TARGET_RECHECK_TICKS = 10;

    private final PathfinderMob mob;

    private int attackCooldown;
    private int targetRecheckCooldown;

    public MobWarCombatGoal(PathfinderMob mob) {
        this.mob = mob;

        setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }

    private boolean validTarget(LivingEntity target) {
        return mob.level() instanceof ServerLevel level
                && MobWarAI.isValidCombatTarget(
                        mob, target, level
                );
    }

    private LivingEntity findTarget() {
        if (!(mob.level() instanceof ServerLevel level)
                || !MobWarCommands.enabled
                || !MobWarAI.inDomain(level, mob)
                || FogDomainTaming.isTamed(mob)) {
            return null;
        }

        List<LivingEntity> candidates =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        mob.getBoundingBox().inflate(SEARCH_RADIUS),
                        entity -> entity != mob
                                && MobWarAI.isValidCombatTarget(
                                        mob, entity, level
                                )
                );

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distance = mob.distanceToSqr(candidate);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    @Override
    public boolean canUse() {
        if (!MobWarCommands.enabled
                || !mob.isAlive()
                || mob.isRemoved()
                || FogDomainTaming.isTamed(mob)
                || !(mob.level() instanceof ServerLevel level)
                || !MobWarAI.inDomain(level, mob)) {
            return false;
        }

        LivingEntity currentTarget = mob.getTarget();

        if (validTarget(currentTarget)) {
            return true;
        }

        if (targetRecheckCooldown > 0) {
            targetRecheckCooldown--;
            return false;
        }

        targetRecheckCooldown = TARGET_RECHECK_TICKS;

        LivingEntity target = findTarget();

        if (target == null) {
            return false;
        }

        mob.setTarget(target);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return MobWarCommands.enabled
                && mob.isAlive()
                && !mob.isRemoved()
                && !FogDomainTaming.isTamed(mob)
                && validTarget(mob.getTarget());
    }

    @Override
    public void start() {
        attackCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();

        if (!validTarget(target)) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            return;
        }

        mob.getLookControl().setLookAt(
                target, 30.0F, 30.0F
        );

        double distance = mob.distanceToSqr(target);

        if (distance > ATTACK_DISTANCE_SQR) {
            mob.getNavigation().moveTo(
                    target, MOVE_SPEED
            );
            return;
        }

        mob.getNavigation().stop();

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (attackCooldown <= 0) {
            target.hurt(
                    mob.damageSources().mobAttack(mob),
                    ATTACK_DAMAGE
            );

            attackCooldown = ATTACK_COOLDOWN_TICKS;
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();

        LivingEntity target = mob.getTarget();

        if (!validTarget(target)) {
            mob.setTarget(null);
        }
    }
}
