
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Warden;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainTaming {

    private static int tickCounter = 0;

    private static final double FOLLOW_DISTANCE = 3.0D;
    private static final double ATTACK_DISTANCE = 2.0D;
    private static final double FOLLOW_SPEED = 1.2D;

    private FogDomainTaming() {
    }

    public static boolean isTamed(Entity entity) {
        if (entity == null) {
            return false;
        }

        UUID id = entity.getUUID();

        for (List<UUID> ids : FogDomainSkills.TAMED.values()) {
            if (ids.contains(id)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isOwner(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        if (++tickCounter < 2) {
            return;
        }

        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {

            ServerPlayer owner = findOwner(level);

            if (owner == null || !owner.isAlive()) {
                continue;
            }

            List<UUID> owned = FogDomainSkills.TAMED.get(
                    owner.getUUID()
            );

            if (owned == null || owned.isEmpty()) {
                continue;
            }

            List<UUID> remove = new ArrayList<>();

            for (UUID id : owned) {

                Entity entity = level.getEntity(id);

                if (!(entity instanceof Mob mob)
                        || !mob.isAlive()
                        || mob.isRemoved()) {
                    remove.add(id);
                    continue;
                }

                strengthenOnce(mob);

                // Creeper nô dịch không được tự phát nổ.
                if (mob instanceof Creeper creeper) {
                    creeper.setSwellDir(-1);
                }

                // Không cho phép tấn công chủ nhân hoặc đồng minh.
                LivingEntity currentTarget = mob.getTarget();

                if (currentTarget != null
                        && (isOwner(currentTarget)
                        || isTamed(currentTarget))) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }

                LivingEntity target = chooseTarget(owner, mob);

                if (target != null) {

                    mob.setTarget(target);

                    mob.getLookControl().setLookAt(
                            target,
                            30.0F,
                            30.0F
                    );

                    if (mob.distanceToSqr(target) > ATTACK_DISTANCE) {
                        mob.getNavigation().moveTo(
                                target,
                                FOLLOW_SPEED
                        );
                    } else {
                        mob.getNavigation().stop();
                    }

                } else {

                    mob.setTarget(null);

                    // Warden không được tự săn mồi bằng âm thanh.
                    if (mob instanceof Warden) {
                        mob.getNavigation().stop();
                    }

                    // Đi theo chủ nhân.
                    if (mob.distanceToSqr(owner) > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
                        mob.getNavigation().moveTo(
                                owner,
                                FOLLOW_SPEED
                        );
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }

            owned.removeAll(remove);

            if (owned.isEmpty()) {
                FogDomainSkills.TAMED.remove(owner.getUUID());
            }
        }
    }

    private static void strengthenOnce(Mob mob) {

        // Dùng thuộc tính gốc để không cộng sức mạnh lặp lại.
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth != null
                && !mob.getPersistentData().getBoolean("fog_tamed_strength")) {

            double health = maxHealth.getBaseValue();

            maxHealth.setBaseValue(Math.max(health * 2.0D, health + 20.0D));

            AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);

            if (damage != null) {
                damage.setBaseValue(
                        Math.max(damage.getBaseValue() * 2.0D, 8.0D)
                );
            }

            AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);

            if (armor != null) {
                armor.setBaseValue(
                        Math.max(armor.getBaseValue(), 10.0D)
                );
            }

            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);

            if (speed != null) {
                speed.setBaseValue(
                        Math.max(speed.getBaseValue() * 1.25D, 0.25D)
                );
            }

            mob.setHealth((float) maxHealth.getBaseValue());

            mob.getPersistentData().putBoolean(
                    "fog_tamed_strength",
                    true
            );
        }
    }

    private static ServerPlayer findOwner(ServerLevel level) {

        for (ServerPlayer player : level.players()) {
            if (isOwner(player) && player.isAlive()) {
                return player;
            }
        }

        return null;
    }

    private static LivingEntity chooseTarget(
            ServerPlayer owner,
            Mob companion) {

        // Kẻ vừa tấn công chủ nhân.
        LivingEntity attacker = owner.getLastHurtByMob();

        if (isValidTarget(owner, companion, attacker)) {

            // Warden chỉ phản công Warden hoang dã.
            if (companion instanceof Warden
                    && !(attacker instanceof Warden)) {
                return null;
            }

            return attacker;
        }

        // Mục tiêu chủ nhân vừa tấn công.
        LivingEntity ownerTarget = owner.getLastHurtMob();

        if (isValidTarget(owner, companion, ownerTarget)) {

            if (companion instanceof Warden
                    && !(ownerTarget instanceof Warden)) {
                return null;
            }

            return ownerTarget;
        }

        return null;
    }

    private static boolean isValidTarget(
            ServerPlayer owner,
            Mob companion,
            LivingEntity target) {

        if (target == null
                || !target.isAlive()
                || target.isRemoved()
                || target == owner
                || target == companion
                || target.level() != companion.level()) {
            return false;
        }

        // Không đánh đồng minh nô dịch.
        if (isTamed(target)) {
            return false;
        }

        // Không bao giờ đánh chủ nhân.
        if (isOwner(target)) {
            return false;
        }

        return true;
    }
}
