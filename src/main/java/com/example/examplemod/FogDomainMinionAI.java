
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FogDomainMinionAI {

    private static final double TARGET_RANGE = 10.0D;
    private static final double RETURN_DISTANCE = 32.0D;
    private static final double TELEPORT_BEHIND_DISTANCE = 2.5D;

    private static final float ATTACK_DAMAGE = 4.0F;
    private static final long ATTACK_INTERVAL = 20L;

    // Máu dưới 25% sẽ kích hoạt triệu hồi quân thay thế.
    private static final float REPLACEMENT_HEALTH_PERCENT = 0.25F;

    // UUID linh thể -> UUID chủ nhân.
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();

    // Thời gian được phép tấn công tiếp.
    private static final Map<UUID, Long> ATTACK_TIMES = new HashMap<>();

    // Bộ Xương Nhiều Tay đã triệu hồi 7 quân ban đầu.
    private static final Set<UUID> INITIAL_SUMMONED = new HashSet<>();

    // Bộ Xương Nhiều Tay -> danh sách Thây Ma Linh Hồn.
    private static final Map<UUID, Set<UUID>> SOUL_ZOMBIE_GROUPS =
            new HashMap<>();

    // Đánh dấu Thây Ma Linh Hồn đã được thay thế.
    private static final Set<UUID> REPLACED_ZOMBIES = new HashSet<>();

    private FogDomainMinionAI() {
    }

    public static void bindOwner(Entity minion, ServerPlayer owner) {
        if (minion == null || owner == null) return;

        OWNERS.put(minion.getUUID(), owner.getUUID());
    }

    public static UUID getOwnerId(Entity minion) {
        if (minion == null) return null;

        return OWNERS.get(minion.getUUID());
    }

    public static void removeMinion(Entity minion) {
        if (minion == null) return;

        UUID id = minion.getUUID();

        OWNERS.remove(id);
        ATTACK_TIMES.remove(id);
        REPLACED_ZOMBIES.remove(id);

        INITIAL_SUMMONED.remove(id);
        SOUL_ZOMBIE_GROUPS.remove(id);

        // Xóa Thây Ma Linh Hồn khỏi danh sách của Bộ Xương.
        for (Set<UUID> group : SOUL_ZOMBIE_GROUPS.values()) {
            group.remove(id);
        }
    }

    public static void tickMinion(Mob minion) {

        if (minion == null || minion.level().isClientSide()) return;

        if (!(minion.level() instanceof ServerLevel level)) return;

        UUID ownerId = getOwnerId(minion);

        if (ownerId == null) return;

        ServerPlayer owner = level.getServer()
                .getPlayerList()
                .getPlayer(ownerId);

        if (owner == null || !owner.isAlive()
                || owner.level() != level) {

            minion.setTarget(null);
            minion.getNavigation().stop();
            return;
        }

        // Không để linh thể đi quá xa chủ nhân.
        if (minion.distanceToSqr(owner)
                > RETURN_DISTANCE * RETURN_DISTANCE) {

            Vec3 behind = owner.getLookAngle()
                    .normalize()
                    .scale(-TELEPORT_BEHIND_DISTANCE);

            Vec3 destination = owner.position().add(behind);

            minion.getNavigation().stop();
            minion.setTarget(null);

            minion.teleportTo(
                    level,
                    destination.x,
                    destination.y,
                    destination.z,
                    Set.of(),
                    owner.getYRot(),
                    owner.getXRot(),
                    true
            );

            return;
        }

        // Tìm sinh vật sống trong bán kính 10 khối quanh chủ nhân.
        AABB searchBox = owner.getBoundingBox().inflate(TARGET_RANGE);

        LivingEntity target = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> isValidTarget(entity, minion, owner)
        ).stream()
                .min((a, b) -> Double.compare(
                        minion.distanceToSqr(a),
                        minion.distanceToSqr(b)
                ))
                .orElse(null);

        // -----------------------------------------
        // BỘ XƯƠNG NHIỀU TAY
        // -----------------------------------------
        if (minion instanceof MultiArmedSkeletonEntity) {

            if (target != null) {

                minion.setTarget(target);
                minion.getNavigation().moveTo(target, 1.25D);

                // Khống chế mục tiêu trước khi triệu hồi.
                controlTarget(target);

                // Chỉ triệu hồi 7 con sau khi đã khống chế.
                if (INITIAL_SUMMONED.add(minion.getUUID())) {

                    summonSoulZombies(
                            minion,
                            owner,
                            level,
                            7
                    );
                }
            }

            // Sau lần triệu hồi đầu tiên, luôn duy trì đội quân.
            if (INITIAL_SUMMONED.contains(minion.getUUID())) {

                maintainSoulZombieArmy(
                        minion,
                        owner,
                        level
                );
            }
        }

        if (target == null) {

            minion.setTarget(null);
            minion.getNavigation().stop();
            return;
        }

        // Các linh thể khác vẫn tấn công mục tiêu bình thường.
        if (!(minion instanceof MultiArmedSkeletonEntity)) {

            minion.setTarget(target);
            minion.getNavigation().moveTo(target, 1.25D);
        }

        // Tấn công khi ở đủ gần.
        double attackDistance = minion.getBbWidth()
                + target.getBbWidth() + 0.5D;

        if (minion.distanceToSqr(target)
                <= attackDistance * attackDistance) {

            long now = level.getGameTime();

            long nextAttack = ATTACK_TIMES.getOrDefault(
                    minion.getUUID(),
                    0L
            );

            if (now >= nextAttack) {

                target.hurtServer(
                        level,
                        minion.damageSources().mobAttack(minion),
                        ATTACK_DAMAGE
                );

                ATTACK_TIMES.put(
                        minion.getUUID(),
                        now + ATTACK_INTERVAL
                );
            }
        }
    }

    // Khống chế mục tiêu.
    private static void controlTarget(LivingEntity target) {

        if (target == null || !target.isAlive()) return;

        // Làm chậm cấp độ cao trong 2 giây.
        target.addEffect(
                new MobEffectInstance(
                        MobEffects.SLOWNESS,
                        40,
                        10,
                        false,
                        false,
                        true
                )
        );

        // Ngăn mục tiêu di chuyển trong thời gian khống chế.
        target.setDeltaMovement(Vec3.ZERO);

        if (target instanceof Mob targetMob) {
            targetMob.getNavigation().stop();
            targetMob.setTarget(null);
        }
    }

    private static boolean isValidTarget(
            LivingEntity entity,
            Mob minion,
            ServerPlayer owner
    ) {

        if (entity == null || !entity.isAlive()
                || entity.isRemoved()) {
            return false;
        }

        // Không tấn công chủ nhân.
        if (entity.getUUID().equals(owner.getUUID())) {
            return false;
        }

        // Không đánh Armor Stand.
        if (entity instanceof ArmorStand) {
            return false;
        }

        // Không đánh chính mình.
        if (entity.getUUID().equals(minion.getUUID())) {
            return false;
        }

        // Không đánh linh thể cùng chủ nhân.
        UUID otherOwner = OWNERS.get(entity.getUUID());

        if (owner.getUUID().equals(otherOwner)) {
            return false;
        }

        return true;
    }

    // Triệu hồi số lượng Thây Ma Linh Hồn.
    private static void summonSoulZombies(
            Mob skeleton,
            ServerPlayer owner,
            ServerLevel level,
            int amount
    ) {

        UUID skeletonId = skeleton.getUUID();

        Set<UUID> group = SOUL_ZOMBIE_GROUPS.computeIfAbsent(
                skeletonId,
                key -> new HashSet<>()
        );

        for (int i = 0; i < amount; i++) {

            var soulZombie = FogDomainEntities.SOUL_ZOMBIE
                    .get()
                    .create(
                            level,
                            net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED
                    );

            if (soulZombie == null) continue;

            double angle = (Math.PI * 2.0D * i) / amount;

            double x = skeleton.getX()
                    + Math.cos(angle) * 2.0D;

            double z = skeleton.getZ()
                    + Math.sin(angle) * 2.0D;

            soulZombie.snapTo(
                    x,
                    skeleton.getY(),
                    z,
                    skeleton.getYRot(),
                    0.0F
            );

            soulZombie.setPersistenceRequired();

            if (level.addFreshEntity(soulZombie)) {

                bindOwner(soulZombie, owner);

                group.add(soulZombie.getUUID());
            }
        }
    }

    // Duy trì đội quân và triệu hồi quân thay thế.
    private static void maintainSoulZombieArmy(
            Mob skeleton,
            ServerPlayer owner,
            ServerLevel level
    ) {

        UUID skeletonId = skeleton.getUUID();

        Set<UUID> group = SOUL_ZOMBIE_GROUPS.computeIfAbsent(
                skeletonId,
                key -> new HashSet<>()
        );

        int healthyCount = 0;

        for (UUID zombieId : new HashSet<>(group)) {

            Entity entity = level.getEntity(zombieId);

            if (!(entity instanceof LivingEntity zombie)
                    || !zombie.isAlive()
                    || zombie.isRemoved()) {

                group.remove(zombieId);
                REPLACED_ZOMBIES.remove(zombieId);
                continue;
            }

            float maxHealth = zombie.getMaxHealth();

            if (maxHealth <= 0.0F) continue;

            float healthPercent = zombie.getHealth() / maxHealth;

            // Nếu quân còn trên 25% máu thì vẫn tính là quân khỏe.
            if (healthPercent > REPLACEMENT_HEALTH_PERCENT) {

                healthyCount++;

            } else {

                // Quân yếu sắp chết: đánh dấu để thay thế.
                REPLACED_ZOMBIES.add(zombieId);
            }
        }

        // Bổ sung quân để luôn có 7 con khỏe mạnh.
        int needed = 7 - healthyCount;

        if (needed > 0) {

            summonSoulZombies(
                    skeleton,
                    owner,
                    level,
                    needed
            );
        }
    }
}
