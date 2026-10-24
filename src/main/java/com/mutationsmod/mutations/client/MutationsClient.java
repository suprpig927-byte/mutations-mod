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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import com.mutationsmod.mutations.ability.Telekinesis;
import com.mutationsmod.mutations.ability.power.Absorber;
import com.mutationsmod.mutations.ability.power.Chameleon;
import com.mutationsmod.mutations.ability.power.Host;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import com.mutationsmod.mutations.client.render.HostLayer;
import com.mutationsmod.mutations.client.render.CryoLayer;
import com.mutationsmod.mutations.client.render.ColossusLayer;
import com.mutationsmod.mutations.client.render.ColossusRenderData;
import com.mutationsmod.mutations.client.render.CryoRenderData;
import com.mutationsmod.mutations.client.render.CrystalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import com.mutationsmod.mutations.client.render.FirstPersonTendrils;
import com.mutationsmod.mutations.client.render.HostRenderData;
import com.mutationsmod.mutations.client.render.HostRenderLogic;
import com.mutationsmod.mutations.client.render.TendrilModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        modEventBus.addListener(MutationsClient::registerRenderState);
        modEventBus.addListener(MutationsClient::registerLayerDefinitions);
        modEventBus.addListener(MutationsClient::addLayers);
        NeoForge.EVENT_BUS.addListener(MutationsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MutationsClient::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(FirstPersonTendrils::onSubmit);
        NeoForge.EVENT_BUS.addListener(ColossusLayer::onRenderPre);
        NeoForge.EVENT_BUS.addListener(ColossusLayer::onRenderPost);
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

    /** Siren / Spatial Sense: draws a glowing outline on creatures only this player can sense. */
    private static void registerRenderState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    if (ClientPowers.GLOWING.contains(entity.getId())) state.outlineColor = 0xFFFFFFFF;
                });
        // Chameleon's Mirror Skin: armor turns invisible along with the player.
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                // Host: tell the organism layer how to draw this player.
                if (avatar instanceof Player hostPlayer) {
                    MutationData hd = hostPlayer.getData(ModAttachments.DATA);
                    if (hd.has(Mutation.HOST)) {
                        float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                        state.setRenderData(HostRenderData.KEY, HostRenderLogic.compute(hostPlayer, hd, state.bodyRot, pt));
                    } else {
                        state.setRenderData(HostRenderData.KEY, null);
                    }
                    // Colossus: steel body, cracks while Ground Pound's effect lasts.
                    if (hd.has(Mutation.COLOSSUS)) {
                        boolean cracks = hd.timer("pound_anim") > hostPlayer.level().getGameTime();
                        state.setRenderData(ColossusRenderData.KEY, new ColossusRenderData(cracks));
                    } else {
                        state.setRenderData(ColossusRenderData.KEY, null);
                    }
                    // Cryo: crystals shrink in the heat and pulse on Ice Blast.
                    if (hd.has(Mutation.CRYO)) {
                        float scale = CryoVisuals.hot(hostPlayer) ? 0.5f : 1f;
                        long t = hostPlayer.level().getGameTime();
                        float pulse = Math.max(0, hd.timer("iceblast_anim") - t) / 8f;
                        state.setRenderData(CryoRenderData.KEY, new CryoRenderData(scale * (1f + 0.35f * pulse)));
                    } else {
                        state.setRenderData(CryoRenderData.KEY, null);
                    }
                }
                if (avatar instanceof Player player && player.isInvisible()
                        && player.getData(ModAttachments.DATA).has(Secondary.MIRROR_SKIN)) {
                    state.headEquipment = ItemStack.EMPTY;
                    state.chestEquipment = ItemStack.EMPTY;
                    state.legsEquipment = ItemStack.EMPTY;
                    state.feetEquipment = ItemStack.EMPTY;
                }
            }
        });
    }

    public static final ModelLayerLocation TENDRILS =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "host_tendrils"), "main");

    public static final ModelLayerLocation CRYSTALS =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "cryo_crystals"), "main");
    /** Frost shells slightly bigger than armor: helmet, chestplate, leggings, boots. */
    public static final ModelLayerLocation[] FROST_ARMOR = {
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "frost_armor"), "head"),
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "frost_armor"), "chest"),
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "frost_armor"), "legs"),
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MutationsMod.MODID, "frost_armor"), "feet")};

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TENDRILS, TendrilModel::createLayer);
        event.registerLayerDefinition(CRYSTALS, CrystalModel::createLayer);
        var frost = HumanoidModel.createArmorMeshSet(new CubeDeformation(0.6f), new CubeDeformation(1.1f));
        event.registerLayerDefinition(FROST_ARMOR[0], () -> LayerDefinition.create(frost.head(), 64, 32));
        event.registerLayerDefinition(FROST_ARMOR[1], () -> LayerDefinition.create(frost.chest(), 64, 32));
        event.registerLayerDefinition(FROST_ARMOR[2], () -> LayerDefinition.create(frost.legs(), 64, 32));
        event.registerLayerDefinition(FROST_ARMOR[3], () -> LayerDefinition.create(frost.feet(), 64, 32));
    }

    /** Adds the Host organism layer to both player models (wide and slim arms). */
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
            if (renderer != null) {
                var models = event.getEntityModels();
                renderer.addLayer(new HostLayer(renderer, new TendrilModel(models.bakeLayer(TENDRILS))));
                renderer.addLayer(new ColossusLayer(renderer));
                renderer.addLayer(new CryoLayer(renderer, new CrystalModel(models.bakeLayer(CRYSTALS)),
                        new HumanoidModel<>(models.bakeLayer(FROST_ARMOR[0])), new HumanoidModel<>(models.bakeLayer(FROST_ARMOR[1])),
                        new HumanoidModel<>(models.bakeLayer(FROST_ARMOR[2])), new HumanoidModel<>(models.bakeLayer(FROST_ARMOR[3]))));
            }
        }
        FirstPersonTendrils.model = new TendrilModel(event.getEntityModels().bakeLayer(TENDRILS));
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
        if (mc.level != null) ClientPowers.tick(mc, mc.player, mc.player.getData(ModAttachments.DATA));
        CryoVisuals.tick(mc);
    }

    /**
     * Glues the block you're holding with telekinesis to your view every frame (not just 20 times
     * a second), so it flows smoothly as you look around.
     */
    private static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (ModPayloads.LOCAL_HELD.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        long nowMs = System.currentTimeMillis();
        for (var entry : List.copyOf(ModPayloads.LOCAL_HELD.entrySet())) {
            Entity block = mc.level.getEntity(entry.getKey());
            Long seen = ModPayloads.LOCAL_HELD_SEEN.get(entry.getKey());
            if (block == null || seen == null || nowMs - seen > 500) {
                ModPayloads.LOCAL_HELD.remove(entry.getKey());
                ModPayloads.LOCAL_HELD_SEEN.remove(entry.getKey());
                continue;
            }
            placeHeld(event, mc, player, block, entry.getValue());
        }
    }

    private static void placeHeld(RenderFrameEvent.Pre event, Minecraft mc, LocalPlayer player, Entity block, int slot) {
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 eye = new Vec3(
                Mth.lerp(pt, player.xo, player.getX()),
                Mth.lerp(pt, player.yo, player.getY()) + player.getEyeHeight(),
                Mth.lerp(pt, player.zo, player.getZ()));
        float yaw = player.getViewYRot(pt) * Mth.DEG_TO_RAD;
        float pitch = player.getViewXRot(pt) * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 target = Telekinesis.holdPosition(eye, look, slot);

        AABB box = block.getBoundingBox().move(target.subtract(block.position()));
        if (!mc.level.noCollision(block, box)) return; // don't push it through walls
        block.setPos(target);
        block.setDeltaMovement(Vec3.ZERO);
        // Same position for "last tick" too, so the renderer draws it exactly here this frame.
        block.xo = block.xOld = target.x;
        block.yo = block.yOld = target.y;
        block.zo = block.zOld = target.z;
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
        MutationIcons.draw(graphics, base, x, y, 16);
        graphics.text(font, base.displayName() + "  • Lv " + data.level(), x + 20, y + 4, base.color(), true);
        y += 20;

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
        y += 2;
        for (String line : statusLines(mc, data, now)) {
            graphics.text(font, line, x, y, 0xFFE0E0E0, true);
            y += font.lineHeight + 1;
        }
        if (data.has(Secondary.TUNED_EARS) && data.has(Mutation.SIREN)) {
            for (String line : ClientPowers.tunedEars(mc.level, mc.player)) {
                graphics.text(font, line, x, y, 0xFFFF8FB8, true);
                y += font.lineHeight + 1;
            }
        }
    }

    /** Extra HUD lines: meters and timers for whatever mutations you have. */
    private static List<String> statusLines(Minecraft mc, MutationData d, long now) {
        List<String> out = new java.util.ArrayList<>();
        if (d.has(Mutation.PHASER) && d.speedTier() > 0) out.add("Speed tier " + d.speedTier());
        if (d.has(Mutation.ABSORBER)) {
            float cap = Absorber.capacity(d);
            out.add(String.format("Charge %.0f / %.0f", d.charge(), cap) + (d.charge() > cap ? "  OVERCHARGED!" : ""));
        }
        if (d.has(Mutation.HOST) && d.timer("host_fed") > 0) {
            long left = d.timer("host_fed") + Host.graceTicks(d) - now;
            out.add(left > 0 ? "Organism fed: " + formatTime(left) : "Organism STARVING");
        }
        if (d.has(Mutation.PYRO) && d.timer("flame_flight") > now) out.add("Flame Flight: " + formatTime(d.timer("flame_flight") - now));
        if (d.has(Secondary.ITEM_MAGNET) && d.timer("magnet") > 0) out.add("Item Magnet: ON");
        if (d.timer("levitate") > now) out.add("Levitating: " + formatTime(d.timer("levitate") - now));
        if (d.timer("barrier") > now) out.add("Barrier: " + formatTime(d.timer("barrier") - now));
        if (d.has(Mutation.HOST)) out.add(d.timer("bonded") > 0 ? "Organism: BONDED" : "Organism: dormant");
        if (d.timer("phasing") > now) out.add("Phasing: " + formatTime(d.timer("phasing") - now));
        if (d.timer("vibrate") > now) out.add("Vibrating!");
        if (d.timer("immune") > now) out.add("IMMUNE: " + formatTime(d.timer("immune") - now));
        for (Chameleon.Trait t : Chameleon.Trait.values()) {
            if (d.timer(t.timer()) > now) out.add("Copycat (" + t.label + "): " + formatTime(d.timer(t.timer()) - now));
        }
        if (Mastery.canRoll(d) || !d.rollOptions().isEmpty()) out.add("Secondary roll ready! (" + OPEN_SCREEN.getTranslatedKeyMessage().getString() + ")");
        else if (d.statPoints() > 0) out.add(d.statPoints() + " stat points to spend (" + OPEN_SCREEN.getTranslatedKeyMessage().getString() + ")");
        return out;
    }

    public static String formatTime(long ticks) {
        long secs = (ticks + 19) / 20;
        if (secs >= 60) return (secs / 60) + "m " + (secs % 60) + "s";
        return secs + "s";
    }
}
