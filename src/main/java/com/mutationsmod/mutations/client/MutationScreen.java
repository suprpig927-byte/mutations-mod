package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.AbilityHandler;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.mutation.Stat;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The mutation screen (default key M). Two tabs:
 * Mutation - your base, secondaries, abilities, and secondary rolls;
 * Stats - mastery level, XP, and spending stat points.
 */
public class MutationScreen extends Screen {
    private static final int PANEL_W = 420;
    private static int tab = 0; // remembered while the game is open

    /** What the widgets were built for; rebuilt when it changes (e.g. a roll arrives from the server). */
    private String builtFor = "";

    public MutationScreen() {
        super(Component.literal("Mutations"));
    }

    private MutationData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? MutationData.EMPTY : mc.player.getData(ModAttachments.DATA);
    }

    /** Panel height: as tall as fits (up to 300), so long ability lists have room. */
    private int panelH() {
        return Math.max(220, Math.min(300, this.height - 56));
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - panelH()) / 2;
    }

    private String signature(MutationData d) {
        return tab + "|" + d.rollOptions() + "|" + Mastery.canRoll(d) + "|" + d.secondaries() + "|" + d.statPoints() + "|" + d.stats();
    }

    @Override
    protected void init() {
        MutationData d = data();
        builtFor = signature(d);
        int left = left();
        int top = top();

        addRenderableWidget(Button.builder(Component.literal("Mutation"), b -> switchTab(0))
                .bounds(left, top - 22, 80, 20).build()).active = tab != 0;
        addRenderableWidget(Button.builder(Component.literal("Stats" + (d.statPoints() > 0 ? " (" + d.statPoints() + ")" : "")), b -> switchTab(1))
                .bounds(left + 84, top - 22, 80, 20).build()).active = tab != 1;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left + PANEL_W - 60, top + panelH() + 4, 60, 20).build());

        if (tab == 0) initMutationTab(d, left, top);
        else initStatsTab(d, left, top);
    }

    private void switchTab(int t) {
        tab = t;
        rebuildWidgets();
    }

    private void initMutationTab(MutationData d, int left, int top) {
        int rx = left + 222;
        int bottom = top + panelH() - 8;
        if (!d.rollOptions().isEmpty()) {
            List<String> options = d.rollOptions();
            for (int i = 0; i < options.size(); i++) {
                String id = options.get(i);
                Secondary s = Secondary.byId(id).orElse(null);
                if (s == null) continue;
                int y = bottom - 22 * (options.size() - i);
                Button b = Button.builder(Component.literal((s.active() != null ? "[A] " : "") + s.displayName()),
                                btn -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.CHOOSE_ROLL, id)))
                        .bounds(rx, y, 188, 20)
                        .tooltip(Tooltip.create(Component.literal(s.displayName() + "\n" + s.description())))
                        .build();
                addRenderableWidget(b);
            }
        } else if (Mastery.canRoll(d)) {
            addRenderableWidget(Button.builder(Component.literal("Roll secondary mutation"),
                            b -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.START_ROLL, "")))
                    .bounds(rx, bottom - 20, 188, 20).build());
        }
        if (d.has(Secondary.POCKET_SPACE)) {
            addRenderableWidget(Button.builder(Component.literal("Pocket Space"),
                            b -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.OPEN_POCKET, "")))
                    .bounds(left + 10, top + panelH() - 28, 100, 20).build());
        }
    }

    private void initStatsTab(MutationData d, int left, int top) {
        int rx = left + 190;
        int y = top + 30;
        for (Stat stat : Stat.values()) {
            Button plus = Button.builder(Component.literal("+"), b -> {
                        int amount = Minecraft.getInstance().hasShiftDown() ? 5 : 1;
                        ClientPacketDistributor.sendToServer(new ModPayloads.SpendStat(stat.id(), amount));
                    })
                    .bounds(rx + 200, y - 4, 20, 16)
                    .tooltip(Tooltip.create(Component.literal("Spend a point on " + stat.displayName() + " (shift: 5 points)")))
                    .build();
            plus.active = d.statPoints() > 0;
            addRenderableWidget(plus);
            y += 30;
        }
        Button respec = Button.builder(Component.literal("Respec (" + Mastery.RESPEC_COST_LEVELS + " levels)"),
                        b -> ClientPacketDistributor.sendToServer(new ModPayloads.ScreenAction(ModPayloads.ScreenAction.RESPEC, "")))
                .bounds(left + 10, top + panelH() - 28, 150, 20)
                .tooltip(Tooltip.create(Component.literal("Refund every spent stat point. Costs " + Mastery.RESPEC_COST_LEVELS + " Minecraft XP levels.")))
                .build();
        respec.active = d.totalSpentStats() > 0;
        addRenderableWidget(respec);
    }

    @Override
    public void tick() {
        super.tick();
        if (!signature(data()).equals(builtFor)) rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // The game already draws the blurred background before this method runs.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            int left = left();
            int top = top();
            g.fill(left, top, left + PANEL_W, top + panelH(), 0xE0101018);
            g.fill(left, top, left + PANEL_W, top + 1, 0xFF6B5BD6);
            g.fill(left, top + panelH() - 1, left + PANEL_W, top + panelH(), 0xFF6B5BD6);
            MutationData d = data();
            if (tab == 0) drawMutationTab(g, mc, d, left, top);
            else drawStatsTab(g, d, left, top);
        }
        this.renderables.forEach(r -> r.extractRenderState(g, mouseX, mouseY, partialTick));
    }

    private void drawMutationTab(GuiGraphicsExtractor g, Minecraft mc, MutationData d, int left, int top) {
        int x = left + 10;
        int y = top + 10;
        int mid = left + 214;
        g.fill(mid, top + 8, mid + 1, top + panelH() - 8, 0x40FFFFFF);

        List<Mutation> muts = d.mutationList();
        if (muts.isEmpty()) {
            g.text(this.font, "You have no mutation.", x, y, 0xFFAAAAAA, false);
            return;
        }
        Mutation base = muts.get(0);
        MutationIcons.draw(g, base, x, y, 32);
        String title = base.displayName().toUpperCase();
        g.text(this.font, title, x + 38, y + 6, base.color(), true);
        g.text(this.font, "Mastery level " + d.level(), x + 38, y + 18, 0xFFFFD45A, false);
        y += 38;
        y = wrapped(g, "+ " + base.upside(), x, y, 196, 0xFF7CE38B);
        y = wrapped(g, "- " + base.downside(), x, y, 196, 0xFFE07C7C);
        for (int i = 1; i < muts.size(); i++) {
            MutationIcons.draw(g, muts.get(i), x, y - 1, 10);
            g.text(this.font, muts.get(i).displayName() + " (extra)", x + 13, y, muts.get(i).color(), false);
            y += 12;
        }
        y += 6;

        g.text(this.font, "ABILITIES", x, y, 0xFFFFD45A, false);
        y += 11;
        long now = mc.level.getGameTime();
        List<ActiveAbility> actives = d.actives();
        for (int i = 0; i < actives.size(); i++) {
            ActiveAbility a = actives.get(i);
            String key = i < MutationsClient.ABILITY_KEYS.length ? MutationsClient.ABILITY_KEYS[i].getTranslatedKeyMessage().getString() : "-";
            long remaining = d.readyAt(a) - now;
            String status = remaining > 0 ? MutationsClient.formatTime(remaining) : "ready";
            g.text(this.font, "[" + key + "] " + a.displayName(), x, y, 0xFFFFFFFF, false);
            g.text(this.font, status, mid - 8 - this.font.width(status), y, remaining > 0 ? 0xFF999999 : 0xFF7CE38B, false);
            y += 10;
            String cd = MutationsClient.formatTime(AbilityHandler.cooldown(a, d));
            y = wrapped(g, a.description() + " (" + cd + ")", x + 6, y, 190, 0xFF8A8A8A);
            y += 1;
        }
        com.mutationsmod.mutations.mutation.Ultimate ult = com.mutationsmod.mutations.mutation.Ultimate.of(base);
        if (ult != null) {
            boolean awake = d.level() >= com.mutationsmod.mutations.mutation.Ultimate.LEVEL;
            long remaining = d.omegaReady() - now;
            String status = !awake ? "Lv " + com.mutationsmod.mutations.mutation.Ultimate.LEVEL : remaining > 0 ? MutationsClient.formatTime(remaining) : "ready";
            g.text(this.font, "[" + MutationsClient.ULTIMATE.getTranslatedKeyMessage().getString() + "] \u03A9 " + ult.displayName(), x, y,
                    awake ? base.color() : 0xFF777777, false);
            g.text(this.font, status, mid - 8 - this.font.width(status), y, !awake ? 0xFF777777 : remaining > 0 ? 0xFF999999 : 0xFF7CE38B, false);
            y += 10;
            y = wrapped(g, (awake ? "" : "Omega Awakening at mastery level 75: ") + ult.description() + " (10m)", x + 6, y, 190, 0xFF8A8A8A);
            y += 1;
        }
        if (d.has(Mutation.PHASER)) {
            String keys = MutationsClient.SPEED_UP.getTranslatedKeyMessage().getString() + "/"
                    + MutationsClient.SPEED_DOWN.getTranslatedKeyMessage().getString();
            g.text(this.font, "Speed tier " + d.speedTier() + " [" + keys + "]", x, y + 2, 0xFFFFE05A, false);
        }

        // ---- right column: secondaries ----
        int rx = mid + 8;
        int ry = top + 10;
        g.text(this.font, "SECONDARY MUTATIONS  " + d.secondaries().size() + "/" + Mastery.MAX_SECONDARIES, rx, ry, 0xFFC9B8FF, false);
        ry += 13;
        for (Secondary s : d.secondaryList()) {
            boolean works = d.has(s.base());
            g.text(this.font, (s.active() != null ? "[A] " : "") + s.displayName(), rx, ry, works ? 0xFFFFFFFF : 0xFF777777, false);
            ry += 10;
            ry = wrapped(g, s.description(), rx + 4, ry, 184, 0xFF8A8A8A);
            ry += 3;
        }
        if (!d.rollOptions().isEmpty()) {
            int by = top + panelH() - 8 - 22 * d.rollOptions().size() - 12;
            g.text(this.font, "Choose one (hover for details):", rx, by, 0xFFFFD45A, false);
        } else if (!Mastery.canRoll(d) && d.secondaries().size() < Mastery.MAX_SECONDARIES) {
            String next = "";
            for (int lvl : Mastery.ROLL_LEVELS) {
                if (d.level() < lvl) {
                    next = "Next roll at mastery level " + lvl;
                    break;
                }
            }
            if (!next.isEmpty()) g.text(this.font, next, rx, top + panelH() - 20, 0xFF777777, false);
        }
    }

    private void drawStatsTab(GuiGraphicsExtractor g, MutationData d, int left, int top) {
        int x = left + 10;
        int y = top + 10;
        g.text(this.font, "MASTERY", x, y, 0xFFFFD45A, true);
        y += 14;
        g.text(this.font, "Level " + d.level(), x, y, 0xFFFFFFFF, false);
        y += 12;
        int need = Mastery.xpToNext(d.level());
        int barW = 150;
        int filled = (int) (barW * Math.min(1.0, d.xp() / (double) need));
        g.fill(x, y, x + barW, y + 6, 0xFF2A2A35);
        g.fill(x, y, x + filled, y + 6, 0xFF8FD14F);
        y += 9;
        g.text(this.font, d.xp() + " / " + need + " XP", x, y, 0xFFAAAAAA, false);
        y += 16;
        g.text(this.font, "Unspent points: " + d.statPoints(), x, y, d.statPoints() > 0 ? 0xFF7CE38B : 0xFFAAAAAA, false);
        y += 12;
        y = wrapped(g, "+" + Mastery.STAT_POINTS_PER_LEVEL + " points every mastery level. Earn XP by killing mobs and using active abilities.",
                x, y, 165, 0xFF777777);

        int rx = left + 190;
        g.fill(rx - 8, top + 8, rx - 7, top + panelH() - 8, 0x40FFFFFF);
        int ry = top + 10;
        g.text(this.font, "STATS", rx, ry, 0xFFFFD45A, true);
        ry += 20;
        for (Stat stat : Stat.values()) {
            g.text(this.font, stat.displayName(), rx, ry, 0xFFFFFFFF, false);
            String value = String.valueOf(d.stat(stat));
            g.text(this.font, value, rx + 190 - this.font.width(value), ry, 0xFFFFD45A, false);
            g.text(this.font, stat.perPoint() + " per point", rx, ry + 10, 0xFF777777, false);
            ry += 30;
        }
    }

    private int wrapped(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : this.font.split(Component.literal(text), width)) {
            g.text(this.font, line, x, y, color);
            y += 10;
        }
        return y;
    }
}
