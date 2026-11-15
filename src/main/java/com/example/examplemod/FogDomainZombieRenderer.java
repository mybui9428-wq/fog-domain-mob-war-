
package com.example.examplemod;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;

public class FogDomainZombieRenderer<T extends PathfinderMob>
        extends HumanoidMobRenderer<
                T,
                ZombieRenderState,
                ZombieModel<ZombieRenderState>
        > {

    private final Identifier texture;

    public FogDomainZombieRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation modelLayer,
            Identifier texture
    ) {
        super(
                context,
                new ZombieModel<>(
                        context.bakeLayer(modelLayer)
                ),
                0.5F
        );

        this.texture = texture;
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(
            ZombieRenderState state
    ) {
        return this.texture;
    }
}
