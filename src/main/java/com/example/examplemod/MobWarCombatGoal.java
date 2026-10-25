
package com.example.examplemod;

import java.util.EnumSet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class MobWarCombatGoal extends Goal {

    private static final double RADIUS = 30.0D;
    private static final double ATTACK_DISTANCE_SQR = 4.0D;

    private final PathfinderMob mob;
    private int attackCooldown = 0;

    public MobWarCombatGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean inDomain(LivingEntity entity) {
        if (mob.level() != entity.level()) {
            return false;
        }

        for (ServerPlayer player : mob.level()
                .getServer()
                .getPlayerList()
                .getPlayers()) {

            if (player.getName().getString()
                    .equalsIgnoreCase("tubi2531")
                    && player.isAlive()
                    && player.level() == mob.level()
                    && player.distanceToSqr(entity) <= RADIUS * RADIUS) {
                return true;
            }
        }

        return false;
    }

    private boolean validTarget(LivingEntity target) {
        if (!MobWarCommands.enabled
                || target == null
                || target == mob
                || !mob.isAlive()
                || !target.isAlive()
                || target.isRemoved()
                || target.level() != mob.level()) {
            return false;
        }

        if (!(target instanceof Mob)
                && !(target instanceof Player)) {
            return false;
        }

        if (target instanceof Player player
                && player.getName().getString()
                        .equalsIgnoreCase("tubi2531")) {
            return false;
        }

        return inDomain(mob) && inDomain(target);
    }

    @Override
    public boolean canUse() {
        return validTarget(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return validTarget(mob.getTarget());
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();

        if (!validTarget(target)) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            return;
        }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (mob.distanceToSqr(target) > ATTACK_DISTANCE_SQR) {
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
