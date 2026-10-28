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
import net.minecraft.world.entity.Mob;
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
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainSkills {

    private static final double RANGE = 25.0D;
    private static final double RADIUS = 30.0D;
    private static final int MAX_TAMED = 5;
    private static final int CAGE_TICKS = 200;

    static final Map<UUID, List<UUID>> TAMED = new HashMap<>();

    private static final Map<UUID, CageData> CAGES = new HashMap<>();
    private static final Map<UUID, BeamData> BEAMS = new HashMap<>();
    private static final Map<UUID, UUID> LAST_TARGET = new HashMap<>();
    private static final Map<UUID, Integer> SLTE_INDEX = new HashMap<>();

    private static int selectedSkill = 0;

    private static final String[] SKILLS = {
            "Tia năng lượng",
            "Lồng nhốt",
            "Thu phục sinh vật"
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

    private static final class BeamData {
        final ServerLevel level;
        final UUID playerId;
        final UUID targetId;
        final List<UUID> standIds;
        int ticks;

        BeamData(ServerLevel level, UUID playerId,
                 UUID targetId, List<UUID> standIds) {
            this.level = level;
            this.playerId = playerId;
            this.targetId = targetId;
            this.standIds = standIds;
            this.ticks = 12;
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

    private static void giveArmor(
            ArmorStand stand, boolean netherite) {

        if (netherite) {
            stand.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(Items.NETHERITE_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(Items.NETHERITE_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS,
                    new ItemStack(Items.NETHERITE_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET,
                    new ItemStack(Items.NETHERITE_BOOTS));
        } else {
            stand.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(Items.DIAMOND_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(Items.DIAMOND_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS,
                    new ItemStack(Items.DIAMOND_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET,
                    new ItemStack(Items.DIAMOND_BOOTS));
        }

        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setShowArms(true);
        stand.setCustomNameVisible(false);
    }

    private static ArmorStand spawnStand(
            ServerLevel level, double x, double y, double z,
            boolean netherite) {

        ArmorStand stand = new ArmorStand(level, x, y, z);
        giveArmor(stand, netherite);
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
                    level, pos[0], pos[1], pos[2], true);
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

    private static void startBeam(
            ServerPlayer player, LivingEntity target) {

        ServerLevel level = (ServerLevel) player.level();
        List<UUID> standIds = new ArrayList<>();

        Vec3 right = Vec3.directionFromRotation(
                0.0F, player.getYRot() + 90.0F).normalize();

        Vec3 leftPos = player.position().add(right.scale(-1.25D));
        Vec3 rightPos = player.position().add(right.scale(1.25D));

        ArmorStand left = spawnStand(
                level, leftPos.x, leftPos.y, leftPos.z, false);
        ArmorStand rightStand = spawnStand(
                level, rightPos.x, rightPos.y, rightPos.z, false);

        Vec3 back = player.getLookAngle().normalize().scale(-0.8D);
        Vec3 backPos = player.position().add(back);

        ArmorStand netherite = spawnStand(
                level, backPos.x, backPos.y, backPos.z, true);

        if (left != null) standIds.add(left.getUUID());
        if (rightStand != null) standIds.add(rightStand.getUUID());
        if (netherite != null) standIds.add(netherite.getUUID());

        BEAMS.put(player.getUUID(),
                new BeamData(level, player.getUUID(),
                        target.getUUID(), standIds));

        player.sendSystemMessage(Component.literal(
                "Đang hợp nhất giá giáp để tích tụ năng lượng."));
    }

    private static void fireBeam(
            ServerPlayer player, LivingEntity target) {

        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 delta = end.subtract(start);
        double distance = delta.length();

        if (distance > 0.0D) {
            Vec3 direction = delta.normalize();

            for (double d = 0.0D; d < distance; d += 0.3D) {
                Vec3 point = start.add(direction.scale(d));

                level.sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);

                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z,
                        1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }

        target.hurtServer(
                level, player.damageSources().magic(), 10.0F);

        player.sendSystemMessage(Component.literal(
                "Đã bắn tia năng lượng."));
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

        LAST_TARGET.put(player.getUUID(), target.getUUID());

        switch (selectedSkill) {
            case 0 -> startBeam(player, target);
            case 1 -> createCage(player, target);

            case 2 -> {
                if (!(target instanceof Mob mob)) {
                    player.sendSystemMessage(Component.literal(
                            "Kỹ năng nô dịch chỉ nhận sinh vật, không nhận người chơi."));
                    return 0;
                }

                List<UUID> owned = TAMED.computeIfAbsent(
                        player.getUUID(), k -> new ArrayList<>());

                ServerLevel level = (ServerLevel) player.level();

                owned.removeIf(id -> {
                    Entity entity = level.getEntity(id);
                    return !(entity instanceof Mob m) || !m.isAlive();
                });

                if (owned.contains(mob.getUUID())) {
                    player.sendSystemMessage(Component.literal(
                            mob.getName().getString()
                                    + " đã thuộc quyền nô dịch của bạn."));
                    return 1;
                }

                if (owned.size() >= MAX_TAMED) {
                    player.sendSystemMessage(Component.literal(
                            "Đã đủ 5 sinh vật. Không thể nô dịch thêm."));
                    return 0;
                }

                owned.add(mob.getUUID());
                SLTE_INDEX.put(player.getUUID(), owned.size() - 1);

                player.sendSystemMessage(Component.literal(
                        "Đã nô dịch " + mob.getName().getString()
                                + ". Tổng số: " + owned.size()
                                + "/" + MAX_TAMED));
            }

            default -> {
                return 0;
            }
        }

        return 1;
    }

    private static int selectTamed(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        List<UUID> owned = TAMED.get(player.getUUID());

        if (owned == null || owned.isEmpty()) {
            SLTE_INDEX.remove(player.getUUID());
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa có sinh vật nô dịch nào."));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        owned.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return !(entity instanceof Mob mob)
                    || !mob.isAlive() || mob.isRemoved();
        });

        if (owned.isEmpty()) {
            SLTE_INDEX.remove(player.getUUID());
            player.sendSystemMessage(Component.literal(
                    "Không còn sinh vật nô dịch nào đang hoạt động."));
            return 0;
        }

        int previous = SLTE_INDEX.getOrDefault(
                player.getUUID(), -1);
        int selected = (previous + 1) % owned.size();

        SLTE_INDEX.put(player.getUUID(), selected);

        Entity entity = level.getEntity(owned.get(selected));

        if (!(entity instanceof Mob mob)) {
            player.sendSystemMessage(Component.literal(
                    "Không tìm thấy sinh vật được chọn."));
            return 0;
        }

        player.sendSystemMessage(Component.literal(
                "========== [ NÔ DỊCH "
                        + (selected + 1) + " ] =========="));

        player.sendSystemMessage(Component.literal(
                "Sinh vật: " + mob.getName().getString()));

        player.sendSystemMessage(Component.literal(
                "Đã chọn " + (selected + 1) + "/" + owned.size()
                        + ". Dùng /fogskill swap để đổi vị trí."));

        return 1;
    }

    private static int swap(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        List<UUID> owned = TAMED.get(player.getUUID());

        if (owned == null || owned.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa có sinh vật nô dịch nào."));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        owned.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return !(entity instanceof Mob mob)
                    || !mob.isAlive() || mob.isRemoved();
        });

        if (owned.isEmpty()) {
            SLTE_INDEX.remove(player.getUUID());
            player.sendSystemMessage(Component.literal(
                    "Không còn sinh vật nô dịch nào đang hoạt động."));
            return 0;
        }

        int selected = SLTE_INDEX.getOrDefault(
                player.getUUID(), -1);

        if (selected < 0) {
            player.sendSystemMessage(Component.literal(
                    "Hãy dùng /fogskill slte để chọn sinh vật trước."));
            return 0;
        }

        selected = Math.floorMod(selected, owned.size());
        SLTE_INDEX.put(player.getUUID(), selected);

        Entity entity = level.getEntity(owned.get(selected));

        if (!(entity instanceof Mob mob) || !mob.isAlive()) {
            player.sendSystemMessage(Component.literal(
                    "Sinh vật được chọn không còn tồn tại."));
            return 0;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        double mobX = mob.getX();
        double mobY = mob.getY();
        double mobZ = mob.getZ();
        float mobYaw = mob.getYRot();
        float mobPitch = mob.getXRot();

        // Dùng đúng overload có Set<Relative> của NeoForge 26.2.
        player.teleportTo(
                level,
                mobX, mobY, mobZ,
                Set.of(),
                mobYaw, mobPitch,
                true
        );

        mob.teleportTo(playerX, playerY, playerZ);
        mob.setYRot(playerYaw);
        mob.setXRot(playerPitch);
        mob.setYBodyRot(playerYaw);
        mob.setYHeadRot(playerYaw);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.hurtMarked = true;

        player.sendSystemMessage(Component.literal(
                "Đã đổi vị trí với Nô dịch "
                        + (selected + 1)
                        + ": " + mob.getName().getString()));

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
                                                            + RANGE
                                                            + " | Nô dịch tối đa: "
                                                            + MAX_TAMED));
                                    return 1;
                                }))

                        .then(Commands.literal("cast")
                                .executes(context ->
                                        cast(context.getSource()
                                                .getPlayerOrException())))

                        .then(Commands.literal("slte")
                                .executes(context ->
                                        selectTamed(context.getSource()
                                                .getPlayerOrException())))

                        .then(Commands.literal("swap")
                                .executes(context ->
                                        swap(context.getSource()
                                                .getPlayerOrException())))
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

        Iterator<Map.Entry<UUID, BeamData>> beamIterator =
                BEAMS.entrySet().iterator();

        while (beamIterator.hasNext()) {
            Map.Entry<UUID, BeamData> entry =
                    beamIterator.next();
            BeamData beam = entry.getValue();

            Entity playerEntity =
                    beam.level.getEntity(beam.playerId);
            Entity targetEntity =
                    beam.level.getEntity(beam.targetId);

            if (!(playerEntity instanceof ServerPlayer player)
                    || !(targetEntity instanceof LivingEntity target)
                    || !player.isAlive()
                    || !target.isAlive()) {
                removeStands(beam.level, beam.standIds);
                beamIterator.remove();
                continue;
            }

            beam.ticks--;

            if (beam.ticks <= 0) {
                removeStands(beam.level, beam.standIds);
                fireBeam(player, target);
                beamIterator.remove();
            }
        }
    }
    }
