package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.entity.PrisonBoss;
import com.mutationsmod.mutations.entity.SubjectZero;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Prison bosses: humanoid. Subject Zero's texture changes with its current form (and when suppressed). */
public class BossRenderer<T extends PrisonBoss> extends HumanoidMobRenderer<T, BossRenderer.State, HumanoidModel<BossRenderer.State>> {
    /** Render state that carries which skin to draw (Subject Zero's changes with its form). */
    public static class State extends HumanoidRenderState {
        public String skin;
    }
    private final String base;

    public BossRenderer(EntityRendererProvider.Context context, String base, float scale) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.6f * scale);
        this.base = base;
        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        String skin = base;
        if (entity instanceof SubjectZero z) skin = z.suppressed() ? "subject_zero" : "subject_zero_" + z.form().id();
        state.skin = skin;
    }

    @Override
    public Identifier getTextureLocation(State state) {
        String s = state.skin;
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/boss/" + (s == null ? base : s) + ".png");
    }
}
