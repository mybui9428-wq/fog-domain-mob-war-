
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.concurrent.ThreadLocalRandom;

public final class FogDomainMinionAI {

    private static final double TARGET_RANGE = 10.0D;
    private static final double RETURN_DISTANCE = 32.0D;
    private static final double TELEPORT_BEHIND_DISTANCE = 2.5D;
    private static final float ATTACK_DAMAGE = 4.0F;
    private static final long ATTACK_INTERVAL = 20L;

    // UUID của linh thể -> UUID chủ nhân.
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();

    // Thời điểm được phép đánh tiếp.
    private static final Map<UUID, Long> ATTACK_TIMES = new HashMap<>();

    // Đánh dấu bộ xương đã triệu hồi quân.
    private static final Set<UUID> SUMMONED = new HashSet<>();

    private FogDomainMinionAI() {
    }

    // Gắn chủ nhân cho linh thể khi triệu hồi.
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
        SUMMONED.remove(id);
    }

    // AI dùng chung cho cả ba linh thể chính và Thây Ma Linh Hồn.
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

        // Bộ Xương Nhiều Tay triệu hồi 5–7 Thây Ma Linh Hồn.
        if (minion instanceof MultiArmedSkeletonEntity) {
            summonSoulZombies(minion, owner, level);
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

        // Tìm tất cả sinh vật sống trong bán kính 10 khối quanh chủ nhân.
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

        if (target == null) {
            minion.setTarget(null);
            minion.getNavigation().stop();
            return;
        }

        minion.setTarget(target);
        minion.getNavigation().moveTo(target, 1.25D);

        // Tấn công khi ở đủ gần.
        double attackDistance = minion.getBbWidth()
                + target.getBbWidth() + 0.5D;

        if (minion.distanceToSqr(target)
                <= attackDistance * attackDistance) {

            long now = level.getGameTime();

            long nextAttack = ATTACK_TIMES.getOrDefault(
                    minion.getUUID(), 0L
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

    private static boolean isValidTarget(
            LivingEntity entity,
            Mob minion,
            ServerPlayer owner
    ) {
        if (entity == null || !entity.isAlive()
                || entity.isRemoved()) {
            return false;
        }

        // Tuyệt đối không tấn công chủ nhân.
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

        // Không đánh các linh thể cùng chủ nhân.
        UUID otherOwner = OWNERS.get(entity.getUUID());

        if (owner.getUUID().equals(otherOwner)) {
            return false;
        }

        return true;
    }

    private static void summonSoulZombies(
            Mob skeleton,
            ServerPlayer owner,
            ServerLevel level
    ) {
        UUID skeletonId = skeleton.getUUID();

        if (SUMMONED.contains(skeletonId)) return;

        SUMMONED.add(skeletonId);

        int amount = ThreadLocalRandom.current().nextInt(5, 8);

        for (int i = 0; i < amount; i++) {
            var soulZombie = FogDomainEntities.SOUL_ZOMBIE
        .get()
        .create(
                level,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED
        );

            if (soulZombie == null) continue;

            double angle = (Math.PI * 2.0D * i) / amount;
            double x = skeleton.getX() + Math.cos(angle) * 1.5D;
            double z = skeleton.getZ() + Math.sin(angle) * 1.5D;

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
            }
        }
    }
}
