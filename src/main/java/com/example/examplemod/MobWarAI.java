
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
import java.util.WeakHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {
    private static final double RADIUS = 30.0D;
    private static final int MAX_FOCUS = 5;
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_REGISTER_PER_SCAN = 10;

    private static int tickCounter = 0;

    private static final Map<PathfinderMob, Boolean> REGISTERED =
            new WeakHashMap<>();

    private static final Set<UUID> FOCUS = new HashSet<>();

    private MobWarAI() {
    }

    private static boolean isController(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getGameProfile().name()
                .equalsIgnoreCase("tubi2531");
    }

    private static boolean inDomain(
            ServerLevel level, Entity entity) {
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

    private static List<LivingEntity> getCandidates(
            ServerLevel level) {
        return level.getEntitiesOfClass(
                LivingEntity.class,
                level.getWorldBorder().getCollisionShape()
                        .bounds().inflate(0),
                entity -> false
        );
    }

    private static List<LivingEntity> scanEntities(
            ServerLevel level) {
        List<LivingEntity> result = new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (!isController(player) || !player.isAlive()) {
                continue;
            }

            List<LivingEntity> nearby =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            player.getBoundingBox().inflate(RADIUS),
                            entity -> entity.isAlive()
                                    && entity != player
                                    && !isController(entity)
                                    && inDomain(level, entity)
                    );

            for (LivingEntity entity : nearby) {
                if (!result.contains(entity)) {
                    result.add(entity);
                }
            }
        }

        return result;
    }

    private static boolean isFocused(Entity entity) {
        return FOCUS.contains(entity.getUUID());
    }

    private static void updateFocus(
            ServerLevel level, List<LivingEntity> entities) {
        Set<UUID> alive = new HashSet<>();

        for (LivingEntity entity : entities) {
            alive.add(entity.getUUID());
        }

        FOCUS.removeIf(id -> !alive.contains(id));

        if (entities.size() <= MAX_FOCUS) {
            FOCUS.clear();

            for (LivingEntity entity : entities) {
                FOCUS.add(entity.getUUID());
            }
        } else {
            List<LivingEntity> available = new ArrayList<>();

            for (LivingEntity entity : entities) {
                if (!FOCUS.contains(entity.getUUID())) {
                    available.add(entity);
                }
            }

            Collections.shuffle(available);

            while (FOCUS.size() < MAX_FOCUS
                    && !available.isEmpty()) {
                FOCUS.add(available.remove(0).getUUID());
            }
        }

        for (LivingEntity entity : entities) {
            if (isFocused(entity)) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, 60, 0, false, true));
            }
        }
    }

    private static boolean validTarget(
            PathfinderMob attacker,
            LivingEntity target,
            ServerLevel level) {
        if (!MobWarCommands.enabled
                || attacker == target
                || target == null
                || !target.isAlive()
                || attacker.isRemoved()
                || isController(attacker)
                || isController(target)
                || !inDomain(level, attacker)
                || !inDomain(level, target)) {
            return false;
        }

        List<LivingEntity> entities = scanEntities(level);

        if (entities.size() <= MAX_FOCUS) {
            return true;
        }

        return isFocused(target);
    }

    private static void registerMob(PathfinderMob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);

        mob.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                        mob,
                        LivingEntity.class,
                        10,
                        true,
                        false,
                        target -> {
                            if (!(mob.level() instanceof ServerLevel level)) {
                                return false;
                            }
                            return validTarget(mob, target, level);
                        }
                )
        );

        mob.goalSelector.addGoal(
                1,
                new MeleeAttackGoal(mob, 1.2D, true)
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!MobWarCommands.enabled) {
            FOCUS.clear();
            return;
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<LivingEntity> entities = scanEntities(level);

            updateFocus(level, entities);

            int registered = 0;

            for (LivingEntity entity : entities) {
                if (entity instanceof PathfinderMob mob) {
                    registerMob(mob);
                    registered++;

                    if (registered >= MAX_REGISTER_PER_SCAN) {
                        break;
                    }
                }
            }

            if (entities.size() <= MAX_FOCUS) {
                for (LivingEntity entity : entities) {
                    if (entity instanceof Mob mob
                            && !isController(mob)) {
                        LivingEntity target = null;

                        for (LivingEntity candidate : entities) {
                            if (candidate != mob
                                    && candidate.isAlive()) {
                                target = candidate;
                                break;
                            }
                        }

                        if (target != null) {
                            mob.setTarget(target);
                        }
                    }
                }
            }
        }
    }
}
