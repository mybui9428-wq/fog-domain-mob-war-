
package com.example.examplemod;

import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;

public class FogDomainSkeletonRenderer<T extends PathfinderMob>
        extends HumanoidMobRenderer<
                T,
                SkeletonRenderState,
                SkeletonModel<SkeletonRenderState>
        > {

    private final Identifier texture;

    public FogDomainSkeletonRenderer(
            EntityRendererProvider.Context context,
            Identifier texture
    ) {
        super(
                context,
                new SkeletonModel<>(
                        context.bakeLayer(ModelLayers.SKELETON)
                ),
                0.5F
        );

        this.texture = texture;
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public Identifier getTextureLocation(
            SkeletonRenderState state
    ) {
        return this.texture;
    }
}
