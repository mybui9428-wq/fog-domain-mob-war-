
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Terrain {

    private static final int RADIUS = 30;
    private static final int FIRST_DEPTH = 3;
    private static final int SECOND_DEPTH = 15;

    private static final int CLEAR_HEIGHT = 30;
    private static final int MOVE_DISTANCE = 20;

    // Giới hạn xử lý để giảm lag.
    private static final int MAX_POSITIONS_PER_TICK = 300;
    private static final int MAX_CHANGES_PER_TICK = 100;

    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS
                    | Block.UPDATE_NEIGHBORS
                    | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                    | Block.UPDATE_SUPPRESS_DROPS;

    private static final Map<UUID, Map<BlockPos, SavedBlock>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    private static final Map<UUID, PoolBuild> BUILDS =
            new HashMap<>();

    private static final Map<UUID, RestoreTask> RESTORES =
            new HashMap<>();

    private static final Map<UUID, Map<BlockPos, BlockState>> FOOT_BLOCKS =
            new HashMap<>();

    private static final Map<UUID, BlockPos> LAST_FOOT_BLOCK =
            new HashMap<>();

    private record SavedBlock(
            BlockState state,
            CompoundTag blockEntityData
    ) {}

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
        private final boolean secondPool;

        private final Map<BlockPos, SavedBlock> saved =
                new LinkedHashMap<>();

        // 0: lưu dữ liệu và xóa cây cỏ.
        // 1: xóa các block còn lại.
        // 2: tạo hồ nước và Barrier.
        private int stage = 0;

        private int cursor = 0;

        private PoolBuild(
                ServerLevel level,
                int centerX,
                int centerZ,
                int surfaceY,
                int depth,
                boolean secondPool
        ) {
            this.level = level;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.surfaceY = surfaceY;
            this.depth = depth;
            this.secondPool = secondPool;
        }
    }

    private static final class RestoreTask {

        private final ServerLevel level;
        private final Iterator<Map.Entry<BlockPos, SavedBlock>> iterator;

        private final boolean createNextPool;
        private final int nextX;
        private final int nextZ;
        private final int nextSurfaceY;
        private final int nextDepth;
        private final boolean nextSecondPool;

        private RestoreTask(
                ServerLevel level,
                Map<BlockPos, SavedBlock> saved,
                boolean createNextPool,
                int nextX,
                int nextZ,
                int nextSurfaceY,
                int nextDepth,
                boolean nextSecondPool
        ) {
            this.level = level;
            this.iterator = new ArrayList<>(saved.entrySet()).iterator();

            this.createNextPool = createNextPool;
            this.nextX = nextX;
            this.nextZ = nextZ;
            this.nextSurfaceY = nextSurfaceY;
            this.nextDepth = nextDepth;
            this.nextSecondPool = nextSecondPool;
        }
    }

    private FogDomain5Terrain() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static int getMinY(int surfaceY) {
        return surfaceY - SECOND_DEPTH;
    }

    private static int getMaxY(int surfaceY) {
        return surfaceY + CLEAR_HEIGHT;
    }

    private static int getColumnCount() {
        return (RADIUS * 2 + 1) * (RADIUS * 2 + 1);
    }

    private static int getLayerCount(int surfaceY) {
        return getMaxY(surfaceY) - getMinY(surfaceY) + 1;
    }

    private static boolean insideCircle(int x, int z) {
        return x * x + z * z <= RADIUS * RADIUS;
    }

    private static boolean isBoundary(int x, int z) {
        return !insideCircle(x + 1, z)
                || !insideCircle(x - 1, z)
                || !insideCircle(x, z + 1)
                || !insideCircle(x, z - 1);
    }

    public static int createPool(ServerPlayer player) {

        UUID id = player.getUUID();

        if (ORIGINAL.containsKey(id)
                || BUILDS.containsKey(id)
                || RESTORES.containsKey(id)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        int groundY = findSurface(
                level,
                player.blockPosition()
        );

        if (groundY == Integer.MIN_VALUE) {
            return 0;
        }

        // Không cộng thêm 1 block.
        startBuild(
                id,
                level,
                player.blockPosition().getX(),
                player.blockPosition().getZ(),
                groundY,
                FIRST_DEPTH,
                false
        );

        return 1;
    }

    private static void startBuild(
            UUID id,
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY,
            int depth,
            boolean secondPool
    ) {

        PoolBuild build = new PoolBuild(
                level,
                centerX,
                centerZ,
                surfaceY,
                depth,
                secondPool
        );

        BUILDS.put(id, build);
        ORIGINAL.put(id, build.saved);
    }

    private static int findSurface(
            ServerLevel level,
            BlockPos start
    ) {

        for (int y = start.getY() + 1;
             y >= start.getY() - 8;
             y--) {

            BlockPos pos = new BlockPos(
                    start.getX(),
                    y,
                    start.getZ()
            );

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.isSolid()
                    || !state.getFluidState().isEmpty()
                    || state.is(Blocks.BEDROCK)
                    || state.is(Blocks.BARRIER)) {
                continue;
            }

            return y;
        }

        return Integer.MIN_VALUE;
    }

    private static void processBuild(
            UUID id,
            PoolBuild build
    ) {

        int positions = 0;
        int changes = 0;

        int columns = getColumnCount();
        int layers = getLayerCount(build.surfaceY);

        int totalScanPositions = columns * layers;

        while (positions < MAX_POSITIONS_PER_TICK
                && changes < MAX_CHANGES_PER_TICK) {

            if (build.stage == 0 || build.stage == 1) {

                if (build.cursor >= totalScanPositions) {

                    build.cursor = 0;
                    build.stage++;

                    continue;
                }

                int layerIndex = build.cursor / columns;
                int columnIndex = build.cursor % columns;

                build.cursor++;
                positions++;

                int x = columnIndex / (RADIUS * 2 + 1) - RADIUS;
                int z = columnIndex % (RADIUS * 2 + 1) - RADIUS;

                if (!insideCircle(x, z)) {
                    continue;
                }

                int y = getMinY(build.surfaceY) + layerIndex;

                BlockPos pos = new BlockPos(
                        build.centerX + x,
                        y,
                        build.centerZ + z
                );

                if (!build.level.hasChunkAt(pos)) {
                    continue;
                }

                BlockState state = build.level.getBlockState(pos);

                if (build.stage == 0) {

                    CompoundTag blockEntityData = null;

                    BlockEntity blockEntity =
                            build.level.getBlockEntity(pos);

                    if (blockEntity != null) {
                        blockEntityData =
                                blockEntity.saveWithFullMetadata(
                                        build.level.registryAccess()
                                ).copy();
                    }

                    build.saved.put(
                            pos.immutable(),
                            new SavedBlock(state, blockEntityData)
                    );

                    // Xóa cây cỏ, hoa và các block có thể thay thế trước.
                    if (!state.isAir()
                            && state.canBeReplaced()
                            && !state.is(Blocks.BEDROCK)) {

                        build.level.setBlock(
                                pos,
                                Blocks.AIR.defaultBlockState(),
                                UPDATE_FLAGS
                        );

                        changes++;
                    }

                } else {

                    // Xóa công trình và các block còn lại.
                    if (!state.isAir()
                            && !state.is(Blocks.BEDROCK)) {

                        build.level.setBlock(
                                pos,
                                Blocks.AIR.defaultBlockState(),
                                UPDATE_FLAGS
                        );

                        changes++;
                    }
                }

                continue;
            }

            if (build.stage == 2) {

                if (build.cursor >= columns) {
                    finishBuild(id, build);
                    return;
                }

                int columnIndex = build.cursor++;

                int x = columnIndex / (RADIUS * 2 + 1) - RADIUS;
                int z = columnIndex % (RADIUS * 2 + 1) - RADIUS;

                positions++;

                if (!insideCircle(x, z)) {
                    continue;
                }

                if (changes + build.depth + 1 > MAX_CHANGES_PER_TICK) {
                    build.cursor--;
                    return;
                }

                int worldX = build.centerX + x;
                int worldZ = build.centerZ + z;

                boolean boundary = isBoundary(x, z);

                for (int y = 0; y < build.depth; y++) {

                    BlockPos pos = new BlockPos(
                            worldX,
                            build.surfaceY - y,
                            worldZ
                    );

                    if (!build.level.hasChunkAt(pos)) {
                        continue;
                    }

                    BlockState replacement = boundary
                            ? Blocks.BARRIER.defaultBlockState()
                            : Blocks.WATER.defaultBlockState();

                    build.level.setBlock(
                            pos,
                            replacement,
                            UPDATE_FLAGS
                    );

                    changes++;
                }

                // Sàn Barrier dưới đáy hồ.
                BlockPos floor = new BlockPos(
                        worldX,
                        build.surfaceY - build.depth,
                        worldZ
                );

                if (build.level.hasChunkAt(floor)) {

                    build.level.setBlock(
                            floor,
                            Blocks.BARRIER.defaultBlockState(),
                            UPDATE_FLAGS
                    );

                    changes++;
                }
            }
        }
    }

    private static void finishBuild(
            UUID id,
            PoolBuild build
    ) {

        BUILDS.remove(id);

        POOLS.put(id, new PoolData(
                build.level,
                build.centerX,
                build.centerZ,
                build.surfaceY,
                build.depth,
                build.secondPool
        ));
    }

    private static void restoreBlock(
            ServerLevel level,
            BlockPos pos,
            SavedBlock saved
    ) {

        if (!level.hasChunkAt(pos)) {
            return;
        }

        BlockState current = level.getBlockState(pos);

        // Không ghi đè block mới do người chơi đặt vào.
        if (!current.isAir()
                && !current.is(Blocks.WATER)
                && !current.is(Blocks.BARRIER)) {
            return;
        }

        level.setBlock(
                pos,
                saved.state(),
                UPDATE_FLAGS
        );

        if (saved.blockEntityData() != null) {

            BlockEntity restored = BlockEntity.loadStatic(
                    pos,
                    saved.state(),
                    saved.blockEntityData(),
                    level.registryAccess()
            );

            if (restored != null) {
                level.setBlockEntity(restored);
                restored.setChanged();
            }
        }
    }

    private static void startRestore(
            UUID id,
            ServerLevel level,
            Map<BlockPos, SavedBlock> saved,
            boolean createNextPool,
            int nextX,
            int nextZ,
            int nextSurfaceY,
            int nextDepth,
            boolean nextSecondPool
    ) {

        if (saved == null) {
            return;
        }

        RESTORES.put(
                id,
                new RestoreTask(
                        level,
                        saved,
                        createNextPool,
                        nextX,
                        nextZ,
                        nextSurfaceY,
                        nextDepth,
                        nextSecondPool
                )
        );
    }

    private static void processRestore(UUID id, RestoreTask task) {

        int processed = 0;

        while (processed < MAX_POSITIONS_PER_TICK
                && task.iterator.hasNext()) {

            Map.Entry<BlockPos, SavedBlock> entry =
                    task.iterator.next();

            restoreBlock(
                    task.level,
                    entry.getKey(),
                    entry.getValue()
            );

            processed++;
        }

        if (task.iterator.hasNext()) {
            return;
        }

        RESTORES.remove(id);
        ORIGINAL.remove(id);
        POOLS.remove(id);

        if (task.createNextPool
                && FogDomain5.isActive(id)) {

            startBuild(
                    id,
                    task.level,
                    task.nextX,
                    task.nextZ,
                    task.nextSurfaceY,
                    task.nextDepth,
                    task.nextSecondPool
            );
        }
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

            level.setBlock(
                    oldPos,
                    original,
                    UPDATE_FLAGS
            );
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

        if (newPos.equals(LAST_FOOT_BLOCK.get(id))) {
            return;
        }

        removeFootBlock(id, level);

        if (!level.hasChunkAt(newPos)) {
            return;
        }

        BlockState oldState = level.getBlockState(newPos);

        // Chỉ đặt Barrier thay cho nước hoặc không khí.
        if (!oldState.isAir()
                && !oldState.is(Blocks.WATER)) {
            return;
        }

        FOOT_BLOCKS.computeIfAbsent(
                id,
                key -> new LinkedHashMap<>()
        ).put(newPos.immutable(), oldState);

        level.setBlock(
                newPos,
                Blocks.BARRIER.defaultBlockState(),
                UPDATE_FLAGS
        );

        LAST_FOOT_BLOCK.put(id, newPos.immutable());
    }

    public static int restorePool(ServerPlayer player) {

        UUID id = player.getUUID();

        PoolBuild build = BUILDS.remove(id);

        removeFootBlock(
                id,
                (ServerLevel) player.level()
        );

        Map<BlockPos, BlockState> footSaved =
                FOOT_BLOCKS.remove(id);

        LAST_FOOT_BLOCK.remove(id);

        if (footSaved != null) {
            for (Map.Entry<BlockPos, BlockState> entry
                    : footSaved.entrySet()) {

                BlockPos pos = entry.getKey();

                if (player.level().hasChunkAt(pos)
                        && player.level().getBlockState(pos)
                        .is(Blocks.BARRIER)) {

                    player.level().setBlock(
                            pos,
                            entry.getValue(),
                            UPDATE_FLAGS
                    );
                }
            }
        }

        Map<BlockPos, SavedBlock> saved = ORIGINAL.get(id);

        if (saved == null) {
            if (build != null) {
                ORIGINAL.put(id, build.saved);
                saved = build.saved;
            } else {
                return 0;
            }
        }

        PoolData pool = POOLS.get(id);

        ServerLevel level = pool != null
                ? pool.level()
                : build != null
                    ? build.level
                    : (ServerLevel) player.level();

        int count = saved.size();

        RESTORES.remove(id);

        startRestore(
                id,
                level,
                saved,
                false,
                0,
                0,
                0,
                0,
                false
        );

        return count;
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

        Map<BlockPos, SavedBlock> oldBlocks = ORIGINAL.get(id);

        if (oldBlocks == null) {
            return;
        }

        // Khôi phục xong vùng cũ mới bắt đầu tạo hồ mới.
        startRestore(
                id,
                oldPool.level(),
                oldBlocks,
                true,
                newX,
                newZ,
                oldPool.surfaceY(),
                oldPool.depth(),
                oldPool.secondPool()
        );
    }

    private static void createSecondPool(
            UUID id,
            ServerPlayer player,
            PoolData oldPool
    ) {

        Map<BlockPos, SavedBlock> oldBlocks = ORIGINAL.get(id);

        if (oldBlocks == null) {
            return;
        }

        startRestore(
                id,
                oldPool.level(),
                oldBlocks,
                true,
                oldPool.centerX(),
                oldPool.centerZ(),
                oldPool.surfaceY(),
                SECOND_DEPTH,
                true
        );
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

        // Khôi phục và xây hồ vẫn chạy kể cả khi đã tắt.
        RestoreTask restore = RESTORES.get(id);

        if (restore != null) {
            processRestore(id, restore);
            return;
        }

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

        // Khi người chơi xuống đáy hồ đầu tiên,
        // thay bằng hồ sâu 15 block.
        if (!pool.secondPool()
                && player.getY()
                <= pool.surfaceY() - FIRST_DEPTH) {

            createSecondPool(id, player, pool);
            return;
        }

        movePool(id, player, pool);

        updateFootBlock(id, player);

        player.fallDistance = 0.0F;
    }
}
