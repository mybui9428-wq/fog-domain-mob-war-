
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class FogDomain5Terrain {

    private static final int RADIUS = 30;
    private static final int DEPTH = 3;

    private static final Map<UUID, Map<BlockPos, BlockState>> ORIGINAL =
            new HashMap<>();

    private FogDomain5Terrain() {
    }

    public static int createPool(ServerPlayer player) {
        UUID id = player.getUUID();

        if (ORIGINAL.containsKey(id)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        int centerX = player.blockPosition().getX();
        int surfaceY = player.blockPosition().getY() - 1;
        int centerZ = player.blockPosition().getZ();

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

                    // Không tải chunk mới chỉ để tạo hồ.
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }

                    // Không thay thế block có dữ liệu riêng,
                    // ví dụ rương hoặc lò nung.
                    if (level.getBlockEntity(pos) != null) {
                        continue;
                    }

                    BlockState oldState = level.getBlockState(pos);

                    // Không thay thế bedrock hoặc barrier.
                    if (oldState.is(Blocks.BEDROCK)
                            || oldState.is(Blocks.BARRIER)) {
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

        ORIGINAL.put(id, saved);

        return saved.size();
    }

    public static int restorePool(ServerPlayer player) {
        UUID id = player.getUUID();

        Map<BlockPos, BlockState> saved = ORIGINAL.remove(id);

        if (saved == null || saved.isEmpty()) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        int restored = 0;

        for (Map.Entry<BlockPos, BlockState> entry : saved.entrySet()) {

            BlockPos pos = entry.getKey();

            // Không tải chunk mới khi khôi phục.
            if (!level.hasChunkAt(pos)) {
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
}
