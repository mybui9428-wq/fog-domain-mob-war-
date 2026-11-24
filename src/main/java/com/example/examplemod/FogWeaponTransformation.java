
package com.example.examplemod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponTransformation {

    private static final String MARKER =
            "fog_domain_bat_diet_luu_anh";

    private static final double TELEPORT_RADIUS = 1000.0;

    private static final Map<UUID, Integer> SELECTED =
            new HashMap<>();

    private record WeaponForm(
            String name,
            Supplier<? extends Item> item
    ) {}

    // Thứ tự chọn vũ khí.
    private static final List<WeaponForm> WEAPONS = List.of(

            new WeaponForm(
                    "Trường Đao",
                    () -> FogWeaponItems.SWORD.get()
            ),

            new WeaponForm(
                    "Khiên",
                    () -> FogWeaponItems.SHIELD.get()
            ),

            new WeaponForm(
                    "Trường Thương",
                    () -> FogWeaponItems.LONG_SPEAR.get()
            ),

            new WeaponForm(
                    "Cung",
                    () -> FogWeaponItems.BOW.get()
            ),

            new WeaponForm(
                    "Bất Diệt Lưu Ảnh",
                    () -> FogWeaponItems.BAT_DIET_LUU_ANH.get()
            )
    );

    private FogWeaponTransformation() {
    }

    // Chỉ tubi2531 được sử dụng.
    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    // Lấy vũ khí đang được chọn.
    private static int getSelected(ServerPlayer player) {
        return SELECTED.getOrDefault(player.getUUID(), 0);
    }

    public static String getSelectedWeaponName(
            ServerPlayer player
    ) {
        return WEAPONS.get(getSelected(player)).name();
    }

    // Đăng ký các lệnh.
    @SubscribeEvent
    public static void registerCommands(
            RegisterCommandsEvent event
    ) {

        event.getDispatcher().register(
                Commands.literal("batdiet")

                        // Lệnh chọn vũ khí.
                        .then(Commands.literal("select")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "Ban khong co quyen su dung lenh nay."
                                                )
                                        );
                                        return 0;
                                    }

                                    int next =
                                            (getSelected(player) + 1)
                                                    % WEAPONS.size();

                                    SELECTED.put(
                                            player.getUUID(),
                                            next
                                    );

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "Da chon vu khi: "
                                                            + WEAPONS.get(next).name()
                                            )
                                    );

                                    return 1;
                                })
                        )

                        // Lệnh biến đổi vũ khí.
                        .then(Commands.literal("transform")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "Ban khong co quyen su dung lenh nay."
                                                )
                                        );
                                        return 0;
                                    }

                                    return transform(player);
                                })
                        )

                        // Lệnh dịch chuyển.
                        .then(Commands.literal("teleport")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "Ban khong co quyen su dung lenh nay."
                                                )
                                        );
                                        return 0;
                                    }

                                    return teleportToArtifact(player);
                                })
                        )
        );
    }

    // Kiểm tra vật phẩm có phải hình dạng của
    // Bất Diệt Lưu Ảnh hay không.
    private static boolean isTransformed(ItemStack stack) {

        CustomData data =
                stack.get(DataComponents.CUSTOM_DATA);

        return data != null
        && data.copyTag().contains(MARKER);
    }

    // Đánh dấu vật phẩm thuộc Bất Diệt Lưu Ảnh.
    private static void markAsArtifact(ItemStack stack) {

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putString(MARKER, "true")
        );
    }

    // Tìm Bất Diệt Lưu Ảnh trong kho đồ.
    private static int findArtifactSlot(ServerPlayer player) {

        for (int i = 0;
             i < player.getInventory().getContainerSize();
             i++) {

            ItemStack stack =
                    player.getInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(FogWeaponItems.BAT_DIET_LUU_ANH.get())
                    || isTransformed(stack)) {

                return i;
            }
        }

        return -1;
    }

    // Biến đổi Bất Diệt Lưu Ảnh.
    private static int transform(ServerPlayer player) {

        int selected = getSelected(player);
        WeaponForm form = WEAPONS.get(selected);

        int foundSlot = findArtifactSlot(player);

        if (foundSlot < 0) {

            player.sendSystemMessage(
                    Component.literal(
                            "Khong tim thay Bat Diet Luu Anh trong kho do."
                    )
            );

            return 0;
        }

        ItemStack replacement =
                new ItemStack(form.item().get());

        markAsArtifact(replacement);

        // Giữ nguyên ô chứa vật phẩm.
        player.getInventory().setItem(
                foundSlot,
                replacement
        );

        player.getInventory().setChanged();

        player.sendSystemMessage(
                Component.literal(
                        "Bat Diet Luu Anh da bien thanh: "
                                + form.name()
                )
        );

        return 1;
    }

    // Dịch chuyển đến vật phẩm Bất Diệt Lưu Ảnh
    // đang rơi hoặc nằm trên mặt đất.
    private static int teleportToArtifact(
            ServerPlayer player
    ) {

        double radius = TELEPORT_RADIUS;

        AABB searchArea = new AABB(
                player.getX() - radius,
                player.getY() - radius,
                player.getZ() - radius,
                player.getX() + radius,
                player.getY() + radius,
                player.getZ() + radius
        );

        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity entity : player.level().getEntitiesOfClass(
        ItemEntity.class,
        searchArea
)) {

            // Chỉ tìm vật phẩm thuộc Bất Diệt Lưu Ảnh.
            if (!stack.is(
                    FogWeaponItems.BAT_DIET_LUU_ANH.get()
            ) && !isTransformed(stack)) {
                continue;
            }

            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();

            // Tính khoảng cách hình tròn trên mặt phẳng ngang.
            double distance = dx * dx + dz * dz;

            if (distance <= radius * radius
                    && distance < nearestDistance) {

                nearest = entity;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {

            player.sendSystemMessage(
                    Component.literal(
                            "Khong tim thay Bat Diet Luu Anh trong ban kinh 1000 block."
                    )
            );

            return 0;
        }

        // Dịch chuyển đến gần vật phẩm.
        player.teleportTo(
                nearest.getX(),
                nearest.getY() + 1.0,
                nearest.getZ()
        );

        player.sendSystemMessage(
                Component.literal(
                        "Khong Gian Trieu Hoi: Dich chuyen thanh cong!"
                )
        );

        return 1;
    }
}
