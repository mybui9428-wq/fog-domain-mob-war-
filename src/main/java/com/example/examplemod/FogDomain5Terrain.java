
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

    // Di chuyển hồ sau mỗi 3 block.
    private static final int MOVE_DISTANCE = 3;

    private static final Map<UUID, Map<BlockPos, BlockState>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    // Lưu nước bị thay bằng Barrier dưới chân.
    private static final Map<UUID, BlockPos> FOOT_POSITIONS =
            new HashMap<>();

    private static final Map<UUID, BlockState> FOOT_ORIGINAL =
            new HashMap<>();

    private record PoolData(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY
    ) {}

    private FogDomain5Terrain() {}

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    public static boolean isReady(UUID id) {
        return ORIGINAL.containsKey(id)
                && POOLS.containsKey(id);
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

                    if (level.getBlockEntity(pos) != null
                            || oldState.is(Blocks.BEDROCK)
                            || oldState.is(Blocks.BARRIER)
                            || oldState.getDestroySpeed(level, pos) < 0) {
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
        for (int y = start.getY() + 2;
             y >= start.getY() - 5;
             y--) {

            BlockPos ground = new BlockPos(
                    start.getX(), y, start.getZ()
            );

            if (!level.hasChunkAt(ground)) {
                continue;
            }

            BlockState state = level.getBlockState(ground);

            if (!state.isSolid()
                    || !state.getFluidState().isEmpty()
                    || state.is(Blocks.BEDROCK)
                    || state.is(Blocks.BARRIER)) {
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

            BlockState current = level.getBlockState(pos);

            if (!current.is(Blocks.WATER)
                    && !current.is(Blocks.BARRIER)) {
                continue;
            }

            level.setBlock(pos, entry.getValue(), 3);
            restored++;
        }

        return restored;
    }

    private static void removeFootBlock(UUID id, ServerLevel level) {
        BlockPos pos = FOOT_POSITIONS.remove(id);
        BlockState original = FOOT_ORIGINAL.remove(id);

        if (pos == null || original == null
                || !level.hasChunkAt(pos)) {
            return;
        }

        if (level.getBlockState(pos).is(Blocks.BARRIER)) {
            level.setBlock(pos, original, 3);
        }
    }

    private static void updateFootBlock(
            UUID id,
            ServerPlayer player,
            PoolData pool
    ) {
        ServerLevel level = (ServerLevel) player.level();

        // Chỉ đặt Barrier ở lớp nước trên cùng.
        BlockPos newPos = player.blockPosition().below();

        if (newPos.getY() != pool.surfaceY()) {
            removeFootBlock(id, level);
            return;
        }

        if (newPos.equals(FOOT_POSITIONS.get(id))) {
            return;
        }

        removeFootBlock(id, level);

        int dx = newPos.getX() - pool.centerX();
        int dz = newPos.getZ() - pool.centerZ();

        if (dx * dx + dz * dz > RADIUS * RADIUS) {
            return;
        }

        if (!level.hasChunkAt(newPos)) {
            return;
        }

        BlockState oldState = level.getBlockState(newPos);

        if (!oldState.is(Blocks.WATER)) {
            return;
        }

        FOOT_ORIGINAL.put(id, oldState);

        level.setBlock(
                newPos,
                Blocks.BARRIER.defaultBlockState(),
                3
        );

        FOOT_POSITIONS.put(id, newPos.immutable());
    }

    private static void movePool(
            UUID id,
            ServerPlayer player,
            PoolData oldPool
    ) {
        int newX = player.blockPosition().getX();
        int newZ = player.blockPosition().getZ();

        int dx = newX - oldPool.centerX();
        int dz = newZ - oldPool.centerZ();

        if (dx * dx + dz * dz
                < MOVE_DISTANCE * MOVE_DISTANCE) {
            return;
        }

        Map<BlockPos, BlockState> oldBlocks = ORIGINAL.get(id);

        if (oldBlocks == null) {
            return;
        }

        ServerLevel level = oldPool.level();

        // Gỡ Barrier dưới chân trước khi khôi phục hồ.
        removeFootBlock(id, level);

        // Khôi phục hồ cũ.
        restoreBlocks(level, oldBlocks);
        oldBlocks.clear();

        // Tìm mặt đất tại vị trí mới.
        int newSurfaceY = findSurface(
                level,
                new BlockPos(
                        newX,
                        player.blockPosition().getY(),
                        newZ
                )
        );

        if (newSurfaceY == Integer.MIN_VALUE) {
            ORIGINAL.remove(id);
            POOLS.remove(id);
            return;
        }

        // Tạo hồ mới.
        Map<BlockPos, BlockState> newBlocks = buildPool(
                level,
                newX,
                newZ,
                newSurfaceY
        );

        if (newBlocks.isEmpty()) {
            ORIGINAL.remove(id);
            POOLS.remove(id);
            return;
        }

        ORIGINAL.put(id, newBlocks);

        POOLS.put(id, new PoolData(
                level,
                newX,
                newZ,
                newSurfaceY
        ));
    }

    public static int restorePool(ServerPlayer player) {
        UUID id = player.getUUID();

        PoolData pool = POOLS.remove(id);
        Map<BlockPos, BlockState> saved = ORIGINAL.remove(id);

        removeFootBlock(id, (ServerLevel) player.level());

        if (pool == null || saved == null) {
            return 0;
        }

        return restoreBlocks(pool.level(), saved);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isController(player)) {
            return;
        }

        UUID id = player.getUUID();

        if (!FogDomain5.isActive(id)) {
            return;
        }

        PoolData pool = POOLS.get(id);

        if (pool == null || pool.level() != player.level()) {
            return;
        }

        movePool(id, player, pool);

        pool = POOLS.get(id);

        if (pool == null) {
            return;
        }

        updateFootBlock(id, player, pool);

        player.fallDistance = 0.0F;

        Vec3 motion = player.getDeltaMovement();

        if (player.getY() < pool.surfaceY() + 1.0D
                && player.getY() > pool.surfaceY() - 2.0D) {
            player.setDeltaMovement(
                    motion.x,
                    Math.max(0.0D, motion.y),
                    motion.z
            );
        }
    }
}
