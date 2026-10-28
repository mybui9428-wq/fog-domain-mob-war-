
package com.example.examplemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainTaming {
    private static int tickCounter = 0;

    private FogDomainTaming() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < 10) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            ServerPlayer owner = findOwner(level);
            if (owner == null || !owner.isAlive()) {
                continue;
            }

            List<UUID> owned = FogDomainSkills.TAMED.get(owner.getUUID());
            if (owned == null || owned.isEmpty()) {
                continue;
            }

            List<UUID> remove = new ArrayList<>();

            for (UUID mobId : owned) {
                Entity entity = level.getEntity(mobId);

                if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                    remove.add(mobId);
                    continue;
                }

                LivingEntity target = chooseTarget(owner, mob);

                if (target != null) {
                    mob.setTarget(target);
                    mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

                    if (mob.distanceToSqr(target) > 3.0D) {
                        mob.getNavigation().moveTo(target, 1.2D);
                    }
                } else {
                    if (mob.getTarget() == owner) {
                        mob.setTarget(null);
                    }

                    double distance = mob.distanceToSqr(owner);

                    if (distance > 25.0D) {
                        mob.getNavigation().moveTo(owner, 1.2D);
                    } else if (distance < 9.0D) {
                        mob.getNavigation().stop();
                    }
                }
            }

            owned.removeAll(remove);

            if (owned.isEmpty()) {
                FogDomainSkills.TAMED.remove(owner.getUUID());
            }
        }
    }

    private static ServerPlayer findOwner(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.getName().getString()
        .equalsIgnoreCase("tubi2531")) {
                return player;
            }
        }
        return null;
    }

    private static LivingEntity chooseTarget(
            ServerPlayer owner, Mob companion) {

        LivingEntity attacker = owner.getLastHurtByMob();

        if (isValidTarget(owner, companion, attacker)) {
            return attacker;
        }

        LivingEntity ownerTarget = owner.getLastHurtMob();

        if (isValidTarget(owner, companion, ownerTarget)) {
            return ownerTarget;
        }

        return null;
    }

    private static boolean isValidTarget(
            ServerPlayer owner, Mob companion, LivingEntity target) {

        if (target == null || !target.isAlive()
                || target == owner || target == companion) {
            return false;
        }

        if (target.level() != companion.level()) {
            return false;
        }

        List<UUID> owned = FogDomainSkills.TAMED.get(owner.getUUID());

        return owned == null || !owned.contains(target.getUUID());
    }
}
