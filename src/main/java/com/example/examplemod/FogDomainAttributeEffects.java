
package com.example.examplemod;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Xử lý hiệu ứng thuộc tính của người chơi.
 *
 * Nhanh nhẹn: mỗi điểm trên 5 tăng 2% tốc độ di chuyển.
 * Sức mạnh: mỗi điểm trên 5 tăng 2% sát thương đánh thường.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainAttributeEffects {

    private FogDomainAttributeEffects() {
    }

    private static final Identifier AGILITY_SPEED_ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "agility_movement_speed"
            );

    private static final Identifier STRENGTH_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "strength_attack_damage"
            );

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().getServer() == null) {
            return;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(player.level().getServer());

        data.initializeCharacter(player.getUUID());

        applyAgility(player, data);
        applyStrength(player, data);
    }

    private static void applyAgility(
            ServerPlayer player,
            FogDomainAbilityData data
    ) {
        int agility = data.getAttribute(
                player.getUUID(),
                "agility"
        );

        AttributeInstance movement =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movement == null) {
            return;
        }

        if (agility <= 5) {
            movement.removeModifier(AGILITY_SPEED_ID);
            return;
        }

        double bonus = (agility - 5) * 0.02D;

        AttributeModifier modifier = new AttributeModifier(
                AGILITY_SPEED_ID,
                bonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        movement.addOrUpdateTransientModifier(modifier);
    }

    private static void applyStrength(
            ServerPlayer player,
            FogDomainAbilityData data
    ) {
        int strength = data.getAttribute(
                player.getUUID(),
                "strength"
        );

        AttributeInstance attackDamage =
                player.getAttribute(Attributes.ATTACK_DAMAGE);

        if (attackDamage == null) {
            return;
        }

        if (strength <= 5) {
            attackDamage.removeModifier(STRENGTH_DAMAGE_ID);
            return;
        }

        double bonus = (strength - 5) * 0.02D;

        AttributeModifier modifier = new AttributeModifier(
                STRENGTH_DAMAGE_ID,
                bonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        attackDamage.addOrUpdateTransientModifier(modifier);
    }
}
