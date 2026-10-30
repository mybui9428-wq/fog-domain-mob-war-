
package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
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
        if (!isController(player) || isPossessing(player)) {
            return false;
        }

        if (!(target instanceof Mob)) {
            return false;
        }

        if (!target.isAlive()
                || target.level() != player.level()
                || player.distanceToSqr(target)
                        > MAX_RANGE * MAX_RANGE) {
            return false;
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

        boolean wasInvulnerable =
                OLD_INVULNERABLE.getOrDefault(
                        playerId,
                        false
                );

        OLD_INVULNERABLE.remove(playerId);

        player.setInvulnerable(wasInvulnerable);

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

        // Giới hạn dữ liệu điều khiển từ client
        forward = Math.max(-1.0F, Math.min(1.0F, forward));
        strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

        // Xoay sinh vật theo hướng camera
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);
        mob.yRotO = yaw;
        mob.xRotO = pitch;

        // Tính hướng di chuyển theo camera
        float radians = yaw * ((float) Math.PI / 180.0F);

        double sin = Math.sin(radians);
        double cos = Math.cos(radians);

        double motionX = (-sin * forward) + (cos * strafe);
        double motionZ = (cos * forward) + (sin * strafe);

        Vec3 movement = new Vec3(
                motionX,
                0.0D,
                motionZ
        );

        if (movement.lengthSqr() > 1.0D) {
            movement = movement.normalize();
        }

        // Di chuyển sinh vật
        mob.setDeltaMovement(
                movement.x * 0.25D,
                mob.getDeltaMovement().y,
                movement.z * 0.25D
        );

        mob.setSpeed(0.25F);
        mob.hurtMarked = true;

        // Nhảy
        if (jump && mob.onGround()) {
            mob.jumpFromGround();
        }

        // Tấn công mục tiêu phía trước
        if (attack) {
            Entity target = mob.pick(
                    4.0D,
                    0.0F,
                    false
            ).getType() == net.minecraft.world.phys.HitResult.Type.ENTITY
                    ? ((net.minecraft.world.phys.EntityHitResult) mob.pick(
                            4.0D,
                            0.0F,
                            false
                    )).getEntity()
                    : null;

            if (target instanceof net.minecraft.world.entity.LivingEntity living
                    && living != mob
                    && living != player
                    && living.isAlive()) {
                mob.doHurtTarget(player.level(), living);
            }
        }
    }
}
