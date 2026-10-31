
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.effect.MobEffects;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class MobWarCombatGoal extends Goal {

    private static final double SEARCH_RADIUS = 30.0D;
    private static final int TARGET_RECHECK_TICKS = 10;

    private final PathfinderMob mob;

    private int recheckCooldown = 0;

    // Ghi nhớ mục tiêu do Mob War chọn để có thể bỏ mục tiêu
    // này khi tắt Mob War mà không xóa mục tiêu vanilla khác.
    private UUID assignedTarget = null;

    public MobWarCombatGoal(PathfinderMob mob) {
        this.mob = mob;

        // Không chiếm quyền di chuyển hoặc nhìn của AI gốc.
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    private boolean validTarget(LivingEntity target) {
        return mob.level() instanceof ServerLevel level
                && MobWarAI.isValidCombatTarget(
                        mob,
                        target,
                        level
                );
    }

    private LivingEntity findTarget() {

        if (!(mob.level() instanceof ServerLevel level)
                || !MobWarCommands.enabled
                || !MobWarAI.inDomain(level, mob)) {
            return null;
        }

        List<LivingEntity> candidates =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        mob.getBoundingBox().inflate(SEARCH_RADIUS),
                        entity ->
                                entity != mob
                                && MobWarAI.isValidCombatTarget(
                                        mob,
                                        entity,
                                        level
                                )
                );

        if (candidates.isEmpty()) {
            return null;
        }

        // Ưu tiên sinh vật phát sáng.
        // Nếu cùng trạng thái phát sáng thì chọn con gần nhất.
        candidates.sort(
                Comparator
                        .comparing(
                                (LivingEntity entity) ->
                                        !MobWarAI.isFocused(entity)
                        )
                        .thenComparingDouble(
                                entity -> mob.distanceToSqr(entity)
                        )
        );

        return candidates.get(0);
    }

    private void assignTarget(LivingEntity target) {
        if (target == null) {
            return;
        }

        mob.setTarget(target);
        assignedTarget = target.getUUID();
    }

    @Override
    public boolean canUse() {

        if (!MobWarCommands.enabled
                || !mob.isAlive()
                || mob.isRemoved()
                || !(mob.level() instanceof ServerLevel level)
                || !MobWarAI.inDomain(level, mob)
                || FogDomainTaming.isTamed(mob)) {
            return false;
        }

        LivingEntity current = mob.getTarget();

        if (validTarget(current)) {
            return true;
        }

        LivingEntity target = findTarget();

        if (target == null) {
            return false;
        }

        assignTarget(target);
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
        recheckCooldown = 0;
    }

    @Override
    public void tick() {

        if (recheckCooldown > 0) {
            recheckCooldown--;
            return;
        }

        recheckCooldown = TARGET_RECHECK_TICKS;

        LivingEntity current = mob.getTarget();

        if (!validTarget(current)) {
            LivingEntity replacement = findTarget();

            if (replacement != null) {
                assignTarget(replacement);
            } else {
                mob.setTarget(null);
                assignedTarget = null;
            }

            return;
        }

        LivingEntity preferred = findTarget();

        // Chỉ đổi mục tiêu khi tìm thấy mục tiêu phát sáng
        // tốt hơn mục tiêu hiện tại.
        if (preferred != null
                && preferred != current
                && MobWarAI.isFocused(preferred)
                && !MobWarAI.isFocused(current)) {

            assignTarget(preferred);
        }
    }

    @Override
    public void stop() {

        // Chỉ xóa mục tiêu mà chính Mob War đã gán.
        LivingEntity current = mob.getTarget();

        if (current != null
                && assignedTarget != null
                && current.getUUID().equals(assignedTarget)) {
            mob.setTarget(null);
        }

        assignedTarget = null;
        recheckCooldown = 0;
    }
}
