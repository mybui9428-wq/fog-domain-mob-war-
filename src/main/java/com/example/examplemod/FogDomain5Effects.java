
package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Effects {

    private static final int RADIUS = 30;
    private static final int POWER = 5;
    private static final int EFFECT_INTERVAL = 20;
    private static final int MAX_FREEZE_TICKS = 140;
    private static final int FREEZE_AFTER_RAIN = 200;

    private static final Map<UUID, Integer> RAIN_EXPOSURE =
            new HashMap<>();

    private FogDomain5Effects() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isController(player)) {
            return;
        }

        if (!FogDomain5.isActive(player.getUUID())) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        double centerX = player.getX();
        double centerY = player.getY();
        double centerZ = player.getZ();

        AABB area = new AABB(
                centerX - RADIUS,
                centerY - 12,
                centerZ - RADIUS,
                centerX + RADIUS,
                centerY + 12,
                centerZ + RADIUS
        );

        List<LivingEntity> entities =
                level.getEntitiesOfClass(LivingEntity.class, area);

        FogDomainAbilityData data =
                FogDomainAbilityData.get(level.getServer());

        Set<UUID> activeEntities = new HashSet<>();

        for (LivingEntity target : entities) {

            if (target == player || !target.isAlive()) {
                continue;
            }

            double dx = target.getX() - centerX;
            double dz = target.getZ() - centerZ;

            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }

            activeEntities.add(target.getUUID());

            // Giữ sao chép năng lực trong nước.
            if (target.isInWater()) {
                copyCreatureAbilities(player, target, data);
                sinkEntity(target);
            }

            applyRainCold(target);

            if (level.getGameTime() % EFFECT_INTERVAL == 0) {
                applySnow(target, level);
            }
        }

        RAIN_EXPOSURE.keySet().removeIf(
                id -> !activeEntities.contains(id)
        );

        if (level.getGameTime() % EFFECT_INTERVAL != 0) {
            return;
        }

        spawnDomainWeather(
                level,
                centerX,
                centerY,
                centerZ
        );

        for (LivingEntity target : entities) {

            if (target == player || !target.isAlive()) {
                continue;
            }

            double dx = target.getX() - centerX;
            double dz = target.getZ() - centerZ;

            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }

            applyWind(target, centerX, centerZ);
            applyRainFog(target, level);
            applyFire(target);
        }

        applyWeakAttributes(player, level, data);
    }

    // ==================== GIÓ ====================

    private static void applyWind(
            LivingEntity target,
            double centerX,
            double centerZ
    ) {
        double dx = target.getX() - centerX;
        double dz = target.getZ() - centerZ;

        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.01D) {
            dx = target.getLookAngle().x;
            dz = target.getLookAngle().z;
            distance = Math.sqrt(dx * dx + dz * dz);
        }

        if (distance < 0.01D) {
            return;
        }

        dx /= distance;
        dz /= distance;

        Vec3 motion = target.getDeltaMovement();

        double horizontalPower = 1.05D;
        double upwardPower = 0.65D;

        target.setDeltaMovement(
                motion.x + dx * horizontalPower,
                Math.max(motion.y, upwardPower),
                motion.z + dz * horizontalPower
        );

        target.hurtMarked = true;

        target.level().addParticle(
                ParticleTypes.CLOUD,
                target.getX(),
                target.getY() + 0.8D,
                target.getZ(),
                dx * 0.8D,
                0.15D,
                dz * 0.8D
        );
    }

    // ==================== MƯA VÀ LẠNH ====================

    private static void applyRainCold(LivingEntity target) {

        UUID id = target.getUUID();

        int exposure = RAIN_EXPOSURE.getOrDefault(id, 0) + 1;

        RAIN_EXPOSURE.put(id, exposure);

        if (exposure >= FREEZE_AFTER_RAIN) {

            int frozen = target.getTicksFrozen();

            target.setTicksFrozen(
                    Math.min(MAX_FREEZE_TICKS, frozen + 2)
            );

            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    60,
                    1,
                    false,
                    false,
                    true
            ));

            if (exposure >= FREEZE_AFTER_RAIN * 2) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS,
                        60,
                        2,
                        false,
                        false,
                        true
                ));
            }
        }
    }

    private static void applyRainFog(
            LivingEntity target,
            ServerLevel level
    ) {
        level.sendParticles(
                ParticleTypes.CLOUD,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                8,
                0.65D,
                0.8D,
                0.65D,
                0.015D
        );

        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                45,
                0,
                false,
                false,
                true
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS,
                45,
                0,
                false,
                false,
                true
        ));
    }

    // ==================== TUYẾT VÀ BĂNG ====================

    private static void applySnow(
            LivingEntity target,
            ServerLevel level
    ) {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        if (random.nextInt(3) != 0) {
            return;
        }

        target.setTicksFrozen(
                Math.min(
                        MAX_FREEZE_TICKS,
                        target.getTicksFrozen() + 25
                )
        );

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS,
                60,
                2,
                false,
                false,
                true
        ));

        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        for (int i = 0; i < 16; i++) {

            double angle = Math.PI * 2.0D * i / 16.0D;

            double offsetX = Math.cos(angle) * 0.65D;
            double offsetZ = Math.sin(angle) * 0.65D;

            for (int height = 0; height < 3; height++) {

                double offsetY = height * 0.65D + 0.25D;

                level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        x + offsetX,
                        y + offsetY,
                        z + offsetZ,
                        1,
                        0.02D,
                        0.02D,
                        0.02D,
                        0.005D
                );
            }
        }

        level.sendParticles(
                ParticleTypes.CLOUD,
                x,
                y + 1.0D,
                z,
                12,
                0.5D,
                0.7D,
                0.5D,
                0.02D
        );
    }

    // ==================== LỬA RƠI ====================

    private static void applyFire(LivingEntity target) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        if (random.nextInt(2) == 0) {
            target.setRemainingFireTicks(
                    Math.max(
                            target.getRemainingFireTicks(),
                            180
                    )
            );
        }
    }

    // ==================== THỜI TIẾT LĨNH VỰC ====================

    private static void spawnDomainWeather(
            ServerLevel level,
            double centerX,
            double centerY,
            double centerZ
    ) {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        int rainCount = 220;
        int snowCount = 90;
        int windCount = 65;
        int fireCount = 90;

        for (int i = 0; i < rainCount; i++) {

            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (!insideCircle(x, z, centerX, centerZ)) {
                continue;
            }

            double y = centerY + random.nextDouble(8.0D, 20.0D);

            level.sendParticles(
                    ParticleTypes.FALLING_WATER,
                    x, y, z,
                    1,
                    0.02D, 0.12D, 0.02D,
                    0.025D
            );
        }

        for (int i = 0; i < snowCount; i++) {

            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (!insideCircle(x, z, centerX, centerZ)) {
                continue;
            }

            double y = centerY + random.nextDouble(8.0D, 20.0D);

            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    1,
                    0.12D, 0.18D, 0.12D,
                    0.02D
            );
        }

        for (int i = 0; i < windCount; i++) {

            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (!insideCircle(x, z, centerX, centerZ)) {
                continue;
            }

            double y = centerY + random.nextDouble(0.0D, 6.0D);

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    2,
                    0.4D, 0.1D, 0.4D,
                    0.04D
            );
        }

        for (int i = 0; i < fireCount; i++) {

            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (!insideCircle(x, z, centerX, centerZ)) {
                continue;
            }

            double y = centerY + random.nextDouble(9.0D, 20.0D);

            level.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z,
                    2,
                    0.08D, 0.12D, 0.08D,
                    0.04D
            );

            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x, y + 0.3D, z,
                    1,
                    0.04D, 0.08D, 0.04D,
                    0.02D
            );
        }
    }

    private static boolean insideCircle(
            double x,
            double z,
            double centerX,
            double centerZ
    ) {
        double dx = x - centerX;
        double dz = z - centerZ;

        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }

    // ==================== SAO CHÉP NĂNG LỰC ====================

    private static void copyCreatureAbilities(
            ServerPlayer owner,
            LivingEntity creature,
            FogDomainAbilityData data
    ) {
        String entityTypeId = BuiltInRegistries.ENTITY_TYPE
                .getKey(creature.getType())
                .toString();

        boolean newAbility = data.addAbility(
                owner.getUUID(),
                entityTypeId
        );

        int newSkills = data.unlockCreatureSkills(
                owner.getUUID(),
                entityTypeId
        );

        if (newAbility || newSkills > 0) {
            owner.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§b[Lĩnh Vực] §fĐã sao chép: "
                                    + entityTypeId
                                    + (newSkills > 0
                                    ? " | Kỹ năng mới: " + newSkills
                                    : "")
                    )
            );
        }
    }

    // ==================== THUỘC TÍNH YẾU ====================

    private static void applyWeakAttributes(
            ServerPlayer player,
            ServerLevel level,
            FogDomainAbilityData data
    ) {
        UUID id = player.getUUID();

        if (data.isAttributeWeak(id, "agility")) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    60,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (data.isAttributeWeak(id, "stamina")) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    60,
                    0,
                    false,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    60,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (data.isAttributeWeak(id, "strength")) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    60,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (data.isAttributeWeak(id, "spirit")) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    60,
                    0,
                    false,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.NAUSEA,
                    60,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (data.isAttributeWeak(id, "charisma")) {

            AABB area = player.getBoundingBox().inflate(16.0D);

            List<Mob> mobs =
                    level.getEntitiesOfClass(Mob.class, area);

            for (Mob mob : mobs) {
                if (mob.isAlive()) {
                    mob.setTarget(player);
                }
            }
        }
    }

    // ==================== KÉO CHÌM TRONG NƯỚC ====================

    private static void sinkEntity(LivingEntity target) {

        Vec3 motion = target.getDeltaMovement();

        double pull = 0.115D;
        double minimumDownSpeed = 0.335D;

        double newY = Math.min(
                motion.y - pull,
                -minimumDownSpeed
        );

        target.setDeltaMovement(
                motion.x * 0.85D,
                newY,
                motion.z * 0.85D
        );

        target.hurtMarked = true;
    }
}
