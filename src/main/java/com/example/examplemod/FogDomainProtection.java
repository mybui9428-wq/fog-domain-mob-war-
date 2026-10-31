
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainProtection {

    private static final double RADIUS = 30.0D;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;

    private static final double PUSH_STRENGTH = 0.12D;
    private static final int UPDATE_INTERVAL = 5;

    private static int tickCounter = 0;

    // Vị trí gần nhất entity đứng bên trong lĩnh vực.
    private static final Map<UUID, Vec3> LAST_INSIDE =
            new HashMap<>();

    // Lưu quyền bay ban đầu của tubi2531.
    private static final Map<UUID, FlightState> FLIGHT_STATES =
            new HashMap<>();

    private static final class FlightState {
        final boolean mayfly;
        final boolean flying;

        FlightState(boolean mayfly, boolean flying) {
            this.mayfly = mayfly;
            this.flying = flying;
        }
    }

    private FogDomainProtection() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static ServerPlayer findController(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (isController(player) && player.isAlive()) {
                return player;
            }
        }

        return null;
    }

    private static boolean insideDomain(
            ServerPlayer controller,
            Entity entity) {

        return controller != null
                && controller.isAlive()
                && controller.level() == entity.level()
                && controller.distanceToSqr(entity)
                <= RADIUS_SQUARED;
    }

    private static void enableFlight(ServerPlayer player) {

        UUID id = player.getUUID();

        if (!FLIGHT_STATES.containsKey(id)) {
            FLIGHT_STATES.put(
                    id,
                    new FlightState(
                            player.getAbilities().mayfly,
                            player.getAbilities().flying
                    )
            );
        }

        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
    }

    private static void restoreFlight(ServerPlayer player) {

        FlightState state = FLIGHT_STATES.remove(
                player.getUUID()
        );

        if (state == null) {
            return;
        }

        player.getAbilities().mayfly = state.mayfly;

        if (!state.mayfly) {
            player.getAbilities().flying = false;
        } else {
            player.getAbilities().flying = state.flying;
        }

        player.onUpdateAbilities();
    }

    /*
     * Vụ nổ trong lĩnh vực không phá block.
     * Ngoài lĩnh vực hoặc khi tắt thì bình thường.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(
            ExplosionEvent.Detonate event) {

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

        Vec3 position = event.getExplosion().center();

        if (controller.distanceToSqr(
                position.x,
                position.y,
                position.z
        ) <= RADIUS_SQUARED) {

            event.getAffectedBlocks().clear();
        }
    }

    /*
     * Đẩy nhẹ entity đã ở trong lĩnh vực
     * khi chúng cố đi ra ngoài.
     */
    private static void updateBoundary(
            ServerLevel level,
            ServerPlayer controller) {
        
        for (Entity entity : level.getEntitiesOfClass(
        Entity.class,
        controller.getBoundingBox().inflate(RADIUS + 3.0D),
        candidate -> candidate != controller
                && !candidate.isRemoved()
)) {
            
            UUID id = entity.getUUID();

            if (insideDomain(controller, entity)) {
                LAST_INSIDE.put(id, entity.position());
                continue;
            }

            // Chỉ đẩy entity đã từng ở trong lĩnh vực.
            if (!LAST_INSIDE.containsKey(id)) {
                continue;
            }

            Vec3 direction = controller.position()
                    .subtract(entity.position());

            if (direction.lengthSqr() > 0.0001D) {

                direction = direction.normalize();

                Vec3 velocity = entity.getDeltaMovement();

                entity.setDeltaMovement(
                        velocity.add(
                                direction.scale(PUSH_STRENGTH)
                        )
                );

                entity.hurtMarked = true;
            }
        }
    }

    private static void cleanRemovedEntities(
            net.minecraft.server.MinecraftServer server) {

        Iterator<UUID> iterator =
                LAST_INSIDE.keySet().iterator();

        while (iterator.hasNext()) {

            UUID id = iterator.next();
            boolean found = false;

            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(id);

                if (entity != null && !entity.isRemoved()) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        if (!MobWarCommands.enabled) {

            tickCounter = 0;
            LAST_INSIDE.clear();

            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    if (isController(player)) {
                        restoreFlight(player);
                    }
                }
            }

            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {

            ServerPlayer controller = findController(level);

            if (controller == null) {
                continue;
            }

            enableFlight(controller);

            if (++tickCounter >= UPDATE_INTERVAL) {
                tickCounter = 0;
                updateBoundary(level, controller);
                cleanRemovedEntities(event.getServer());
            }
        }
    }
}
