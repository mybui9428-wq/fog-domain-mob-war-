
package com.example.examplemod;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainCreatureSkillCommands {

    private static final int DEFAULT_COOLDOWN = 20 * 15;

    private FogDomainCreatureSkillCommands() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString().equalsIgnoreCase("tubi2531");
    }

    private static FogDomainAbilityData getData(ServerPlayer player) {
        return FogDomainAbilityData.get(
                ((ServerLevel) player.level()).getServer()
        );
    }

    private static int showList(ServerPlayer player) {
        if (!isController(player)) {
            return 0;
        }

        FogDomainAbilityData data = getData(player);
        List<String> skills = data.getCreatureSkills(player.getUUID());
        String selected = data.getSelectedCreatureSkill(player.getUUID());

        player.sendSystemMessage(Component.literal(
                "===== KỸ NĂNG SINH VẬT ====="
        ));

        if (skills.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa mở khóa kỹ năng sinh vật nào."
            ));
            return 1;
        }

        for (int i = 0; i < skills.size(); i++) {
            String id = skills.get(i);
            boolean isSelected = id.equals(selected);

            player.sendSystemMessage(Component.literal(
                    (i + 1) + ". "
                            + FogDomainCreatureSkills.getSkillName(id)
                            + (isSelected ? " [ĐANG CHỌN]" : "")
            ));
        }

        player.sendSystemMessage(Component.literal(
                "Dùng /fogskill created để đổi kỹ năng."
        ));

        player.sendSystemMessage(Component.literal(
                "Dùng /fogskill creaturecast để sử dụng kỹ năng đang chọn."
        ));

        player.sendSystemMessage(Component.literal(
                "Hoặc dùng /fogskill creatureuse <số>."
        ));

        return 1;
    }

    private static int cycleSkill(ServerPlayer player) {
        if (!isController(player)) {
            return 0;
        }

        FogDomainAbilityData data = getData(player);

        String nextSkill = data.cycleCreatureSkill(player.getUUID());

        if (nextSkill == null) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa mở khóa kỹ năng sinh vật nào."
            ));
            return 0;
        }

        player.sendSystemMessage(Component.literal(
                "Đã đổi sang: "
                        + FogDomainCreatureSkills.getSkillName(nextSkill)
        ));

        return 1;
    }

    private static int useSelectedSkill(ServerPlayer player) {
        if (!isController(player)) {
            return 0;
        }

        FogDomainAbilityData data = getData(player);

        String selected = data.getSelectedCreatureSkill(player.getUUID());

        if (selected == null) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa có kỹ năng sinh vật nào."
            ));
            return 0;
        }

        return useSkill(player, selected);
    }

    private static int useSkillByNumber(ServerPlayer player, int number) {
        if (!isController(player)) {
            return 0;
        }

        FogDomainAbilityData data = getData(player);

        String skillId = data.getCreatureSkillAt(
                player.getUUID(),
                number - 1
        );

        if (skillId == null) {
            player.sendSystemMessage(Component.literal(
                    "Số thứ tự kỹ năng không hợp lệ."
            ));
            return 0;
        }

        data.setSelectedCreatureSkill(player.getUUID(), skillId);

        return useSkill(player, skillId);
    }

    private static int useSkill(ServerPlayer player, String skillId) {
        ServerLevel level = (ServerLevel) player.level();
        FogDomainAbilityData data = getData(player);

        long gameTime = level.getGameTime();

        long remaining = data.getCooldownRemaining(
                player.getUUID(),
                skillId,
                gameTime
        );

        if (remaining > 0L) {
            player.sendSystemMessage(Component.literal(
                    "Kỹ năng còn hồi chiêu "
                            + ((remaining + 19L) / 20L)
                            + " giây."
            ));
            return 0;
        }

        boolean success = executeSkill(player, skillId);

        if (!success) {
            player.sendSystemMessage(Component.literal(
                    "Không thể sử dụng kỹ năng lúc này."
            ));
            return 0;
        }

        data.startCooldown(
                player.getUUID(),
                skillId,
                gameTime,
                getCooldown(skillId)
        );

        player.sendSystemMessage(Component.literal(
                "Đã sử dụng: "
                        + FogDomainCreatureSkills.getSkillName(skillId)
        ));

        return 1;
    }

    private static long getCooldown(String skillId) {
        return switch (skillId) {
            case FogDomainCreatureSkills.CREEPER_EXPLOSION -> 20L * 30L;
            case FogDomainCreatureSkills.ENDERMAN_TELEPORT -> 20L * 10L;
            case FogDomainCreatureSkills.WARDEN_SONIC_BOOM -> 20L * 25L;
            case FogDomainCreatureSkills.WARDEN_SONAR -> 20L * 15L;
            case FogDomainCreatureSkills.IRON_GOLEM_SMASH -> 20L * 15L;
            case FogDomainCreatureSkills.WITCH_POTION -> 20L * 20L;
            case FogDomainCreatureSkills.BLAZE_FIREBALL -> 20L * 12L;
            case FogDomainCreatureSkills.GHAST_FIREBALL -> 20L * 20L;
            case FogDomainCreatureSkills.SPIDER_WEB -> 20L * 15L;
            case FogDomainCreatureSkills.ENDER_DRAGON_BREATH -> 20L * 40L;
            default -> DEFAULT_COOLDOWN;
        };
    }

    private static boolean executeSkill(
            ServerPlayer player,
            String skillId
    ) {
        ServerLevel level = (ServerLevel) player.level();

        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = findTarget(player, 25.0D);

        switch (skillId) {

            case FogDomainCreatureSkills.CREEPER_EXPLOSION -> {
                areaDamage(player, 4.0D, 12.0F, 3.0D);

                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        player.getX() + look.x * 2.0D,
                        player.getY() + 1.0D + look.y * 2.0D,
                        player.getZ() + look.z * 2.0D,
                        1, 0.2D, 0.2D, 0.2D, 0.0D
                );
                return true;
            }

            case FogDomainCreatureSkills.ENDERMAN_TELEPORT -> {
                Vec3 destination = player.position().add(
                        look.scale(8.0D)
                );

                player.teleportTo(
                        level,
                        destination.x,
                        destination.y,
                        destination.z,
                        java.util.Set.of(),
                        player.getYRot(),
                        player.getXRot(),
                        true
                );

                level.sendParticles(
                        ParticleTypes.PORTAL,
                        destination.x,
                        destination.y + 1.0D,
                        destination.z,
                        50, 0.5D, 1.0D, 0.5D, 0.2D
                );
                return true;
            }

            case FogDomainCreatureSkills.WARDEN_SONIC_BOOM -> {
                if (target == null) return false;

                target.hurtServer(
                        level,
                        player.damageSources().magic(),
                        20.0F
                );

                level.sendParticles(
                        ParticleTypes.SONIC_BOOM,
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D
                );
                return true;
            }

            case FogDomainCreatureSkills.WARDEN_SONAR -> {
                areaDamage(player, 12.0D, 4.0F, 1.0D);

                level.sendParticles(
                        ParticleTypes.SCULK_SOUL,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        70, 5.0D, 3.0D, 5.0D, 0.1D
                );
                return true;
            }

            case FogDomainCreatureSkills.IRON_GOLEM_SMASH -> {
                areaDamage(player, 5.0D, 15.0F, 2.5D);

                level.sendParticles(
                        ParticleTypes.CRIT,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        35, 2.0D, 0.5D, 2.0D, 0.2D
                );
                return true;
            }

            case FogDomainCreatureSkills.WITCH_POTION -> {
                if (target == null) return false;

                target.addEffect(new MobEffectInstance(
                        MobEffects.POISON, 20 * 8, 1
                ));

                target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, 20 * 8, 1
                ));

                level.sendParticles(
                        ParticleTypes.WITCH,
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        30, 0.4D, 0.5D, 0.4D, 0.1D
                );
                return true;
            }

            case FogDomainCreatureSkills.BLAZE_FIREBALL -> {
                if (target == null) return false;

                target.hurtServer(
                        level,
                        player.damageSources().onFire(),
                        10.0F
                );

                target.igniteForSeconds(5.0F);
                return true;
            }

            case FogDomainCreatureSkills.GHAST_FIREBALL -> {
                if (target == null) return false;

                target.hurtServer(
                        level,
                        player.damageSources().magic(),
                        18.0F
                );

                target.igniteForSeconds(5.0F);

                level.sendParticles(
                        ParticleTypes.FLAME,
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        35, 0.5D, 0.5D, 0.5D, 0.1D
                );
                return true;
            }

            case FogDomainCreatureSkills.SPIDER_WEB -> {
                if (target == null) return false;

                target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, 20 * 10, 5
                ));

                target.addEffect(new MobEffectInstance(
                        MobEffects.JUMP_BOOST, 20 * 10, 128
                ));

                level.sendParticles(
                        ParticleTypes.ITEM_SNOWBALL,
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        25, 0.5D, 0.7D, 0.5D, 0.1D
                );
                return true;
            }

            case FogDomainCreatureSkills.ENDER_DRAGON_BREATH -> {
                areaDamage(player, 8.0D, 18.0F, 2.0D);

                level.sendParticles(
                        ParticleTypes.PORTAL,
                        player.getX() + look.x * 3.0D,
                        player.getY() + 1.0D + look.y * 3.0D,
                        player.getZ() + look.z * 3.0D,
                        100, 2.0D, 1.0D, 2.0D, 0.05D
                );
                return true;
            }
        }

        return false;
    }

    private static LivingEntity findTarget(
            ServerPlayer player,
            double range
    ) {
        ServerLevel level = (ServerLevel) player.level();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        AABB box = player.getBoundingBox().inflate(range);

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && !(entity instanceof ServerPlayer p
                        && isController(p))
        );

        LivingEntity best = null;
        double bestDistance = range;

        for (LivingEntity entity : entities) {
            Vec3 delta = entity.getBoundingBox()
                    .getCenter().subtract(eye);

            double projection = delta.dot(look);

            if (projection < 0.0D || projection > range) {
                continue;
            }

            Vec3 closest = eye.add(look.scale(projection));

            if (entity.getBoundingBox().getCenter()
                    .distanceTo(closest) > 1.5D) {
                continue;
            }

            if (projection < bestDistance) {
                bestDistance = projection;
                best = entity;
            }
        }

        return best;
    }

    private static void areaDamage(
            ServerPlayer player,
            double radius,
            float damage,
            double knockback
    ) {
        ServerLevel level = (ServerLevel) player.level();

        AABB area = player.getBoundingBox().inflate(radius);

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player
                        && entity.isAlive()
                        && !(entity instanceof ServerPlayer p
                        && isController(p))
        );

        for (LivingEntity target : entities) {
            target.hurtServer(
                    level,
                    player.damageSources().magic(),
                    damage
            );

            Vec3 direction = target.position()
                    .subtract(player.position());

            if (direction.lengthSqr() > 0.01D) {
                Vec3 normalized = direction.normalize();

                target.push(
                        normalized.x * knockback,
                        0.35D,
                        normalized.z * knockback
                );

                target.hurtMarked = true;
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("fogskill")

                        .then(Commands.literal("created")
                                .executes(context ->
                                        cycleSkill(
                                                context.getSource()
                                                        .getPlayerOrException()
                                        )
                                )
                        )

                        .then(Commands.literal("creaturelist")
                                .executes(context ->
                                        showList(
                                                context.getSource()
                                                        .getPlayerOrException()
                                        )
                                )
                        )

                        .then(Commands.literal("creaturecast")
                                .executes(context ->
                                        useSelectedSkill(
                                                context.getSource()
                                                        .getPlayerOrException()
                                        )
                                )
                        )

                        .then(Commands.literal("creatureuse")
                                .then(Commands.argument(
                                        "number",
                                        IntegerArgumentType.integer(1)
                                )
                                        .executes(context ->
                                                useSkillByNumber(
                                                        context.getSource()
                                                                .getPlayerOrException(),
                                                        IntegerArgumentType.getInteger(
                                                                context, "number"
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }
}
