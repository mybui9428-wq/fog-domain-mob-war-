
package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Effects {

    private static final int RADIUS = 30;
    private static final int EFFECT_INTERVAL = 20;
    private static final int MAX_FREEZE_TICKS = 140;

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

        if (!FogDomain5.isActive(player.getUUID())
                || FogDomain5.getTier(player.getUUID()) != 1) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (level.getGameTime() % EFFECT_INTERVAL != 0) {
            return;
        }

        double centerX = player.getX();
        double centerY = player.getY();
        double centerZ = player.getZ();

        // Tạo thời tiết rơi từ trên cao xuống toàn bộ khu vực hồ.
        spawnDomainWeather(
                level,
                centerX,
                centerY,
                centerZ
        );

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

        for (LivingEntity target : entities) {

            if (target == player || !target.isAlive()) {
                continue;
            }

            double dx = target.getX() - centerX;
            double dz = target.getZ() - centerZ;

            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }

            // Sao chép năng lực khi sinh vật đang ở trong nước.
            if (target.isInWater()) {
                copyCreatureAbilities(player, target, data);
            }

            applyWind(target);
            applyRain(target);
            applySnow(target);
            applyFire(target);
        }
    }

    private static void spawnDomainWeather(
            ServerLevel level,
            double centerX,
            double centerY,
            double centerZ
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Mưa rơi từ trên cao.
        for (int i = 0; i < 100; i++) {
            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (distanceSquared(x, z, centerX, centerZ)
                    > RADIUS * RADIUS) {
                continue;
            }

            double y = centerY + random.nextDouble(8.0D, 18.0D);

            level.sendParticles(
                    ParticleTypes.FALLING_WATER,
                    x, y, z,
                    1,
                    0.03D, 0.05D, 0.03D,
                    0.01D
            );
        }

        // Tuyết rơi từ trên cao.
        for (int i = 0; i < 55; i++) {
            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (distanceSquared(x, z, centerX, centerZ)
                    > RADIUS * RADIUS) {
                continue;
            }

            double y = centerY + random.nextDouble(8.0D, 20.0D);

            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    1,
                    0.15D, 0.12D, 0.15D,
                    0.015D
            );
        }

        // Gió cuốn mây và bụi ngang qua hồ.
        for (int i = 0; i < 30; i++) {
            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);
            double y = centerY + random.nextDouble(0.0D, 5.0D);

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    1,
                    0.35D, 0.08D, 0.08D,
                    0.025D
            );
        }

        // Hạt lửa xuất hiện trên cao và rơi xuống.
        for (int i = 0; i < 18; i++) {
            double x = centerX + random.nextDouble(-RADIUS, RADIUS);
            double z = centerZ + random.nextDouble(-RADIUS, RADIUS);

            if (distanceSquared(x, z, centerX, centerZ)
                    > RADIUS * RADIUS) {
                continue;
            }

            double y = centerY + random.nextDouble(7.0D, 15.0D);

            level.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z,
                    1,
                    0.08D, 0.15D, 0.08D,
                    0.035D
            );

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y + 0.2D, z,
                    1,
                    0.05D, 0.08D, 0.05D,
                    0.015D
            );
        }
    }

    private static double distanceSquared(
            double x,
            double z,
            double centerX,
            double centerZ
    ) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz;
    }

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
                            "§b[Lĩnh Vực Thôn Phệ] §fĐã sao chép: "
                                    + entityTypeId
                                    + (newSkills > 0
                                    ? " | Kỹ năng mới: " + newSkills
                                    : "")
                    )
            );
        }
    }

    private static void applyWind(LivingEntity target) {

        target.addEffect(new MobEffectInstance(
                MobEffects.NAUSEA, 60, 0, false, false, true
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, 60, 0, false, false, true
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, 60, 0, false, false, true
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS, 40, 0, false, false, true
        ));

        target.addEffect(new MobEffectInstance(
                MobEffects.POISON, 60, 0, false, false, true
        ));
    }

    private static void applyRain(LivingEntity target) {

        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS, 60, 0, false, false, true
        ));
    }

    private static void applySnow(LivingEntity target) {

        if (randomChance(3)) {
            int frozen = target.getTicksFrozen();

            target.setTicksFrozen(
                    Math.min(MAX_FREEZE_TICKS, frozen + 2)
            );
        }
    }

    private static void applyFire(LivingEntity target) {

        // Lửa vẫn gây cháy cho sinh vật trong vùng.
        if (randomChance(2)) {
            target.setRemainingFireTicks(
                    Math.max(target.getRemainingFireTicks(), 60)
            );
        }
    }

    private static boolean randomChance(int denominator) {
        return ThreadLocalRandom.current()
                .nextInt(denominator) == 0;
    }
}
