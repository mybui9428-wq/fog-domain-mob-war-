
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * Cơ chế phòng thủ của Khiên Kỳ Vật.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponEffects {

    private static final ThreadLocal<Boolean> REFLECTING =
            ThreadLocal.withInitial(() -> false);

    private FogWeaponEffects() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {

        // Không xử lý đòn phản lại.
        if (REFLECTING.get()) {
            return;
        }

        // Chỉ xử lý người chơi.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Phải đang sử dụng Khiên Kỳ Vật.
        if (!player.isBlocking()
                || !player.getUseItem().is(FogWeaponItems.SHIELD.get())) {
            return;
        }

        // Chỉ chặn khi đòn đánh thỏa điều kiện hướng đỡ của Minecraft.
        if (!event.getOriginalBlock()) {
            return;
        }

        float damage = event.getOriginalBlockedDamage();

        if (damage <= 0.0F) {
            return;
        }

        // Xác nhận chặn đòn hoàn toàn.
        event.setBlocked(true);
        event.setBlockedDamage(damage);

        // Khiên không mất độ bền.
        event.setShieldDamage(0);

        // Phản lại 50% sát thương.
        Entity source = event.getDamageSource().getEntity();

        if (!(source instanceof LivingEntity attacker)
                || attacker == player
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        try {
            REFLECTING.set(true);

            attacker.hurtServer(
                    level,
                    level.damageSources().thorns(player),
                    damage * 0.5F
            );

        } finally {
            REFLECTING.set(false);
        }
    }
}
