
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
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
    private static final double FOLLOW_DISTANCE = 3.0D;

    private static final double TELEPORT_BEHIND_DISTANCE = 2.5D;

    private static final float ATTACK_DAMAGE = 4.0F;
    private static final long ATTACK_INTERVAL = 20L;

    private static final float REPLACEMENT_HEALTH_PERCENT = 0.25F;

    private static final Map<UUID, UUID> OWNERS = new HashMap<>();
    private static final Map<UUID, Long> ATTACK_TIMES = new HashMap<>();

    private static final Set<UUID> INITIAL_SUMMONED = new HashSet<>();

    private static final Map<UUID, Set<UUID>> SOUL_ZOMBIE_GROUPS =
            new HashMap<>();

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

        Set<UUID> removedGroup = SOUL_ZOMBIE_GROUPS.remove(id);

        if (removedGroup != null) {
            for (UUID zombieId : removedGroup) {
                OWNERS.remove(zombieId);
                ATTACK_TIMES.remove(zombieId);
                REPLACED_ZOMBIES.remove(zombieId);
            }
        }

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

        boolean flying = minion instanceof IceSpiritEntity
                || minion instanceof MultiArmedSkeletonEntity;

        // Nếu quá xa chủ nhân, đưa linh thể về phía sau chủ nhân.
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

        // Tìm mục tiêu trong phạm vi 10 khối quanh chủ nhân.
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

                // Khống chế mục tiêu trước.
                controlTarget(target);

                // Chỉ triệu hồi lần đầu sau khi đã khống chế.
                if (INITIAL_SUMMONED.add(minion.getUUID())) {

                    summonSoulZombies(
                            minion,
                            owner,
                            level,
                            7
                    );
                }
            }

            // Sau khi triệu hồi lần đầu, duy trì đội quân.
            if (INITIAL_SUMMONED.contains(minion.getUUID())) {

                maintainSoulZombieArmy(
                        minion,
                        owner,
                        level
                );
            }
        }

        // Không tìm thấy mục tiêu: đi theo chủ nhân.
        if (target == null) {

            minion.setTarget(null);

            if (minion.distanceToSqr(owner)
                    > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {

                if (flying) {
                    flyToward(minion, owner, 0.22D);
                } else {
                    minion.getNavigation().moveTo(owner, 1.15D);
                }

            } else {
                minion.getNavigation().stop();

                if (flying) {
                    Vec3 velocity = minion.getDeltaMovement();

                    minion.setDeltaMovement(
                            velocity.x * 0.7D,
                            Math.sin(minion.tickCount * 0.08D) * 0.025D,
                            velocity.z * 0.7D
                    );
                }
            }

            return;
        }

        // Di chuyển đến mục tiêu.
        minion.setTarget(target);

        if (flying) {
            flyToward(minion, target, 0.24D);
        } else {
            minion.getNavigation().moveTo(target, 1.25D);
        }

        // Bộ Xương Nhiều Tay tiếp tục khống chế mục tiêu.
        if (minion instanceof MultiArmedSkeletonEntity) {
            controlTarget(target);
        }

        // Tấn công khi đủ gần.
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

    // Điều khiển chuyển động bay trực tiếp.
    private static void flyToward(
            Mob minion,
            LivingEntity target,
            double speed
    ) {

        Vec3 direction = target.position()
                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(
                        minion.position().add(
                                0.0D,
                                minion.getBbHeight() * 0.5D,
                                0.0D
                        )
                );

        if (direction.lengthSqr() < 0.01D) {
            minion.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 movement = direction.normalize().scale(speed);

        minion.setDeltaMovement(
                movement.x,
                movement.y + Math.sin(minion.tickCount * 0.08D) * 0.01D,
                movement.z
        );

        minion.move(MoverType.SELF, minion.getDeltaMovement());
    }

    // Khống chế mục tiêu.
    private static void controlTarget(LivingEntity target) {

        if (target == null || !target.isAlive()) return;

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

        if (entity.getUUID().equals(owner.getUUID())) {
            return false;
        }

        if (entity instanceof ArmorStand) {
            return false;
        }

        if (entity.getUUID().equals(minion.getUUID())) {
            return false;
        }

        UUID otherOwner = OWNERS.get(entity.getUUID());

        if (owner.getUUID().equals(otherOwner)) {
            return false;
        }

        return true;
    }

    // Triệu hồi Thây Ma Linh Hồn.
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

    // Duy trì đúng 7 quân khỏe mạnh.
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
        int newlyReplaced = 0;

        for (UUID zombieId : new HashSet<>(group)) {

            Entity entity = level.getEntity(zombieId);

            if (!(entity instanceof LivingEntity zombie)
                    || !zombie.isAlive()
                    || zombie.isRemoved()) {

                group.remove(zombieId);
                REPLACED_ZOMBIES.remove(zombieId);
                OWNERS.remove(zombieId);
                ATTACK_TIMES.remove(zombieId);
                continue;
            }

            float maxHealth = zombie.getMaxHealth();

            if (maxHealth <= 0.0F) continue;

            float healthPercent = zombie.getHealth() / maxHealth;

            if (healthPercent > REPLACEMENT_HEALTH_PERCENT) {

                healthyCount++;

            } else if (REPLACED_ZOMBIES.add(zombieId)) {

                // Mỗi quân yếu chỉ kích hoạt thay thế một lần.
                newlyReplaced++;
            }
        }

        // Bổ sung quân khỏe còn thiếu.
        int needed = Math.max(
                0,
                7 - healthyCount
        );

        // Chỉ triệu hồi khi thực sự thiếu quân khỏe.
        if (needed > 0 && newlyReplaced >= 0) {

            summonSoulZombies(
                    skeleton,
                    owner,
                    level,
                    needed
            );
        }
    }
}
