package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogArmorEffects {
    private static final double DOMAIN_RADIUS = 30.0D;
    private static final float FULL_SET_DAMAGE_MULTIPLIER = 0.5F;
    private static final Map<UUID, LivingEntity> SUPPRESSED = new HashMap<>();
    private static final Map<UUID, Long> SOUNDWAVE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> DASH_COOLDOWNS = new HashMap<>();

    private static final Identifier DOMAIN_HEALTH =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "equal_domain_health");
    private static final Identifier DOMAIN_SPEED =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "equal_domain_speed");
    private static final Identifier DOMAIN_STRENGTH =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "equal_domain_strength");
    private static final Identifier DOMAIN_ARMOR =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "equal_domain_armor");
    private static final Identifier DOMAIN_TOUGHNESS =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "equal_domain_toughness");

    private FogArmorEffects() {}

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString().equalsIgnoreCase("tubi2531");
    }

    private static boolean hasFullSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(FogArmorItems.CROWN.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(FogArmorItems.CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(FogArmorItems.LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(FogArmorItems.BOOTS.get());
    }

    private static boolean hasCrown(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(FogArmorItems.CROWN.get());
    }

    private static boolean hasChestplate(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(FogArmorItems.CHESTPLATE.get());
    }

    private static boolean hasLeggings(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.LEGS).is(FogArmorItems.LEGGINGS.get());
    }

    private static boolean hasBoots(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(FogArmorItems.BOOTS.get());
    }

    private static void storeOldArmor(ServerPlayer player, EquipmentSlot slot) {
        ItemStack old = player.getItemBySlot(slot);
        if (old.isEmpty()) return;
        ItemStack copy = old.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    private static int activate(ServerPlayer player) {
        if (!isController(player)) {
            player.sendSystemMessage(Component.literal("Chỉ tubi2531 mới có thể sử dụng bộ Kỳ Vật này."));
            return 0;
        }

        storeOldArmor(player, EquipmentSlot.HEAD);
        storeOldArmor(player, EquipmentSlot.CHEST);
        storeOldArmor(player, EquipmentSlot.LEGS);
        storeOldArmor(player, EquipmentSlot.FEET);

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FogArmorItems.CROWN.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FogArmorItems.CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(FogArmorItems.LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(FogArmorItems.BOOTS.get()));

        player.sendSystemMessage(Component.literal(
                "Đã tự động trang bị đủ Bộ Giáp Kỳ Vật. Bộ giáp giảm 50% sát thương nhận vào."));
        return 1;
    }

    private static int soundwave(ServerPlayer player) {
        if (!isController(player) || !hasChestplate(player)) {
            player.sendSystemMessage(Component.literal("Bạn cần mặc Áo Sóng Âm để sử dụng kỹ năng."));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        long readyAt = SOUNDWAVE_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            player.sendSystemMessage(Component.literal("Sóng Âm đang hồi chiêu."));
            return 0;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().inflate(18.0D);
        LivingEntity target = null;
        double best = 18.0D;

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, search,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer p && isController(p)))) {
            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = delta.length();
            if (distance > 18.0D || distance < 0.1D) continue;
            double dot = delta.normalize().dot(look);
            if (dot < 0.94D) continue;
            if (distance < best) {
                best = distance;
                target = candidate;
            }
        }

        if (target == null) {
            player.sendSystemMessage(Component.literal("Không có mục tiêu trước mặt trong phạm vi 18 block."));
            return 0;
        }

        level.sendParticles(ParticleTypes.SONIC_BOOM,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        target.hurtServer(level, level.damageSources().playerAttack(player), 12.0F);
        Vec3 push = target.position().subtract(player.position()).normalize().scale(1.1D);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.25D, push.z));
        target.hurtMarked = true;
        SOUNDWAVE_COOLDOWNS.put(player.getUUID(), now + 100L);
        player.sendSystemMessage(Component.literal("Đã phóng Sóng Âm!"));
        return 1;
    }

    private static int dash(ServerPlayer player) {
        if (!isController(player) || !hasLeggings(player)) {
            player.sendSystemMessage(Component.literal("Bạn cần mặc Quần Ảnh Bộ để sử dụng kỹ năng."));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        long readyAt = DASH_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            player.sendSystemMessage(Component.literal("Ảnh Bộ đang hồi chiêu."));
            return 0;
        }

        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * 1.8D, Math.max(0.18D, look.y * 0.5D), look.z * 1.8D);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                18, 0.35D, 0.25D, 0.35D, 0.04D);
        DASH_COOLDOWNS.put(player.getUUID(), now + 60L);
        player.sendSystemMessage(Component.literal("Ảnh Bộ!"));
        return 1;
    }

    private static void removeDomainModifiers(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, DOMAIN_HEALTH);
        remove(entity, Attributes.MOVEMENT_SPEED, DOMAIN_SPEED);
        remove(entity, Attributes.ATTACK_DAMAGE, DOMAIN_STRENGTH);
        remove(entity, Attributes.ARMOR, DOMAIN_ARMOR);
        remove(entity, Attributes.ARMOR_TOUGHNESS, DOMAIN_TOUGHNESS);
    }

    private static void remove(LivingEntity entity,
                               net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                               Identifier id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static void apply(LivingEntity entity,
                              net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                              Identifier id, double reduction) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (reduction <= 0.0D) {
            instance.removeModifier(id);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id, -reduction, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void suppress(LivingEntity entity, double reduction) {
        float oldHealth = entity.getHealth();
        apply(entity, Attributes.MAX_HEALTH, DOMAIN_HEALTH, reduction);
        apply(entity, Attributes.MOVEMENT_SPEED, DOMAIN_SPEED, reduction);
        apply(entity, Attributes.ATTACK_DAMAGE, DOMAIN_STRENGTH, reduction);
        apply(entity, Attributes.ARMOR, DOMAIN_ARMOR, reduction);
        apply(entity, Attributes.ARMOR_TOUGHNESS, DOMAIN_TOUGHNESS, reduction);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        } else if (oldHealth > 0.0F && entity.getHealth() > oldHealth) {
            entity.setHealth(oldHealth);
        }
    }

    private static void updateDomain(MinecraftServer server) {
        ServerPlayer controller = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isController(player)) {
                controller = player;
                break;
            }
        }

        Set<UUID> currentlyInside = new HashSet<>();
        if (controller != null && hasCrown(controller)
                && controller.level() instanceof ServerLevel level) {
            final ServerPlayer domainOwner = controller;
            FogDomainAbilityData data = FogDomainAbilityData.get(server);
            int controllerLevel = data.getCharacterLevel(domainOwner.getUUID());

            for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    domainOwner.getBoundingBox().inflate(DOMAIN_RADIUS),
                    e -> e != domainOwner && e.isAlive()
                            && e.distanceToSqr(domainOwner) <= DOMAIN_RADIUS * DOMAIN_RADIUS)) {
                int creatureLevel = 1; // Mọi sinh vật chưa có cấp độ riêng mặc định cấp 1.
                int levelDifference = Math.max(0, controllerLevel - creatureLevel);
                double reduction = Math.min(0.90D, levelDifference * 0.10D);
                if (reduction > 0.0D) {
                    suppress(entity, reduction);
                    currentlyInside.add(entity.getUUID());
                    SUPPRESSED.put(entity.getUUID(), entity);
                }
            }
        }

        Iterator<Map.Entry<UUID, LivingEntity>> iterator = SUPPRESSED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = iterator.next();
            if (!currentlyInside.contains(entry.getKey())) {
                removeDomainModifiers(entry.getValue());
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        updateDomain(event.getServer());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isController(player) && hasFullSet(player)) {
            event.setAmount(event.getAmount() * FULL_SET_DAMAGE_MULTIPLIER);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (hasBoots(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)
                || isController(attacker)
                || !(event.getTarget() instanceof LivingEntity target)
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer controller = null;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (isController(player) && hasCrown(player)
                    && player.level() == level
                    && player.distanceToSqr(attacker) <= DOMAIN_RADIUS * DOMAIN_RADIUS) {
                controller = player;
                break;
            }
        }
        if (controller == null || target.distanceToSqr(controller) > DOMAIN_RADIUS * DOMAIN_RADIUS) return;

        int controllerLevel = FogDomainAbilityData.get(level.getServer())
                .getCharacterLevel(controller.getUUID());
        double reduction = Math.min(0.90D,
                Math.max(0, controllerLevel - 1) * 0.10D);
        if (reduction <= 0.0D || !event.isCriticalHit()) return;

        float oldMultiplier = event.getDamageMultiplier();
        event.setDamageMultiplier(1.0F + (oldMultiplier - 1.0F) * (float) (1.0D - reduction));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fogarmor")
                .then(Commands.literal("activate")
                        .executes(c -> activate(c.getSource().getPlayerOrException())))
                .then(Commands.literal("soundwave")
                        .executes(c -> soundwave(c.getSource().getPlayerOrException())))
                .then(Commands.literal("dash")
                        .executes(c -> dash(c.getSource().getPlayerOrException())))
                .then(Commands.literal("status")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            if (!isController(player)) return 0;
                            player.sendSystemMessage(Component.literal(
                                    "Vương miện: " + hasCrown(player)
                                            + " | Áo: " + hasChestplate(player)
                                            + " | Quần: " + hasLeggings(player)
                                            + " | Giày: " + hasBoots(player)
                                            + " | Đủ bộ: " + hasFullSet(player)));
                            return 1;
                        })));
    }
}
