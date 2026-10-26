package com.mutationsmod.mutations.item;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Weapons;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A mutation's specialized weapon. Only that mutation can hold it; the special power is theirs alone. */
public class MutantWeapon extends Item {
    public enum Kind {
        BLOODTHORN("Hits heal you; much more when you're below 40% health."),
        CHAKRAM("Right-click: throw it. It curves toward where you look, cuts through up to 3 mobs, then flies back to you."),
        MAUL("Hits call lightning down on the target (every 2s). Hits harder in the rain."),
        DAGGERS("Strikes faster with every Phaser speed tier."),
        HALBERD("Long reach. Hits slow and frost. Right-click: a line of ice spikes in front of you."),
        RIFT_KNIFE("Right-click: throw it and teleport to wherever it lands. It returns to your hand."),
        EARTHBREAKER("In Steel Form, every hit sends out a shockwave that hurts and knocks back mobs around the target."),
        CAPACITOR("Hits store 1 charge. Right-click: spend 10 charge on a long-range energy slash."),
        SHADE_FANG("2.5x damage when you strike from behind or while hidden or invisible."),
        SCYTHE("Hits set mobs on fire. Right-click: sweep an arc of flame in front of you."),
        WHIP("Long reach. Hits yank mobs toward you and push back the organism's hunger."),
        GUNBLADE("Right-click: fire a round of your selected ammo that builds no heat (1s cooldown)."),
        GLAIVE("Hits deal 50% more while you're in the air. Right-click: a wing-powered dash forward (2s cooldown).");

        public final String power;
        Kind(String power) { this.power = power; }
    }

    private final Mutation mutation;
    private final Kind kind;

    public MutantWeapon(Item.Properties properties, Mutation mutation, Kind kind) {
        super(properties);
        this.mutation = mutation;
        this.kind = kind;
    }

    public Mutation mutation() { return mutation; }
    public Kind kind() { return kind; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) {
            return kind == Kind.CHAKRAM || kind == Kind.RIFT_KNIFE || kind == Kind.HALBERD || kind == Kind.CAPACITOR || kind == Kind.SCYTHE
                    || kind == Kind.GUNBLADE || kind == Kind.GLAIVE
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!Weapons.canWield(sp, ModAttachments.get(sp), this)) return InteractionResult.FAIL;
        return Weapons.use(sp, stack, hand, this) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.literal(mutation.displayName() + " weapon").withStyle(s -> s.withColor(mutation.color() & 0xFFFFFF)));
        out.accept(Component.literal(kind.power).withStyle(ChatFormatting.GRAY));
        out.accept(Component.literal("Only a " + mutation.displayName() + " can wield it.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
