
package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class FogDomainPossession {
    private FogDomainPossession() {}

    private static final String CONTROLLER = "tubi2531";
    private static final double MAX_RANGE = 25.0D;

    private static final Map<UUID, UUID> POSSESSED = new HashMap<>();
    private static final Map<UUID, Vec3> BODY_POSITIONS = new HashMap<>();
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

        return player.level().getEntity(targetId);
    }

    public static boolean start(ServerPlayer player, Entity target) {
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

        POSSESSED.put(player.getUUID(), target.getUUID());
        BODY_POSITIONS.put(
                player.getUUID(),
                player.position()
        );
        OLD_INVULNERABLE.put(
                player.getUUID(),
                player.isInvulnerable()
        );

        player.setInvulnerable(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);

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
                OLD_INVULNERABLE.getOrDefault(playerId, false);

        OLD_INVULNERABLE.remove(playerId);
        player.setInvulnerable(wasInvulnerable);
        player.setDeltaMovement(Vec3.ZERO);

        return true;
    }

    public static Vec3 getBodyPosition(ServerPlayer player) {
        return BODY_POSITIONS.get(player.getUUID());
    }
}
