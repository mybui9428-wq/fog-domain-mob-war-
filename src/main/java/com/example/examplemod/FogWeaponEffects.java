package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** Phòng thủ và phản sát thương của Khiên Kỳ Vật. */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponEffects {

    private static final ThreadLocal<Boolean> REFLECTING =
            ThreadLocal.withInitial(() -> false);

    private FogWeaponEffects() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (REFLECTING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isBlocking()
                || !player.getUseItem().is(FogWeaponItems.SHIELD.get())) return;

        float incomingDamage = event.getDamageContainer().getNewDamage();
        if (incomingDamage <= 0.0F) return;

        // Chặn toàn bộ sát thương và không làm mất độ bền khiên.
        event.setBlocked(true);
        event.setBlockedDamage(incomingDamage);
        event.setShieldDamage(0);

        // Phản lại 50% sát thương cho sinh vật/người chơi gây đòn đánh.
        Entity source = event.getDamageSource().getEntity();
        if (!(source instanceof LivingEntity attacker)
                || attacker == player
                || !(player.level() instanceof ServerLevel level)) return;

        try {
            REFLECTING.set(true);
            attacker.hurtServer(
                    level,
                    level.damageSources().thorns(player),
                    incomingDamage * 0.5F
            );
        } finally {
            REFLECTING.set(false);
        }
    }
}
