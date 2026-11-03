
package com.example.examplemod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.commands.Commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainPassive {

    private static final String CONTROLLER = "tubi2531";

    private static final double SUCTION_RADIUS = 15.0D;
    private static final double ASSIMILATION_RANGE = 6.0D;

    private static final int WEAKNESS_TICKS = 20 * 60;
    private static final int COOLDOWN_TICKS = 20 * 60;

    private static final Map<UUID, PassiveData> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private static final class PassiveData {
        final ServerLevel level;
        final UUID playerId;
        final Vec3 blackHolePosition;
        int ticks;

        PassiveData(ServerLevel level, UUID playerId, Vec3 position) {
            this.level = level;
            this.playerId = playerId;
            this.blackHolePosition = position;
            this.ticks = 0;
        }
    }

    private FogDomainPassive() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString().equalsIgnoreCase(CONTROLLER);
    }

    // Kích hoạt khi sát thương sắp giết tubi2531.
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isController(player)) {
    return;
        }

        UUID id = player.getUUID();

        if (ACTIVE.containsKey(id)) {
            event.setNewDamage(0.0F);
            player.setHealth(1.0F);
            return;
        }

        if (COOLDOWNS.containsKey(id)) {
            if (player.getHealth() <= 1.0F) {
                event.setNewDamage(0.0F);
                player.setHealth(1.0F);
            }
            return;
        }

        float health = player.getHealth();
        float damage = event.getNewDamage();

        if (health - damage <= 1.0F) {
            event.setNewDamage(Math.max(0.0F, health - 1.0F));
            player.setHealth(1.0F);
            startPassive(player);
        }
    }

    private static void startPassive(ServerPlayer player) {

        UUID id = player.getUUID();

        if (ACTIVE.containsKey(id) || COOLDOWNS.containsKey(id)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Lưu vị trí cơ thể trước khi linh hồn bắt đầu di chuyển.
        Vec3 originalPosition = player.position();

        PassiveData data = new PassiveData(level, id, originalPosition);
        ACTIVE.put(id, data);
        COOLDOWNS.put(id, COOLDOWN_TICKS);

        player.setHealth(1.0F);

        // Bật chế độ linh hồn trong hệ thống điều khiển hiện có.
        FogDomainPossession.startSoul(player);

        level.sendParticles(
                ParticleTypes.PORTAL,
                originalPosition.x,
                originalPosition.y + 1.0D,
                originalPosition.z,
                180, 1.5D, 1.5D, 1.5D, 0.5D
        );

        level.sendParticles(
                ParticleTypes.SOUL,
                originalPosition.x,
                originalPosition.y + 1.0D,
                originalPosition.z,
                100, 1.0D, 1.0D, 1.0D, 0.2D
        );

        player.sendSystemMessage(Component.literal(
                "§5§lNHẬP HỒN! §dCơ thể đã biến thành hố đen. Linh hồn được tự do di chuyển."
        ));

        player.sendSystemMessage(Component.literal(
                "§dNgắm vào sinh vật hoặc người chơi rồi dùng /fogassimilation để nhập hồn."
        ));
    }

    // Hố đen hút thực thể quanh vị trí cơ thể ban đầu.
    private static void pullNearby(PassiveData data, ServerPlayer player) {

        ServerLevel level = data.level;
        Vec3 hole = data.blackHolePosition.add(0.0D, 1.0D, 0.0D);

        AABB area = new AABB(
                hole.x - SUCTION_RADIUS,
                hole.y - SUCTION_RADIUS,
                hole.z - SUCTION_RADIUS,
                hole.x + SUCTION_RADIUS,
                hole.y + SUCTION_RADIUS,
                hole.z + SUCTION_RADIUS
        );

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != player
                        && target.isAlive()
                        && !target.isRemoved()
                        && target.distanceToSqr(hole)
                                <= SUCTION_RADIUS * SUCTION_RADIUS
        );

        for (LivingEntity target : nearby) {

            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 direction = hole.subtract(targetCenter);
            double distance = direction.length();

            if (distance > 0.1D) {
                Vec3 velocity = direction.normalize().scale(
                        Math.min(0.8D, 0.15D + distance * 0.06D)
                );

                target.setDeltaMovement(velocity);
                target.hurtMarked = true;
            }

            if (data.ticks % 5 == 0) {
                level.sendParticles(
                        ParticleTypes.PORTAL,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(),
                        3, 0.2D, 0.3D, 0.2D, 0.05D
                );
            }

            // Sinh vật chạm tới hố đen bị hấp thụ và hồi máu.
            // Người chơi khác chỉ bị hút, không tự động bị giết.
            if (!(target instanceof ServerPlayer) && distance <= 1.5D) {
                target.discard();
                player.heal(2.0F);
            }
        }

        if (data.ticks % 5 == 0) {
            level.sendParticles(
                    ParticleTypes.SOUL,
                    hole.x,
                    hole.y,
                    hole.z,
                    12, 0.5D, 0.5D, 0.5D, 0.1D
            );

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    hole.x,
                    hole.y,
                    hole.z,
                    8, 0.4D, 0.4D, 0.4D, 0.1D
            );
        }
    }

    // Tìm thực thể gần nhất nằm trên đường ngắm của linh hồn.
    private static LivingEntity findTarget(ServerPlayer player) {

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(ASSIMILATION_RANGE));

        AABB searchArea = player.getBoundingBox()
                .expandTowards(look.scale(ASSIMILATION_RANGE))
                .inflate(1.0D);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                target -> target != player
                        && target.isAlive()
                        && !target.isRemoved()
                        && (!(target instanceof ServerPlayer)
                            || !isController((ServerPlayer) target))
        );

        LivingEntity closest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity target : candidates) {

            Optional<Vec3> hit = target.getBoundingBox()
                    .inflate(0.3D)
                    .clip(start, end);

            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    closest = target;
                }
            }
        }

        return closest;
    }

    // Lệnh /fogassimilation để nhập thực thể đang ngắm.
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("fogassimilation")
                        .requires(source ->
                                source.getEntity() instanceof ServerPlayer
                        )
                        .executes(context -> {

                            ServerPlayer player =
                                    context.getSource().getPlayerOrException();

                            if (!isController(player)) {
                                player.sendSystemMessage(Component.literal(
                                        "§cBạn không phải người điều khiển lĩnh vực."
                                ));
                                return 0;
                            }

                            if (!ACTIVE.containsKey(player.getUUID())) {
                                player.sendSystemMessage(Component.literal(
                                        "§cBạn không ở chế độ linh hồn."
                                ));
                                return 0;
                            }

                            LivingEntity target = findTarget(player);

                            if (target == null) {
                                player.sendSystemMessage(Component.literal(
                                        "§cKhông tìm thấy mục tiêu trong tầm ngắm 6 khối."
                                ));
                                return 0;
                            }

                            assimilate(player, target);
                            return 1;
                        })
        );
    }

    private static void assimilate(
            ServerPlayer player,
            LivingEntity target
    ) {

        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

        if (target == player) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (target.level() != level) {
            return;
        }

        UUID playerId = player.getUUID();

        // Lưu vị trí mục tiêu để tubi2531 xuất hiện tại đó.
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        FogDomainAbilityData data =
                FogDomainAbilityData.get(level.getServer());

        data.initializePower(playerId, true);

        String typeId = BuiltInRegistries.ENTITY_TYPE
                .getKey(target.getType())
                .toString();

        level.sendParticles(
                ParticleTypes.SOUL,
                x,
                y + target.getBbHeight() * 0.5D,
                z,
                80, 0.6D, 0.8D, 0.6D, 0.2D
        );

        if (target instanceof ServerPlayer absorbedPlayer) {

            // Nhận 50% sức mạnh từng kỹ năng của người chơi.
            data.transferPlayerPowers(
                    playerId,
                    absorbedPlayer.getUUID()
            );

            absorbedPlayer.hurtServer(
                    (ServerLevel) absorbedPlayer.level(),
                    absorbedPlayer.level().damageSources().generic(),
                    100000.0F
            );

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa người chơi "
                            + absorbedPlayer.getName().getString()
                            + ". Bạn nhận 50% sức mạnh từng kỹ năng."
            ));

        } else if (target instanceof Mob mob) {

            data.initializePower(mob.getUUID(), false);

            data.transferCreaturePowers(playerId, mob.getUUID());

            data.addAbility(playerId, typeId);

            int unlocked = data.unlockCreatureSkills(
                    playerId,
                    typeId
            );

            mob.discard();

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa sinh vật: "
                            + typeId
                            + " | Kỹ năng mở khóa: "
                            + unlocked
            ));

        } else {

            target.discard();

            data.addAbility(playerId, typeId);

            player.sendSystemMessage(Component.literal(
                    "§dĐã đồng hóa: " + typeId
            ));
        }

        // Kết thúc linh hồn, trả lại người chơi gốc.
        ACTIVE.remove(playerId);
        FogDomainPossession.stopSoul(player);

        player.setPos(x, y, z);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                WEAKNESS_TICKS,
                0
        ));

        level.sendParticles(
                ParticleTypes.EXPLOSION,
                x,
                y + 1.0D,
                z,
                8, 1.0D, 1.0D, 1.0D, 0.0D
        );

        player.sendSystemMessage(Component.literal(
                "§5§lNHẬP HỒN HOÀN TẤT! §dBạn đã trở lại với cơ thể của chính mình."
        ));

        player.sendSystemMessage(Component.literal(
                "§cBạn bị suy yếu trong 1 phút."
        ));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        // Đếm thời gian hồi kỹ năng.
        Iterator<Map.Entry<UUID, Integer>> cooldownIterator =
                COOLDOWNS.entrySet().iterator();

        while (cooldownIterator.hasNext()) {

            Map.Entry<UUID, Integer> entry = cooldownIterator.next();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                cooldownIterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        Iterator<Map.Entry<UUID, PassiveData>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, PassiveData> entry = iterator.next();
            PassiveData data = entry.getValue();

            if (!(data.level.getEntity(data.playerId)
                    instanceof ServerPlayer player)
                    || !player.isAlive()
                    || player.isRemoved()) {

                iterator.remove();
                continue;
            }

            // Linh hồn không thể chết trong thời gian Nhập Hồn.
            player.setHealth(1.0F);

            data.ticks++;

            // Hố đen hút ở vị trí cơ thể ban đầu.
            pullNearby(data, player);
        }
    }
}
