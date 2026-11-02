
package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainBlackHole {

    private static final int DURATION = 40;
    private static final double ABSORB_RANGE = 25.0D;
    private static final double CONE_DOT = 0.35D;

    private static final int EFFECT_DURATION = 20 * 60;

    private static final Map<UUID, HoleData> ACTIVE = new HashMap<>();

    private static final class HoleData {
        final ServerLevel level;
        final UUID playerId;
        final Vec3 center;
        final List<UUID> targets;
        int ticks;

        HoleData(
                ServerLevel level,
                UUID playerId,
                Vec3 center,
                List<UUID> targets
        ) {
            this.level = level;
            this.playerId = playerId;
            this.center = center;
            this.targets = targets;
            this.ticks = DURATION;
        }
    }

    private FogDomainBlackHole() {
    }

    public static boolean startSingle(ServerPlayer player, Mob target) {
        if (player == null || target == null
                || !player.isAlive() || !target.isAlive()
                || player.level() != target.level()) {
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();

        List<UUID> targets = new ArrayList<>();
        targets.add(target.getUUID());

        startHole(player, level, targets);

        player.sendSystemMessage(Component.literal(
                "Hố đen đã hút mục tiêu. Đang hấp thụ năng lực..."));

        return true;
    }

    /**
     * Thôn Phệ Vạn Vật:
     * Hút sinh vật và người chơi phía trước trong phạm vi 25 khối.
     */
    public static boolean startMass(ServerPlayer player) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);

        AABB area = player.getBoundingBox().inflate(ABSORB_RANGE);

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity != player
                        && !(entity instanceof ArmorStand)
                        && entity.distanceToSqr(player)
                                <= ABSORB_RANGE * ABSORB_RANGE
                        && isInFront(origin, look, entity)
        );

        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Không có sinh vật hoặc người chơi phía trước để hấp thụ."));
            return false;
        }

        List<UUID> targets = new ArrayList<>();

        for (LivingEntity entity : nearby) {
            targets.add(entity.getUUID());
        }

        startHole(player, level, targets);

        player.sendSystemMessage(Component.literal(
                "Thôn Phệ Vạn Vật đã kích hoạt! Đang hút "
                        + targets.size() + " mục tiêu."));

        return true;
    }

    private static boolean isInFront(
            Vec3 origin,
            Vec3 look,
            LivingEntity target
    ) {
        Vec3 targetPosition = target.getBoundingBox().getCenter();
        Vec3 direction = targetPosition.subtract(origin);

        if (direction.lengthSqr() < 0.01D) {
            return true;
        }

        return look.dot(direction.normalize()) >= CONE_DOT;
    }

    private static void startHole(
            ServerPlayer player,
            ServerLevel level,
            List<UUID> targets
    ) {
        ACTIVE.put(
                player.getUUID(),
                new HoleData(
                        level,
                        player.getUUID(),
                        player.position().add(0.0D, 1.0D, 0.0D),
                        targets
                )
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                100,
                1.0D,
                1.0D,
                1.0D,
                0.5D
        );

        level.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                35,
                0.8D,
                0.8D,
                0.8D,
                0.1D
        );
    }

    /**
     * Sinh vật thân thiện hoặc trung lập:
     * biến mất và trao hiệu ứng có lợi cho người hấp thụ.
     *
     * Trả về true nếu loài đó thuộc nhóm này.
     */
    
