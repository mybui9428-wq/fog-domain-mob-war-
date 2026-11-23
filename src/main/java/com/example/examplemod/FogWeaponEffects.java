
package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * Cơ chế phòng thủ của Khiên Kỳ Vật.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponEffects {

    private FogWeaponEffects() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {

        // Chỉ xử lý người chơi.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Chỉ kích hoạt khi đang giơ Khiên Kỳ Vật.
        if (!player.getUseItem().is(FogWeaponItems.SHIELD.get())) {
            return;
        }

        // Lấy toàn bộ sát thương hiện tại.
        float damage = event.getDamageContainer().getNewDamage();

        if (damage <= 0.0F) {
            return;
        }

        // Ép khiên chặn đòn.
        event.setBlocked(true);

        // Chặn toàn bộ sát thương.
        event.setBlockedDamage(damage);

        // Khiên không mất độ bền.
        event.setShieldDamage(0);
    }
}
