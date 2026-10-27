
package com.example.examplemod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

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

    private static final Map<UUID, UUID> CAGE_OWNER =
            new HashMap<>();

    private static final Map<UUID, UUID> LAST_TARGET =
            new HashMap<>();

    private static final Map<UUID, Integer> CAGE_TIME =
            new HashMap<>();

    private static final Map<UUID, UUID> UNUSED =
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
                && player.distanceToSqr(entity)
                <= RADIUS * RADIUS;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        HitResult hit = player.pick(RANGE, 0.0F, false);
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();

            if (entity instanceof LivingEntity living
                    && living != player
                    && living.isAlive()
                    && inDomain(player, living)) {
                if (living instanceof ServerPlayer targetPlayer
                        && isController(targetPlayer)) {
                    return null;
                }
                return living;
            }
        }

        return level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(RANGE),
                entity -> entity != player
                        && entity.isAlive()
                        && inDomain(player, entity)
                        && !(entity instanceof ServerPlayer p
                        && isController(p))
        ).stream()
                .min(Comparator.comparingDouble(
                        player::distanceToSqr))
                .orElse(null);
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

    private static int cast(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal(
                    "Bạn không phải người điều khiển lĩnh vực."));
            return 0;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "Không tìm thấy mục tiêu trong phạm vi."));
            return 0;
        }

        switch (selectedSkill) {
            case 0 -> {
                target.hurtServer(
                        player.serverLevel(),
                        player.damageSources().magic(),
                        10.0F);
                player.sendSystemMessage(Component.literal(
                        "Đã bắn tia năng lượng."));
            }
            case 1 -> {
                CAGED.put(target.getUUID(), CAGE_TICKS);
                CAGE_OWNER.put(target.getUUID(), player.getUUID());
                LAST_TARGET.put(player.getUUID(), target.getUUID());

                target.setDeltaMovement(0, 0, 0);
                target.hurtMarked = true;

                player.sendSystemMessage(Component.literal(
                        "Đã nhốt mục tiêu trong 10 giây."));
            }
            case 2 -> {
                if (!(target instanceof Mob mob)
                        || target instanceof Warden) {
                    player.sendSystemMessage(Component.literal(
                            "Chỉ có thể thu phục sinh vật phù hợp; không thể thu phục Warden."));
                    return 0;
                }

                List<UUID> list = TAMED.computeIfAbsent(
                        player.getUUID(), k -> new ArrayList<>());

                if (list.size() >= MAX_TAMED) {
                    player.sendSystemMessage(Component.literal(
                            "Bạn đã thu phục đủ 5 sinh vật."));
                    return 0;
                }

                if (list.contains(mob.getUUID())) {
                    player.sendSystemMessage(Component.literal(
                            "Sinh vật này đã được thu phục."));
                    return 0;
                }

                list.add(mob.getUUID());
                LAST_TARGET.put(player.getUUID(), mob.getUUID());

                player.sendSystemMessage(Component.literal(
                        "Đã thu phục sinh vật. Tổng số: "
                                + list.size() + "/" + MAX_TAMED));
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

        UUID targetId = LAST_TARGET.get(player.getUUID());
        if (targetId == null) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa chọn sinh vật để hoán đổi."));
            return 0;
        }

        List<UUID> owned = TAMED.getOrDefault(
                player.getUUID(), Collections.emptyList());

        if (!owned.contains(targetId)) {
            player.sendSystemMessage(Component.literal(
                    "Mục tiêu gần nhất không phải sinh vật đã thu phục."));
            return 0;
        }

        Entity entity = player.serverLevel().getEntity(targetId);
        if (!(entity instanceof Mob mob)
                || !mob.isAlive()
                || !inDomain(player, mob)) {
            player.sendSystemMessage(Component.literal(
                    "Sinh vật đã thu phục không ở gần hoặc không còn tồn tại."));
            return 0;
        }

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double mx = mob.getX();
        double my = mob.getY();
        double mz = mob.getZ();

        mob.teleportTo(px, py, pz);
        player.teleportTo(mx, my, mz);

        player.sendSystemMessage(Component.literal(
                "Đã hoán đổi vị trí."));
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(
            net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Iterator<Map.Entry<UUID, Integer>> iterator =
                    CAGED.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                Entity entity = level.getEntity(entry.getKey());

                if (!(entity instanceof LivingEntity living)
                        || !living.isAlive()) {
                    iterator.remove();
                    CAGE_OWNER.remove(entry.getKey());
                    continue;
                }

                int remaining = entry.getValue() - 1;

                living.setDeltaMovement(0, 0, 0);
                living.hurtMarked = true;

                if (remaining <= 0) {
                    iterator.remove();
                    CAGE_OWNER.remove(entry.getKey());
                } else {
                    entry.setValue(remaining);
                }
            }
        }
    }
}
