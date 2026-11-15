
package com.example.examplemod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;

public class FogDomainEntityRenderer<T extends PathfinderMob>
        extends HumanoidMobRenderer<
                T,
                HumanoidRenderState,
                HumanoidModel<HumanoidRenderState>
        > {

    private final Identifier texture;

    public FogDomainEntityRenderer(
            EntityRendererProvider.Context context,
            Identifier texture
    ) {
        super(
                context,
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.PLAYER)
                ),
                0.5F
        );

        this.texture = texture;
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(
            HumanoidRenderState state
    ) {
        return texture;
    }
}
