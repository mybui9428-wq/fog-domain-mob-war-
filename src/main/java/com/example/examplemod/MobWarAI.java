
package com.example.examplemod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {

    private static final double RADIUS = 30.0D;
    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_MOBS_PER_SCAN = 8;
    private static final int GROUP_SIZE = 5;
    private static final int GLOW_TICKS = 160;

    private static int tickCounter = 0;
    private static int scanRotation = 0;

    private static final Map<PathfinderMob, Boolean> REGISTERED =
            new WeakHashMap<>();

    private static final Map<UUID, List<UUID>> GROUPS =
            new HashMap<>();

    private static final Map<UUID, Integer> GROUP_TIMERS =
            new HashMap<>();

    private MobWarAI() {
    }

    private static boolean isController(Player player) {
        return player.getGameProfile().getName()
                .equalsIgnoreCase("tubi2531");
    }

    private static boolean inDomain(
            ServerLevel level, Entity entity) {

        for (ServerPlayer owner : level.players()) {
            if (isController(owner)
                    && owner.isAlive()
                    && owner.distanceToSqr(entity)
                    <= RADIUS * RADIUS) {
                return true;
            }
        }

        return false;
    }

    private static ServerPlayer getController(
            ServerLevel level, Entity entity) {

        for (ServerPlayer owner : level.players()) {
            if (isController(owner)
                    && owner.isAlive()
                    && owner.distanceToSqr(entity)
                    <= RADIUS * RADIUS) {
                return owner;
            }
        }

        return null;
    }

    private static boolean validTarget(
            Mob attacker,
            LivingEntity target,
            ServerLevel level) {

        if (!MobWarCommands.enabled
                || attacker == target
                || attacker.isRemoved()
                || !attacker.isAlive()
                || target == null
                || target.isRemoved()
                || !target.isAlive()
                || target.level() != level) {
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

    private static void registerCombat(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }

        if (REGISTERED.containsKey(pathfinder)) {
            return;
        }

        pathfinder.goalSelector.addGoal(
                3,
                new MeleeAttackGoal(pathfinder, 1.0D, true)
        );

        REGISTERED.put(pathfinder, Boolean.TRUE);
    }

    private static List<LivingEntity> getEntities(
            ServerLevel level, ServerPlayer owner) {

        return level.getEntitiesOfClass(
                LivingEntity.class,
                owner.getBoundingBox().inflate(RADIUS),
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity != owner
                        && (entity instanceof Mob
                        || entity instanceof Player)
                        && !(entity instanceof Player player
                        && isController(player))
                        && owner.distanceToSqr(entity)
                        <= RADIUS * RADIUS
        );
    }

    private static List<Mob> getMobs(
            ServerLevel level, ServerPlayer owner) {

        return level.getEntitiesOfClass(
                Mob.class,
                owner.getBoundingBox().inflate(RADIUS),
                mob -> mob.isAlive()
                        && !mob.isRemoved()
                        && owner.distanceToSqr(mob)
                        <= RADIUS * RADIUS
        );
    }

    private static List<UUID> getCurrentGroup(
            ServerPlayer owner) {

        return GROUPS.computeIfAbsent(
                owner.getUUID(), id -> new ArrayList<>());
    }

    private static boolean isInCurrentGroup(
            ServerPlayer owner, LivingEntity entity) {

        return getCurrentGroup(owner).contains(entity.getUUID());
    }

    private static void chooseNewGroup(
            ServerLevel level, ServerPlayer owner) {

        List<LivingEntity> entities = getEntities(level, owner);
        List<UUID> group = new ArrayList<>();

        if (entities.size() <= GROUP_SIZE) {
            // Neu con 5 hoac it hon, tat ca deu la muc tieu.
            for (LivingEntity entity : entities) {
                group.add(entity.getUUID());
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        GLOW_TICKS,
                        0,
                        false,
                        true
                ));
            }
        } else {
            // Chon ngau nhien 5 thuc the.
            Collections.shuffle(entities);

            for (int i = 0; i < GROUP_SIZE; i++) {
                LivingEntity entity = entities.get(i);
                group.add(entity.getUUID());

                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        GLOW_TICKS,
                        0,
                        false,
                        true
                ));
            }
        }

        GROUPS.put(owner.getUUID(), group);
        GROUP_TIMERS.put(owner.getUUID(), 0);
    }

    private static boolean currentGroupHasLivingTargets(
            ServerLevel level,
            ServerPlayer owner) {

        List<UUID> group = getCurrentGroup(owner);

        for (UUID uuid : group) {
            Entity entity = level.getEntity(uuid);

            if (entity instanceof LivingEntity living
                    && living.isAlive()
                    && !living.isRemoved()
                    && inDomain(level, living)) {
                return true;
            }
        }

        return false;
    }

    private static LivingEntity findTarget(
            Mob attacker,
            List<Mob> mobs,
            ServerLevel level,
            ServerPlayer owner) {

        LivingEntity nearestGroupTarget = null;
        double groupDistance = Double.MAX_VALUE;

        LivingEntity nearestOtherTarget = null;
        double otherDistance = Double.MAX_VALUE;

        boolean attackerIsTarget =
                isInCurrentGroup(owner, attacker);

        for (Mob candidate : mobs) {
            if (!validTarget(attacker, candidate, level)) {
                continue;
            }

            if (!attackerIsTarget
                    && !isInCurrentGroup(owner, candidate)) {
                // Ngoai nhom: uu tien cac muc tieu duoc chon.
                continue;
            }

            double distance = attacker.distanceToSqr(candidate);

            if (attackerIsTarget) {
                if (distance < otherDistance) {
                    otherDistance = distance;
                    nearestOtherTarget = candidate;
                }
            } else if (distance < groupDistance) {
                groupDistance = distance;
                nearestGroupTarget = candidate;
            }
        }

        for (ServerPlayer player : level.players()) {
            if (!validTarget(attacker, player, level)) {
                continue;
            }

            boolean isGroup = isInCurrentGroup(owner, player);
            double distance = attacker.distanceToSqr(player);

            if (attackerIsTarget) {
                // Thanh vien trong nhom danh nhau khi con <= 5.
                if (isGroup && distance < otherDistance) {
                    otherDistance = distance;
                    nearestOtherTarget = player;
                }
            } else if (isGroup && distance < groupDistance) {
                nearestGroupTarget = player;
                groupDistance = distance;
            }
        }

        if (attackerIsTarget) {
            return nearestOtherTarget;
        }

        return nearestGroupTarget;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        if (!MobWarCommands.enabled) {
            clearState();
            return;
        }

        if (++tickCounter % SCAN_INTERVAL != 0) {
            return;
        }

        int processed = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer owner : level.players()) {
                if (!isController(owner) || !owner.isAlive()) {
                    continue;
                }

                List<LivingEntity> entities =
                        getEntities(level, owner);

                List<UUID> group = getCurrentGroup(owner);

                if (group.isEmpty()
                        || !currentGroupHasLivingTargets(level, owner)) {
                    chooseNewGroup(level, owner);
                }

                // Neu con <= 5 thuc the, cho ca nhom danh lan nhau.
                if (entities.size() <= GROUP_SIZE) {
                    chooseNewGroup(level, owner);
                }

                List<Mob> mobs = getMobs(level, owner);

                if (mobs.isEmpty()) {
                    continue;
                }

                int count = Math.min(
                        MAX_MOBS_PER_SCAN - processed,
                        mobs.size()
                );

                if (count <= 0) {
                    return;
                }

                int start = Math.floorMod(scanRotation, mobs.size());

                for (int i = 0; i < count; i++) {
                    Mob attacker =
                            mobs.get((start + i) % mobs.size());

                    registerCombat(attacker);

                    LivingEntity target =
                            findTarget(attacker, mobs, level, owner);

                    if (target != null) {
                        attacker.setTarget(target);
                    } else {
                        attacker.setTarget(null);
                    }
                }

                processed += count;
                scanRotation = (start + count) % mobs.size();

                if (processed >= MAX_MOBS_PER_SCAN) {
                    return;
                }
            }
        }
    }

    private static void clearState() {
        for (PathfinderMob mob :
                new ArrayList<>(REGISTERED.keySet())) {
            if (mob != null && !mob.isRemoved()) {
                mob.setTarget(null);
            }
        }

        REGISTERED.clear();
        GROUPS.clear();
        GROUP_TIMERS.clear();
        tickCounter = 0;
        scanRotation = 0;
    }

    @SubscribeEvent
    public static void protectController(
            LivingIncomingDamageEvent event) {

        if (!MobWarCommands.enabled) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isController(player)) {
            return;
        }

        if (inDomain((ServerLevel) player.level(), player)) {
            event.setCanceled(true);
        }
    }
}
