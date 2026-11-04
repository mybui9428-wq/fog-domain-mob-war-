
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

    private static final Map<UUID, Map<BlockPos, BlockState>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    private record PoolData(
            ServerLevel level,
            int centerX,
            int centerZ,
            int waterY
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

        int centerX = player.blockPosition().getX();
        int centerZ = player.blockPosition().getZ();

        int groundY = findGround(level, player.blockPosition());

        if (groundY == Integer.MIN_VALUE) {
            return 0;
        }

        int waterY = groundY + 1;

        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                if (x * x + z * z > RADIUS * RADIUS) {
                    continue;
                }

                BlockPos ground = new BlockPos(
                        centerX + x,
                        groundY,
                        centerZ + z
                );

                BlockPos water = ground.above();

                if (!level.hasChunkAt(ground)
                        || !level.hasChunkAt(water)) {
                    continue;
                }

                BlockState groundState = level.getBlockState(ground);
                BlockState oldWaterState = level.getBlockState(water);

                // Chỉ phủ trên nền cứng, không đào hoặc thay nền.
                if (!groundState.isSolid()
                        || !groundState.getFluidState().isEmpty()) {
                    continue;
                }

                // Chỉ đặt nước vào không gian trống hoặc nước có sẵn.
                if (!oldWaterState.isAir()
                        && !oldWaterState.is(Blocks.WATER)) {
                    continue;
                }

                if (level.getBlockEntity(water) != null) {
                    continue;
                }

                saved.put(water.immutable(), oldWaterState);

                // Đặt nước nguồn tại mặt đất.
                level.setBlock(
                        water,
                        Blocks.WATER.defaultBlockState(),
                        3
                );
            }
        }

        if (saved.isEmpty()) {
            return 0;
        }

        ORIGINAL.put(id, saved);
        POOLS.put(
                id,
                new PoolData(level, centerX, centerZ, waterY)
        );

        return saved.size();
    }

    private static int findGround(
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

            if (state.isSolid()
                    && state.getFluidState().isEmpty()
                    && level.getBlockState(ground.above()).isAir()) {
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

            // Chỉ khôi phục nếu vị trí vẫn là nước của hồ.
            if (!level.getBlockState(pos).is(Blocks.WATER)) {
                continue;
            }

            level.setBlock(pos, entry.getValue(), 3);
            restored++;
        }

        return restored;
    }

    public static boolean isOverPool(ServerPlayer player) {
        PoolData pool = POOLS.get(player.getUUID());

        if (pool == null || pool.level() != player.level()) {
            return false;
        }

        double dx = player.getX() - pool.centerX();
        double dz = player.getZ() - pool.centerZ();

        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getName().getString().equalsIgnoreCase("tubi2531")) {
            return;
        }

        PoolData pool = POOLS.get(player.getUUID());

        if (pool == null || pool.level() != player.level()) {
            return;
        }

        if (!isOverPool(player)) {
            return;
        }

        double dx = player.getX() - pool.centerX();
        double dz = player.getZ() - pool.centerZ();

        BlockPos under = BlockPos.containing(
                player.getX(),
                pool.waterY(),
                player.getZ()
        );

        if (!pool.level().hasChunkAt(under)
                || !pool.level().getBlockState(under).is(Blocks.WATER)) {
            return;
        }

        // Giữ người điều khiển đứng trên mặt nước.
        if (player.getY() <= pool.waterY() + 1.05
                && player.getY() >= pool.waterY() - 0.2) {

            Vec3 motion = player.getDeltaMovement();

            player.setDeltaMovement(
                    motion.x,
                    Math.max(0.0D, motion.y),
                    motion.z
            );

            player.fallDistance = 0.0F;

            if (player.getY() < pool.waterY() + 1.0) {
                player.setPos(
                        player.getX(),
                        pool.waterY() + 1.0,
                        player.getZ()
                );
            }
        }
    }
}
