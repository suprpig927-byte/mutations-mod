package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.GeneSplicer;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Gene Splicer: respec stats or splice out a secondary mutation, paid in Mutagen Crystals and XP levels. */
public class GeneSplicerScreen extends Screen {
    private static final int W = 300;
    private String builtFor = "";

    public GeneSplicerScreen() {
        super(Component.literal("Gene Splicer"));
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new GeneSplicerScreen());
    }

    private MutationData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? MutationData.EMPTY : mc.player.getData(ModAttachments.DATA);
    }

    private int panelH(MutationData d) {
        int rows = d.rollOptions().isEmpty() ? 1 + d.secondaries().size() : d.rollOptions().size();
        return 90 + rows * 24;
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top(MutationData d) {
        return (this.height - panelH(d)) / 2;
    }

    private String signature(MutationData d) {
        return d.rollOptions() + "|" + d.secondaries() + "|" + d.totalSpentStats();
    }

    @Override
    protected void init() {
        MutationData d = data();
        builtFor = signature(d);
        int x = left() + 10;
        int y = top(d) + 50;
        int bw = W - 20;

        if (!d.rollOptions().isEmpty()) {
            // A splice is in progress: pick the new secondary.
            for (String id : d.rollOptions()) {
                Secondary s = Secondary.byId(id).orElse(null);
                if (s == null) continue;
                Button b = Button.builder(Component.literal((s.active() != null ? "[A] " : "") + s.displayName()),
                                btn -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.CHOOSE_ROLL, id)))
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(s.description())))
                        .bounds(x, y, bw, 20).build();
                addRenderableWidget(b);
                y += 24;
            }
        } else {
            Button respec = Button.builder(Component.literal("Respec stats  (" + GeneSplicer.RESPEC_CRYSTALS + " crystals + "
                                    + GeneSplicer.RESPEC_LEVELS + " levels)"),
                            b -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.SPLICE_RESPEC, "")))
                    .bounds(x, y, bw, 20).build();
            respec.active = d.totalSpentStats() > 0;
            addRenderableWidget(respec);
            y += 24;
            for (String id : d.secondaries()) {
                Secondary s = Secondary.byId(id).orElse(null);
                if (s == null) continue;
                addRenderableWidget(Button.builder(Component.literal("Splice out " + s.displayName() + "  ("
                                        + GeneSplicer.REROLL_CRYSTALS + " crystals + " + GeneSplicer.REROLL_LEVELS + " levels)"),
                                b -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.SPLICE_REROLL, id)))
                        .bounds(x, y, bw, 20).build());
                y += 24;
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left() + W - 60, top(d) + panelH(d) + 4, 60, 20).build());
    }

    @Override
    public void tick() {
        if (!signature(data()).equals(builtFor)) rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            MutationData d = data();
            int left = left(), top = top(d), h = panelH(d);
            g.fill(left, top, left + W, top + h, 0xE0101018);
            g.fill(left, top, left + W, top + 1, 0xFF4CE68A);
            g.fill(left, top + h - 1, left + W, top + h, 0xFF4CE68A);
            g.text(this.font, "GENE SPLICER", left + 10, top + 10, 0xFF4CE68A, true);
            g.text(this.font, "Mutagen Crystals: " + GeneSplicer.crystals(mc.player) + "    XP levels: " + mc.player.experienceLevel,
                    left + 10, top + 24, 0xFFDDDDDD, false);
            String hint = d.rollOptions().isEmpty()
                    ? (d.secondaries().isEmpty() ? "No secondary mutations to splice yet." : "Splice out a secondary to roll 3 new choices.")
                    : "Choose the new secondary mutation:";
            g.text(this.font, hint, left + 10, top + 36, 0xFFAAAAAA, false);
        }
        this.renderables.forEach(r -> r.extractRenderState(g, mouseX, mouseY, partialTick));
    }
}
