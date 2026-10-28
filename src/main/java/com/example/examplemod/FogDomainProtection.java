
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

    private static final Map<UUID, Vec3> LAST_INSIDE = new HashMap<>();

    private FogDomainProtection() {
    }

    private static ServerPlayer findController(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.getGameProfile().name()
                    .equalsIgnoreCase("tubi2531")
                    && player.isAlive()) {
                return player;
            }
        }
        return null;
    }

    private static boolean insideDomain(
            ServerPlayer controller, Entity entity) {
        return controller != null
                && controller.isAlive()
                && controller.level() == entity.level()
                && controller.distanceToSqr(entity) <= RADIUS_SQUARED;
    }

    /*
     * Khi lĩnh vực bật, vụ nổ trong phạm vi
     * không được phá block.
     * Khi tắt, Minecraft hoạt động bình thường.
     */
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
     * tubi2531 miễn nhiễm sát thương khi ở trong lĩnh vực.
     * Ra ngoài lĩnh vực hoặc tắt lĩnh vực thì sát thương
     * được xử lý như bình thường.
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(
            LivingIncomingDamageEvent event) {

        if (!MobWarCommands.enabled) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getGameProfile().name()
                .equalsIgnoreCase("tubi2531")) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer controller = findController(level);

        if (controller == player && insideDomain(controller, player)) {
            event.setCanceled(true);
        }
    }

    /*
     * Ghi nhớ vị trí bên trong lĩnh vực.
     * Khi entity cố đi ra ngoài, đưa nó về vị trí gần nhất
     * đã ghi nhận ở bên trong.
     */
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
                if (entity == controller
                        || entity.isRemoved()
                        || entity.level() != level) {
                    continue;
                }

                UUID id = entity.getUUID();

                if (insideDomain(controller, entity)) {
                    LAST_INSIDE.put(id, entity.position());
                    continue;
                }

                Vec3 lastPosition = LAST_INSIDE.get(id);

                if (lastPosition != null) {
                    entity.teleportTo(
                            lastPosition.x,
                            lastPosition.y,
                            lastPosition.z
                    );
                    entity.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        Iterator<UUID> iterator = LAST_INSIDE.keySet().iterator();

        while (iterator.hasNext()) {
            UUID id = iterator.next();
            boolean found = false;

            for (ServerLevel level : event.getServer().getAllLevels()) {
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
}
