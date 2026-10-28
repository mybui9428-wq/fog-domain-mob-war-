
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainTaming {

    private static int tickCounter = 0;

    private FogDomainTaming() {
    }

    public static boolean isTamed(Entity entity) {
        if (entity == null) {
            return false;
        }

        for (List<UUID> ids : FogDomainSkills.TAMED.values()) {
            if (ids.contains(entity.getUUID())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isOwner(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < 5) {
            return;
        }
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServerPlayer owner = findOwner(level);

            if (owner == null || !owner.isAlive()) {
                continue;
            }

            List<UUID> owned = FogDomainSkills.TAMED.get(
                    owner.getUUID()
            );

            if (owned == null || owned.isEmpty()) {
                continue;
            }

            List<UUID> remove = new ArrayList<>();

            for (UUID id : owned) {
                Entity entity = level.getEntity(id);

                if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                    remove.add(id);
                    continue;
                }

                // Không bao giờ tấn công chủ nhân.
                if (mob.getTarget() == owner) {
                    mob.setTarget(null);
                }

                // Chỉ chọn mục tiêu nếu chủ nhân vừa bị tấn công
                // hoặc vừa chủ động tấn công mục tiêu đó.
                LivingEntity target = chooseTarget(owner, mob);

                if (target != null) {
                    mob.setTarget(target);
                    mob.getLookControl().setLookAt(
                            target, 30.0F, 30.0F
                    );

                    if (mob.distanceToSqr(target) > 3.0D) {
                        mob.getNavigation().moveTo(target, 1.2D);
                    } else {
                        mob.getNavigation().stop();
                    }
                } else {
                    // Xóa mục tiêu tự chọn của AI thông thường.
                    mob.setTarget(null);

                    // Khi không có mục tiêu, chỉ đi theo chủ nhân.
                    if (mob.distanceToSqr(owner) > 9.0D) {
                        mob.getNavigation().moveTo(owner, 1.2D);
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }

            owned.removeAll(remove);

            if (owned.isEmpty()) {
                FogDomainSkills.TAMED.remove(owner.getUUID());
            }
        }
    }

    private static ServerPlayer findOwner(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (isOwner(player)) {
                return player;
            }
        }
        return null;
    }

    private static LivingEntity chooseTarget(
            ServerPlayer owner,
            Mob companion) {

        // Ưu tiên sinh vật vừa tấn công chủ nhân.
        LivingEntity attacker = owner.getLastHurtByMob();

        if (isValidTarget(owner, companion, attacker)) {
            return attacker;
        }

        // Tiếp theo là mục tiêu chủ nhân vừa tấn công.
        LivingEntity ownerTarget = owner.getLastHurtMob();

        if (isValidTarget(owner, companion, ownerTarget)) {
            return ownerTarget;
        }

        return null;
    }

    private static boolean isValidTarget(
            ServerPlayer owner,
            Mob companion,
            LivingEntity target) {

        if (target == null
                || !target.isAlive()
                || target.isRemoved()
                || target == owner
                || target == companion
                || target.level() != companion.level()) {
            return false;
        }

        // Không đánh sinh vật đã được thuần hóa.
        if (isTamed(target)) {
            return false;
        }

        // Không bao giờ chọn tubi2531 làm mục tiêu.
        if (isOwner(target)) {
            return false;
        }

        return true;
    }
}
