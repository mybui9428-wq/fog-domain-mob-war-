
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
import java.util.IdentityHashMap;
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

    // Mỗi thế giới có một đợt chiến đấu riêng.
    private static final Map<ServerLevel, WaveState> WAVES =
            new IdentityHashMap<>();

    private static final class WaveState {
        final Set<UUID> members = new HashSet<>();
        final Set<UUID> focus = new HashSet<>();
    }

    private MobWarAI() {}

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

    // Bắt đầu một đợt mới.
    // Chỉ gọi khi chưa có thành viên nào của đợt trước còn lại.
    private static void beginWave(
            WaveState state,
            List<LivingEntity> entities) {

        state.members.clear();
        state.focus.clear();

        List<LivingEntity> shuffled = new ArrayList<>(entities);

        Collections.shuffle(shuffled);

        // Ghi nhận toàn bộ sinh vật/người chơi của đợt.
        for (LivingEntity entity : shuffled) {
            state.members.add(entity.getUUID());
        }

        // Chọn tối đa 5 mục tiêu phát sáng.
        int count = Math.min(MAX_FOCUS, shuffled.size());

        for (int i = 0; i < count; i++) {
            LivingEntity chosen = shuffled.get(i);
            state.focus.add(chosen.getUUID());
        }
    }

    private static void updateWave(
            ServerLevel level,
            List<LivingEntity> entities) {

        WaveState state = WAVES.computeIfAbsent(
                level,
                ignored -> new WaveState()
        );

        Set<UUID> currentlyPresent = new HashSet<>();

        for (LivingEntity entity : entities) {
            currentlyPresent.add(entity.getUUID());
        }

        // Thành viên chết, biến mất hoặc rời khỏi lĩnh vực
        // sẽ được loại khỏi đợt hiện tại.
        state.members.removeIf(
                id -> !currentlyPresent.contains(id)
        );

        state.focus.removeIf(
                id -> !state.members.contains(id)
        );

        // Chỉ bắt đầu đợt mới khi toàn bộ thành viên
        // của đợt trước đã không còn trong lĩnh vực.
        if (state.members.isEmpty() && !entities.isEmpty()) {
            beginWave(state, entities);
        }

        // Cấp hiệu ứng phát sáng cho nhóm đã chọn.
        // Không chọn người thay thế khi một thành viên chết.
        for (LivingEntity entity : entities) {
            if (state.focus.contains(entity.getUUID())) {
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

        if (entity.level() instanceof ServerLevel level) {
            WaveState state = WAVES.get(level);

            if (state != null) {
                state.members.remove(entity.getUUID());
                state.focus.remove(entity.getUUID());
            }
        }

        if (entity instanceof PathfinderMob mob) {
            REGISTERED.remove(mob);
        }
    }

    static boolean isFocused(Entity entity) {
        if (entity == null
                || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        WaveState state = WAVES.get(level);

        return state != null
                && state.focus.contains(entity.getUUID());
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

        WaveState state = WAVES.get(level);

        if (state == null) {
            return false;
        }

        // Chỉ thành viên của đợt hiện tại mới được chiến đấu.
        // Sinh vật mới xuất hiện phải chờ đợt sau.
        return state.members.contains(attacker.getUUID())
                && state.members.contains(target.getUUID());
    }

    private static void registerMob(PathfinderMob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
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
            WAVES.clear();
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {

            List<LivingEntity> entities = scanEntities(level);

            // Cập nhật đợt chiến đấu.
            updateWave(level, entities);

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
