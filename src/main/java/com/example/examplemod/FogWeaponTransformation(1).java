
package com.example.examplemod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogWeaponTransformation {

    private static final String MARKER =
            "fog_domain_bat_diet_luu_anh";

    private static final String ABSORBED_ITEMS =
            "fog_domain_absorbed_items";

    private static final double TELEPORT_RADIUS = 1000.0;

    private static final Map<UUID, Integer> SELECTED =
            new HashMap<>();

    private record WeaponForm(
            String name,
            Supplier<? extends Item> item,
            ItemStack storedStack
    ) {}

    private static final List<WeaponForm> FIXED_WEAPONS = List.of(
            new WeaponForm("Trường Đao",
                    () -> FogWeaponItems.SWORD.get(), null),
            new WeaponForm("Khiên",
                    () -> FogWeaponItems.SHIELD.get(), null),
            new WeaponForm("Trường Thương",
                    () -> FogWeaponItems.LONG_SPEAR.get(), null),
            new WeaponForm("Cung",
                    () -> FogWeaponItems.BOW.get(), null),
            new WeaponForm("Bất Diệt Lưu Ảnh",
                    () -> FogWeaponItems.BAT_DIET_LUU_ANH.get(), null)
    );

    private FogWeaponTransformation() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    private static void send(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static boolean isTransformed(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);

        return data != null && data.copyTag().contains(MARKER);
    }

    private static int findArtifactSlot(ServerPlayer player) {
        for (int i = 0;
             i < player.getInventory().getContainerSize();
             i++) {

            ItemStack stack = player.getInventory().getItem(i);

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

    private static ItemStack getArtifact(ServerPlayer player) {
        int slot = findArtifactSlot(player);

        if (slot < 0) {
            return ItemStack.EMPTY;
        }

        return player.getInventory().getItem(slot);
    }

    private static CompoundTag getArtifactData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);

        if (data == null) {
            return new CompoundTag();
        }

        return data.copyTag();
    }

    private static void saveArtifactData(
            ItemStack stack,
            CompoundTag artifactData
    ) {
        CompoundTag merged = getArtifactData(stack);

        merged.putString(MARKER, "true");

        artifactData.getList(ABSORBED_ITEMS).ifPresent(list ->
                merged.put(ABSORBED_ITEMS, list.copy())
        );

        stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(merged)
        );
    }

    private static ListTag getAbsorbedList(ItemStack artifact) {
        CompoundTag data = getArtifactData(artifact);

        return data.getList(ABSORBED_ITEMS)
                .orElseGet(ListTag::new);
    }

    private static List<ItemStack> getAbsorbedStacks(
            ServerPlayer player,
            ItemStack artifact
    ) {
        List<ItemStack> result = new ArrayList<>();

        ListTag list = getAbsorbedList(artifact);

        for (int i = 0; i < list.size(); i++) {
            ItemStack.CODEC.parse(
                    player.level().registryAccess()
                            .createSerializationContext(NbtOps.INSTANCE),
                    list.get(i)
            ).result().ifPresent(stack -> {
                if (!stack.isEmpty()) {
                    result.add(stack.copyWithCount(1));
                }
            });
        }

        return result;
    }

    private static List<WeaponForm> getForms(ServerPlayer player) {
        List<WeaponForm> forms = new ArrayList<>(FIXED_WEAPONS);

        ItemStack artifact = getArtifact(player);

        if (!artifact.isEmpty()) {
            List<ItemStack> absorbed =
                    getAbsorbedStacks(player, artifact);

            for (ItemStack stack : absorbed) {
                forms.add(new WeaponForm(
                        stack.getHoverName().getString(),
                        null,
                        stack
                ));
            }
        }

        return forms;
    }

    private static int getSelected(ServerPlayer player) {
        List<WeaponForm> forms = getForms(player);

        int selected = SELECTED.getOrDefault(
                player.getUUID(),
                0
        );

        return Math.floorMod(selected, forms.size());
    }

    public static String getSelectedWeaponName(ServerPlayer player) {
        List<WeaponForm> forms = getForms(player);
        return forms.get(getSelected(player)).name();
    }

    // Giữ dữ liệu Bất Diệt Lưu Ảnh trên mũi tên khi cung lấy đạn.
    @SubscribeEvent
    public static void preserveArtifactArrow(LivingGetProjectileEvent event) {
        ItemStack projectile = event.getProjectileItemStack();

        if (projectile != null && !projectile.isEmpty()
                && isTransformed(projectile)) {
            // Sao chép cả CustomData để mũi tên sau khi bắn vẫn nhận diện được.
            event.setProjectileItemStack(projectile.copy());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("batdiet")

                        .then(Commands.literal("select")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        send(player, "Ban khong co quyen su dung lenh nay.");
                                        return 0;
                                    }

                                    List<WeaponForm> forms = getForms(player);

                                    int next = (getSelected(player) + 1)
                                            % forms.size();

                                    SELECTED.put(player.getUUID(), next);

                                    send(player,
                                            "Da chon hinh thai: "
                                                    + forms.get(next).name());

                                    return 1;
                                })
                        )

                        .then(Commands.literal("transform")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        send(player, "Ban khong co quyen su dung lenh nay.");
                                        return 0;
                                    }

                                    return transform(player);
                                })
                        )

                        .then(Commands.literal("teleport")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        send(player, "Ban khong co quyen su dung lenh nay.");
                                        return 0;
                                    }

                                    return teleportToArtifact(player);
                                })
                        )

                        .then(Commands.literal("devour")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        send(player, "Ban khong co quyen su dung lenh nay.");
                                        return 0;
                                    }

                                    return devour(player);
                                })
                        )

                        .then(Commands.literal("list")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!isController(player)) {
                                        send(player, "Ban khong co quyen su dung lenh nay.");
                                        return 0;
                                    }

                                    return listAbsorbed(player);
                                })
                        )
        );
    }

    // Hấp thụ đúng một vật phẩm trong tay chính.
    private static int devour(ServerPlayer player) {

        ItemStack artifact = getArtifact(player);

        if (artifact.isEmpty()) {
            send(player, "Ban can co Bat Diet Luu Anh trong tui do.");
            return 0;
        }

        ItemStack held = player.getMainHandItem();

        if (held.isEmpty()) {
            send(player, "Hay cam vat pham muon hap thu tren tay chinh.");
            return 0;
        }

        if (held == artifact
                || held.is(FogWeaponItems.BAT_DIET_LUU_ANH.get())
                || isTransformed(held)) {
            send(player, "Khong the hap thu chinh Bat Diet Luu Anh.");
            return 0;
        }

        ItemStack oneItem = held.copyWithCount(1);

        Optional<CompoundTag> encoded = ItemStack.CODEC.encodeStart(
                player.level().registryAccess()
                        .createSerializationContext(NbtOps.INSTANCE),
                oneItem
        ).result().filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast);

        if (encoded.isEmpty()) {
            send(player, "Khong the luu vat pham nay.");
            return 0;
        }

        CompoundTag artifactData = getArtifactData(artifact);

        ListTag absorbed = artifactData.getList(ABSORBED_ITEMS)
                .orElseGet(ListTag::new);

        absorbed.add(encoded.get());
        artifactData.put(ABSORBED_ITEMS, absorbed);

        saveArtifactData(artifact, artifactData);

        // Chỉ hấp thụ một vật phẩm.
        held.shrink(1);

        player.getInventory().setChanged();

        send(player,
                "Van Vat Thon Phe: Da hap thu "
                        + oneItem.getHoverName().getString()
                        + " (1 vat pham).");

        return 1;
    }

    // Hiển thị danh sách vật phẩm đã hấp thụ.
    private static int listAbsorbed(ServerPlayer player) {

        ItemStack artifact = getArtifact(player);

        if (artifact.isEmpty()) {
            send(player, "Khong tim thay Bat Diet Luu Anh trong tui do.");
            return 0;
        }

        List<ItemStack> absorbed =
                getAbsorbedStacks(player, artifact);

        send(player, "===== VAT PHAM DA THON PHE =====");
        send(player, "5 hinh thai co dinh: Truong Dao, Khien, Truong Thuong, Cung, Bat Diet Luu Anh.");

        if (absorbed.isEmpty()) {
            send(player, "Chua hap thu vat pham nao.");
            return 1;
        }

        for (int i = 0; i < absorbed.size(); i++) {
            send(player,
                    (i + 6) + ". "
                            + absorbed.get(i).getHoverName().getString());
        }

        send(player, "Tong so vat pham da hap thu: " + absorbed.size());

        return 1;
    }

    // Biến đổi chính Bất Diệt Lưu Ảnh.
    private static int transform(ServerPlayer player) {

        int slot = findArtifactSlot(player);

        if (slot < 0) {
            send(player, "Khong tim thay Bat Diet Luu Anh trong tui do.");
            return 0;
        }

        List<WeaponForm> forms = getForms(player);
        int selected = getSelected(player);

        WeaponForm form = forms.get(selected);

        ItemStack original =
                player.getInventory().getItem(slot);

        CompoundTag artifactData = getArtifactData(original);

        ItemStack replacement;

        if (form.storedStack() != null) {
            replacement = form.storedStack().copyWithCount(1);
        } else {
            replacement = new ItemStack(form.item().get());
        }

        // Giữ dấu hiệu và danh sách vật phẩm đã hấp thụ.
        saveArtifactData(replacement, artifactData);

        player.getInventory().setItem(slot, replacement);
        player.getInventory().setChanged();

        send(player,
                "Bat Diet Luu Anh da bien thanh: "
                        + form.name());

        return 1;
    }

    // Tìm Bất Diệt Lưu Ảnh đang rơi hoặc đang là mũi tên đã bắn.
    private static int teleportToArtifact(ServerPlayer player) {

        double radius = TELEPORT_RADIUS;

        AABB searchArea = new AABB(
                player.getX() - radius,
                player.getY() - radius,
                player.getZ() - radius,
                player.getX() + radius,
                player.getY() + radius,
                player.getZ() + radius
        );

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Tìm vật phẩm rơi trên mặt đất.
        for (ItemEntity entity :
                player.level().getEntitiesOfClass(
                        ItemEntity.class,
                        searchArea
                )) {

            ItemStack stack = entity.getItem();

            if (!stack.is(FogWeaponItems.BAT_DIET_LUU_ANH.get())
                    && !isTransformed(stack)) {
                continue;
            }

            double distance = player.distanceToSqr(entity);

            if (distance <= radius * radius
                    && distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }

        // Tìm cả mũi tên Bất Diệt Lưu Ảnh đã bắn.
        for (AbstractArrow arrow :
                player.level().getEntitiesOfClass(
                        AbstractArrow.class,
                        searchArea
                )) {

            ItemStack pickupStack = arrow.getPickupItemStackOrigin();

            if (pickupStack.isEmpty() || !isTransformed(pickupStack)) {
                continue;
            }

            double distance = player.distanceToSqr(arrow);

            if (distance <= radius * radius
                    && distance < nearestDistance) {
                nearest = arrow;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            send(player,
                    "Khong tim thay Bat Diet Luu Anh hoac mui ten cua no trong ban kinh 1000 block.");
            return 0;
        }

        // Chỉ dịch chuyển đến vị trí; không thu hồi vật phẩm/mũi tên.
        player.teleportTo(
                nearest.getX(),
                nearest.getY() + 1.0,
                nearest.getZ()
        );

        send(player,
                "Khong Gian Trieu Hoi: Da dich chuyen den vi tri Bat Diet Luu Anh!");

        return 1;
    }

}
