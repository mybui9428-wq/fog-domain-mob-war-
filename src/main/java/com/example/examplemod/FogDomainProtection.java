
package com.example.examplemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainProtection {

    private static final double RADIUS = 30.0D;
    private static final double PUSH_DISTANCE = 0.08D;
    private static final double CHECK_DISTANCE = 31.0D;

    private FogDomainProtection() {
    }

    private static boolean isController(Player player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static ServerPlayer getController(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (isController(player) && player.isAlive()) {
                return player;
            }
        }

        return null;
    }

    private static boolean inDomain(
            ServerLevel level, Entity entity) {

        ServerPlayer owner = getController(level);

        return owner != null
                && owner.distanceToSqr(entity)
                        <= RADIUS * RADIUS;
    }

    @SubscribeEvent
    public static void protectExplosion(
            ExplosionEvent.Detonate event) {

        if (!MobWarCommands.enabled) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity source =
                event.getExplosion().getIndirectSourceEntity();

        if (source == null) {
            source = event.getExplosion().getDirectSourceEntity();
        }

        boolean protect = source != null
                && inDomain(level, source);

        if (!protect) {
            Vec3 center = event.getExplosion().center();

            ServerPlayer owner = getController(level);

            if (owner != null
                    && owner.distanceToSqr(center)
                            <= RADIUS * RADIUS) {
                protect = true;
            }
        }

        if (protect) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void pushEntitiesInside(
            ServerTickEvent.Post event) {

        if (!MobWarCommands.enabled) {
            return;
        }

        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            ServerPlayer owner = getController(level);

            if (owner == null) {
                continue;
            }

            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity)
                        || entity == owner
                        || entity.isRemoved()) {
                    continue;
                }

                double dx = entity.getX() - owner.getX();
                double dy = entity.getY() - owner.getY();
                double dz = entity.getZ() - owner.getZ();

                double distanceSquared =
                        dx * dx + dy * dy + dz * dz;

                if (distanceSquared <= RADIUS * RADIUS
                        || distanceSquared
                                > CHECK_DISTANCE * CHECK_DISTANCE) {
                    continue;
                }

                double distance = Math.sqrt(distanceSquared);

                if (distance < 0.001D) {
                    continue;
                }

                double pushX = -dx / distance * PUSH_DISTANCE;
                double pushY = -dy / distance * PUSH_DISTANCE;
                double pushZ = -dz / distance * PUSH_DISTANCE;

                entity.setDeltaMovement(
                        entity.getDeltaMovement().add(
                                pushX, pushY, pushZ));
            }
        }
    }
}
