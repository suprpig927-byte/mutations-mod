package com.mutationsmod.mutations;

import com.mojang.logging.LogUtils;
import com.mutationsmod.mutations.ability.AbilityHandler;
import com.mutationsmod.mutations.command.MutationCommand;
import com.mutationsmod.mutations.network.ModPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MutationsMod.MODID)
public class MutationsMod {
    public static final String MODID = "mutations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MutationsMod(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        com.mutationsmod.mutations.registry.ModContent.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(AbilityHandler::onLogin);
        game.addListener(AbilityHandler::onLogout);
        game.addListener(AbilityHandler::onRespawn);
        game.addListener(AbilityHandler::onChangeDimension);
        game.addListener(AbilityHandler::onPlayerTick);
        game.addListener(AbilityHandler::onLevelTick);
        game.addListener(AbilityHandler::onIncomingDamage);
        game.addListener(AbilityHandler::onEffectApplicable);
        game.addListener(AbilityHandler::onStruckByLightning);
        game.addListener(AbilityHandler::onDeath);
        game.addListener(AbilityHandler::onDrops);
        game.addListener(AbilityHandler::onChangeTarget);
        game.addListener(AbilityHandler::onRightClickItem);
        game.addListener(MutationCommand::register);
        game.addListener(com.mutationsmod.mutations.ability.Weapons::onProjectileImpact);
        game.addListener(com.mutationsmod.mutations.ability.extra.ShadowPower::onBreak);
        game.addListener(com.mutationsmod.mutations.ability.extra.VerdantPower::onBreak);
        game.addListener(com.mutationsmod.mutations.ability.extra.EarthshaperPower::onBreakSpeed);
        game.addListener(com.mutationsmod.mutations.ability.extra.VerdantPower::onRightClickBlock);
        game.addListener(com.mutationsmod.mutations.ability.extra.VerdantPower::onUseStart);
        game.addListener(com.mutationsmod.mutations.ability.extra.VerdantPower::onUseFinish);
        game.addListener(com.mutationsmod.mutations.ability.extra.SoulbinderPower::onHeal);
        game.addListener(com.mutationsmod.mutations.ability.extra.LandlordPower::onFinalizeSpawn);
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            if (e.getLevel().isClientSide()) return;
            com.mutationsmod.mutations.ability.extra.BeastcallerPower.onInteract(e);
            if (!e.isCanceled()) com.mutationsmod.mutations.ability.extra.MicroPower.onInteract(e);
            if (!e.isCanceled() && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) com.mutationsmod.mutations.ability.extra.TaxManPower.onVillagerInteract(sp, e.getTarget());
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) com.mutationsmod.mutations.ability.extra.SoulbinderPower.onLogin(sp);
        });
        // Omega decoys left behind in unloaded chunks reload as plain mannequins: remove them.
        game.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && e.getEntity().entityTags().contains(com.mutationsmod.mutations.entity.Decoy.TAG)
                    && !(e.getEntity() instanceof com.mutationsmod.mutations.entity.Decoy)) e.setCanceled(true);
            if (!e.getLevel().isClientSide() && e.loadedFromDisk() && e.getEntity().entityTags().contains(com.mutationsmod.mutations.ability.extra.Minions.TAG)) e.setCanceled(true);
        });
    }
}
