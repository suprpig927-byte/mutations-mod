package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.entity.HavenMutant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/** Haven mutants: humanoid, dressed for their role. */
public class HavenMutantRenderer extends HumanoidMobRenderer<HavenMutant, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ContextKey<HavenMutant.Role> ROLE = new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "haven_role"));

    public HavenMutantRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(HavenMutant entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.setRenderData(ROLE, entity.role());
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        HavenMutant.Role r = state.getRenderData(ROLE);
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/haven/" + (r == null ? "cook" : r.id()) + ".png");
    }
}
