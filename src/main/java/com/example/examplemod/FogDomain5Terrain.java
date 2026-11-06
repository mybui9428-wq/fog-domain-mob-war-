
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5Terrain {

    private static final int RADIUS = 30;
    private static final int DEPTH = 3;
    private static final int CLEAR_HEIGHT = 30;
    private static final int MOVE_DISTANCE = 3;

    private static final int WATER_FLAGS = 3;

    private record SavedBlock(
            BlockState state,
            CompoundTag blockEntityData
    ) {}

    private record PoolData(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY
    ) {}

    private static final Map<UUID, Map<BlockPos, SavedBlock>> ORIGINAL =
            new HashMap<>();

    private static final Map<UUID, PoolData> POOLS =
            new HashMap<>();

    private static final Map<UUID, Map<BlockPos, BlockState>> FOOT_BLOCKS =
            new HashMap<>();

    private FogDomain5Terrain() {}

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    public static boolean isReady(UUID id) {
        return ORIGINAL.containsKey(id) && POOLS.containsKey(id);
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

        Map<BlockPos, SavedBlock> saved = buildPool(
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

    private static int findSurface(
            ServerLevel level,
            BlockPos start
    ) {
        for (int y = start.getY() - 1;
             y >= start.getY() - 10;
             y--) {

            BlockPos pos = new BlockPos(
                    start.getX(), y, start.getZ()
            );

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.isAir()
                    && state.isSolid()
                    && state.getFluidState().isEmpty()
                    && !state.is(net.minecraft.tags.BlockTags.LEAVES)
                    && !state.is(Blocks.BEDROCK)
                    && !state.is(Blocks.BARRIER)) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }

    private static SavedBlock saveBlock(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        CompoundTag data = null;

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity != null) {
            data = blockEntity.saveWithFullMetadata(
                    level.registryAccess()
            );
        }

        return new SavedBlock(state, data);
    }

    private static Map<BlockPos, SavedBlock> buildPool(
            ServerLevel level,
            int centerX,
            int centerZ,
            int surfaceY
    ) {
        Map<BlockPos, SavedBlock> saved = new LinkedHashMap<>();

        int radiusSquared = RADIUS * RADIUS;

        int minY = surfaceY - DEPTH + 1;
        int maxY = surfaceY + CLEAR_HEIGHT;

        // Giai đoạn 1: lưu toàn bộ block gốc.
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                if (x * x + z * z > radiusSquared) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(
                            centerX + x,
                            y,
                            centerZ + z
                    );

                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);

                    if (y > surfaceY && state.isAir()) {
                        continue;
                    }

                    saved.put(
                            pos.immutable(),
                            saveBlock(level, pos)
                    );
                }
            }
        }

        // Giai đoạn 2: xóa cỏ, hoa, lá và cây trước.
        for (Map.Entry<BlockPos, SavedBlock> entry : saved.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (state.canBeReplaced()
                    || state.is(net.minecraft.tags.BlockTags.LEAVES)
                    || state.is(net.minecraft.tags.BlockTags.FLOWERS)
                    || state.is(net.minecraft.tags.BlockTags.LOGS)) {

                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        WATER_FLAGS
                );
            }
        }

        // Giai đoạn 3: dọn các block còn lại phía trên mặt hồ.
        for (Map.Entry<BlockPos, SavedBlock> entry : saved.entrySet()) {
            BlockPos pos = entry.getKey();

            if (pos.getY() <= surfaceY) {
                continue;
            }

            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        WATER_FLAGS
                );
            }
        }

        // Giai đoạn 4: tạo hồ nước sâu 3 block.
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

                    level.setBlock(
                            pos,
                            Blocks.WATER.defaultBlockState(),
                            WATER_FLAGS
                    );
                }
            }
        }

        // Giai đoạn 5: tạo vòng Barrier quanh hồ.
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {

                int distance = x * x + z * z;

                if (distance <= radiusSquared
                        || distance > (RADIUS + 1) * (RADIUS + 1)) {
                    continue;
                }

                BlockPos pos = new BlockPos(
                        centerX + x,
                        surfaceY,
                        centerZ + z
                );

                if (!level.hasChunkAt(pos)) {
                    continue;
                }

                if (!saved.containsKey(pos)) {
                    saved.put(
                            pos.immutable(),
                            saveBlock(level, pos)
                    );
                }

                level.setBlock(
                        pos,
                        Blocks.BARRIER.defaultBlockState(),
                        WATER_FLAGS
                );
            }
        }

        return saved;
    }

    private static int restoreBlocks(
            ServerLevel level,
            Map<BlockPos, SavedBlock> saved
    ) {
        int restored = 0;

        for (Map.Entry<BlockPos, SavedBlock> entry : saved.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState current = level.getBlockState(pos);

            if (!current.isAir()
                    && !current.is(Blocks.WATER)
                    && !current.is(Blocks.BARRIER)) {
                continue;
            }

            SavedBlock original = entry.getValue();

            level.setBlock(
                    pos,
                    original.state(),
                    WATER_FLAGS
            );

            if (original.blockEntityData() != null) {
                BlockEntity blockEntity = level.getBlockEntity(pos);

                if (blockEntity != null) {
                    blockEntity.loadWithComponents(
                            TagValueInput.create(
                                    ProblemReporter.DISCARDING,
                                    level.registryAccess(),
                                    original.blockEntityData()
                            )
                    );

                    blockEntity.setChanged();
                }
            }

            restored++;
        }

        return restored;
    }

    private static void updateFootBlocks(
            UUID id,
            ServerPlayer player,
            PoolData pool
    ) {
        ServerLevel level = pool.level();

        Map<BlockPos, BlockState> old =
                FOOT_BLOCKS.computeIfAbsent(
                        id,
                        key -> new HashMap<>()
                );

        BlockPos feet = player.blockPosition().below();

        int dx = feet.getX() - pool.centerX();
        int dz = feet.getZ() - pool.centerZ();

        if (feet.getY() != pool.surfaceY()
                || dx * dx + dz * dz > RADIUS * RADIUS) {
            removeFootBlocks(id, level);
            return;
        }

        Set<BlockPos> wanted = new HashSet<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = feet.offset(x, 0, z);

                int px = pos.getX() - pool.centerX();
                int pz = pos.getZ() - pool.centerZ();

                if (px * px + pz * pz > RADIUS * RADIUS) {
                    continue;
                }

                wanted.add(pos.immutable());
            }
        }

        // Khôi phục nước tại vị trí đã rời đi.
        var iterator = old.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iterator.next();

            if (wanted.contains(entry.getKey())) {
                continue;
            }

            BlockPos pos = entry.getKey();

            if (level.hasChunkAt(pos)
                    && level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(
                        pos,
                        entry.getValue(),
                        WATER_FLAGS
                );
            }

            iterator.remove();
        }

        // Đặt Barrier ở vùng đỡ mới.
        for (BlockPos pos : wanted) {
            if (old.containsKey(pos) || !level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.is(Blocks.WATER)) {
                continue;
            }

            old.put(pos, state);

            level.setBlock(
                    pos,
                    Blocks.BARRIER.defaultBlockState(),
                    WATER_FLAGS
            );
        }
    }

    private static void removeFootBlocks(
            UUID id,
            ServerLevel level
    ) {
        Map<BlockPos, BlockState> blocks = FOOT_BLOCKS.remove(id);

        if (blocks == null) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();

            if (level.hasChunkAt(pos)
                    && level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(
                        pos,
                        entry.getValue(),
                        WATER_FLAGS
                );
            }
        }
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

        ServerLevel level = oldPool.level();

        removeFootBlocks(id, level);

        // Khôi phục hồ cũ.
        restoreBlocks(level, oldBlocks);
        oldBlocks.clear();

        // Tìm mặt đất mới.
        int newSurfaceY = findSurface(
                level,
                player.blockPosition()
        );

        if (newSurfaceY == Integer.MIN_VALUE) {
            ORIGINAL.remove(id);
            POOLS.remove(id);
            return;
        }

        // Tạo hồ mới.
        Map<BlockPos, SavedBlock> newBlocks = buildPool(
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
        Map<BlockPos, SavedBlock> saved = ORIGINAL.remove(id);

        removeFootBlocks(
                id,
                (ServerLevel) player.level()
        );

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

        updateFootBlocks(id, player, pool);

        player.fallDistance = 0.0F;

        Vec3 motion = player.getDeltaMovement();

        if (player.getY() < pool.surfaceY() + 0.85D
                && player.getY() > pool.surfaceY() - 2.0D) {

            player.setPos(
                    player.getX(),
                    pool.surfaceY() + 1.0D,
                    player.getZ()
            );

            player.setDeltaMovement(
                    motion.x,
                    Math.max(0.0D, motion.y),
                    motion.z
            );
        }
    }
}
