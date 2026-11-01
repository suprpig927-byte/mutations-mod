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
        BLOODTHORN("Hits heal you; much more when you're below 40% health.", false),
        CHAKRAM("Right-click: throw it. It curves toward where you look, cuts through up to 3 mobs, then flies back to you.", true),
        MAUL("Hits call lightning down on the target (every 2s). Hits harder in the rain.", false),
        DAGGERS("Strikes faster with every Phaser speed tier.", false),
        HALBERD("Long reach. Hits slow and frost. Right-click: a line of ice spikes in front of you.", true),
        RIFT_KNIFE("Right-click: throw it and teleport to wherever it lands. It returns to your hand.", true),
        EARTHBREAKER("In Steel Form, every hit sends out a shockwave that hurts and knocks back mobs around the target.", false),
        CAPACITOR("Hits store 1 charge. Right-click: spend 10 charge on a long-range energy slash.", true),
        SHADE_FANG("2.5x damage when you strike from behind or while hidden or invisible.", false),
        SCYTHE("Hits set mobs on fire. Right-click: sweep an arc of flame in front of you.", true),
        WHIP("Long reach. Hits yank mobs toward you and push back the organism's hunger.", false),
        GUNBLADE("Right-click: fire a round of your selected ammo that builds no heat (1s cooldown).", true),
        GLAIVE("Hits deal 50% more while you're in the air. Right-click: a wing-powered dash forward (2s cooldown).", true),
        DUSKBLADE("Hits blind the target for 2s and deal 40% more damage at light level 7 or lower.", false),
        CLEAVER("Every hit adds 2 Radiation Sickness stacks.", false),
        DAWN_SABER("Sets undead on fire and deals 50% more damage to them.", false),
        FLAIL("Right-click hooks the mob you're looking at and yanks it to you (3s cooldown).", true),
        KUKRI("Each hit on the same mob raises its poison by one level, up to Poison IV.", false),
        STAFF("Hits entangle the target (Slowness III, 2s). Right-click bonemeals a 5x5 area (10s cooldown).", true),
        QUARRY_MAUL("Mines like a pickaxe. Hits send a tremor that hurts mobs behind the target.", false),
        PACK_SPEAR("Hits mark the target for 10s; your animals and spirit wolves go after marked mobs.", false),
        HOURGLASS("Hits slow the target (Slowness II, 2s). Right-click rewinds your last 3 seconds (20s cooldown).", true),
        RAPIER("Fast hits that poison, and each hit leaves an insect stinging the target for 3s.", false),
        EVENT_HORIZON("Hits pin the target to the ground (Slowness III, 2s).", false),
        CORAL_TRIDENT("Right-click throws it and it always comes back. Twice as strong in water or rain.", true),
        GEODE_BLADE("Hits have a 25% chance to burst into shards that hit nearby mobs.", false),
        RECOIL_HAMMER("Hits knock mobs twice as far and bounce you up a little.", false),
        SICKLE("Kills with it give 2 souls instead of 1.", false),
        SCALE_HAMMER("Grows with you: quicker but weaker while tiny, crushing while big.", false),
        GAVEL("Hits sometimes knock gold nuggets out of the target. Right-click: 10 Revenue for a slam that stuns mobs around you (5s cooldown).", true),
        MASTER_KEY("Hits deal 50% more on your Property. Right-click: your next hit evicts the mob (5s cooldown).", true);

        public final String power;
        public final boolean rightClick;
        Kind(String power, boolean rightClick) { this.power = power; this.rightClick = rightClick; }
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
            return kind.rightClick ? InteractionResult.SUCCESS : InteractionResult.PASS;
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
