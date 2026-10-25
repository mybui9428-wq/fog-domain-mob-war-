
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainProtection {

    private static final double RADIUS = 30.0D;

    private FogDomainProtection() {
    }

    private static boolean isController(Player player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static boolean inDomain(ServerLevel level, Entity entity) {
        for (ServerPlayer player : level.players()) {
            if (isController(player)
                    && player.isAlive()
                    && player.distanceToSqr(entity)
                            <= RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void protectExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity source = event.getExplosion().getIndirectSourceEntity();

        if (source == null) {
            source = event.getExplosion().getDirectSourceEntity();
        }

        boolean protect = false;

        if (source != null) {
            protect = inDomain(level, source);
        }

        if (!protect) {
            for (ServerPlayer player : level.players()) {
                if (isController(player)
                        && player.isAlive()
                        && player.distanceToSqr(
                                event.getExplosion().center())
                                <= RADIUS * RADIUS) {
                    protect = true;
                    break;
                }
            }
        }

        if (protect) {
            event.getAffectedBlocks().clear();
        }
    }
}
