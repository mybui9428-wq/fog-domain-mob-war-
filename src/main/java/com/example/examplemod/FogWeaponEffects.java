package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Hiệu ứng phản sát thương của Khiên Kỳ Vật.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponEffects {

    private static final ThreadLocal<Boolean> REFLECTING =
            ThreadLocal.withInitial(() -> false);

    private FogWeaponEffects() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (REFLECTING.get()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Chỉ kích hoạt khi người chơi đang giơ đúng Khiên Kỳ Vật để đỡ đòn.
        if (!player.isBlocking()
                || !player.getUseItem().is(FogWeaponItems.SHIELD.get())) {
            return;
        }

        float incomingDamage = event.getAmount();
        if (incomingDamage <= 0.0F) {
            return;
        }

        // Khiên giảm 50% sát thương nhận vào.
        event.setAmount(incomingDamage * 0.5F);

        // Phản lại 50% sát thương gốc cho sinh vật/người chơi đã đánh.
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)
                || attacker == player
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        float reflectedDamage = incomingDamage * 0.5F;

        try {
            REFLECTING.set(true);
            attacker.hurtServer(
                    level,
                    level.damageSources().thorns(player),
                    reflectedDamage
            );
        } finally {
            REFLECTING.set(false);
        }
    }
}
