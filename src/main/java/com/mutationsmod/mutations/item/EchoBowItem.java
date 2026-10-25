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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Siren's weapon: arrows burst in a sonic boom where they hit. */
public class EchoBowItem extends BowItem {
    public static final String TAG = "mutations_echo_arrow";

    public EchoBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && !Weapons.canWield(sp, ModAttachments.get(sp), this)) return InteractionResult.FAIL;
        return super.use(level, player, hand);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
        Projectile p = super.createProjectile(level, shooter, weapon, projectile, isCrit);
        p.addTag(TAG);
        return p;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.literal("Siren weapon").withStyle(s -> s.withColor(Mutation.SIREN.color() & 0xFFFFFF)));
        out.accept(Component.literal("Arrows burst in a sonic boom where they hit, hurting and dazing everything nearby.").withStyle(ChatFormatting.GRAY));
        out.accept(Component.literal("Only a Siren can wield it.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
