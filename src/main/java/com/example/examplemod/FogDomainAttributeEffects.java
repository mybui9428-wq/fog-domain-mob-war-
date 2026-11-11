
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

    /**
     * Mỗi điểm Nhanh nhẹn trên 5 tăng 2% tốc độ di chuyển.
     *
     * Ví dụ:
     * 5 điểm  = tốc độ bình thường.
     * 10 điểm = tăng 10%.
     * 15 điểm = tăng 20%.
     * 20 điểm = tăng 30%.
     */
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

        int agility = data.getAttribute(
                player.getUUID(),
                "agility"
        );

        AttributeInstance movement =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movement == null) {
            return;
        }

        // Nhanh nhẹn từ 5 trở xuống: không có tốc độ cộng thêm.
        if (agility <= 5) {
            movement.removeModifier(AGILITY_SPEED_ID);
            return;
        }

        // Mỗi điểm trên 5 tăng 2% tốc độ.
        double bonus = (agility - 5) * 0.02D;

        AttributeModifier modifier = new AttributeModifier(
                AGILITY_SPEED_ID,
                bonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        // Cập nhật modifier, không cộng dồn nhiều lần.
        movement.addOrUpdateTransientModifier(modifier);
    }
}
