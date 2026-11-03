
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainPassive {

    private static final String CONTROLLER = "tubi2531";

    private static final double SUCTION_RADIUS = 15.0D;
    private static final double ASSIMILATION_RADIUS = 40.0D;

    private static final int SUCTION_TICKS = 40;
    private static final int WEAKNESS_TICKS = 20 * 60;
    private static final int COOLDOWN_TICKS = 20 * 60;

    private static final Map<UUID, PassiveData> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, Integer> COOLDOWNS =
            new HashMap<>();

    private static final Random RANDOM = new Random();

    private static final class PassiveData {
        final ServerLevel level;
        final UUID playerId;
        int ticks;

        PassiveData(ServerLevel level, UUID playerId) {
            this.level = level;
            this.playerId = playerId;
            this.ticks = SUCTION_TICKS;
        }
    }

    private FogDomainPassive() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
        .equalsIgnoreCase(CONTROLLER);
    }

    /**
     * Khi sát thương sắp làm tubi2531 chết,
     * giữ lại đúng 1 HP và kích hoạt Nhập Hồn.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isController(player)) {
            return;
        }

        if (!MobWarCommands.enabled) {
            return;
        }

        UUID id = player.getUUID();

        if (ACTIVE.containsKey(id)) {
            event.setNewDamage(0.0F);
            player.setHealth(1.0F);
            return;
        }

        if (COOLDOWNS.containsKey(id)) {
            if (player.getHealth() <= 1.0F) {
                event.setNewDamage(0.0F);
                player.setHealth(1.0F);
            }
            return;
        }

        float health = player.getHealth();
        float damage = event.getNewDamage();

        if (health - damage <= 1.0F) {

            // Không cho sát thương làm người chơi chết.
            event.setNewDamage(Math.max(0.0F, health - 1.0F));

            player.setHealth(1.0F);

            startPassive(player);
        }
    }

    private static void startPassive(ServerPlayer player) {

        UUID id = player.getUUID();

        if (ACTIVE.containsKey(id) || COOLDOWNS.containsKey(id)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        ACTIVE.put(id, new PassiveData(level, id));

        COOLDOWNS.put(id, COOLDOWN_TICKS);

        player.setHealth(1.0F);

        player.sendSystemMessage(Component.literal(
                "§5§lNHẬP HỒN! §dCơ thể đang mở hố đen hấp thụ..."));

        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                180,
                1.5D,
                1.5D,
                1.5D,
                0.5D
        );

        level.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                80,
                1.0D,
                1.0D,
                1.0D,
                0.2D
        );
    }

    private static void pullNearby(PassiveData data, ServerPlayer player) {

        ServerLevel level = data.level;

        AABB area = player.getBoundingBox().inflate(SUCTION_RADIUS);

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && entity.distanceToSqr(player)
                                <= SUCTION_RADIUS * SUCTION_RADIUS
        );

        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);

        for (LivingEntity target : nearby) {

            Vec3 targetCenter = target.getBoundingBox().getCenter();

            Vec3 direction = center.subtract(targetCenter);

            double distance = direction.length();

            if (distance > 0.01D) {

                Vec3 velocity = direction.normalize().scale(
                        Math.min(0.8D, 0.15D + distance * 0.06D)
                );

                target.setDeltaMovement(velocity);
                target.hurtMarked = true;
            }

            level.sendParticles(
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
        }

        level.sendParticles(
                ParticleTypes.SOUL,
                center.x,
                center.y,
                center.z,
                10,
                0.5D,
                0.5D,
                0.5D,
                0.1D
        );
    }

    /**
     * Chọn ngẫu nhiên một sinh vật hoặc người chơi
     * trong phạm vi 40 khối để đồng hóa.
     */
    private static LivingEntity chooseTarget(ServerPlayer player) {

        ServerLevel level = (ServerLevel) player.level();

        AABB area = player.getBoundingBox().inflate(
                ASSIMILATION_RADIUS
        );

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && entity.distanceToSqr(player)
                                <= ASSIMILATION_RADIUS
                                * ASSIMILATION_RADIUS
        );

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    private static void assimilate(
            ServerPlayer player,
            LivingEntity target
    ) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

      FogDomainAbilityData data =
        FogDomainAbilityData.get(((ServerLevel) player.level()).getServer());

        data.initializePower(player.getUUID(), true);

        String typeId = BuiltInRegistries.ENTITY_TYPE
                .getKey(target.getType())
                .toString();

        ((ServerLevel) player.level()).sendParticles(
                ParticleTypes.SOUL,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                60,
                0.6D,
                0.8D,
                0.6D,
                0.2D
        );

        if (target instanceof ServerPlayer absorbedPlayer) {

            data.transferPlayerPowers(
                    player.getUUID(),
                    absorbedPlayer.getUUID()
            );

            absorbedPlayer.hurtServer(
                    (ServerLevel) absorbedPlayer.level(),
                    absorbedPlayer.level().damageSources().generic(),
                    100000.0F
            );

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa người chơi "
                            + absorbedPlayer.getName().getString()
                            + ". Bạn nhận 50% sức mạnh của họ."
            ));

        } else if (target instanceof Mob mob) {

            data.initializePower(mob.getUUID(), false);

            data.transferCreaturePowers(
                    player.getUUID(),
                    mob.getUUID()
            );

            data.addAbility(player.getUUID(), typeId);

            int unlocked = data.unlockCreatureSkills(
                    player.getUUID(),
                    typeId
            );

            mob.discard();

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa sinh vật: "
                            + typeId
                            + " | Kỹ năng mở khóa: "
                            + unlocked
            ));

        } else {
            // Sinh vật không thuộc Mob: loại bỏ sau khi đồng hóa.
            target.discard();

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa: " + typeId
            ));
        }

        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                WEAKNESS_TICKS,
                0
        ));

        player.sendSystemMessage(Component.literal(
                "§5Nhập Hồn hoàn tất! Bạn bị suy yếu trong 1 phút."
        ));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        // Đếm thời gian hồi của Nhập Hồn.
        var cooldownIterator = COOLDOWNS.entrySet().iterator();

        while (cooldownIterator.hasNext()) {

            Map.Entry<UUID, Integer> entry = cooldownIterator.next();

            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                cooldownIterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        var iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, PassiveData> entry = iterator.next();

            PassiveData data = entry.getValue();

            Entity entity = data.level.getEntity(data.playerId);

            if (!(entity instanceof ServerPlayer player)
                    || !player.isAlive()
                    || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            // Giữ máu ở nửa tim trong lúc hố đen hoạt động.
            player.setHealth(1.0F);

            data.ticks--;

            pullNearby(data, player);

            if (data.ticks <= 0) {

                iterator.remove();

                data.level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        8,
                        1.0D,
                        1.0D,
                        1.0D,
                        0.0D
                );

                LivingEntity target = chooseTarget(player);

                if (target == null) {

                    player.setHealth(player.getMaxHealth());

                    player.sendSystemMessage(Component.literal(
                            "§cNhập Hồn thất bại: không có mục tiêu trong 40 khối."
                    ));

                } else {

                    assimilate(player, target);
                }
            }
        }
    }
}
