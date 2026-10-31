
package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FogDomainPossession {

    private FogDomainPossession() {}

    private static final String CONTROLLER = "tubi2531";
    private static final double MAX_RANGE = 25.0D;

    private static final Map<UUID, UUID> POSSESSED =
            new HashMap<>();

    private static final Map<UUID, Vec3> BODY_POSITIONS =
            new HashMap<>();

    private static final Map<UUID, Boolean> OLD_INVULNERABLE =
            new HashMap<>();

    private static final Map<UUID, Long> LAST_ATTACK =
            new HashMap<>();

    public static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase(CONTROLLER);
    }

    public static boolean isPossessing(ServerPlayer player) {
        return POSSESSED.containsKey(player.getUUID());
    }

    public static Entity getTarget(ServerPlayer player) {
        UUID targetId = POSSESSED.get(player.getUUID());

        if (targetId == null) {
            return null;
        }

        Entity target = player.level().getEntity(targetId);

        if (target == null || !target.isAlive()) {
            return null;
        }

        return target;
    }

    public static boolean start(
            ServerPlayer player,
            Entity target
    ) {
        if (!isController(player)) {
            return false;
        }

        if (!(target instanceof Mob)) {
            return false;
        }

        if (!target.isAlive()
                || target.level() != player.level()
                || player.distanceTo(target) > MAX_RANGE + 2.0D) {
            return false;
        }

        if (isPossessing(player)) {
            stop(player);
        }

        UUID playerId = player.getUUID();

        POSSESSED.put(playerId, target.getUUID());
        BODY_POSITIONS.put(playerId, player.position());

        OLD_INVULNERABLE.put(
                playerId,
                player.isInvulnerable()
        );

        player.setInvulnerable(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);

        PacketDistributor.sendToPlayer(
                player,
                new FogPossessionSyncPayload(
                        1,
                        target.getId()
                )
        );

        return true;
    }

    public static boolean stop(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (!POSSESSED.containsKey(playerId)) {
            return false;
        }

        POSSESSED.remove(playerId);
        BODY_POSITIONS.remove(playerId);
        LAST_ATTACK.remove(playerId);

        boolean wasInvulnerable =
                OLD_INVULNERABLE.getOrDefault(
                        playerId,
                        false
                );

        OLD_INVULNERABLE.remove(playerId);

        player.setInvulnerable(wasInvulnerable);
        player.setDeltaMovement(Vec3.ZERO);

        PacketDistributor.sendToPlayer(
                player,
                new FogPossessionSyncPayload(0, 0)
        );

        return true;
    }

    public static Vec3 getBodyPosition(ServerPlayer player) {
        return BODY_POSITIONS.get(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (!isPossessing(player)) {
            return;
        }

        Entity target = getTarget(player);

        if (target == null
                || target.level() != player.level()
                || !target.isAlive()) {
            stop(player);
            return;
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
    }

    public static void stopAll() {
        POSSESSED.clear();
        BODY_POSITIONS.clear();
        OLD_INVULNERABLE.clear();
        LAST_ATTACK.clear();
    }

    public static void control(
            ServerPlayer player,
            float forward,
            float strafe,
            float yaw,
            float pitch,
            boolean jump,
            boolean attack
    ) {
        if (!isController(player) || !isPossessing(player)) {
            return;
        }

        Entity entity = getTarget(player);

        if (!(entity instanceof Mob mob) || !mob.isAlive()) {
            stop(player);
            return;
        }

        // Giới hạn dữ liệu điều khiển
        forward = Math.max(-1.0F, Math.min(1.0F, forward));
        strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

        // Xoay sinh vật theo camera
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);
        mob.yRotO = yaw;
        mob.xRotO = pitch;

        // Tính hướng nhìn theo camera
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double cosPitch = Math.cos(pitchRad);

        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * cosPitch;

        // Hướng sang trái/phải
        double sideX = Math.cos(yawRad);
        double sideZ = Math.sin(yawRad);

        double moveX = lookX * forward + sideX * strafe;
        double moveZ = lookZ * forward + sideZ * strafe;

        boolean canMoveVertically =
                mob.isNoGravity() || mob.isInWater();

        double moveY = canMoveVertically
                ? lookY * forward
                : 0.0D;

        Vec3 movement = new Vec3(moveX, moveY, moveZ);

        if (movement.lengthSqr() > 1.0D) {
            movement = movement.normalize();
        }

        // Điều khiển bằng MoveControl của sinh vật
        if (movement.lengthSqr() > 0.001D) {
            double distance = 3.0D;

            mob.getMoveControl().setWantedPosition(
                    mob.getX() + movement.x * distance,
                    mob.getY() + movement.y * distance,
                    mob.getZ() + movement.z * distance,
                    1.0D
            );
        } else {
            mob.getNavigation().stop();
        }

        // Nhảy
        if (jump && mob.onGround()) {
            mob.jumpFromGround();
        }

        // Tấn công có thời gian hồi
        if (attack) {
            long gameTime = player.level().getGameTime();

            long lastAttack = LAST_ATTACK.getOrDefault(
                    player.getUUID(),
                    0L
            );

            if (gameTime - lastAttack >= 10L) {
                HitResult hit = mob.pick(
                        4.0D,
                        0.0F,
                        false
                );

                if (hit.getType() == HitResult.Type.ENTITY
                        && hit instanceof EntityHitResult entityHit) {

                    Entity victim = entityHit.getEntity();

                    if (victim instanceof LivingEntity living
                            && victim != mob
                            && victim != player
                            && victim.isAlive()) {

                        mob.doHurtTarget(
                                player.level(),
                                living
                        );

                        LAST_ATTACK.put(
                                player.getUUID(),
                                gameTime
                        );
                    }
                }
            }
        }
    }
}
