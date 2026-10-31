
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class MobWarCombatGoal extends Goal {

    private static final double SEARCH_RADIUS = 30.0D;
    private static final double ATTACK_DISTANCE_SQR = 4.0D;
    private static final double MOVE_SPEED = 1.1D;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final int ATTACK_COOLDOWN = 20;

    private final PathfinderMob mob;
    private final boolean customMelee;

    private int attackCooldown;
    private int recheckCooldown;

    private UUID assignedTarget;

    public MobWarCombatGoal(PathfinderMob mob) {
        this.mob = mob;

        // Quái vật MONSTER giữ AI chiến đấu gốc.
        // Các nhóm khác được hỗ trợ đánh cận chiến.
        this.customMelee =
                mob.getType().getCategory()
                != MobCategory.MONSTER;

        if (customMelee) {
            setFlags(EnumSet.of(
                    Goal.Flag.MOVE,
                    Goal.Flag.LOOK
            ));
        } else {
            setFlags(EnumSet.noneOf(Goal.Flag.class));
        }
    }

    // Chặn AI mặc định chọn tubi2531 hoặc mục tiêu
    // không hợp lệ trong Mob War.
    @SubscribeEvent
    public static void onMobChangeTarget(
            LivingChangeTargetEvent event) {

        if (!MobWarCommands.enabled) {
            return;
        }

        if (!(event.getEntity() instanceof PathfinderMob attacker)) {
            return;
        }

        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        if (!MobWarAI.inDomain(level, attacker)) {
            return;
        }

        LivingEntity target =
                event.getNewAboutToBeSetTarget();

        if (target == null) {
            return;
        }

        if (!MobWarAI.isValidCombatTarget(
                attacker,
                target,
                level
        )) {
            event.setNewAboutToBeSetTarget(null);
        }
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
                || !MobWarAI.inDomain(level, mob)
                || FogDomainTaming.isTamed(mob)) {
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
        if (target != null) {
            mob.setTarget(target);
            assignedTarget = target.getUUID();
        }
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
        attackCooldown = 0;
        recheckCooldown = 0;
    }

    @Override
    public void tick() {

        if (recheckCooldown > 0) {
            recheckCooldown--;
        } else {
            recheckCooldown = 10;

            LivingEntity current = mob.getTarget();
            LivingEntity preferred = findTarget();

            if (preferred != null
                    && (current == null
                    || !validTarget(current))) {
                assignTarget(preferred);
            }
        }

        LivingEntity target = mob.getTarget();

        if (!validTarget(target)) {
            return;
        }
        
                // Creeper cần tiến sát mục tiêu để kích hoạt ngòi nổ gốc.
        if (mob instanceof net.minecraft.world.entity.monster.Creeper) {

            mob.getLookControl().setLookAt(
                    target,
                    30.0F,
                    30.0F
            );

            if (mob.distanceToSqr(target) > 9.0D) {
                mob.getNavigation().moveTo(
                        target,
                        MOVE_SPEED
                );
            } else {
                // Để AI Creeper gốc tự đếm ngược và phát nổ.
                mob.getNavigation().stop();
            }

            return;
        }

        // Các quái vật MONSTER khác giữ AI chiến đấu gốc.
        if (!customMelee) {
            return;
        }

        mob.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        if (mob.distanceToSqr(target) > ATTACK_DISTANCE_SQR) {
            mob.getNavigation().moveTo(
                    target,
                    MOVE_SPEED
            );
            return;
        }

        mob.getNavigation().stop();

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        mob.swing(InteractionHand.MAIN_HAND);

        target.hurt(
                mob.damageSources().mobAttack(mob),
                ATTACK_DAMAGE
        );

        attackCooldown = ATTACK_COOLDOWN;
    }

    @Override
    public void stop() {

        if (customMelee) {
            mob.getNavigation().stop();
        }

        LivingEntity current = mob.getTarget();

        if (current != null
                && assignedTarget != null
                && current.getUUID().equals(assignedTarget)) {
            mob.setTarget(null);
        }

        assignedTarget = null;
        attackCooldown = 0;
        recheckCooldown = 0;
    }
}
