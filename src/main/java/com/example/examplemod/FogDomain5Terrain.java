
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Terrain {

    private static final int RADIUS = 30;
    private static final int DEPTH = 3;
    private static final int OPEN_SKY_CHECK = 8;

    private static final Map<UUID, Map<BlockPos, BlockState>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    private record PoolData(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY
    ) {
    }

    private FogDomain5Terrain() {
    }

    public static int createPool(ServerPlayer player) {
        UUID id = player.getUUID();

        if (ORIGINAL.containsKey(id)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        int centerX = playerPos.getX();
        int centerZ = playerPos.getZ();

        int surfaceY = findSurface(level, playerPos);

        if (surfaceY == Integer.MIN_VALUE) {
            return 0;
        }

        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        int radiusSquared = RADIUS * RADIUS;

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                if (x * x + z * z > radiusSquared) {
                    continue;
                }

                for (int depth = 0; depth < DEPTH; depth++) {

                    BlockPos pos = new BlockPos(
                            centerX + x,
                            surfaceY - depth,
                            centerZ + z
                    );

                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    if (level.getBlockEntity(pos) != null) {
                        continue;
                    }

                    // Không phá bedrock hoặc barrier.
                    if (oldState.is(Blocks.BEDROCK)
                            || oldState.is(Blocks.BARRIER)) {
                        continue;
                    }

                    // Không thay block không thể phá.
                    if (oldState.getDestroySpeed(level, pos) < 0) {
                        continue;
                    }

                    saved.put(pos.immutable(), oldState);

                    level.setBlock(
                            pos,
                            Blocks.WATER.defaultBlockState(),
                            3
                    );
                }
            }
        }

        if (saved.isEmpty()) {
            return 0;
        }

        ORIGINAL.put(id, saved);
        POOLS.put(
                id,
                new PoolData(level, centerX, centerZ, surfaceY)
        );

        return saved.size();
    }

    private static int findSurface(
            ServerLevel level,
            BlockPos start
    ) {
        int startY = start.getY();

        for (int y = startY + 2; y >= startY - 5; y--) {

            BlockPos ground = new BlockPos(
                    start.getX(),
                    y,
                    start.getZ()
            );

            if (!level.hasChunkAt(ground)) {
                continue;
            }

            BlockState state = level.getBlockState(ground);

            if (!state.isSolid()
                    || !state.getFluidState().isEmpty()) {
                continue;
            }

            boolean open = true;

            for (int h = 1; h <= OPEN_SKY_CHECK; h++) {
                BlockPos above = ground.above(h);

                if (!level.hasChunkAt(above)
                        || !level.getBlockState(above).isAir()) {
                    open = false;
                    break;
                }
            }

            if (open) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }

    public static int restorePool(ServerPlayer player) {
        UUID id = player.getUUID();

        Map<BlockPos, BlockState> saved = ORIGINAL.remove(id);
        POOLS.remove(id);

        if (saved == null) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        int restored = 0;

        for (Map.Entry<BlockPos, BlockState> entry : saved.entrySet()) {

            BlockPos pos = entry.getKey();

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            // Chỉ khôi phục vị trí vẫn là nước do hồ tạo ra.
            if (!level.getBlockState(pos).is(Blocks.WATER)) {
                continue;
            }

            level.setBlock(
                    pos,
                    entry.getValue(),
                    3
            );

            restored++;
        }

        return restored;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getName().getString()
                .equalsIgnoreCase("tubi2531")) {
            return;
        }

        PoolData pool = POOLS.get(player.getUUID());

        if (pool == null || pool.level() != player.level()) {
            return;
        }

        double dx = player.getX() - pool.centerX();
        double dz = player.getZ() - pool.centerZ();

        if (dx * dx + dz * dz > RADIUS * RADIUS) {
            return;
        }

        BlockPos waterPos = new BlockPos(
                player.blockPosition().getX(),
                pool.surfaceY(),
                player.blockPosition().getZ()
        );

        if (!pool.level().hasChunkAt(waterPos)
                || !pool.level().getBlockState(waterPos)
                        .is(Blocks.WATER)) {
            return;
        }

        // Giữ người điều khiển trên mặt hồ.
        double targetY = pool.surfaceY() + 1.0D;

        if (player.getY() < targetY
                && player.getY() > pool.surfaceY() - 0.5D) {

            player.setPos(
                    player.getX(),
                    targetY,
                    player.getZ()
            );

            Vec3 motion = player.getDeltaMovement();

            player.setDeltaMovement(
                    motion.x,
                    Math.max(0.0D, motion.y),
                    motion.z
            );

            player.fallDistance = 0.0F;
        }
    }
}
