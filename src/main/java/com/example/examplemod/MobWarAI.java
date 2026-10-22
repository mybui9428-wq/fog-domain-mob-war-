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

    private static final Map<Mob, Boolean> REGISTERED =
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
                || target == null
                || target == attacker
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

    private static boolean hasValidTarget(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        return validTarget(mob, mob.getTarget(), level);
    }

    private static void registerMob(Mob mob) {
        if (mob.isRemoved() || REGISTERED.containsKey(mob)) {
            return;
        }

        REGISTERED.put(mob, Boolean.TRUE);

        // Cho sinh vật chọn mọi Mob khác hoặc người chơi khác
        // làm mục tiêu, ngoại trừ tubi2531.
        mob.targetSelector.addGoal(
                0,
                new NearestAttackableTargetGoal<LivingEntity>(
                        mob,
                        LivingEntity.class,
                        10,
                        true,
                        false,
                        (target, level) ->
                                validTarget(mob, target, level)) {

                    @Override
                    protected double getFollowDistance() {
                        return RADIUS;
                    }

                    @Override
                    public boolean canUse() {
                        return MobWarCommands.enabled
                                && !mob.isRemoved()
                                && inDomain(
                                        (ServerLevel) mob.level(), mob)
                                && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return MobWarCommands.enabled
                                && hasValidTarget(mob)
                                && super.canContinueToUse();
                    }
                });

        // Chỉ thêm AI cận chiến cho loại có hỗ trợ Pathfinding.
        // Những Mob không thuộc PathfinderMob vẫn được đăng ký
        // mục tiêu và có thể dùng AI chiến đấu vốn có của chúng.
        if (mob instanceof PathfinderMob pathfinder) {
            pathfinder.goalSelector.addGoal(
                    1,
                    new MeleeAttackGoal(
                            pathfinder, 1.0D, true) {

                        @Override
                        public boolean canUse() {
                            return MobWarCommands.enabled
                                    && !pathfinder.isRemoved()
                                    && hasValidTarget(pathfinder)
                                    && super.canUse();
                        }

                        @Override
                        public boolean canContinueToUse() {
                            return MobWarCommands.enabled
                                    && !pathfinder.isRemoved()
                                    && hasValidTarget(pathfinder)
                                    && super.canContinueToUse();
                        }
                    });
        }
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

                // Duyệt Mob thay vì PathfinderMob để không gặp lỗi
                // chuyển kiểu Mob -> PathfinderMob.
                List<Mob> mobs = level.getEntitiesOfClass(
                        Mob.class,
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
                    Mob candidate =
                            mobs.get((start + i) % mobs.size());

                    registerMob(candidate);
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
