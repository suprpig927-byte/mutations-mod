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
import net.minecraft.util.context.ContextKey;

/** Prison bosses: humanoid. Subject Zero's texture changes with its current form (and when suppressed). */
public class BossRenderer<T extends PrisonBoss> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ContextKey<String> SKIN = new ContextKey<>(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "boss_skin"));
    private final String base;

    public BossRenderer(EntityRendererProvider.Context context, String base, float scale) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.6f * scale);
        this.base = base;
        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(T entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        String skin = base;
        if (entity instanceof SubjectZero z) skin = z.suppressed() ? "subject_zero" : "subject_zero_" + z.form().id();
        state.setRenderData(SKIN, skin);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        String s = state.getRenderData(SKIN);
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/entity/boss/" + (s == null ? base : s) + ".png");
    }
}
