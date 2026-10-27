
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainProtection {

    private static final double RADIUS = 30.0D;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;

    private static final Map<UUID, Vec3> LAST_INSIDE = new HashMap<>();

    private FogDomainProtection() {
    }

    private static ServerPlayer findController(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.getName().getString().equalsIgnoreCase("tubi2531")) {
                return player;
            }
        }
        return null;
    }

    private static boolean insideDomain(ServerPlayer controller, Entity entity) {
        return controller != null
                && controller.isAlive()
                && controller.distanceToSqr(entity) <= RADIUS_SQUARED;
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!MobWarCommands.enabled) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer controller = findController(level);
        if (controller == null) {
            return;
        }

        Vec3 explosionPos = event.getExplosion().center();
        if (controller.distanceToSqr(
                explosionPos.x,
                explosionPos.y,
                explosionPos.z
        ) <= RADIUS_SQUARED) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!MobWarCommands.enabled) {
            LAST_INSIDE.clear();
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServerPlayer controller = findController(level);

            if (controller == null) {
                continue;
            }

            for (Entity entity : level.getAllEntities()) {
                if (entity == controller || entity.isRemoved()) {
                    continue;
                }

                UUID id = entity.getUUID();

                if (insideDomain(controller, entity)) {
                    LAST_INSIDE.put(id, entity.position());
                    continue;
                }

                Vec3 lastPosition = LAST_INSIDE.get(id);

                if (lastPosition != null) {
                    entity.setPos(
                            lastPosition.x,
                            lastPosition.y,
                            lastPosition.z
                    );
                    entity.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        LAST_INSIDE.keySet().removeIf(id -> {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity != null && !entity.isRemoved()) {
                    return false;
                }
            }
            return true;
        });
    }
}
