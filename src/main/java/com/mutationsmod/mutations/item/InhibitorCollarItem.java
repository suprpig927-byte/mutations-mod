package com.mutationsmod.mutations.item;

import com.mutationsmod.mutations.ability.Suppression;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Inhibitor Collar: snap it onto a player or mob to shut their powers off for 60 seconds. Single use. */
public class InhibitorCollarItem extends Item {
    public static final int DURATION = 60 * 20;

    public InhibitorCollarItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (target instanceof ServerPlayer p) {
            Suppression.apply(p, DURATION, true);
            p.sendSystemMessage(Component.literal(player.getName().getString() + " locked an Inhibitor Collar on you!").withStyle(ChatFormatting.GREEN));
        } else {
            target.addEffect(new MobEffectInstance(ModContent.SUPPRESSED, DURATION, 0, false, true, true));
        }
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
