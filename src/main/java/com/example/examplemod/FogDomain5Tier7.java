
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Tier7 {

    private static final double SUCTION_RADIUS = 10.0D;
    private static final double DEVOUR_DISTANCE = 1.6D;
    private static final int TICK_INTERVAL = 5;

    private static final Map<UUID, Map<BlockPos, BlockState>>
            ORIGINAL_BLOCKS = new HashMap<>();

    private static final Map<UUID, UUID>
            SPECIAL_CREATURES = new HashMap<>();

    private static final Map<UUID, Integer>
            DEVOUR_COUNT = new HashMap<>();

    private FogDomain5Tier7() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    // Tạo làng tại vị trí người điều khiển.
    public static int start(ServerPlayer player) {

        if (!isController(player)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        UUID ownerId = player.getUUID();

        if (ORIGINAL_BLOCKS.containsKey(ownerId)) {
            return 0;
        }

        Map<BlockPos, BlockState> saved = new HashMap<>();

        BlockPos center = player.blockPosition();
        int groundY = center.getY() - 1;

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        // Quảng trường nhỏ ở trung tâm.
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {

                pos.set(
                        center.getX() + x,
                        groundY,
                        center.getZ() + z
                );

                if (level.isEmptyBlock(pos.above())) {
                    placeBlock(
                            level,
                            pos.immutable(),
                            Blocks.STONE_BRICKS.defaultBlockState(),
                            saved
                    );
                }
            }
        }

        // Bốn trụ đánh dấu ngôi làng.
        for (int x : new int[]{-5, 5}) {
            for (int z : new int[]{-5, 5}) {

                for (int y = 0; y < 4; y++) {

                    BlockPos pillar = new BlockPos(
                            center.getX() + x,
                            groundY + y,
                            center.getZ() + z
                    );

                    placeBlock(
                            level,
                            pillar,
                            Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                            saved
                    );
                }
            }
        }

        // Bệ trung tâm.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {

                BlockPos altar = new BlockPos(
                        center.getX() + x,
                        groundY + 1,
                        center.getZ() + z
                );

                placeBlock(
                        level,
                        altar,
                        Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                        saved
                );
            }
        }

        ORIGINAL_BLOCKS.put(ownerId, saved);

        // Tạo sinh vật đặc biệt.
        Ravager beast = EntityType.RAVAGER.create(level);

        if (beast != null) {

            beast.moveTo(
                    center.getX() + 2.5D,
                    groundY + 1,
                    center.getZ() + 2.5D,
                    0.0F,
                    0.0F
            );

            beast.setCustomName(
                    Component.literal("§5Thú Thôn Phệ")
            );

            beast.setCustomNameVisible(true);
            beast.setPersistenceRequired();

            level.addFreshEntity(beast);

            SPECIAL_CREATURES.put(
                    ownerId,
                    beast.getUUID()
            );
        }

        DEVOUR_COUNT.put(ownerId, 0);

        player.sendSystemMessage(
                Component.literal(
                        "§5[Lĩnh Vực Tầng 7] §fNgôi làng thôn phệ đã xuất hiện!"
                )
        );

        return saved.size();
    }

    private static void placeBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState newState,
            Map<BlockPos, BlockState> saved
    ) {

        BlockState oldState = level.getBlockState(pos);

        // Không ghi đè công trình hoặc block có sẵn.
        if (!oldState.isAir()) {
            return;
        }

        saved.put(pos.immutable(), oldState);

        level.setBlock(
                pos,
                newState,
                3
        );
    }

    // Dọn làng và sinh vật do tầng 7 tạo ra.
    public static int stop(ServerPlayer player) {

        if (!isController(player)) {
            return 0;
        }

        UUID ownerId = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();

        Map<BlockPos, BlockState> saved =
                ORIGINAL_BLOCKS.remove(ownerId);

        int restored = 0;

        if (saved != null) {

            for (Map.Entry<BlockPos, BlockState> entry
                    : saved.entrySet()) {

                BlockPos pos = entry.getKey();

                BlockState current = level.getBlockState(pos);

                // Chỉ khôi phục nếu block vẫn là block do làng tạo.
                if (current.is(Blocks.STONE_BRICKS)
                        || current.is(Blocks.DEEPSLATE_BRICKS)
                        || current.is(Blocks.POLISHED_BLACKSTONE)) {

                    level.setBlock(
                            pos,
                            entry.getValue(),
                            3
                    );

                    restored++;
                }
            }
        }

        UUID beastId = SPECIAL_CREATURES.remove(ownerId);

        if (beastId != null) {

            Entity beast = level.getEntity(beastId);

            if (beast != null) {
                beast.discard();
            }
        }

        DEVOUR_COUNT.remove(ownerId);

        player.sendSystemMessage(
                Component.literal(
                        "§5[Lĩnh Vực Tầng 7] §fĐã dọn ngôi làng. Khôi phục "
                                + restored + " block."
                )
        );

        return restored;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isController(player)) {
            return;
        }

        UUID ownerId = player.getUUID();

        if (!FogDomain5.isActive(ownerId)
                || FogDomain5.getTier(ownerId) != 7) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (level.getGameTime() % TICK_INTERVAL != 0) {
            return;
        }

        Vec3 center = player.position();

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SUCTION_RADIUS)
        )) {

            if (target == player || !target.isAlive()) {
                continue;
            }

            if (target.getUUID().equals(
                    SPECIAL_CREATURES.get(ownerId))) {
                continue;
            }

            double distance = target.position().distanceTo(center);

            if (distance > SUCTION_RADIUS) {
                continue;
            }

            // Hút sinh vật về phía người điều khiển.
            Vec3 direction = center.subtract(target.position());

            if (direction.lengthSqr() > 0.0001D) {

                Vec3 pull = direction.normalize().scale(0.35D);

                target.setDeltaMovement(
                        target.getDeltaMovement().scale(0.35D).add(pull)
                );

                target.hurtMarked = true;
            }

            // Thôn phệ khi đã đến gần trung tâm.
            if (distance <= DEVOUR_DISTANCE) {
                devourCreature(player, target);
            }
        }
    }

    private static void devourCreature(
            ServerPlayer owner,
            LivingEntity creature
    ) {

        if (!creature.isAlive()) {
            return;
        }

        if (creature instanceof ServerPlayer) {
            return;
        }

        ServerLevel level = (ServerLevel) owner.level();

        FogDomainAbilityData data =
                FogDomainAbilityData.get(level.getServer());

        String entityTypeId =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(creature.getType())
                        .toString();

        // Chuyển sức mạnh và mở khóa năng lực sinh vật.
        data.addAbility(owner.getUUID(), entityTypeId);

        data.unlockCreatureSkills(
                owner.getUUID(),
                entityTypeId
        );

        data.transferCreaturePowers(
                owner.getUUID(),
                creature.getUUID()
        );

        int count = DEVOUR_COUNT.getOrDefault(
                owner.getUUID(),
                0
        ) + 1;

        DEVOUR_COUNT.put(owner.getUUID(), count);

        creature.discard();

        owner.sendSystemMessage(
                Component.literal(
                        "§5[Thôn Phệ] §fĐã nuốt: "
                                + entityTypeId
                                + " | Số lượng: "
                                + count
                )
        );
    }

    public static int getDevourCount(UUID ownerId) {
        return DEVOUR_COUNT.getOrDefault(ownerId, 0);
    }
}
