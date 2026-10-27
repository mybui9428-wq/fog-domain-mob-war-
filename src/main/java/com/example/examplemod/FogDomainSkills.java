
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

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
    private static final int MAX_TAMED = 5;
    private static final int CAGE_TICKS = 200;

    private static final Map<UUID, List<UUID>> TAMED =
            new HashMap<>();

    private static final Map<UUID, Integer> CAGED =
            new HashMap<>();

    private static final Map<UUID, List<UUID>> CAGE_STANDS =
            new HashMap<>();

    private static final Map<UUID, UUID> LAST_TARGET =
            new HashMap<>();

    private static int selectedSkill = 0;

    private static final String[] SKILLS = {
            "Tia năng lượng",
            "Lồng nhốt",
            "Thu phục sinh vật"
    };

    private FogDomainSkills() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getGameProfile().name()
                .equalsIgnoreCase("tubi2531");
    }

    private static boolean inDomain(
            ServerPlayer player, Entity entity) {
        return player.level() == entity.level()
                && player.distanceToSqr(entity) <= RADIUS * RADIUS;
    }

    /*
     * Chọn sinh vật nằm phía trước tâm ngắm.
     * Không tự chuyển sang chọn sinh vật gần nhất nếu
     * người chơi đang ngắm lệch mục tiêu.
     */
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
            ServerLevel level,
            double x, double y, double z,
            boolean netherite) {

        ArmorStand stand = new ArmorStand(level, x, y, z);
        giveArmor(stand, netherite);

        if (level.addFreshEntity(stand)) {
            return stand;
        }

        return null;
    }

    private static void removeCageStands(
            ServerLevel level, UUID targetId) {

        List<UUID> ids = CAGE_STANDS.remove(targetId);

        if (ids == null) {
            return;
        }

        for (UUID id : ids) {
            Entity entity = level.getEntity(id);

            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void createCage(
            ServerPlayer player, LivingEntity target) {

        ServerLevel level = (ServerLevel) player.level();
        UUID targetId = target.getUUID();

        removeCageStands(level, targetId);

        List<UUID> stands = new ArrayList<>();

        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        double[][] positions = {
                {x + 1.2D, y, z},
                {x - 1.2D, y, z},
                {x, y, z + 1.2D},
                {x, y, z - 1.2D}
        };

        for (double[] pos : positions) {
            ArmorStand stand = spawnStand(
                    level, pos[0], pos[1], pos[2], true);

            if (stand != null) {
                stands.add(stand.getUUID());
            }
        }

        CAGE_STANDS.put(targetId, stands);
        CAGED.put(targetId, CAGE_TICKS);

        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;

        player.sendSystemMessage(Component.literal(
                "Đã tạo lồng nhốt trong 10 giây."));
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

            for (double d = 0.0D; d < distance; d += 0.35D) {
                Vec3 point = start.add(direction.scale(d));

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D
                );

                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z,
                        1, 0.03D, 0.03D, 0.03D, 0.0D
                );
            }
        }

        target.hurtServer(
                level,
                player.damageSources().magic(),
                10.0F
        );

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
                    "Hãy ngắm chính xác vào sinh vật trong phạm vi 25 block."));
            return 0;
        }

        LAST_TARGET.put(player.getUUID(), target.getUUID());

        switch (selectedSkill) {
            case 0 -> fireBeam(player, target);

            case 1 -> createCage(player, target);

            case 2 -> {
                if (!(target instanceof Mob mob)) {
                    player.sendSystemMessage(Component.literal(
                            "Chỉ có thể thu phục sinh vật, không thể thu phục người chơi."));
                    return 0;
                }

                List<UUID> owned = TAMED.computeIfAbsent(
                        player.getUUID(), k -> new ArrayList<>());

                ServerLevel level = (ServerLevel) player.level();

                owned.removeIf(id -> {
                    Entity entity = level.getEntity(id);
                    return !(entity instanceof Mob living)
                            || !living.isAlive();
                });

                if (!owned.contains(mob.getUUID())
                        && owned.size() >= MAX_TAMED) {
                    player.sendSystemMessage(Component.literal(
                            "Bạn đã thu phục đủ 5 sinh vật. Hãy chọn sinh vật đã thu phục để thay thế."));
                    return 0;
                }

                if (!owned.contains(mob.getUUID())) {
                    owned.add(mob.getUUID());
                }

                player.sendSystemMessage(Component.literal(
                        "Đã thu phục " + mob.getName().getString()
                                + ". Tổng số: " + owned.size()
                                + "/" + MAX_TAMED
                ));
            }

            default -> {
                return 0;
            }
        }

        return 1;
    }

    private static int swap(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        LivingEntity target = findTarget(player);

        if (!(target instanceof Mob mob)) {
            player.sendSystemMessage(Component.literal(
                    "Hãy ngắm vào sinh vật bạn muốn thu phục."));
            return 0;
        }

        List<UUID> owned = TAMED.computeIfAbsent(
                player.getUUID(), k -> new ArrayList<>());

        ServerLevel level = (ServerLevel) player.level();

        owned.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return !(entity instanceof Mob living)
                    || !living.isAlive();
        });

        if (owned.contains(mob.getUUID())) {
            owned.remove(mob.getUUID());
            owned.add(0, mob.getUUID());

            player.sendSystemMessage(Component.literal(
                    "Đã chọn " + mob.getName().getString()
                            + " làm sinh vật thu phục ưu tiên."));
            return 1;
        }

        if (owned.size() >= MAX_TAMED) {
            UUID oldId = owned.remove(0);
            owned.add(mob.getUUID());

            player.sendSystemMessage(Component.literal(
                    "Đã thay sinh vật thu phục cũ bằng "
                            + mob.getName().getString() + "."));
        } else {
            owned.add(mob.getUUID());

            player.sendSystemMessage(Component.literal(
                    "Đã thêm " + mob.getName().getString()
                            + " vào danh sách thu phục."));
        }

        LAST_TARGET.put(player.getUUID(), mob.getUUID());
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
                                                            + " | Phạm vi ngắm: "
                                                            + RANGE
                                                            + " | Thu phục tối đa: "
                                                            + MAX_TAMED));
                                    return 1;
                                }))
                        .then(Commands.literal("cast")
                                .executes(context -> cast(
                                        context.getSource()
                                                .getPlayerOrException())))
                        .then(Commands.literal("swap")
                                .executes(context -> swap(
                                        context.getSource()
                                                .getPlayerOrException())))
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Iterator<Map.Entry<UUID, Integer>> iterator =
                    CAGED.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                Entity entity = level.getEntity(entry.getKey());

                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }

                if (!living.isAlive()) {
                    removeCageStands(level, entry.getKey());
                    iterator.remove();
                    continue;
                }

                int remaining = entry.getValue() - 1;

                living.setDeltaMovement(Vec3.ZERO);
                living.hurtMarked = true;

                if (remaining <= 0) {
                    removeCageStands(level, entry.getKey());
                    iterator.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
        }
    }
}
