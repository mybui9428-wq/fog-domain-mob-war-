
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

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

        result.addAll(level.getEntitiesOfClass(
                LivingEntity.class,
                controller.getBoundingBox().inflate(RADIUS),
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity != controller
                        && !isController(entity)
                        && (entity instanceof Mob
                            || entity instanceof ServerPlayer)
                        && controller.distanceToSqr(entity)
                        <= RADIUS * RADIUS
        ));

        return result;
    }

    private static void updateFocus(List<LivingEntity> entities) {
        Set<UUID> alive = new HashSet<>();

        for (LivingEntity entity : entities) {
            alive.add(entity.getUUID());
        }

        // Xóa mục tiêu đã chết hoặc rời khỏi lĩnh vực.
        FOCUS.retainAll(alive);

        // Giữ nhóm hiện tại, chỉ bổ sung khi thiếu thành viên.
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

        // Làm mới hiệu ứng phát sáng cho nhóm được chọn.
        for (LivingEntity entity : entities) {
            if (FOCUS.contains(entity.getUUID())) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        40,
                        0,
                        false,
                        true,
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

        if (entities.isEmpty()) {
            return false;
        }

        if (entities.size() > MAX_FOCUS) {
            // Khi có hơn 5 đối tượng:
            // chỉ sinh vật không phát sáng săn nhóm phát sáng.
            return !isFocused(attacker) && isFocused(target);
        }

        // Khi còn tối đa 5 đối tượng:
        // sinh vật có thể nhắm những đối tượng còn lại.
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
