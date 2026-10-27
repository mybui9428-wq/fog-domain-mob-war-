
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {

    static final double RADIUS = 30.0D;
    private static final int MAX_FOCUS = 5;
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_NEW_GOALS_PER_SCAN = 10;

    private static int tickCounter = 0;

    private static final Map<PathfinderMob, Boolean> REGISTERED =
            new WeakHashMap<>();

    private static final Set<UUID> FOCUS = new HashSet<>();

    private MobWarAI() {
    }

    static boolean isController(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getGameProfile().name()
                .equalsIgnoreCase("tubi2531");
    }

    static ServerPlayer findController(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (isController(player) && player.isAlive()) {
                return player;
            }
        }
        return null;
    }

    static boolean inDomain(ServerLevel level, Entity entity) {
        ServerPlayer controller = findController(level);

        return controller != null
                && entity.level() == level
                && controller.distanceToSqr(entity)
                <= RADIUS * RADIUS;
    }

    private static List<LivingEntity> scanEntities(ServerLevel level) {
        List<LivingEntity> result = new ArrayList<>();
        ServerPlayer controller = findController(level);

        if (controller == null) {
            return result;
        }

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                controller.getBoundingBox().inflate(RADIUS),
                e -> e.isAlive()
                        && !e.isRemoved()
                        && e != controller
                        && !isController(e)
                        && (e instanceof Mob
                            || e instanceof ServerPlayer)
                        && controller.distanceToSqr(e)
                            <= RADIUS * RADIUS)) {

            result.add(entity);
        }

        return result;
    }

    private static void updateFocus(List<LivingEntity> entities) {
        Set<UUID> alive = new HashSet<>();

        for (LivingEntity entity : entities) {
            alive.add(entity.getUUID());
        }

        FOCUS.retainAll(alive);

        List<LivingEntity> available = new ArrayList<>();

        for (LivingEntity entity : entities) {
            if (!FOCUS.contains(entity.getUUID())) {
                available.add(entity);
            }
        }

        Collections.shuffle(available);

        while (FOCUS.size() < MAX_FOCUS && !available.isEmpty()) {
            FOCUS.add(available.remove(0).getUUID());
        }

        for (LivingEntity entity : entities) {
            if (FOCUS.contains(entity.getUUID())) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        60,
                        0,
                        false,
                        true
                ));
            }
        }
    }

    static boolean isFocused(Entity entity) {
        return entity != null && FOCUS.contains(entity.getUUID());
    }

    static boolean isValidCombatTarget(
            Mob attacker,
            LivingEntity target,
            ServerLevel level) {

        if (!MobWarCommands.enabled
                || attacker == null
                || target == null
                || attacker == target
                || !attacker.isAlive()
                || !target.isAlive()
                || target.isRemoved()
                || attacker.level() != level
                || target.level() != level
                || isController(attacker)
                || isController(target)
                || !(target instanceof Mob
                     || target instanceof ServerPlayer)
                || !inDomain(level, attacker)
                || !inDomain(level, target)) {
            return false;
        }

        List<LivingEntity> entities = scanEntities(level);

        // Nếu còn hơn 5 đối tượng, chỉ nhắm nhóm phát sáng.
        if (entities.size() > MAX_FOCUS) {
            return isFocused(target);
        }

        // Nếu còn tối đa 5 đối tượng, chúng có thể đánh nhau.
        return entities.contains(target);
    }

    private static void registerMob(PathfinderMob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);
        mob.goalSelector.addGoal(1, new MobWarCombatGoal(mob));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!MobWarCommands.enabled) {
            FOCUS.clear();
            tickCounter = 0;
            return;
        }

        tickCounter++;

        if (tickCounter < SCAN_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<LivingEntity> entities = scanEntities(level);

            updateFocus(entities);

            int newGoals = 0;

            for (LivingEntity entity : entities) {
                if (!(entity instanceof PathfinderMob mob)
                        || REGISTERED.containsKey(mob)) {
                    continue;
                }

                registerMob(mob);
                newGoals++;

                if (newGoals >= MAX_NEW_GOALS_PER_SCAN) {
                    break;
                }
            }
        }
    }
}
