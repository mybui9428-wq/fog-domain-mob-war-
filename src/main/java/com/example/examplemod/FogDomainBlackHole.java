
package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
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
    private static final double MASS_RADIUS = 10.0D;

    private static final Map<UUID, HoleData> ACTIVE =
            new HashMap<>();

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
                List<UUID> targets) {

            this.level = level;
            this.playerId = playerId;
            this.center = center;
            this.targets = targets;
            this.ticks = DURATION;
        }
    }

    private FogDomainBlackHole() {
    }

    public static boolean startSingle(
            ServerPlayer player,
            Mob target) {

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

    public static boolean startMass(ServerPlayer player) {

        if (player == null || !player.isAlive()) {
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();

        AABB area = player.getBoundingBox().inflate(MASS_RADIUS);

        List<Mob> nearby = level.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> mob.isAlive()
                        && !mob.isRemoved()
                        && mob.distanceToSqr(player)
                        <= MASS_RADIUS * MASS_RADIUS
        );

        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Không có sinh vật nào trong bán kính 10 block."));
            return false;
        }

        List<UUID> targets = new ArrayList<>();

        for (Mob mob : nearby) {
            targets.add(mob.getUUID());
        }

        startHole(player, level, targets);

        player.sendSystemMessage(Component.literal(
                "Hố đen đã xuất hiện! Đang hút "
                        + targets.size() + " sinh vật..."));

        return true;
    }

    private static void startHole(
            ServerPlayer player,
            ServerLevel level,
            List<UUID> targets) {

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

    private static void absorb(
            HoleData hole,
            Mob mob) {

        String typeId = BuiltInRegistries.ENTITY_TYPE
                .getKey(mob.getType())
                .toString();

        FogDomainAbilityData data =
                FogDomainAbilityData.get(hole.level.getServer());

        boolean added = data.addAbility(
                hole.playerId,
                typeId
        );

        hole.level.sendParticles(
                ParticleTypes.SOUL,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                25,
                0.4D,
                0.6D,
                0.4D,
                0.15D
        );

        mob.discard();

        Entity owner = hole.level.getEntity(hole.playerId);

        if (owner instanceof ServerPlayer player) {
            if (added) {
                player.sendSystemMessage(Component.literal(
                        "Đã hấp thụ năng lực: " + typeId
                                + " | Tổng năng lực: "
                                + data.getAbilityCount(hole.playerId)));
            } else {
                player.sendSystemMessage(Component.literal(
                        "Đã hấp thụ sinh vật "
                                + typeId
                                + ", nhưng năng lực này đã được lưu trước đó."));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        Iterator<Map.Entry<UUID, HoleData>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HoleData> entry =
                    iterator.next();

            HoleData hole = entry.getValue();

            Entity owner = hole.level.getEntity(hole.playerId);

            if (!(owner instanceof ServerPlayer player)
                    || !player.isAlive()
                    || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            hole.ticks--;

            Iterator<UUID> targetIterator =
                    hole.targets.iterator();

            while (targetIterator.hasNext()) {
                UUID targetId = targetIterator.next();

                Entity entity = hole.level.getEntity(targetId);

                if (!(entity instanceof Mob mob)
                        || !mob.isAlive()
                        || mob.isRemoved()) {
                    targetIterator.remove();
                    continue;
                }

                Vec3 targetPoint =
                        mob.getBoundingBox().getCenter();

                Vec3 direction =
                        hole.center.subtract(targetPoint);

                double distance = direction.length();

                if (distance > 0.0D) {
                    Vec3 pull = direction.normalize()
                            .scale(Math.min(0.8D,
                                    0.12D + distance * 0.08D));

                    mob.setDeltaMovement(pull);
                    mob.hurtMarked = true;
                }

                hole.level.sendParticles(
                        ParticleTypes.PORTAL,
                        mob.getX(),
                        mob.getY() + mob.getBbHeight() * 0.5D,
                        mob.getZ(),
                        3,
                        0.2D,
                        0.3D,
                        0.2D,
                        0.05D
                );

                if (hole.ticks <= 0 || distance < 1.2D) {
                    absorb(hole, mob);
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
                        "Hố đen đã đóng. Đã lưu năng lực đã hấp thụ."));
            }
        }
    }
}
