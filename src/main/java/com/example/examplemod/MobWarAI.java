
package com.example.examplemod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {

    private static final double RADIUS = 30.0D;
    private static final int SCAN_INTERVAL = 10;
    private static final int MAX_ATTACKERS_PER_SCAN = 8;
    private static final int ATTACK_COOLDOWN = 20;
    private static final float ATTACK_DAMAGE = 2.0F;

    private static int tickCounter = 0;
    private static int rotation = 0;
    private static boolean wasEnabled = false;

    private static final Map<PathfinderMob, Boolean> REGISTERED =
            new WeakHashMap<>();

    private static final Map<Mob, Boolean> CONTROLLED =
            new WeakHashMap<>();

    private static final Map<Mob, Long> NEXT_ATTACK =
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
                    && player.distanceToSqr(entity) <= RADIUS * RADIUS) {
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

    private static LivingEntity findTarget(
            Mob attacker,
            List<LivingEntity> nearby,
            ServerLevel level) {

        LivingEntity current = attacker.getTarget();

        if (validTarget(attacker, current, level)) {
            return current;
        }

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : nearby) {
            if (!validTarget(attacker, candidate, level)) {
                continue;
            }

            double distance = attacker.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private static void registerMob(PathfinderMob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);

        // Một goal riêng thay cho hai goal tấn công cũ.
        mob.goalSelector.addGoal(-1, new MobWarGoal(mob));
    }

    private static final class MobWarGoal extends Goal {

        private final PathfinderMob mob;

        private MobWarGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(mob.level() instanceof ServerLevel level)) {
                return false;
            }

            return validTarget(mob, mob.getTarget(), level);
        }

        @Override
        public boolean canContinueToUse() {
            if (!(mob.level() instanceof ServerLevel level)) {
                return false;
            }

            return validTarget(mob, mob.getTarget(), level);
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level)) {
                return;
            }

            LivingEntity target = mob.getTarget();

            if (!validTarget(mob, target, level)) {
                return;
            }

            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double reach =
                    (mob.getBbWidth() + target.getBbWidth()) * 0.5D
                            + 0.5D;

            double reachSquared = reach * reach;

            if (mob.distanceToSqr(target) > reachSquared) {
                mob.getNavigation().moveTo(target, 1.15D);
                return;
            }

            mob.getNavigation().stop();

            long now = level.getGameTime();
            long nextAttack = NEXT_ATTACK.getOrDefault(mob, 0L);

            if (now >= nextAttack) {
                NEXT_ATTACK.put(
                        mob, now + ATTACK_COOLDOWN);

                // Tự gây sát thương, không phụ thuộc thuộc tính
                // attack_damage vốn có của sinh vật.
                target.hurtServer(
                        level,
                        level.damageSources().mobAttack(mob),
                        ATTACK_DAMAGE);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();

            if (!(mob.level() instanceof ServerLevel level)
                    || !validTarget(mob, mob.getTarget(), level)) {
                mob.setTarget(null);
            }
        }
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

                List<LivingEntity> nearby =
                        level.getEntitiesOfClass(
                                LivingEntity.class,
                                player.getBoundingBox().inflate(RADIUS),
                                entity ->
                                        entity.isAlive()
                                                && player.distanceToSqr(entity)
                                                        <= RADIUS * RADIUS
                                                && (entity instanceof Mob
                                                || entity instanceof Player
                                                && !isController((Player) entity)));

                List<Mob> mobs = new ArrayList<>();

                for (LivingEntity entity : nearby) {
                    if (entity instanceof Mob mob) {
                        mobs.add(mob);
                    }
                }

                if (mobs.isEmpty()) {
                    continue;
                }

                int count = Math.min(
                        MAX_ATTACKERS_PER_SCAN - processed,
                        mobs.size());

                int start = Math.floorMod(rotation, mobs.size());

                for (int i = 0; i < count; i++) {
                    Mob attacker =
                            mobs.get((start + i) % mobs.size());

                    CONTROLLED.put(attacker, Boolean.TRUE);

                    LivingEntity target =
                            findTarget(attacker, nearby, level);

                    if (target != null) {
                        attacker.setTarget(target);
                    }

                    if (attacker instanceof PathfinderMob pathfinder) {
                        registerMob(pathfinder);
                    }
                }

                processed += count;
                rotation = (start + count) % mobs.size();

                if (processed >= MAX_ATTACKERS_PER_SCAN) {
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

            mob.getNavigation().stop();
        }

        CONTROLLED.clear();
        NEXT_ATTACK.clear();
    }

    @SubscribeEvent
    public static void protectTubi(
            LivingIncomingDamageEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isController(player)
                && event.getSource().getEntity() != null) {
            event.setCanceled(true);
        }
    }
}
