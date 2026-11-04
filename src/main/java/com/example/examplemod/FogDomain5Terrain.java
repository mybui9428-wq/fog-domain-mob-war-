
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

        int surfaceY = findSurface(level, playerPos);

        if (surfaceY == Integer.MIN_VALUE) {
            return 0;
        }

        Map<BlockPos, BlockState> saved = buildPool(
                level,
                playerPos.getX(),
                playerPos.getZ(),
                surfaceY
        );

        if (saved.isEmpty()) {
            return 0;
        }

        ORIGINAL.put(id, saved);

        POOLS.put(id, new PoolData(
                level,
                playerPos.getX(),
                playerPos.getZ(),
                surfaceY
        ));

        return saved.size();
    }

    private static Map<BlockPos, BlockState> buildPool(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY
    ) {
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

                    if (oldState.is(Blocks.BEDROCK)
                            || oldState.is(Blocks.BARRIER)) {
                        continue;
                    }

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

        return saved;
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

    private static int restoreBlocks(
            ServerLevel level,
            Map<BlockPos, BlockState> saved
    ) {
        int restored = 0;

        for (Map.Entry<BlockPos, BlockState> entry : saved.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            // Không ghi đè block đã bị người chơi thay đổi.
            if (!level.getBlockState(pos).is(Blocks.WATER)) {
                continue;
            }

            level.setBlock(pos, entry.getValue(), 3);
            restored++;
        }

        return restored;
    }

    public static int restorePool(ServerPlayer player) {
        UUID id = player.getUUID();

        Map<BlockPos, BlockState> saved = ORIGINAL.remove(id);
        PoolData pool = POOLS.remove(id);

        if (saved == null || pool == null) {
            return 0;
        }

        return restoreBlocks(pool.level(), saved);
    }

    private static void movePool(
            ServerPlayer player,
            PoolData oldPool
    ) {
        UUID id = player.getUUID();

        int newX = player.blockPosition().getX();
        int newZ = player.blockPosition().getZ();

        if (newX == oldPool.centerX()
                && newZ == oldPool.centerZ()) {
            return;
        }

        Map<BlockPos, BlockState> oldBlocks = ORIGINAL.get(id);

        if (oldBlocks == null) {
            return;
        }

        // Khôi phục vị trí hồ cũ trước.
        restoreBlocks(oldPool.level(), oldBlocks);

        // Tạo hồ mới theo vị trí hiện tại.
        Map<BlockPos, BlockState> newBlocks = buildPool(
                oldPool.level(),
                newX,
                newZ,
                oldPool.surfaceY()
        );

        ORIGINAL.put(id, newBlocks);

        POOLS.put(id, new PoolData(
                oldPool.level(),
                newX,
                newZ,
                oldPool.surfaceY()
        ));
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

        UUID id = player.getUUID();
        PoolData pool = POOLS.get(id);

        if (pool == null || pool.level() != player.level()) {
            return;
        }

        // Di chuyển hồ theo người điều khiển.
        movePool(player, pool);

        pool = POOLS.get(id);

        if (pool == null) {
            return;
        }

        double dx = player.getX() - pool.centerX();
        double dz = player.getZ() - pool.centerZ();

        if (dx * dx + dz * dz > RADIUS * RADIUS) {
            return;
        }

        double targetY = pool.surfaceY() + 1.0D;

        // Cứu người điều khiển nếu bị chìm dưới mặt hồ.
        if (player.getY() < targetY
                && player.getY() > pool.surfaceY() - 8.0D) {

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
