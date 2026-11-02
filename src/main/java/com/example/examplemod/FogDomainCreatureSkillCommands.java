
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
        if (!isController(player)) return 0;

        FogDomainAbilityData data = getData(player);
        List<String> skills = data.getCreatureSkills(player.getUUID());
        String selected = data.getSelectedCreatureSkill(player.getUUID());

        player.sendSystemMessage(Component.literal("===== KỸ NĂNG SINH VẬT ====="));

        if (skills.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa mở khóa kỹ năng sinh vật nào."
            ));
            return 1;
        }

        for (int i = 0; i < skills.size(); i++) {
            String id = skills.get(i);
            player.sendSystemMessage(Component.literal(
                    (i + 1) + ". "
                            + FogDomainCreatureSkills.getSkillName(id)
                            + (id.equals(selected) ? " [ĐANG CHỌN]" : "")
            ));
        }

        player.sendSystemMessage(Component.literal(
                "Đổi kỹ năng: /fogskill created"
        ));
        player.sendSystemMessage(Component.literal(
                "Dùng kỹ năng: /fogskill creaturecast"
        ));
        player.sendSystemMessage(Component.literal(
                "Chọn theo số: /fogskill creatureuse <số>"
        ));

        return 1;
    }

    private static int cycleSkill(ServerPlayer player) {
        if (!isController(player)) return 0;

        String next = getData(player).cycleCreatureSkill(player.getUUID());

        if (next == null) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa mở khóa kỹ năng sinh vật nào."
            ));
            return 0;
        }

        player.sendSystemMessage(Component.literal(
                "Đã đổi sang: " + FogDomainCreatureSkills.getSkillName(next)
        ));
        return 1;
    }

    private static int useSelectedSkill(ServerPlayer player) {
        if (!isController(player)) return 0;

        String selected = getData(player).getSelectedCreatureSkill(
                player.getUUID()
        );

        if (selected == null) {
            player.sendSystemMessage(Component.literal(
                    "Bạn chưa có kỹ năng sinh vật nào."
            ));
            return 0;
        }

        return useSkill(player, selected);
    }

    private static int useSkillByNumber(ServerPlayer player, int number) {
        if (!isController(player)) return 0;

        FogDomainAbilityData data = getData(player);
        String skillId = data.getCreatureSkillAt(
                player.getUUID(), number - 1
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
                player.getUUID(), skillId, gameTime
        );

        if (remaining > 0L) {
            player.sendSystemMessage(Component.literal(
                    "Kỹ năng còn hồi chiêu "
                            + ((remaining + 19L) / 20L) + " giây."
            ));
            return 0;
        }

        if (!executeSkill(player, skillId)) {
            player.sendSystemMessage(Component.literal(
                    "Không thể sử dụng kỹ năng lúc này."
            ));
            return 0;
        }

        data.startCooldown(
                player.getUUID(), skillId, gameTime, getCooldown(skillId)
        );

        player.sendSystemMessage(Component.literal(
                "Đã sử dụng: "
                        + FogDomainCreatureSkills.getSkillName(skillId)
        ));

        return 1;
    }

    private static long getCooldown(String id) {
        return switch (id) {
            case FogDomainCreatureSkills.CREEPER_EXPLOSION -> 600L;
            case FogDomainCreatureSkills.ENDERMAN_TELEPORT -> 200L;
            case FogDomainCreatureSkills.WARDEN_SONIC_BOOM -> 500L;
            case FogDomainCreatureSkills.ENDER_DRAGON_BREATH -> 800L;
            case FogDomainCreatureSkills.WITHER_STORM -> 800L;
            case FogDomainCreatureSkills.ZOMBIE_HORDE -> 600L;
            case FogDomainCreatureSkills.BEE_STING -> 240L;
            case FogDomainCreatureSkills.SLIME_BOUNCE -> 240L;
            case FogDomainCreatureSkills.DROWNED_TIDAL -> 400L;
            default -> DEFAULT_COOLDOWN;
        };
    }

    private static boolean executeSkill(
            ServerPlayer player,
            String id
    ) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = findTarget(player, 25.0D);

        switch (id) {

            // Kỹ năng cũ
            case FogDomainCreatureSkills.CREEPER_EXPLOSION -> {
                areaDamage(player, 4, 12, 3);
                particles(level, ParticleTypes.EXPLOSION,
                        player.position().add(look.scale(2)).add(0, 1, 0),
                        1, 0.2, 0.2, 0.2);
                return true;
            }

            case FogDomainCreatureSkills.ENDERMAN_TELEPORT -> {
                Vec3 destination = player.position().add(look.scale(8));
                player.teleportTo(
                        level, destination.x, destination.y, destination.z,
                        java.util.Set.of(), player.getYRot(),
                        player.getXRot(), true
                );
                particles(level, ParticleTypes.PORTAL,
                        destination.add(0, 1, 0), 50, 0.5, 1, 0.5);
                return true;
            }

            case FogDomainCreatureSkills.WARDEN_SONIC_BOOM -> {
                if (target == null) return false;
                damage(player, target, 20);
                particles(level, ParticleTypes.SONIC_BOOM,
                        target.position().add(0, 1, 0),
                        1, 0, 0, 0);
                return true;
            }

            case FogDomainCreatureSkills.WARDEN_SONAR -> {
                particles(level, ParticleTypes.SCULK_SOUL,
                        player.position().add(0, 1, 0),
                        70, 5, 3, 5);
                return true;
            }

            case FogDomainCreatureSkills.IRON_GOLEM_SMASH -> {
                areaDamage(player, 5, 15, 2.5);
                particles(level, ParticleTypes.CRIT,
                        player.position().add(0, 1, 0),
                        35, 2, 0.5, 2);
                return true;
            }

            case FogDomainCreatureSkills.WITCH_POTION -> {
                if (target == null) return false;
                effect(target, MobEffects.POISON, 8, 1);
                effect(target, MobEffects.SLOWNESS, 8, 1);
                particles(level, ParticleTypes.WITCH,
                        target.position().add(0, 1, 0),
                        30, 0.4, 0.5, 0.4);
                return true;
            }

            case FogDomainCreatureSkills.BLAZE_FIREBALL -> {
                if (target == null) return false;
                damage(player, target, 10);
                target.igniteForSeconds(5);
                particles(level, ParticleTypes.FLAME,
                        target.position().add(0, 1, 0),
                        25, 0.4, 0.4, 0.4);
                return true;
            }

            case FogDomainCreatureSkills.GHAST_FIREBALL -> {
                if (target == null) return false;
                damage(player, target, 18);
                target.igniteForSeconds(5);
                particles(level, ParticleTypes.FLAME,
                        target.position().add(0, 1, 0),
                        35, 0.5, 0.5, 0.5);
                return true;
            }

            case FogDomainCreatureSkills.SPIDER_WEB,
                 FogDomainCreatureSkills.CAVE_SPIDER_POISON -> {
                if (target == null) return false;
                effect(target, MobEffects.SLOWNESS, 10, 5);
                if (id.equals(FogDomainCreatureSkills.CAVE_SPIDER_POISON)) {
                    effect(target, MobEffects.POISON, 8, 1);
                }
                particles(level, ParticleTypes.ITEM_SNOWBALL,
                        target.position().add(0, 1, 0),
                        30, 0.5, 0.7, 0.5);
                return true;
            }

            case FogDomainCreatureSkills.ENDER_DRAGON_BREATH -> {
                coneDamage(player, 8, 18);
                particles(level, ParticleTypes.DRAGON_BREATH,
                        player.position().add(look.scale(4)).add(0, 1, 0),
                        80, 1.5, 1, 1.5);
                return true;
            }

            // Wither
            case FogDomainCreatureSkills.WITHER_STORM -> {
                areaDamage(player, 9, 12, 2);
                for (LivingEntity e : nearby(player, 9)) {
                    effect(e, MobEffects.WITHER, 6, 1);
                }
                particles(level, ParticleTypes.SOUL,
                        player.position().add(0, 1, 0),
                        100, 4, 2, 4);
                return true;
            }

            // Wither Skeleton
            case FogDomainCreatureSkills.WITHER_SKELETON_SLASH -> {
                if (target == null) return false;
                damage(player, target, 12);
                effect(target, MobEffects.WITHER, 6, 1);
                knockback(player, target, 1.8);
                particles(level, ParticleTypes.SWEEP_ATTACK,
                        target.position().add(0, 1, 0),
                        8, 0.5, 0.5, 0.5);
                return true;
            }

            // Skeleton
            case FogDomainCreatureSkills.SKELETON_ARROW,
                 FogDomainCreatureSkills.BOGGED_ARROW,
                 FogDomainCreatureSkills.PILLAGER_CROSSBOW,
                 FogDomainCreatureSkills.STRAY_FROST -> {
                if (target == null) return false;
                damage(player, target, 9);
                if (id.equals(FogDomainCreatureSkills.BOGGED_ARROW)) {
                    effect(target, MobEffects.POISON, 5, 1);
                }
                if (id.equals(FogDomainCreatureSkills.STRAY_FROST)) {
                    effect(target, MobEffects.SLOWNESS, 6, 2);
                }
                particles(level, ParticleTypes.CRIT,
                        target.position().add(0, 1, 0),
                        15, 0.3, 0.3, 0.3);
                return true;
            }

            // Zombie và biến thể
            case FogDomainCreatureSkills.ZOMBIE_HORDE,
                 FogDomainCreatureSkills.ZOMBIE_VILLAGER_CALL -> {
                areaDamage(player, 6, 7, 1.2);
                for (LivingEntity e : nearby(player, 6)) {
                    effect(e, MobEffects.WEAKNESS, 5, 0);
                }
                particles(level, ParticleTypes.ANGRY_VILLAGER,
                        player.position().add(0, 1, 0),
                        35, 2, 1, 2);
                return true;
            }

            case FogDomainCreatureSkills.HUSK_SAND -> {
                areaDamage(player, 7, 8, 1);
                for (LivingEntity e : nearby(player, 7)) {
                    effect(e, MobEffects.BLINDNESS, 3, 0);
                }
                particles(level, ParticleTypes.POOF,
                        player.position().add(0, 1, 0),
                        50, 2, 1, 2);
                return true;
            }

            // Drowned
            case FogDomainCreatureSkills.DROWNED_TIDAL -> {
                areaDamage(player, 8, 8, 1.5);
                for (LivingEntity e : nearby(player, 8)) {
                    effect(e, MobEffects.SLOWNESS, 4, 1);
                    e.push(0, 0.8, 0);
                    e.hurtMarked = true;
                }
                particles(level, ParticleTypes.SPLASH,
                        player.position().add(0, 1, 0),
                        100, 3, 2, 3);
                return true;
            }

            // Slime
            case FogDomainCreatureSkills.SLIME_BOUNCE,
                 FogDomainCreatureSkills.MAGMA_CUBE_BURN,
                 FogDomainCreatureSkills.SULFUR_CUBE_BLAST -> {
                areaDamage(player, 5, 8, 1);
                player.push(0, 1.1, 0);
                player.hurtMarked = true;
                if (!id.equals(FogDomainCreatureSkills.SLIME_BOUNCE)) {
                    for (LivingEntity e : nearby(player, 5)) {
                        e.igniteForSeconds(3);
                    }
                }
                particles(level, ParticleTypes.ITEM_SLIME,
                        player.position().add(0, 1, 0),
                        45, 1.5, 0.5, 1.5);
                return true;
            }

            // Bee
            case FogDomainCreatureSkills.BEE_STING -> {
                if (target == null) return false;
                damage(player, target, 5);
                effect(target, MobEffects.POISON, 8, 1);
                effect(target, MobEffects.SLOWNESS, 3, 1);
                particles(level, ParticleTypes.ANGRY_VILLAGER,
                        target.position().add(0, 1, 0),
                        20, 0.4, 0.5, 0.4);
                return true;
            }

            // Kỹ năng lao tới / cận chiến
            case FogDomainCreatureSkills.GOAT_RAM,
                 FogDomainCreatureSkills.HOGLIN_CHARGE,
                 FogDomainCreatureSkills.RAVAGER_RAM,
                 FogDomainCreatureSkills.ZOGLIN_CHARGE,
                 FogDomainCreatureSkills.HORSE_CHARGE,
                 FogDomainCreatureSkills.COW_RUSH,
                 FogDomainCreatureSkills.PIG_CHARGE,
                 FogDomainCreatureSkills.CAMEL_STOMP,
                 FogDomainCreatureSkills.POLAR_BEAR_SMASH,
                 FogDomainCreatureSkills.PIGLIN_BRUTE_SMASH,
                 FogDomainCreatureSkills.VINDICATOR_AXE,
                 FogDomainCreatureSkills.WOLF_BITE,
                 FogDomainCreatureSkills.CAT_SCRATCH,
                 FogDomainCreatureSkills.DONKEY_KICK,
                 FogDomainCreatureSkills.MULE_KICK -> {
                if (target == null) return false;
                damage(player, target, 10);
                knockback(player, target, 2.2);
                particles(level, ParticleTypes.CRIT,
                        target.position().add(0, 1, 0),
                        20, 0.5, 0.5, 0.5);
                return true;
            }

            // Kỹ năng khống chế
            case FogDomainCreatureSkills.EVOKER_FANGS,
                 FogDomainCreatureSkills.SHULKER_PROJECTILE,
                 FogDomainCreatureSkills.GUARDIAN_LASER,
                 FogDomainCreatureSkills.ELDER_GUARDIAN_LASER,
                 FogDomainCreatureSkills.GHAST_FIREBALL -> {
                if (target == null) return false;
                damage(player, target, 13);
                effect(target, MobEffects.SLOWNESS, 3, 1);
                particles(level, ParticleTypes.CRIT,
                        target.position().add(0, 1, 0),
                        25, 0.5, 0.5, 0.5);
                return true;
            }

            // Kỹ năng dịch chuyển / lao nhanh
            case FogDomainCreatureSkills.DOLPHIN_DASH,
                 FogDomainCreatureSkills.PHANTOM_DIVE,
                 FogDomainCreatureSkills.VEX_DASH,
                 FogDomainCreatureSkills.RABBIT_JUMP,
                 FogDomainCreatureSkills.OCELOT_STEALTH -> {
                player.push(look.x * 1.8, 0.45, look.z * 1.8);
                player.hurtMarked = true;
                particles(level, ParticleTypes.CLOUD,
                        player.position().add(0, 1, 0),
                        30, 0.6, 0.5, 0.6);
                return true;
            }

            // Hỗ trợ / phòng thủ
            case FogDomainCreatureSkills.AXolotl_REGEN -> {
                effect(player, MobEffects.REGENERATION, 8, 1);
                particles(level, ParticleTypes.HEART,
                        player.position().add(0, 1, 0),
                        15, 0.5, 0.8, 0.5);
                return true;
            }

            case FogDomainCreatureSkills.ARMADILLO_ROLL,
                 FogDomainCreatureSkills.TURTLE_SHELL,
                 FogDomainCreatureSkills.PANDA_ROLL -> {
                effect(player, MobEffects.RESISTANCE, 8, 2);
                particles(level, ParticleTypes.CRIT,
                        player.position().add(0, 1, 0),
                        30, 1, 0.5, 1);
                return true;
            }

            case FogDomainCreatureSkills.ALLAY_SUPPORT,
                 FogDomainCreatureSkills.SNIFFER_SCENT,
                 FogDomainCreatureSkills.VILLAGER_TRADE,
                 FogDomainCreatureSkills.WANDERING_TRADER_POTION -> {
                effect(player, MobEffects.REGENERATION, 5, 0);
                effect(player, MobEffects.SPEED, 8, 1);
                particles(level, ParticleTypes.HAPPY_VILLAGER,
                        player.position().add(0, 1, 0),
                        30, 1, 1, 1);
                return true;
            }

            // Các kỹ năng nguyên tố / sinh vật nhỏ
            case FogDomainCreatureSkills.LLAMA_SPIT,
                 FogDomainCreatureSkills.PUFFERFISH_POISON,
                 FogDomainCreatureSkills.FROG_TONGUE,
                 FogDomainCreatureSkills.PARROT_SONIC,
                 FogDomainCreatureSkills.SALMON_SPLASH,
                 FogDomainCreatureSkills.SQUID_INK,
                 FogDomainCreatureSkills.GLOW_SQUID_BLIND,
                 FogDomainCreatureSkills.BAT_SONAR,
                 FogDomainCreatureSkills.MOOSHROOM_SPORES,
                 FogDomainCreatureSkills.CHICKEN_FLUTTER,
                 FogDomainCreatureSkills.SHEEP_WOOL,
                 FogDomainCreatureSkills.STRIDER_LAVA,
                 FogDomainCreatureSkills.SNOW_GOLEM_SNOW,
                 FogDomainCreatureSkills.HAPPY_GHAST_WIND,
                 FogDomainCreatureSkills.NAUTILUS_WATER,
                 FogDomainCreatureSkills.ZOMBIE_NAUTILUS_TIDE,
                 FogDomainCreatureSkills.FOX_AMBUSH,
                 FogDomainCreatureSkills.PIGLIN_GOLD,
                 FogDomainCreatureSkills.COPPER_GOLEM_SPIN,
                 FogDomainCreatureSkills.CREAKING_STRIKE,
                 FogDomainCreatureSkills.SILVERFISH_SWARM -> {
                if (target != null) {
                    damage(player, target, 7);
                    effect(target, MobEffects.SLOWNESS, 3, 0);
                } else {
                    effect(player, MobEffects.SPEED, 5, 1);
                }
                particles(level, ParticleTypes.CRIT,
                        player.position().add(look.scale(2)).add(0, 1, 0),
                        25, 0.8, 0.8, 0.8);
                return true;
            }
        }

        return false;
    }

    private static void damage(
            ServerPlayer player,
            LivingEntity target,
            float amount
    ) {
        target.hurtServer(
                (ServerLevel) player.level(),
                player.damageSources().magic(),
                amount
        );
    }

    private static void effect(
            LivingEntity target,
            net.minecraft.world.effect.MobEffect effect,
            int seconds,
            int amplifier
    ) {
        target.addEffect(new MobEffectInstance(
                effect, seconds * 20, amplifier
        ));
    }

    private static void knockback(
            ServerPlayer player,
            LivingEntity target,
            double strength
    ) {
        Vec3 direction = target.position().subtract(player.position());
        if (direction.lengthSqr() < 0.01) return;

        direction = direction.normalize();
        target.push(direction.x * strength, 0.35, direction.z * strength);
        target.hurtMarked = true;
    }

    private static List<LivingEntity> nearby(
            ServerPlayer player,
            double radius
    ) {
        ServerLevel level = (ServerLevel) player.level();

        return level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player
                        && entity.isAlive()
                        && !(entity instanceof ServerPlayer p && isController(p))
        );
    }

    private static void areaDamage(
            ServerPlayer player,
            double radius,
            float damage,
            double knockback
    ) {
        for (LivingEntity target : nearby(player, radius)) {
            damage(player, target, damage);
            knockback(player, target, knockback);
        }
    }

    private static void coneDamage(
            ServerPlayer player,
            double range,
            float damage
    ) {
        Vec3 look = player.getLookAngle().normalize();

        for (LivingEntity target : nearby(player, range)) {
            Vec3 delta = target.getBoundingBox().getCenter()
                    .subtract(player.getEyePosition());

            if (delta.lengthSqr() > range * range) continue;

            if (delta.normalize().dot(look) > 0.55) {
                damage(player, target, damage);
            }
        }
    }

    private static void particles(
            ServerLevel level,
            net.minecraft.core.particles.ParticleOptions particle,
            Vec3 position,
            int count,
            double x,
            double y,
            double z
    ) {
        level.sendParticles(
                particle,
                position.x, position.y, position.z,
                count, x, y, z, 0.08
        );
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
                        && !(entity instanceof ServerPlayer p && isController(p))
        );

        LivingEntity best = null;
        double bestDistance = range;

        for (LivingEntity entity : entities) {
            Vec3 delta = entity.getBoundingBox().getCenter().subtract(eye);
            double projection = delta.dot(look);

            if (projection < 0 || projection > range) continue;

            Vec3 closest = eye.add(look.scale(projection));

            if (entity.getBoundingBox().getCenter().distanceTo(closest) > 1.5) {
                continue;
            }

            if (projection < bestDistance) {
                bestDistance = projection;
                best = entity;
            }
        }

        return best;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("fogskill")
                        .then(Commands.literal("created")
                                .executes(context ->
                                        cycleSkill(context.getSource()
                                                .getPlayerOrException())
                                )
                        )
                        .then(Commands.literal("creaturelist")
                                .executes(context ->
                                        showList(context.getSource()
                                                .getPlayerOrException())
                                )
                        )
                        .then(Commands.literal("creaturecast")
                                .executes(context ->
                                        useSelectedSkill(context.getSource()
                                                .getPlayerOrException())
                                )
                        )
                        .then(Commands.literal("creatureuse")
                                .then(Commands.argument(
                                        "number",
                                        IntegerArgumentType.integer(1)
                                ).executes(context ->
                                        useSkillByNumber(
                                                context.getSource()
                                                        .getPlayerOrException(),
                                                IntegerArgumentType.getInteger(
                                                        context, "number"
                                                )
                                        )
                                ))
                        )
        );
    }
}
