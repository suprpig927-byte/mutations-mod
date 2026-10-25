package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.entity.FailedExperiment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Draws Failed Experiments with the zombie model and a texture for their power. */
public class FailedExperimentRenderer extends ZombieRenderer {
    private static final ContextKey<FailedExperiment.Power> POWER =
            new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "experiment_power"));

    public FailedExperimentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Zombie entity, ZombieRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof FailedExperiment e) state.setRenderData(POWER, e.power());
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        FailedExperiment.Power p = state.getRenderData(POWER);
        String name = p == null ? "burning" : p.id();
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/failed_experiment/" + name + ".png");
    }
}
