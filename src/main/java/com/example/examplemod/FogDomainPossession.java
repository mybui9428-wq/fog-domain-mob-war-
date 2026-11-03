
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

    private static final Map<UUID, UUID> POSSESSED = new HashMap<>();
    private static final Map<UUID, Vec3> BODY_POSITIONS = new HashMap<>();
    private static final Map<UUID, Boolean> OLD_INVULNERABLE = new HashMap<>();
    private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();

    private static final Map<UUID, Boolean> SOUL_MODE = new HashMap<>();
    private static final Map<UUID, Boolean> OLD_INVISIBLE = new HashMap<>();
    private static final Map<UUID, Boolean> OLD_NO_GRAVITY = new HashMap<>();
    private static final Map<UUID, Vec3> SOUL_BODY_POSITIONS = new HashMap<>();

    public static boolean isController(ServerPlayer player) {
        return player.getName().getString().equalsIgnoreCase(CONTROLLER);
    }

    public static boolean isPossessing(ServerPlayer player) {
        return POSSESSED.containsKey(player.getUUID());
    }

    public static boolean isSoulMode(ServerPlayer player) {
        return SOUL_MODE.getOrDefault(player.getUUID(), false);
    }

    public static boolean startSoul(ServerPlayer player) {
        if (!isController(player) || isSoulMode(player) || isPossessing(player)) {
            return false;
        }

        UUID id = player.getUUID();

        SOUL_BODY_POSITIONS.put(id, player.position());
        OLD_INVULNERABLE.put(id, player.isInvulnerable());
        OLD_INVISIBLE.put(id, player.isInvisible());
        OLD_NO_GRAVITY.put(id, player.isNoGravity());

        SOUL_MODE.put(id, true);

        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.setHealth(1.0F);

        PacketDistributor.sendToPlayer(
                player,
                new FogPossessionSyncPayload(2, 0)
        );

        return true;
    }

    public static boolean stopSoul(ServerPlayer player) {
    UUID id = player.getUUID();

    // Xóa trạng thái linh hồn trước.
    SOUL_MODE.remove(id);

    // Khôi phục người chơi về trạng thái bình thường.
    player.setInvulnerable(false);
    player.setInvisible(false);
    player.setNoGravity(false);

    // Xóa dữ liệu trạng thái cũ.
    OLD_INVULNERABLE.remove(id);
    OLD_INVISIBLE.remove(id);
    OLD_NO_GRAVITY.remove(id);
    SOUL_BODY_POSITIONS.remove(id);

    // Xóa vận tốc còn sót lại.
    player.setDeltaMovement(Vec3.ZERO);

    // Đồng bộ trạng thái thoát linh hồn về client.
    PacketDistributor.sendToPlayer(
            player,
            new FogPossessionSyncPayload(3, 0)
    );

    return true;
            }

    public static Vec3 getSoulBodyPosition(ServerPlayer player) {
        return SOUL_BODY_POSITIONS.get(player.getUUID());
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

    public static boolean start(ServerPlayer player, Entity target) {
        if (!isController(player)
                || !(target instanceof Mob)
                || !target.isAlive()
                || target.level() != player.level()
                || player.distanceTo(target) > MAX_RANGE + 2.0D
                || isSoulMode(player)) {
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
                new FogPossessionSyncPayload(1, target.getId())
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
                OLD_INVULNERABLE.getOrDefault(playerId, false);

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

        SOUL_MODE.clear();
        OLD_INVISIBLE.clear();
        OLD_NO_GRAVITY.clear();
        SOUL_BODY_POSITIONS.clear();
    }

    public static void controlSoul(
        ServerPlayer player,
        float forward,
        float strafe,
        float yaw,
        float pitch,
        boolean jump
) {
    if (!isController(player) || !isSoulMode(player)) {
        return;
    }

    forward = Math.max(-1.0F, Math.min(1.0F, forward));
    strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
    pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

    player.setYRot(yaw);
    player.setXRot(pitch);
    player.yRotO = yaw;
    player.xRotO = pitch;

    double yawRad = Math.toRadians(yaw);
    double pitchRad = Math.toRadians(pitch);

    double cosPitch = Math.cos(pitchRad);

    Vec3 look = new Vec3(
            -Math.sin(yawRad) * cosPitch,
            -Math.sin(pitchRad),
            Math.cos(yawRad) * cosPitch
    );

    Vec3 side = new Vec3(
            Math.cos(yawRad),
            0.0D,
            Math.sin(yawRad)
    );

    Vec3 movement = look.scale(forward)
            .add(side.scale(strafe));

    // Nhấn Space để bay lên.
    if (jump) {
        movement = movement.add(0.0D, 1.0D, 0.0D);
    }

    if (movement.lengthSqr() > 1.0D) {
        movement = movement.normalize();
    }

    // Bay mượt bằng vận tốc, không dịch chuyển tức thời.
    double speed = 0.32D;

    player.setNoGravity(true);
    player.setInvulnerable(true);
    player.setInvisible(true);

    if (movement.lengthSqr() > 0.001D) {
        player.setDeltaMovement(movement.scale(speed));
    } else {
        player.setDeltaMovement(Vec3.ZERO);
    }
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

        forward = Math.max(-1.0F, Math.min(1.0F, forward));
        strafe = Math.max(-1.0F, Math.min(1.0F, strafe));
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);
        mob.yRotO = yaw;
        mob.xRotO = pitch;

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);

        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * cosPitch;

        double sideX = Math.cos(yawRad);
        double sideZ = Math.sin(yawRad);

        double moveX = lookX * forward + sideX * strafe;
        double moveZ = lookZ * forward + sideZ * strafe;

        boolean canMoveVertically = mob.isNoGravity() || mob.isInWater();

        double moveY = canMoveVertically ? lookY * forward : 0.0D;

        Vec3 movement = new Vec3(moveX, moveY, moveZ);

        if (movement.lengthSqr() > 1.0D) {
            movement = movement.normalize();
        }

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

        if (jump && mob.onGround()) {
            mob.jumpFromGround();
        }

        if (attack) {
            long gameTime = player.level().getGameTime();

            long lastAttack = LAST_ATTACK.getOrDefault(
                    player.getUUID(),
                    0L
            );

            if (gameTime - lastAttack >= 10L) {
                HitResult hit = mob.pick(4.0D, 0.0F, false);

                if (hit.getType() == HitResult.Type.ENTITY
                        && hit instanceof EntityHitResult entityHit) {

                    Entity victim = entityHit.getEntity();

                    if (victim instanceof LivingEntity living
                            && victim != mob
                            && victim != player
                            && victim.isAlive()) {

                        mob.doHurtTarget(player.level(), living);

                        LAST_ATTACK.put(player.getUUID(), gameTime);
                    }
                }
            }
        }
    }
}
