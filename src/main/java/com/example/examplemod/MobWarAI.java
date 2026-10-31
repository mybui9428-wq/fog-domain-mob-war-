
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

    // UUID của các sinh vật thuộc nhóm phát sáng hiện tại.
    // Lưu riêng theo ServerLevel để tránh lẫn giữa các thế giới.
    private static final Map<UUID, ServerLevel> FOCUS =
            new HashMap<>();

    // Chỉ xóa hiệu ứng phát sáng do Mob War cấp.
    private static final Set<UUID> MOD_GLOW =
            new HashSet<>();

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
        if (entity == null
                || entity.isRemoved()
                || entity.level() != level) {
            return false;
        }

        ServerPlayer controller = findController(level);

        return controller != null
                && controller.distanceToSqr(entity)
                <= RADIUS * RADIUS;
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
                entity ->
                        entity.isAlive()
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

    private static void giveGlow(LivingEntity entity) {

        UUID id = entity.getUUID();

        entity.addEffect(new MobEffectInstance(
                MobEffects.GLOWING,
                SCAN_INTERVAL + 10,
                0,
                false,
                true,
                true
        ));

        MOD_GLOW.add(id);
    }

    private static void removeModGlow(
            ServerLevel level,
            UUID id) {

        if (!MOD_GLOW.remove(id)) {
            return;
        }

        Entity entity = level.getEntity(id);

        if (entity instanceof LivingEntity living) {
            living.removeEffect(MobEffects.GLOWING);
        }
    }

    private static void removeFocus(
            ServerLevel level,
            UUID id) {

        FOCUS.remove(id);
        removeModGlow(level, id);
    }

    private static void clearLevelFocus(ServerLevel level) {

        List<UUID> remove = new ArrayList<>();

        for (Map.Entry<UUID, ServerLevel> entry
                : FOCUS.entrySet()) {

            if (entry.getValue() == level) {
                remove.add(entry.getKey());
            }
        }

        for (UUID id : remove) {
            removeFocus(level, id);
        }

        // Dọn các hiệu ứng còn sót lại do Mob War cấp.
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living
                    && MOD_GLOW.remove(entity.getUUID())) {
                living.removeEffect(MobEffects.GLOWING);
            }
        }
    }

    private static void clearAllFocus(
            Iterable<ServerLevel> levels) {

        for (ServerLevel level : levels) {
            clearLevelFocus(level);
        }

        FOCUS.clear();
        MOD_GLOW.clear();
    }

    private static void updateFocus(
            ServerLevel level,
            List<LivingEntity> entities) {

        Set<UUID> eligible = new HashSet<>();

        for (LivingEntity entity : entities) {
            eligible.add(entity.getUUID());
        }

        // Loại khỏi nhóm những sinh vật đã chết,
        // rời lĩnh vực, bị thu phục hoặc không còn hợp lệ.
        List<UUID> invalid = new ArrayList<>();

        for (Map.Entry<UUID, ServerLevel> entry
                : FOCUS.entrySet()) {

            if (entry.getValue() == level
                    && !eligible.contains(entry.getKey())) {
                invalid.add(entry.getKey());
            }
        }

        for (UUID id : invalid) {
            removeFocus(level, id);
        }

        // Làm mới hiệu ứng phát sáng cho nhóm hiện tại.
        boolean hasCurrentFocus = false;

        for (LivingEntity entity : entities) {

            if (FOCUS.get(entity.getUUID()) == level) {
                hasCurrentFocus = true;
                giveGlow(entity);
            }
        }

        // Vẫn còn thành viên trong nhóm cũ:
        // không chọn nhóm mới.
        if (hasCurrentFocus) {
            return;
        }

        // Chỉ chọn nhóm mới khi nhóm cũ đã hết.
        if (entities.isEmpty()) {
            return;
        }

        List<LivingEntity> available =
                new ArrayList<>(entities);

        Collections.shuffle(available);

        int amount = Math.min(MAX_FOCUS, available.size());

        for (int i = 0; i < amount; i++) {

            LivingEntity chosen = available.get(i);

            FOCUS.put(chosen.getUUID(), level);

            giveGlow(chosen);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {

        UUID id = event.getEntity().getUUID();

        ServerLevel level =
                event.getEntity().level() instanceof ServerLevel serverLevel
                        ? serverLevel
                        : null;

        if (level != null) {
            removeFocus(level, id);
        } else {
            FOCUS.remove(id);
            MOD_GLOW.remove(id);
        }

        if (event.getEntity() instanceof PathfinderMob mob) {
            REGISTERED.remove(mob);
        }
    }

    static boolean isFocused(Entity entity) {

        if (entity == null) {
            return false;
        }

        return FOCUS.containsKey(entity.getUUID());
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
                || !inDomain(level, target)
                || !isFocused(target)) {

            return false;
        }

        // Chỉ tấn công mục tiêu đang phát sáng.
        return true;
    }

    private static void registerMob(PathfinderMob mob) {

        if (mob.isRemoved()
                || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);

        mob.goalSelector.addGoal(
                1,
                new MobWarCombatGoal(mob)
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        if (!MobWarCommands.enabled) {
            tickCounter = 0;
            clearAllFocus(event.getServer().getAllLevels());
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {

            if (findController(level) == null) {
                clearLevelFocus(level);
                continue;
            }

            List<LivingEntity> entities =
                    scanEntities(level);

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
