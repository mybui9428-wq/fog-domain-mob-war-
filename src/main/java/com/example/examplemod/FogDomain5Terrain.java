
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
    private static final int FIRST_DEPTH = 3;
    private static final int SECOND_DEPTH = 15;

    // Chỉ cập nhật hồ sau khi đi xa 20 block.
    private static final int MOVE_DISTANCE = 20;
    private static final int MAX_CHANGES_PER_TICK = 100;

    private static final Map<UUID, Map<BlockPos, BlockState>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    private static final Map<UUID, PoolBuild> BUILDS =
            new HashMap<>();

    // Block đỡ tàng hình tạm thời dưới chân người chơi.
    private static final Map<UUID, Map<BlockPos, BlockState>> FOOT_BLOCKS =
            new HashMap<>();

    private static final Map<UUID, BlockPos> LAST_FOOT_BLOCK =
            new HashMap<>();

    private record PoolData(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY,
            int depth,
            boolean secondPool
    ) {}

    private static final class PoolBuild {
        private final ServerLevel level;
        private final int centerX;
        private final int centerZ;
        private final int surfaceY;
        private final int depth;

        private final Map<BlockPos, BlockState> saved =
                new LinkedHashMap<>();

        private int x = -RADIUS;
        private int z = -RADIUS;
        private int y = 0;

        private PoolBuild(
                ServerLevel level,
                int centerX,
                int centerZ,
                int surfaceY,
                int depth
        ) {
            this.level = level;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.surfaceY = surfaceY;
            this.depth = depth;
        }
    }

    private FogDomain5Terrain() {}

    public static int createPool(ServerPlayer player) {
        UUID id = player.getUUID();

        if (ORIGINAL.containsKey(id) || BUILDS.containsKey(id)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        int groundY = findSurface(level, playerPos);

        if (groundY == Integer.MIN_VALUE) {
            return 0;
        }

        // Mặt nước nằm cao hơn mặt đất một block.
        int waterY = groundY + 1;

        PoolBuild build = new PoolBuild(
                level,
                playerPos.getX(),
                playerPos.getZ(),
                waterY,
                FIRST_DEPTH
        );

        BUILDS.put(id, build);
        ORIGINAL.put(id, build.saved);

        POOLS.put(id, new PoolData(
                level,
                playerPos.getX(),
                playerPos.getZ(),
                waterY,
                FIRST_DEPTH,
                false
        ));

        return 1;
    }

    private static int findSurface(
            ServerLevel level,
            BlockPos start
    ) {
        for (int y = start.getY() + 2;
             y >= start.getY() - 5;
             y--) {

            BlockPos pos = new BlockPos(
                    start.getX(), y, start.getZ()
            );

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.isSolid()
                    || !state.getFluidState().isEmpty()) {
                continue;
            }

            boolean open = true;

            for (int h = 1; h <= 8; h++) {
                BlockPos above = pos.above(h);

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

    private static void processBuild(
            UUID id,
            PoolBuild build
    ) {
        int changed = 0;
        int radiusSquared = RADIUS * RADIUS;

        while (changed < MAX_CHANGES_PER_TICK) {

            if (build.x > RADIUS) {
                finishBuild(id, build);
                return;
            }

            if (build.z > RADIUS) {
                build.z = -RADIUS;
                build.x++;
                continue;
            }

            if (build.x * build.x + build.z * build.z
                    > radiusSquared) {
                build.z++;
                continue;
            }

            BlockPos pos;

            // Các lớp nước ở trên, Barrier ở đáy hồ.
            if (build.y < build.depth) {
                pos = new BlockPos(
                        build.centerX + build.x,
                        build.surfaceY - build.y,
                        build.centerZ + build.z
                );
            } else {
                pos = new BlockPos(
                        build.centerX + build.x,
                        build.surfaceY - build.depth,
                        build.centerZ + build.z
                );
            }

            if (!build.level.hasChunkAt(pos)) {
                advanceBuild(build);
                continue;
            }

            BlockState oldState = build.level.getBlockState(pos);

            if (build.level.getBlockEntity(pos) != null
                    || oldState.is(Blocks.BEDROCK)
                    || oldState.is(Blocks.BARRIER)
                    || oldState.getDestroySpeed(build.level, pos) < 0) {
                advanceBuild(build);
                continue;
            }

            build.saved.put(pos.immutable(), oldState);

            BlockState replacement = build.y < build.depth
                    ? Blocks.WATER.defaultBlockState()
                    : Blocks.BARRIER.defaultBlockState();

            build.level.setBlock(pos, replacement, 3);

            changed++;
            advanceBuild(build);
        }
    }

    private static void advanceBuild(PoolBuild build) {
        build.z++;

        if (build.z > RADIUS) {
            build.z = -RADIUS;
            build.x++;
        }

        if (build.x > RADIUS) {
            build.y++;

            if (build.y > build.depth) {
                build.x = RADIUS + 1;
            } else {
                build.x = -RADIUS;
                build.z = -RADIUS;
            }
        }
    }

    private static void finishBuild(UUID id, PoolBuild build) {
        BUILDS.remove(id);

        POOLS.put(id, new PoolData(
                build.level,
                build.centerX,
                build.centerZ,
                build.surfaceY,
                build.depth,
                build.depth == SECOND_DEPTH
        ));
    }

    private static int restoreBlocks(
            ServerLevel level,
            Map<BlockPos, BlockState> saved
    ) {
        int restored = 0;

        for (Map.Entry<BlockPos, BlockState> entry
                : saved.entrySet()) {

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

    private static void removeFootBlock(
            UUID id,
            ServerLevel level
    ) {
        BlockPos oldPos = LAST_FOOT_BLOCK.remove(id);

        if (oldPos == null) {
            return;
        }

        Map<BlockPos, BlockState> saved = FOOT_BLOCKS.get(id);

        if (saved == null) {
            return;
        }

        BlockState original = saved.remove(oldPos);

        if (original != null
                && level.hasChunkAt(oldPos)
                && level.getBlockState(oldPos).is(Blocks.BARRIER)) {
            level.setBlock(oldPos, original, 3);
        }

        if (saved.isEmpty()) {
            FOOT_BLOCKS.remove(id);
        }
    }

    private static void updateFootBlock(
            UUID id,
            ServerPlayer player
    ) {
        ServerLevel level = (ServerLevel) player.level();

        BlockPos newPos = player.blockPosition().below();

        BlockPos previous = LAST_FOOT_BLOCK.get(id);

        if (newPos.equals(previous)) {
            return;
        }

        removeFootBlock(id, level);

        if (!level.hasChunkAt(newPos)) {
            return;
        }

        BlockState oldState = level.getBlockState(newPos);

        if (!oldState.isAir()
                && !oldState.getFluidState().isEmpty()) {
            return;
        }

        FOOT_BLOCKS.computeIfAbsent(
                id,
                key -> new LinkedHashMap<>()
        ).put(newPos.immutable(), oldState);

        level.setBlock(
                newPos,
                Blocks.BARRIER.defaultBlockState(),
                3
        );

        LAST_FOOT_BLOCK.put(id, newPos.immutable());
    }

    public static int restorePool(ServerPlayer player) {
        UUID id = player.getUUID();

        BUILDS.remove(id);

        removeFootBlock(id, (ServerLevel) player.level());

        Map<BlockPos, BlockState> footSaved = FOOT_BLOCKS.remove(id);

        if (footSaved != null) {
            restoreBlocks((ServerLevel) player.level(), footSaved);
        }

        LAST_FOOT_BLOCK.remove(id);

        Map<BlockPos, BlockState> saved = ORIGINAL.remove(id);
        PoolData pool = POOLS.remove(id);

        if (saved == null || pool == null) {
            return 0;
        }

        return restoreBlocks(pool.level(), saved);
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

        restoreBlocks(oldPool.level(), oldBlocks);
        oldBlocks.clear();

        int groundY = findSurface(
                oldPool.level(),
                new BlockPos(newX, player.blockPosition().getY(), newZ)
        );

        if (groundY == Integer.MIN_VALUE) {
            return;
        }

        PoolBuild next = new PoolBuild(
                oldPool.level(),
                newX,
                newZ,
                groundY + 1,
                oldPool.depth()
        );

        ORIGINAL.put(id, next.saved);
        BUILDS.put(id, next);
    }

    private static void createSecondPool(
            UUID id,
            ServerPlayer player,
            PoolData oldPool
    ) {
        Map<BlockPos, BlockState> oldBlocks = ORIGINAL.get(id);

        if (oldBlocks != null) {
            restoreBlocks(oldPool.level(), oldBlocks);
            oldBlocks.clear();
        }

        PoolBuild next = new PoolBuild(
                oldPool.level(),
                player.blockPosition().getX(),
                player.blockPosition().getZ(),
                oldPool.surfaceY(),
                SECOND_DEPTH
        );

        ORIGINAL.put(id, next.saved);
        BUILDS.put(id, next);

        POOLS.put(id, new PoolData(
                oldPool.level(),
                next.centerX,
                next.centerZ,
                next.surfaceY,
                SECOND_DEPTH,
                true
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

        if (!FogDomain5.isActive(id)) {
            return;
        }

        PoolBuild build = BUILDS.get(id);

        if (build != null) {
            processBuild(id, build);
            return;
        }

        PoolData pool = POOLS.get(id);

        if (pool == null || pool.level() != player.level()) {
            return;
        }

        if (!pool.secondPool()
                && player.getY() <= pool.surfaceY() - FIRST_DEPTH) {
            createSecondPool(id, player, pool);
            return;
        }

        movePool(id, player, pool);

        updateFootBlock(id, player);

        Vec3 motion = player.getDeltaMovement();

        player.fallDistance = 0.0F;
    }
}
