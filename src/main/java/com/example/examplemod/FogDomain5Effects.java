
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

            // Chỉ sao chép khi sinh vật thực sự đang ở trong nước.
            if (target.isInWater()) {
                copyCreatureAbilities(player, target, data);
            }

            applyWind(target);
            applyRain(target);
            applySnow(target);
            applyFire(target);
            spawnWeather(level, target);
        }
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

        List<String> sources =
                data.getCreatureSkillSources(
                        owner.getUUID(),
                        "unused_check"
                );

        // Mở khóa kỹ năng của loại sinh vật này.
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

        if (randomChance(2)) {
            target.setRemainingFireTicks(
                    Math.max(target.getRemainingFireTicks(), 60)
            );
        }
    }

    private static void spawnWeather(
            ServerLevel level,
            LivingEntity target
    ) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        level.sendParticles(
                ParticleTypes.CLOUD,
                x, y + 0.8D, z,
                2, 0.5D, 0.3D, 0.5D, 0.02D
        );

        level.sendParticles(
                ParticleTypes.RAIN,
                x, y + 1.5D, z,
                3, 0.4D, 0.5D, 0.4D, 0.02D
        );

        level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                x, y + 1.8D, z,
                1, 0.4D, 0.5D, 0.4D, 0.01D
        );

        if (randomChance(2)) {
            level.sendParticles(
                    ParticleTypes.FLAME,
                    x, y + 0.5D, z,
                    2, 0.4D, 0.3D, 0.4D, 0.01D
            );
        }
    }

    private static boolean randomChance(int denominator) {
        return ThreadLocalRandom.current()
                .nextInt(denominator) == 0;
    }
}
