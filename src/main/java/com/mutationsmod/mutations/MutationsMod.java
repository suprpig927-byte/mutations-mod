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
        // Omega decoys left behind in unloaded chunks reload as plain mannequins: remove them.
        game.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && e.getEntity().getTags().contains(com.mutationsmod.mutations.entity.Decoy.TAG)
                    && !(e.getEntity() instanceof com.mutationsmod.mutations.entity.Decoy)) e.setCanceled(true);
        });
    }
}
