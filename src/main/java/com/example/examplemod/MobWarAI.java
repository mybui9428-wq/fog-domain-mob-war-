
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    private static final Map<UUID, Integer> FOCUS_TICKS = new HashMap<>();

    private MobWarAI() {}

    static boolean isController(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getGameProfile().getName()
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
        if (entity == null || entity.isRemoved()
                || entity.level() != level) {
            return false;
        }

        ServerPlayer controller = findController(level);

        return controller != null
                && controller.distanceToSqr(entity) <= RADIUS * RADIUS;
    }

    private static boolean isTamed(Entity entity) {
        return FogDomainTaming.isTamed(entity);
    }

    private static List<LivingEntity> scanEntities(ServerLevel level) {
        List<LivingEntity> result = new ArrayList<>();

        if (!MobWarCommands.enabled) {
            return result;
        }

        ServerPlayer controller = findController(level);

        if (controller == null) {
            return result;
        }

        result.addAll(level.getEntitiesOfClass(
                LivingEntity.class,
                controller.getBoundingBox().inflate(RADIUS),
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && !isController(entity)
                        && !isTamed(entity)
                        && (entity instanceof Mob
                        || entity instanceof ServerPlayer)
                        && controller.distanceToSqr(entity)
                        <= RADIUS * RADIUS
        ));

        return result;
    }

    private static void updateFocus(List<LivingEntity> entities) {
        Set<UUID> present = new HashSet<>();

        for (LivingEntity entity : entities) {
            present.add(entity.getUUID());
        }

        // Bỏ mục tiêu đã rời khỏi khu vực hoặc không còn hợp lệ.
        FOCUS.removeIf(id -> !present.contains(id));
        FOCUS_TICKS.keySet().removeIf(id -> !present.contains(id));

        // Giữ hiệu ứng phát sáng cho các mục tiêu đã chọn.
        for (LivingEntity entity : entities) {
            UUID id = entity.getUUID();

            if (FOCUS.contains(id)) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        SCAN_INTERVAL + 10,
                        0,
                        false,
                        true,
                        true
                ));

                FOCUS_TICKS.put(id, FOCUS_TICKS.getOrDefault(id, 0) + 1);
            }
        }

        // Chỉ chọn ngẫu nhiên thêm khi chưa đủ 5 mục tiêu.
        List<LivingEntity> available = new ArrayList<>();

        for (LivingEntity entity : entities) {
            if (!FOCUS.contains(entity.getUUID())) {
                available.add(entity);
            }
        }

        Collections.shuffle(available);

        while (FOCUS.size() < MAX_FOCUS && !available.isEmpty()) {
            LivingEntity chosen = available.remove(0);
            FOCUS.add(chosen.getUUID());
            FOCUS_TICKS.put(chosen.getUUID(), 0);
        }

        // Nếu một mục tiêu đã chết hoặc biến mất, số lượng sẽ được bổ sung
        // ở lần quét tiếp theo.
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        UUID id = event.getEntity().getUUID();

        FOCUS.remove(id);
        FOCUS_TICKS.remove(id);

        if (event.getEntity() instanceof PathfinderMob mob) {
            REGISTERED.remove(mob);
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
                || attacker.isRemoved()
                || target.isRemoved()
                || attacker.level() != level
                || target.level() != level
                || isController(attacker)
                || isController(target)
                || isTamed(attacker)
                || isTamed(target)
                || !(target instanceof Mob
                || target instanceof ServerPlayer)
                || !inDomain(level, attacker)
                || !inDomain(level, target)) {
            return false;
        }

        // Không bắt buộc attacker hoặc target phải phát sáng.
        // Việc ưu tiên mục tiêu phát sáng được xử lý trong MobWarCombatGoal.
        return true;
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
            tickCounter = 0;
            FOCUS.clear();
            FOCUS_TICKS.clear();
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<LivingEntity> entities = scanEntities(level);

            updateFocus(entities);

            int added = 0;

            for (LivingEntity entity : entities) {
                if (!(entity instanceof PathfinderMob mob)
                        || REGISTERED.containsKey(mob)) {
                    continue;
                }

                registerMob(mob);

                if (++added >= MAX_NEW_GOALS_PER_SCAN) {
                    break;
                }
            }
        }
    }
}
