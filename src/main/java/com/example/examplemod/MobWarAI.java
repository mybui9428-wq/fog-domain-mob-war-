package com.example.examplemod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {

    private static final double RADIUS = 30.0D;
    private static final int SCAN_INTERVAL = 10;
    private static final int MAX_MOBS_PER_SCAN = 8;

    private static int tickCounter = 0;
    private static int rotation = 0;
    private static boolean wasEnabled = false;

    private static final Map<Mob, Boolean> CONTROLLED =
            new WeakHashMap<>();

    private static final Map<PathfinderMob, Boolean> COMBAT_GOALS =
            new WeakHashMap<>();

    private MobWarAI() {
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

    private static boolean validTarget(
            Mob attacker,
            LivingEntity target,
            ServerLevel level) {

        if (!MobWarCommands.enabled
                || attacker.isRemoved()
                || !attacker.isAlive()
                || target == null
                || target == attacker
                || target.isRemoved()
                || target.level() != level
                || !target.isAlive()) {
            return false;
        }

        if (!(target instanceof Mob)
                && !(target instanceof Player)) {
            return false;
        }

        if (target instanceof Player player
                && isController(player)) {
            return false;
        }

        return inDomain(level, attacker)
                && inDomain(level, target);
    }

    private static void registerCombatGoal(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }

        // Giữ AI gốc của mob thù địch,
        // nhưng cho phép Warden dùng goal chiến đấu của Mob War.
        if (mob instanceof Enemy && !(mob instanceof Warden)) {
            return;
        }

        if (COMBAT_GOALS.containsKey(pathfinder)) {
            return;
        }

        pathfinder.goalSelector.addGoal(
                3,
                new MobWarCombatGoal(pathfinder)
        );

        COMBAT_GOALS.put(pathfinder, Boolean.TRUE);
    }

    private static LivingEntity findTarget(
            Mob attacker,
            List<Mob> mobs,
            ServerLevel level) {

        // Ưu tiên người chơi khác trong vùng trước.
        ServerPlayer nearestPlayer = null;
        double nearestPlayerDistance = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (!validTarget(attacker, player, level)) {
                continue;
            }

            double distance = attacker.distanceToSqr(player);

            if (distance < nearestPlayerDistance) {
                nearestPlayerDistance = distance;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer != null) {
            return nearestPlayer;
        }

        // Giữ mục tiêu mob hiện tại nếu còn hợp lệ.
        LivingEntity current = attacker.getTarget();

        if (current instanceof Mob
                && validTarget(attacker, current, level)) {
            return current;
        }

        // Nếu không có mục tiêu hợp lệ, tìm mob gần nhất.
        LivingEntity nearestMob = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Mob candidate : mobs) {
            if (!validTarget(attacker, candidate, level)) {
                continue;
            }

            double distance = attacker.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMob = candidate;
            }
        }

        return nearestMob;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!MobWarCommands.enabled) {
            if (wasEnabled) {
                clearWarState();
            }

            wasEnabled = false;
            return;
        }

        wasEnabled = true;

        if (++tickCounter % SCAN_INTERVAL != 0) {
            return;
        }

        int processed = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!isController(player) || !player.isAlive()) {
                    continue;
                }

                List<Mob> mobs = level.getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(RADIUS),
                        mob -> mob.isAlive()
                                && player.distanceToSqr(mob)
                                        <= RADIUS * RADIUS);

                if (mobs.isEmpty()) {
                    continue;
                }

                int count = Math.min(
                        MAX_MOBS_PER_SCAN - processed,
                        mobs.size());

                int start = Math.floorMod(rotation, mobs.size());

                for (int i = 0; i < count; i++) {
                    Mob attacker = mobs.get(
                            (start + i) % mobs.size());

                    CONTROLLED.put(attacker, Boolean.TRUE);
                    registerCombatGoal(attacker);

                    LivingEntity target =
                            findTarget(attacker, mobs, level);

                    attacker.setTarget(target);
                }

                processed += count;
                rotation = (start + count) % mobs.size();

                if (processed >= MAX_MOBS_PER_SCAN) {
                    return;
                }
            }
        }
    }

    private static void clearWarState() {
        for (Mob mob : new ArrayList<>(CONTROLLED.keySet())) {
            if (mob == null || mob.isRemoved()) {
                continue;
            }

            mob.setTarget(null);
        }

        CONTROLLED.clear();
    }

    @SubscribeEvent
    public static void protectTubi(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isController(player)
                && event.getSource().getEntity() != null) {
            event.setCanceled(true);
        }
    }
            }
