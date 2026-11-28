package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side five-minute Túc Thanh Giả awakening ritual. */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogTucAwakening {
    private static final long RITUAL_TICKS = 20L * 60L * 5L;

    private FogTucAwakening() {}

    /** Announces the ritual after the server has validated and charged its cost. */
    public static void start(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        player.sendSystemMessage(Component.literal("§6Lửa Tịch Diệt đã bắt đầu thiêu đốt thân thể ngươi!"));
        player.sendSystemMessage(Component.literal("§7Nghi thức thức tỉnh kéo dài 5 phút. Hãy sống sót!"));
        player.level().playSound(
        null,
        player.blockPosition(),
        SoundEvents.BLAZE_AMBIENT,
        SoundSource.MASTER,
        0.8F,
        0.65F
);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        FogDomainAbilityData data = FogDomainAbilityData.get(player.level().getServer());
        double startedValue = data.getPower(player.getUUID(), "tuc_awakening_started");
        if (startedValue <= 0.0D) return;

        long started = (long) startedValue;
        long elapsed = Math.max(0L, level.getGameTime() - started);
        long remaining = Math.max(0L, RITUAL_TICKS - elapsed);

        // Black-flame-like ritual effect; kept lightweight by emitting every 5 ticks.
        if (player.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    8, 0.38D, 0.55D, 0.38D, 0.015D);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    5, 0.32D, 0.45D, 0.32D, 0.01D);
        }

        if (elapsed == 1L) {
            player.sendSystemMessage(Component.literal("§8Lửa dữ thiêu thân, vỏ cũ tróc bỏ."));
        } else if (elapsed == 20L * 60L) {
            player.sendSystemMessage(Component.literal("§6Ngươi lột xác trong đau đớn."));
        } else if (elapsed == 20L * 60L * 3L) {
            player.sendSystemMessage(Component.literal("§cNgươi thăng hoa giữa gian nan."));
        }

        if (remaining > 0L && remaining % (20L * 60L) == 0L && elapsed > 0L) {
            player.sendSystemMessage(Component.literal("§eTịch Diệt Thiêu Đốt: còn " + (remaining / 1200L) + " phút."));
        }

        if (remaining == 30L * 20L && player.tickCount % 20 == 0) {
            player.sendSystemMessage(Component.literal("§6Nghi thức sắp hoàn tất..."));
        }

        if (elapsed >= RITUAL_TICKS) {
            if (data.completeTucThanhGiaAwakening(player.getUUID())) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.65D, player.getDeltaMovement().z);
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        180, 1.4D, 2.2D, 1.4D, 0.12D);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        100, 1.0D, 1.8D, 1.0D, 0.08D);
                player.sendSystemMessage(Component.literal("§6☀ Ánh sáng vàng giáng xuống!"));
                player.sendSystemMessage(Component.literal("§eNgươi đã hoàn thành lễ rửa tội của ngọn lửa Tịch Diệt!"));
                player.level().playSound(
        null,
        player.blockPosition(),
        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
        SoundSource.MASTER,
        1.0F,
        1.0F
);
            }
        }
    }
}
