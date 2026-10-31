package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ExampleModClient {

    public ExampleModClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ExampleMod.LOGGER.info("FOG DOMAIN CLIENT READY");
    }

    @SubscribeEvent
    public static void registerClientPayloads(
            RegisterClientPayloadHandlersEvent event) {

        event.register(FogPossessionSyncPayload.TYPE, (payload, context) -> {

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            if (payload.action() == 1) {
                Entity target = minecraft.level.getEntity(payload.targetId());

                if (target != null) {
                    minecraft.setCameraEntity(target);
                }

            } else if (payload.action() == 0) {
                minecraft.setCameraEntity(minecraft.player);
            }
        });
    }
}
