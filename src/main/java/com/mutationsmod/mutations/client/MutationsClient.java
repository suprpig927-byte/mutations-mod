package com.mutationsmod.mutations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MutationsMod.MODID, dist = Dist.CLIENT)
public class MutationsClient {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "mutations"));

    public static final KeyMapping OPEN_SCREEN = key("open_screen", InputConstants.KEY_M);
    /** "Ability 1".."Ability 6" trigger the player's active abilities in roster order. */
    public static final KeyMapping[] ABILITY_KEYS = {
            key("ability_1", InputConstants.KEY_R),
            key("ability_2", InputConstants.KEY_V),
            key("ability_3", InputConstants.KEY_G),
            key("ability_4", InputConstants.KEY_B),
            key("ability_5", InputConstants.KEY_N),
            key("ability_6", InputConstants.KEY_K),
    };
    public static final KeyMapping SPEED_UP = key("speed_up", InputConstants.KEY_U);
    public static final KeyMapping SPEED_DOWN = key("speed_down", InputConstants.KEY_J);

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key." + MutationsMod.MODID + "." + name, defaultKey, CATEGORY);
    }

    public MutationsClient(IEventBus modEventBus) {
        modEventBus.addListener(MutationsClient::registerKeys);
        modEventBus.addListener(MutationsClient::registerHud);
        NeoForge.EVENT_BUS.addListener(MutationsClient::onClientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_SCREEN);
        for (KeyMapping k : ABILITY_KEYS) event.register(k);
        event.register(SPEED_UP);
        event.register(SPEED_DOWN);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "hud"), MutationsClient::renderHud);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (OPEN_SCREEN.consumeClick()) {
            mc.gui.setScreen(new MutationScreen());
        }
        for (int i = 0; i < ABILITY_KEYS.length; i++) {
            while (ABILITY_KEYS[i].consumeClick()) {
                ClientPacketDistributor.sendToServer(new ModPayloads.UseAbility(i));
            }
        }
        while (SPEED_UP.consumeClick()) ClientPacketDistributor.sendToServer(new ModPayloads.ChangeSpeed(1));
        while (SPEED_DOWN.consumeClick()) ClientPacketDistributor.sendToServer(new ModPayloads.ChangeSpeed(-1));
    }

    /** Small top-left HUD: your mutation(s) and each active ability's key and cooldown. */
    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        MutationData data = mc.player.getData(ModAttachments.DATA);
        List<Mutation> muts = data.mutationList();
        if (muts.isEmpty()) return;

        Font font = mc.font;
        int x = 4;
        int y = 4;
        Mutation base = muts.get(0);
        graphics.text(font, base.displayName() + "  • Lv " + data.level(), x, y, base.color(), true);
        y += font.lineHeight + 2;

        long now = mc.level.getGameTime();
        List<ActiveAbility> actives = data.actives();
        for (int i = 0; i < actives.size() && i < ABILITY_KEYS.length; i++) {
            ActiveAbility a = actives.get(i);
            long left = data.readyAt(a) - now;
            String keyName = ABILITY_KEYS[i].getTranslatedKeyMessage().getString();
            String status = left > 0 ? formatTime(left) : "ready";
            int color = left > 0 ? 0xFF9A9A9A : 0xFFFFFFFF;
            graphics.text(font, "[" + keyName + "] " + a.displayName() + ": " + status, x, y, color, true);
            y += font.lineHeight + 1;
        }
        if (data.has(Mutation.PHASER) && data.speedTier() > 0) {
            graphics.text(font, "Speed tier " + data.speedTier(), x, y, 0xFFFFE05A, true);
        }
    }

    public static String formatTime(long ticks) {
        long secs = (ticks + 19) / 20;
        if (secs >= 60) return (secs / 60) + "m " + (secs % 60) + "s";
        return secs + "s";
    }
}
