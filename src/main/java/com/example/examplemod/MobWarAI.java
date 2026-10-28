
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
                && player.getName().getString()
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
                && entity != null
                && !entity.isRemoved()
                && entity.level() == level
                && controller.distanceToSqr(entity)
                <= RADIUS * RADIUS;
    }

    private static boolean isTamed(Entity entity) {
        return FogDomainTaming.isTamed(entity);
    }

    private static List<LivingEntity> scanEntities(
            ServerLevel level) {

        List<LivingEntity> result = new ArrayList<>();
        ServerPlayer controller = findController(level);

        if (controller == null || !MobWarCommands.enabled) {
            return result;
        }

        result.addAll(level.getEntitiesOfClass(
                LivingEntity.class,
                controller.getBoundingBox().inflate(RADIUS),

                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity != controller
                        && !isController(entity)
                        && !isTamed(entity)
                        && (entity instanceof Mob
                            || entity instanceof ServerPlayer)
                        && controller.distanceToSqr(entity)
                        <= RADIUS * RADIUS
        ));

        return result;
    }

    private static void updateFocus(
            ServerLevel level,
            List<LivingEntity> entities) {

        Set<UUID> validIds = new HashSet<>();

        for (LivingEntity entity : entities) {
            validIds.add(entity.getUUID());
        }

        // Xóa mục tiêu chết, rời lĩnh vực hoặc không còn hợp lệ.
        FOCUS.removeIf(id -> !validIds.contains(id));

        // Loại bỏ hiệu ứng phát sáng nếu mục tiêu không còn được chọn.
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                findController(level) == null
                        ? new net.minecraft.world.phys.AABB(0, 0, 0, 0, 0, 0)
                        : findController(level).getBoundingBox().inflate(RADIUS),
                entity -> entity.hasEffect(MobEffects.GLOWING)
                        && !FOCUS.contains(entity.getUUID()))) {

            if (!isTamed(entity)) {
                entity.removeEffect(MobEffects.GLOWING);
            }
        }

        // Giữ các mục tiêu đang được chọn nếu chúng vẫn hợp lệ.
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

        // Làm mới hiệu ứng phát sáng cho các mục tiêu đã chọn.
        for (LivingEntity entity : entities) {
            if (FOCUS.contains(entity.getUUID())) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        SCAN_INTERVAL + 10,
                        0,
                        false,
                        true,
                        true
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();

        FOCUS.remove(entity.getUUID());

        if (entity instanceof PathfinderMob mob) {
            REGISTERED.remove(mob);
        }
    }

    static boolean isFocused(Entity entity) {
        return entity != null
                && FOCUS.contains(entity.getUUID());
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
                || isTamed(attacker)
                || isTamed(target)
                || !(target instanceof Mob
                     || target instanceof ServerPlayer)
                || !inDomain(level, attacker)
                || !inDomain(level, target)) {

            return false;
        }

        List<LivingEntity> entities = scanEntities(level);

        if (!entities.contains(attacker)
                || !entities.contains(target)) {
            return false;
        }

        // Nếu có hơn 5 mục tiêu hợp lệ:
        // chỉ sinh vật không phát sáng săn mục tiêu phát sáng.
        if (entities.size() > MAX_FOCUS) {
            return !isFocused(attacker)
                    && isFocused(target);
        }

        // Nếu có tối đa 5 sinh vật, chúng vẫn có thể chiến đấu.
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
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {

            List<LivingEntity> entities = scanEntities(level);

            updateFocus(level, entities);

            int added = 0;

            for (LivingEntity entity : entities) {

                if (!(entity instanceof PathfinderMob mob)
                        || REGISTERED.containsKey(mob)) {
                    continue;
                }

                registerMob(mob);

                added++;

                if (added >= MAX_NEW_GOALS_PER_SCAN) {
                    break;
                }
            }
        }
    }
}
