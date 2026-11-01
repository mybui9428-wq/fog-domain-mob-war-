
package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainSoulSplit {

    private static final double RADIUS = 15.0D;
    private static final int DURATION = 40;
    private static final int WEAKNESS_TICKS = 1200;

    private static final Random RANDOM = new Random();

    private static final Map<UUID, SoulData> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, Boolean> WAITING_RESPAWN =
            new HashMap<>();

    private static final class SoulData {
        final ServerLevel level;
        final UUID ownerId;
        final Vec3 center;
        final List<UUID> targets = new ArrayList<>();
        int ticks = DURATION;

        SoulData(ServerLevel level, UUID ownerId, Vec3 center) {
            this.level = level;
            this.ownerId = ownerId;
            this.center = center;
        }
    }

    private FogDomainSoulSplit() {
    }

    private static boolean isController(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    /**
     * Chỉ kích hoạt khi tubi2531 chết thật.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isController(player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        SoulData old = ACTIVE.remove(player.getUUID());

        if (old != null) {
            ACTIVE.remove(old.ownerId);
        }

        SoulData soul = new SoulData(
                level,
                player.getUUID(),
                player.position().add(0.0D, 1.0D, 0.0D)
        );

        ACTIVE.put(player.getUUID(), soul);
        WAITING_RESPAWN.put(player.getUUID(), true);

        AABB area = new AABB(
                player.getX() - RADIUS,
                player.getY() - RADIUS,
                player.getZ() - RADIUS,
                player.getX() + RADIUS,
                player.getY() + RADIUS,
                player.getZ() + RADIUS
        );

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && !(entity instanceof ArmorStand)
                        && entity.distanceToSqr(player)
                        <= RADIUS * RADIUS
        );

        for (LivingEntity entity : nearby) {
            soul.targets.add(entity.getUUID());
        }

        level.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                100,
                1.5D,
                1.5D,
                1.5D,
                0.2D
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                150,
                2.0D,
                2.0D,
                2.0D,
                0.5D
        );
    }

    /**
     * Kéo các mục tiêu về vị trí cơ thể đã chết.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        Iterator<Map.Entry<UUID, SoulData>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {

            SoulData soul = iterator.next().getValue();

            soul.ticks--;

            List<UUID> validTargets = new ArrayList<>();

            for (UUID id : soul.targets) {

                Entity entity = soul.level.getEntity(id);

                if (!(entity instanceof LivingEntity target)
                        || !target.isAlive()
                        || target.isRemoved()) {
                    continue;
                }

                validTargets.add(id);

                Vec3 center = target.getBoundingBox().getCenter();
                Vec3 direction = soul.center.subtract(center);
                double distance = direction.length();

                if (distance > 0.1D) {
                    Vec3 pull = direction.normalize()
                            .scale(Math.min(
                                    0.9D,
                                    0.15D + distance * 0.07D
                            ));

                    target.setDeltaMovement(pull);
                    target.hurtMarked = true;
                }

                soul.level.sendParticles(
                        ParticleTypes.SOUL,
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

            soul.targets.clear();
            soul.targets.addAll(validTargets);

            soul.level.sendParticles(
                    ParticleTypes.PORTAL,
                    soul.center.x,
                    soul.center.y,
                    soul.center.z,
                    15,
                    0.5D,
                    0.5D,
                    0.5D,
                    0.2D
            );

            if (soul.ticks <= 0) {

                explodeAndChoose(soul);

                iterator.remove();
            }
        }
    }

    /**
     * Khi hố đen kết thúc:
     * chọn ngẫu nhiên một mục tiêu để nhận năng lực.
     */
    private static void explodeAndChoose(SoulData soul) {

        soul.level.sendParticles(
                ParticleTypes.EXPLOSION,
                soul.center.x,
                soul.center.y,
                soul.center.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        soul.level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                soul.center.x,
                soul.center.y,
                soul.center.z,
                100,
                2.0D,
                2.0D,
                2.0D,
                0.15D
        );

        if (soul.targets.isEmpty()) {
            return;
        }

        int selected = RANDOM.nextInt(soul.targets.size());
        UUID selectedId = soul.targets.get(selected);

        Entity chosen = soul.level.getEntity(selectedId);

        Entity ownerEntity = soul.level.getEntity(soul.ownerId);

        FogDomainAbilityData data =
                FogDomainAbilityData.get(soul.level.getServer());

        if (chosen instanceof ServerPlayer absorbedPlayer) {

            data.initializePower(soul.ownerId, true);
            data.initializePower(absorbedPlayer.getUUID(), false);

            data.transferPlayerPowers(
                    soul.ownerId,
                    absorbedPlayer.getUUID()
            );

            absorbedPlayer.hurtServer(
                    soul.level,
                    soul.level.damageSources().generic(),
                    100000.0F
            );

        } else if (chosen instanceof Mob mob && mob.isAlive()) {

            String typeId = net.minecraft.core.registries.BuiltInRegistries
                    .ENTITY_TYPE.getKey(mob.getType()).toString();

            data.initializePower(soul.ownerId, true);
            data.initializePower(mob.getUUID(), false);

            data.transferCreaturePowers(
                    soul.ownerId,
                    mob.getUUID()
            );

            data.addAbility(soul.ownerId, typeId);

            mob.discard();
        }

        // Các mục tiêu còn lại được hồi máu.
        for (UUID id : soul.targets) {

            if (id.equals(selectedId)) {
                continue;
            }

            Entity entity = soul.level.getEntity(id);

            if (entity instanceof LivingEntity living
                    && living.isAlive()) {
                living.setHealth(living.getMaxHealth());
            }
        }

        if (ownerEntity instanceof ServerPlayer owner) {
            owner.sendSystemMessage(Component.literal(
                    "Linh hồn đã chọn một nguồn năng lực ngẫu nhiên."));
        }
    }

    /**
     * Khi tubi2531 hồi sinh, kích hoạt trạng thái suy yếu 60 giây.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isController(player)) {
            return;
        }

        if (!WAITING_RESPAWN.containsKey(player.getUUID())) {
            return;
        }

        WAITING_RESPAWN.remove(player.getUUID());

        player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                WEAKNESS_TICKS,
                1,
                false,
                true,
                true
        ));

        player.setHealth(player.getMaxHealth());

        player.sendSystemMessage(Component.literal(
                "TÁCH HỒN: Bạn đã hồi sinh và nhận được trạng thái suy yếu trong 60 giây."));
    }
}
