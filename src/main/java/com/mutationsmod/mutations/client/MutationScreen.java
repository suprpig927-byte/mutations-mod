package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The mutation screen (default key M): your mutations, what they do, your mastery level and XP,
 * stat points, and the secondary-mutation roll slots.
 */
public class MutationScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 220;
    private static final String[] STATS = {"Strength", "Vitality", "Agility", "Endurance", "Luck", "Mutation"};

    public MutationScreen() {
        super(Component.literal("Mutations"));
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), _ -> this.onClose())
                .bounds(left + PANEL_W - 70, top + PANEL_H - 26, 60, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(g, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            drawPanel(g, mc, mc.player.getData(ModAttachments.DATA));
        }
        this.renderables.forEach(r -> r.extractRenderState(g, mouseX, mouseY, partialTick));
    }

    private void drawPanel(GuiGraphicsExtractor g, Minecraft mc, MutationData data) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int mid = left + 200;

        // Panel background + divider
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101018);
        g.fill(left, top, left + PANEL_W, top + 1, 0xFF6B5BD6);
        g.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF6B5BD6);
        g.fill(mid - 6, top + 8, mid - 5, top + PANEL_H - 8, 0x40FFFFFF);

        g.centeredText(this.font, this.title, this.width / 2, top - 12, 0xFFFFFFFF);

        // ---------------- left column: mutations ----------------
        int x = left + 10;
        int y = top + 10;
        List<Mutation> muts = data.mutationList();
        if (muts.isEmpty()) {
            g.text(this.font, "You have no mutation.", x, y, 0xFFAAAAAA, false);
        } else {
            Mutation base = muts.get(0);
            g.text(this.font, base.displayName().toUpperCase(), x, y, base.color(), true);
            g.text(this.font, "BASE", x + this.font.width(base.displayName().toUpperCase()) + 6, y, 0xFF777777, false);
            y += 13;
            y = wrapped(g, "+ " + base.upside(), x, y, 182, 0xFF7CE38B);
            y = wrapped(g, "- " + base.downside(), x, y, 182, 0xFFE07C7C);
            y += 4;

            long now = mc.level.getGameTime();
            List<ActiveAbility> actives = data.actives();
            for (int i = 0; i < actives.size(); i++) {
                ActiveAbility a = actives.get(i);
                String key = i < MutationsClient.ABILITY_KEYS.length
                        ? MutationsClient.ABILITY_KEYS[i].getTranslatedKeyMessage().getString() : "-";
                long left2 = data.readyAt(a) - now;
                String status = left2 > 0 ? MutationsClient.formatTime(left2) : "ready";
                g.text(this.font, "[" + key + "] " + a.displayName(), x, y, 0xFFFFFFFF, false);
                g.text(this.font, status, mid - 12 - this.font.width(status), y, left2 > 0 ? 0xFF999999 : 0xFF7CE38B, false);
                y += 10;
                y = wrapped(g, a.description(), x + 8, y, 174, 0xFFAAAAAA);
                y += 2;
            }
            if (data.has(Mutation.PHASER)) {
                String keys = MutationsClient.SPEED_UP.getTranslatedKeyMessage().getString() + "/"
                        + MutationsClient.SPEED_DOWN.getTranslatedKeyMessage().getString();
                g.text(this.font, "Speed tier " + data.speedTier() + " / " + Math.min(5, 1 + data.level() / 8) + "  [" + keys + "]",
                        x, y, 0xFFFFE05A, false);
                y += 12;
            }

            // Extra mutations given by commands (secondary rolls come in a later version)
            for (int i = 1; i < muts.size(); i++) {
                Mutation m = muts.get(i);
                g.text(this.font, "+ " + m.displayName(), x, y, m.color(), false);
                y += 10;
            }
        }

        // Secondary roll slots
        int slotY = top + PANEL_H - 50;
        g.text(this.font, "Secondary mutations", x, slotY, 0xFFBBBBBB, false);
        slotY += 12;
        for (int i = 0; i < Mastery.ROLL_LEVELS.length; i++) {
            int lvl = Mastery.ROLL_LEVELS[i];
            int sx = x + i * 62;
            boolean unlocked = data.level() >= lvl;
            g.fill(sx, slotY, sx + 58, slotY + 16, unlocked ? 0x606B5BD6 : 0x40FFFFFF);
            String label = unlocked ? "Unlocked" : "Lv " + lvl;
            g.text(this.font, label, sx + (58 - this.font.width(label)) / 2, slotY + 4, unlocked ? 0xFFE0D8FF : 0xFF888888, false);
        }

        // ---------------- right column: mastery & stats ----------------
        int rx = mid + 4;
        int ry = top + 10;
        g.text(this.font, "MASTERY", rx, ry, 0xFFFFD45A, true);
        ry += 13;
        g.text(this.font, "Level " + data.level(), rx, ry, 0xFFFFFFFF, false);
        ry += 12;
        int need = Mastery.xpToNext(data.level());
        int barW = 120;
        int filled = (int) (barW * Math.min(1.0, data.xp() / (double) need));
        g.fill(rx, ry, rx + barW, ry + 6, 0xFF2A2A35);
        g.fill(rx, ry, rx + filled, ry + 6, 0xFF8FD14F);
        ry += 9;
        g.text(this.font, data.xp() + " / " + need + " XP", rx, ry, 0xFFAAAAAA, false);
        ry += 16;

        g.text(this.font, "STATS", rx, ry, 0xFFFFD45A, true);
        g.text(this.font, data.statPoints() + " pts", rx + 120 - this.font.width(data.statPoints() + " pts"), ry, 0xFFFFFFFF, false);
        ry += 13;
        for (String stat : STATS) {
            g.text(this.font, stat, rx, ry, 0xFF888888, false);
            g.text(this.font, "0", rx + 112, ry, 0xFF888888, false);
            ry += 11;
        }
        ry += 2;
        g.text(this.font, "Spending points", rx, ry, 0xFF666666, false);
        g.text(this.font, "arrives next update", rx, ry + 10, 0xFF666666, false);
    }

    private int wrapped(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : this.font.split(Component.literal(text), width)) {
            g.text(this.font, line, x, y, color);
            y += 10;
        }
        return y;
    }
}
