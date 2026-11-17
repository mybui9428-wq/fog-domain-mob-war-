
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainSkills {

    private static final double RANGE = 25.0D;
    private static final double RADIUS = 30.0D;
    private static final int CAGE_TICKS = 200;

    private static final Map<UUID, CageData> CAGES = new HashMap<>();

    private static int selectedSkill = 0;

    private static final String[] SKILLS = {
            "Lồng nhốt"
    };

    private static final class CageData {
        final ServerLevel level;
        final UUID targetId;
        final List<UUID> standIds;
        int ticks;
        int damageTicks;

        CageData(ServerLevel level, UUID targetId,
                 List<UUID> standIds) {
            this.level = level;
            this.targetId = targetId;
            this.standIds = standIds;
            this.ticks = CAGE_TICKS;
            this.damageTicks = 0;
        }
    }

    private FogDomainSkills() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static boolean inDomain(
            ServerPlayer player, Entity entity) {
        return player.level() == entity.level()
                && player.distanceToSqr(entity)
                <= RADIUS * RADIUS;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchBox = player.getBoundingBox().inflate(RANGE);

        List<LivingEntity> candidates =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        entity -> entity != player
                                && entity.isAlive()
                                && !entity.isRemoved()
                                && inDomain(player, entity)
                                && !(entity instanceof ServerPlayer p
                                && isController(p))
                );

        LivingEntity best = null;
        double bestProjection = Double.MAX_VALUE;

        for (LivingEntity entity : candidates) {
            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 delta = center.subtract(eye);
            double projection = delta.dot(look);

            if (projection < 0.0D || projection > RANGE) {
                continue;
            }

            Vec3 closestPoint = eye.add(look.scale(projection));
            double tolerance = Math.max(
                    0.8D,
                    Math.min(1.5D, entity.getBbWidth() * 0.75D)
            );

            if (center.distanceTo(closestPoint) > tolerance) {
                continue;
            }

            if (projection < bestProjection) {
                bestProjection = projection;
                best = entity;
            }
        }

        return best;
    }

    private static void giveArmor(ArmorStand stand) {
        stand.setItemSlot(EquipmentSlot.HEAD,
                new ItemStack(Items.NETHERITE_HELMET));
        stand.setItemSlot(EquipmentSlot.CHEST,
                new ItemStack(Items.NETHERITE_CHESTPLATE));
        stand.setItemSlot(EquipmentSlot.LEGS,
                new ItemStack(Items.NETHERITE_LEGGINGS));
        stand.setItemSlot(EquipmentSlot.FEET,
                new ItemStack(Items.NETHERITE_BOOTS));

        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setShowArms(true);
        stand.setCustomNameVisible(false);
    }

    private static ArmorStand spawnStand(
            ServerLevel level, double x, double y, double z) {

        ArmorStand stand = new ArmorStand(level, x, y, z);
        giveArmor(stand);
        return level.addFreshEntity(stand) ? stand : null;
    }

    private static void removeStands(
            ServerLevel level, List<UUID> standIds) {

        for (UUID id : standIds) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }

        standIds.clear();
    }

    private static void createCage(
            ServerPlayer player, LivingEntity target) {

        ServerLevel level = (ServerLevel) player.level();
        UUID targetId = target.getUUID();

        CageData old = CAGES.remove(targetId);
        if (old != null) {
            removeStands(old.level, old.standIds);
        }

        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        double[][] positions = {
                {x + 1.8D, y, z},
                {x - 1.8D, y, z},
                {x, y, z + 1.8D},
                {x, y, z - 1.8D}
        };

        List<UUID> standIds = new ArrayList<>();

        for (double[] pos : positions) {
            ArmorStand stand = spawnStand(
                    level, pos[0], pos[1], pos[2]);

            if (stand != null) {
                standIds.add(stand.getUUID());
            }
        }

        CAGES.put(targetId,
                new CageData(level, targetId, standIds));

        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;

        target.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS,
                CAGE_TICKS, 10, false, true, true));

        player.sendSystemMessage(Component.literal(
                "Đã tạo lồng Netherite trong 10 giây."));
    }

    private static int cast(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        LivingEntity target = findTarget(player);

        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "Hãy ngắm chính xác vào mục tiêu trong phạm vi 25 block."));
            return 0;
        }

        createCage(player, target);
        return 1;
    }

    private static int summonTripleSpirit(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        IceSpiritEntity iceSpirit = FogDomainEntities.ICE_SPIRIT
                .get()
                .create(
                        level,
                        net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED
                );

        IceZombieEntity iceZombie = FogDomainEntities.ICE_ZOMBIE
                .get()
                .create(
                        level,
                        net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED
                );

        MultiArmedSkeletonEntity skeleton =
                FogDomainEntities.MULTI_ARMED_SKELETON
                        .get()
                        .create(
                                level,
                                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED
                        );

        if (iceSpirit == null || iceZombie == null || skeleton == null) {
            if (iceSpirit != null) iceSpirit.discard();
            if (iceZombie != null) iceZombie.discard();
            if (skeleton != null) skeleton.discard();

            player.sendSystemMessage(Component.literal(
                    "Triệu hồi thất bại: không tạo được đầy đủ ba linh thể."));
            return 0;
        }

        iceSpirit.snapTo(
                player.getX() + 2.0D,
                player.getY() + 1.0D,
                player.getZ(),
                player.getYRot(),
                0.0F
        );

        iceZombie.snapTo(
                player.getX() - 2.0D,
                player.getY(),
                player.getZ(),
                player.getYRot(),
                0.0F
        );

        skeleton.snapTo(
                player.getX(),
                player.getY() + 1.0D,
                player.getZ() - 2.0D,
                player.getYRot(),
                0.0F
        );

        iceSpirit.setPersistenceRequired();
        iceZombie.setPersistenceRequired();
        skeleton.setPersistenceRequired();

        FogDomainMinionAI.bindOwner(iceSpirit, player);
        FogDomainMinionAI.bindOwner(iceZombie, player);
        FogDomainMinionAI.bindOwner(skeleton, player);

        boolean spiritAdded = level.addFreshEntity(iceSpirit);
        boolean zombieAdded = level.addFreshEntity(iceZombie);
        boolean skeletonAdded = level.addFreshEntity(skeleton);

        if (!spiritAdded || !zombieAdded || !skeletonAdded) {
            if (spiritAdded) iceSpirit.discard();
            if (zombieAdded) iceZombie.discard();
            if (skeletonAdded) skeleton.discard();

            FogDomainMinionAI.removeMinion(iceSpirit);
            FogDomainMinionAI.removeMinion(iceZombie);
            FogDomainMinionAI.removeMinion(skeleton);

            player.sendSystemMessage(Component.literal(
                    "Triệu hồi không hoàn tất. Hãy thử lại."));
            return 0;
        }

        player.sendSystemMessage(Component.literal(
                "Tam Linh Hộ Thể đã được triệu hồi!"));

        player.sendSystemMessage(Component.literal(
                "Linh Hồn Băng, Thây Ma Băng và Bộ Xương Nhiều Tay "
                        + "đã nhận bạn làm chủ nhân."));

        return 1;
    }

    private static int showAbilities(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(
                        ((ServerLevel) player.level()).getServer());

        List<String> abilities =
                data.getAbilities(player.getUUID());

        player.sendSystemMessage(Component.literal(
                "===== NĂNG LỰC ĐÃ HẤP THỤ ====="));

        if (abilities.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa hấp thụ năng lực nào."));
        } else {
            for (String ability : abilities) {
                player.sendSystemMessage(
                        Component.literal("- " + ability));
            }
        }

        player.sendSystemMessage(Component.literal(
                "Tổng năng lực: " + abilities.size()));

        return 1;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("fogskill")

                        .then(Commands.literal("cycle")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "Bạn không phải người điều khiển lĩnh vực."));
                                        return 0;
                                    }

                                    selectedSkill =
                                            (selectedSkill + 1)
                                                    % SKILLS.length;

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "Kỹ năng: "
                                                            + SKILLS[selectedSkill]));
                                    return 1;
                                }))

                        .then(Commands.literal("status")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "Kỹ năng đang chọn: "
                                                            + SKILLS[selectedSkill]
                                                            + " | Phạm vi: "
                                                            + RANGE));
                                    return 1;
                                }))

                        .then(Commands.literal("cast")
                                .executes(context ->
                                        cast(context.getSource()
                                                .getPlayerOrException())))

                        .then(Commands.literal("triple")
                                .executes(context ->
                                        summonTripleSpirit(
                                                context.getSource()
                                                        .getPlayerOrException())))

                        .then(Commands.literal("abilities")
                                .executes(context ->
                                        showAbilities(context.getSource()
                                                .getPlayerOrException()))
                        )
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, CageData>> cageIterator =
                CAGES.entrySet().iterator();

        while (cageIterator.hasNext()) {
            Map.Entry<UUID, CageData> entry =
                    cageIterator.next();

            CageData cage = entry.getValue();

            Entity entity = cage.level.getEntity(cage.targetId);

            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || target.isRemoved()
                    || cage.ticks <= 0) {

                removeStands(cage.level, cage.standIds);
                cageIterator.remove();
                continue;
            }

            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;

            cage.ticks--;
            cage.damageTicks++;

            if (cage.damageTicks >= 20) {
                cage.damageTicks = 0;

                if (target.getHealth() > 1.0F) {
                    float damage = Math.min(
                            1.0F, target.getHealth() - 1.0F);

                    if (damage > 0.0F) {
                        target.hurtServer(
                                cage.level,
                                cage.level.damageSources().magic(),
                                damage);
                    }
                }

                cage.level.sendParticles(
                        ParticleTypes.SOUL,
                        target.getX(),
                        target.getY()
                                + target.getBbHeight() * 0.5D,
                        target.getZ(),
                        4, 0.25D, 0.25D, 0.25D, 0.01D);
            }

            if (cage.ticks <= 0) {
                removeStands(cage.level, cage.standIds);
                cageIterator.remove();
            }
        }
    }

    public static String getSelectedSkillName() {
        return SKILLS[selectedSkill];
    }

    // Giữ phương thức tương thích với HUD cũ nếu còn nơi gọi.
    public static int getTamedCount(UUID playerId) {
        return 0;
    }
}
