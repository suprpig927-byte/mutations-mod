package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.entity.HavenMutant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Haven mutants: humanoid, dressed for their role. */
public class HavenMutantRenderer extends HumanoidMobRenderer<HavenMutant, HavenMutantRenderer.State, HumanoidModel<HavenMutantRenderer.State>> {
    /** Render state that carries the trader's role (decides the outfit). */
    public static class State extends HumanoidRenderState {
        public HavenMutant.Role role = HavenMutant.Role.QUARTERMASTER;
        public int look;
    }

    public HavenMutantRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HavenMutant entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.role = entity.role();
        state.look = Math.floorMod(entity.getUUID().hashCode(), 4);   // each trader has their own face and hair
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/haven/" + state.role.id() + "_" + state.look + ".png");
    }
}
