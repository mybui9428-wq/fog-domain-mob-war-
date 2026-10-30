
package com.example.examplemod;

import java.util.Optional;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class FogPossessionCommands {

    private static final double MAX_RANGE = 25.0D;

    private FogPossessionCommands() {}

    public static void register(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("fogpossess")

                        .then(Commands.literal("start")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!FogDomainPossession.isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn không có quyền sử dụng lệnh này."
                                                )
                                        );
                                        return 0;
                                    }

                                    Mob target = getLookedAtMob(player);

                                    if (target == null) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Không tìm thấy sinh vật. Hãy ngắm vào một sinh vật trong phạm vi 25 block."
                                                )
                                        );
                                        return 0;
                                    }

                                    if (!FogDomainPossession.start(
                                            player,
                                            target
                                    )) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Không thể nhập xác sinh vật này."
                                                )
                                        );
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã bắt đầu nhập xác!"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("stop")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!FogDomainPossession.isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn không có quyền sử dụng lệnh này."
                                                )
                                        );
                                        return 0;
                                    }

                                    if (!FogDomainPossession.stop(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn hiện không nhập xác."
                                                )
                                        );
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã kết thúc nhập xác!"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
        );
    }

    private static Mob getLookedAtMob(ServerPlayer player) {

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(MAX_RANGE));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(MAX_RANGE))
                .inflate(1.0D);

        Mob closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                searchBox,
                entity -> entity.isAlive()
                        && entity.level() == player.level()
        )) {

            Optional<Vec3> hit = mob.getBoundingBox()
                    .inflate(0.3D)
                    .clip(start, end);

            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = mob;
                }
            }
        }

        return closest;
    }
}
