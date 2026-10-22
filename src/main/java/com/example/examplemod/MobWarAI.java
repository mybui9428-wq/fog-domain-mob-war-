
package com.example.examplemod;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarAI {

    private static final double RADIUS = 30.0D;
    private static final int MAX_PER_SCAN = 10;
    private static final int SCAN_INTERVAL = 20;

    private static int tickCounter = 0;
    private static int rotation = 0;

    private static final Map<PathfinderMob, Boolean> REGISTERED =
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

    private static boolean inDomain(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        return inDomain(level, entity);
    }

    private static boolean validTarget(
            PathfinderMob attacker,
            LivingEntity target,
            ServerLevel level) {

        if (!MobWarCommands.enabled
                || target == null
                || target == attacker
                || !target.isAlive()
                || !(target instanceof Mob)
                    && !(target instanceof Player)) {
            return false;
        }

        if (target instanceof Player player && isController(player)) {
            return false;
        }

        return inDomain(level, attacker)
                && inDomain(level, target);
    }

    private static boolean hasValidTarget(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        return validTarget(mob, mob.getTarget(), level);
    }

    private static void registerMob(PathfinderMob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);

        mob.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<LivingEntity>(
                        mob,
                        LivingEntity.class,
                        10,
                        true,
                        false,
                        (target, level) ->
                                validTarget(mob, target, level)) {

                    @Override
                    public boolean canUse() {
                        return MobWarCommands.enabled
                                && !mob.isRemoved()
                                && inDomain(mob)
                                && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return MobWarCommands.enabled
                                && !mob.isRemoved()
                                && hasValidTarget(mob)
                                && super.canContinueToUse();
                    }
                });

        mob.goalSelector.addGoal(
                1,
                new MeleeAttackGoal(mob, 1.0D, true) {

                    @Override
                    public boolean canUse() {
                        return MobWarCommands.enabled
                                && !mob.isRemoved()
                                && hasValidTarget(mob)
                                && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return MobWarCommands.enabled
                                && !mob.isRemoved()
                                && hasValidTarget(mob)
                                && super.canContinueToUse();
                    }
                });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!MobWarCommands.enabled) {
            return;
        }

        if (++tickCounter % SCAN_INTERVAL != 0) {
            return;
        }

        int processed = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!isController(player) || !player.isAlive()) {
                    continue;
                }

                List<PathfinderMob> mobs = level.getEntitiesOfClass(
                        PathfinderMob.class,
                        player.getBoundingBox().inflate(RADIUS),
                        mob -> !mob.isRemoved()
                                && player.distanceToSqr(mob)
                                    <= RADIUS * RADIUS);

                if (mobs.isEmpty()) {
                    continue;
                }

                int count = Math.min(
                        MAX_PER_SCAN - processed,
                        mobs.size());

                int start = Math.floorMod(rotation, mobs.size());

                for (int i = 0; i < count; i++) {
                    registerMob(mobs.get((start + i) % mobs.size()));
                }

                processed += count;
                rotation = (start + count) % mobs.size();

                if (processed >= MAX_PER_SCAN) {
                    return;
                }
            }
        }
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
