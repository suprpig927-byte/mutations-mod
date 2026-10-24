package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.mutation.Mutation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** The 16x16 pixel-art icon for each base mutation (assets/mutations/textures/gui/mutation/). */
public final class MutationIcons {
    private MutationIcons() {}

    public static Identifier texture(Mutation m) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, "textures/gui/mutation/" + m.id() + ".png");
    }

    /** Draws the icon at {@code size} pixels (16 = native, 32 = doubled). */
    public static void draw(GuiGraphicsExtractor g, Mutation m, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture(m), x, y, 0f, 0f, size, size, 16, 16, 16, 16);
    }
}