private static boolean absorbFriendly(
        ServerPlayer player,
        Mob mob,
        String typeId
) {
    switch (typeId) {

        // Hồi phục
        case "minecraft:axolotl":
        case "minecraft:mooshroom":
        case "minecraft:sniffer":
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Tốc độ
        case "minecraft:bee":
        case "minecraft:dolphin":
        case "minecraft:horse":
        case "minecraft:donkey":
        case "minecraft:mule":
        case "minecraft:ocelot":
        case "minecraft:cat":
        case "minecraft:rabbit":
            player.addEffect(new MobEffectInstance(
                    MobEffects.SPEED,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Sức mạnh
        case "minecraft:wolf":
        case "minecraft:polar_bear":
        case "minecraft:goat":
        case "minecraft:llama":
        case "minecraft:panda":
            player.addEffect(new MobEffectInstance(
                    MobEffects.STRENGTH,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Kháng sát thương
        case "minecraft:turtle":
        case "minecraft:armadillo":
        case "minecraft:camel":
        case "minecraft:iron_golem":
        case "minecraft:snow_golem":
            player.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Hơi thở dưới nước
        case "minecraft:squid":
        case "minecraft:glow_squid":
        case "minecraft:salmon":
        case "minecraft:cod":
        case "minecraft:tropical_fish":
        case "minecraft:pufferfish":
        case "minecraft:nautilus":
            player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING,
                    EFFECT_DURATION,
                    0
            ));
            break;

        // Kháng lửa
        case "minecraft:strider":
            player.addEffect(new MobEffectInstance(
                    MobEffects.FIRE_RESISTANCE,
                    EFFECT_DURATION,
                    0
            ));
            break;

        // Nhảy cao
        case "minecraft:frog":
        case "minecraft:chicken":
        case "minecraft:parrot":
            player.addEffect(new MobEffectInstance(
                    MobEffects.JUMP_BOOST,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Hấp thụ sát thương
        case "minecraft:allay":
        case "minecraft:bat":
        case "minecraft:villager":
        case "minecraft:wandering_trader":
        case "minecraft:happy_ghast":
        case "minecraft:cow":
        case "minecraft:sheep":
        case "minecraft:pig":
        case "minecraft:fox":
            player.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION,
                    EFFECT_DURATION,
                    1
            ));
            break;

        // Cảm nhận trong bóng tối
        case "minecraft:spider":
        case "minecraft:cave_spider":
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    EFFECT_DURATION,
                    0
            ));
            break;

        default:
            return false;
    }

    // Sinh vật biến mất sau khi trao hiệu ứng.
    mob.discard();

    player.sendSystemMessage(Component.literal(
            "Đã hấp thụ sinh vật: "
                    + typeId
                    + ". Bạn nhận được hiệu ứng có lợi."
    ));

    return true;
}

    private static void absorb(
            HoleData hole,
            LivingEntity target
    ) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

        String typeId = BuiltInRegistries.ENTITY_TYPE
                .getKey(target.getType())
                .toString();

        FogDomainAbilityData data =
                FogDomainAbilityData.get(hole.level.getServer());

        Entity owner = hole.level.getEntity(hole.playerId);

        if (!(owner instanceof ServerPlayer player)
                || !player.isAlive()) {
            return;
        }

        data.initializePower(player.getUUID(), true);

        hole.level.sendParticles(
                ParticleTypes.SOUL,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                25,
                0.4D,
                0.6D,
                0.4D,
                0.15D
        );

        if (target instanceof ServerPlayer absorbedPlayer) {

            if (absorbedPlayer == player) {
                return;
            }

            data.transferPlayerPowers(
                    player.getUUID(),
                    absorbedPlayer.getUUID()
            );

            absorbedPlayer.hurtServer(
                    hole.level,
                    hole.level.damageSources().generic(),
                    100000.0F
            );

            player.sendSystemMessage(Component.literal(
                    "Đã hấp thụ người chơi "
                            + absorbedPlayer.getName().getString()
                            + ". Người đó giữ 50% sức mạnh, bạn nhận 50%."));

        } else if (target instanceof Mob mob) {

            // Sinh vật thân thiện/trung lập: nhận hiệu ứng rồi biến mất.
            if (absorbFriendly(player, mob, typeId)) {
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                return;
            }

            // Sinh vật thù địch: chuyển toàn bộ sức mạnh.
            data.initializePower(mob.getUUID(), false);

            data.transferCreaturePowers(
                    player.getUUID(),
                    mob.getUUID()
            );

            data.addAbility(player.getUUID(), typeId);

            int unlockedSkills = data.unlockCreatureSkills(
                    player.getUUID(),
                    typeId
            );

            mob.discard();

            player.sendSystemMessage(Component.literal(
                    "Đã hấp thụ sinh vật thù địch: "
                            + typeId
                            + " | Kỹ năng mới: "
                            + unlockedSkills
                            + " | Tổng năng lực: "
                            + data.getAbilityCount(player.getUUID())));
        }

        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        Iterator<Map.Entry<UUID, HoleData>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, HoleData> entry = iterator.next();
            HoleData hole = entry.getValue();

            Entity owner = hole.level.getEntity(hole.playerId);

            if (!(owner instanceof ServerPlayer player)
                    || !player.isAlive()
                    || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            hole.ticks--;

            Iterator<UUID> targetIterator = hole.targets.iterator();

            while (targetIterator.hasNext()) {

                UUID targetId = targetIterator.next();

                Entity entity = hole.level.getEntity(targetId);

                if (!(entity instanceof LivingEntity target)
                        || !target.isAlive()
                        || target.isRemoved()) {
                    targetIterator.remove();
                    continue;
                }

                Vec3 targetPoint = target.getBoundingBox().getCenter();
                Vec3 direction = hole.center.subtract(targetPoint);

                double distance = direction.length();

                if (distance > 0.0D) {

                    Vec3 pull = direction.normalize()
                            .scale(Math.min(
                                    0.8D,
                                    0.12D + distance * 0.08D
                            ));

                    target.setDeltaMovement(pull);
                    target.hurtMarked = true;
                }

                hole.level.sendParticles(
                        ParticleTypes.PORTAL,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(),
                        3,
                        0.2D,
                        0.3D,
                        0.2D,
                        0.05D
                );

                if (hole.ticks <= 0 || distance < 1.2D) {
                    absorb(hole, target);
                    targetIterator.remove();
                }
            }

            hole.level.sendParticles(
                    ParticleTypes.PORTAL,
                    hole.center.x,
                    hole.center.y,
                    hole.center.z,
                    12,
                    0.35D,
                    0.35D,
                    0.35D,
                    0.15D
            );

            if (hole.ticks <= 0 || hole.targets.isEmpty()) {

                iterator.remove();

                player.sendSystemMessage(Component.literal(
                        "Hố đen đã đóng. Đã xử lý các mục tiêu."));
            }
        }
    }
}
