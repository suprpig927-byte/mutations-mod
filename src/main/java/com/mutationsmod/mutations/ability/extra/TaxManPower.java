package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Tax Man: every kill pays Revenue; spend it on Levies, Garnishes and Bribes. */
public class TaxManPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.TAX_MAN; }

    public static final String REVENUE = "revenue";
    public static final int MAX = 100;

    static boolean ledger(ServerPlayer p) { return Catalysts.holding(p, Mutation.TAX_MAN); }

    static int cost(MutationData d, int base) { return d.has(Secondary.DEDUCTIONS) ? Math.round(base * 0.7f) : base; }

    static boolean pay(ServerPlayer p, MutationData d, int base) {
        int c = cost(d, base), have = Kit.meter(d, REVENUE);
        if (have < c) {
            p.sendOverlayMessage(Component.literal("Needs " + c + " Revenue (you have " + have + ").").withStyle(ChatFormatting.GREEN));
            return false;
        }
        Kit.setMeter(p, REVENUE, have - c);
        return true;
    }

    static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity it = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
        it.setDefaultPickUpDelay();
        level.addFreshEntity(it);
    }

    static void coins(ServerLevel level, Vec3 at, net.minecraft.util.RandomSource r) {
        drop(level, at, r.nextBoolean() ? new ItemStack(Items.EMERALD) : new ItemStack(Items.GOLD_NUGGET, 2 + r.nextInt(3)));
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        if (now % 40 == 0) {
            for (IronGolem g : level.getEntitiesOfClass(IronGolem.class, p.getBoundingBox().inflate(16), g -> !g.isPlayerCreated() && !Minions.isMinion(g) && g.getTarget() == null)) {
                g.setTarget(p);
            }
        }
        if (d.has(Secondary.COMPOUND_INTEREST) && now % 200 == 0 && Kit.meter(d, REVENUE) >= 50) Kit.addMeter(p, REVENUE, 1, MAX);
        if (d.has(Secondary.TAX_HAVEN) && now % 50 == 0
                && !level.getEntitiesOfClass(AbstractVillager.class, p.getBoundingBox().inflate(12)).isEmpty()) {
            Powers.effect(p, MobEffects.REGENERATION, 60, 0);
        }
        if (Powers.timerActive(d, level, "tax_season")) {
            if (now % 20 == 0) {
                for (LivingEntity e : Kit.hostiles(p, 24)) {
                    Marks.set(e, "taxed", now + 40, 0);
                    Powers.hurt(p, e, 3f * Powers.power(d));
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 3, 0.2, 0.1, 0.2, 0);
                }
            }
        }
        if (now % 5 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> Marks.has(e, "levy") && Marks.value(e, "levy") == p.getId())) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getBbHeight() + 0.4, e.getZ(), 1, 0.1, 0.1, 0.1, 0);
                if (d.has(Secondary.COLLECTIONS_AGENCY)) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 1, false, false));
            }
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (d.has(Secondary.GOLDEN_PARACHUTE) && e.getSource().is(DamageTypes.FALL)) {
            int hearts = (int) Math.ceil(e.getAmount() / 2f);
            int have = Kit.meter(d, REVENUE);
            if (have > 0) {
                int paid = Math.min(have, hearts);
                Kit.setMeter(p, REVENUE, have - paid);
                float rest = e.getAmount() - paid * 2;
                if (rest <= 0) e.setCanceled(true); else e.setAmount(rest);
            }
        }
    }

    @Override
    public void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (Marks.has(target, "levy") && Marks.value(target, "levy") == p.getId()) e.setAmount(e.getAmount() * 1.2f);
    }

    @Override
    public void onKill(ServerPlayer p, MutationData d, LivingEntity victim) {
        if (Minions.isMinion(victim) || victim instanceof net.minecraft.world.entity.player.Player) return;
        int gain = Math.max(1, Math.round(victim.getMaxHealth() * 0.1f * (ledger(p) ? 2 : 1)));
        if (Marks.has(victim, "taxed")) gain += 5;
        Kit.addMeter(p, REVENUE, gain, MAX);
        ServerLevel level = (ServerLevel) p.level();
        if (p.getRandom().nextFloat() < 0.2f) coins(level, victim.position(), p.getRandom());
        if (Marks.has(victim, "taxed")) drop(level, victim.position(), new ItemStack(Items.EMERALD));
    }

    @Override
    public void onDrops(LivingDropsEvent e, ServerPlayer p, MutationData d) {
        LivingEntity v = e.getEntity();
        if (!(Marks.has(v, "levy") && Marks.value(v, "levy") == p.getId())) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity it : e.getDrops()) {
            ItemEntity copy = new ItemEntity(v.level(), it.getX(), it.getY(), it.getZ(), it.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        e.getDrops().addAll(extra);
    }

    @Override
    public boolean cheatDeath(LivingDeathEvent e, ServerPlayer p, MutationData d) {
        if (!d.has(Secondary.LOOPHOLE) || Powers.timerActive(d, p.level(), "loophole_cd") || Kit.meter(d, REVENUE) < 50) return false;
        Kit.setMeter(p, REVENUE, Kit.meter(d, REVENUE) - 50);
        Powers.setTimer(p, "loophole_cd", 12000);
        p.setHealth(4f);
        p.sendOverlayMessage(Component.literal("Loophole! 50 Revenue paid off death.").withStyle(ChatFormatting.GREEN));
        Powers.particles(p, ParticleTypes.HAPPY_VILLAGER, 30, 0.6, 0.1);
        Powers.sound(p, SoundEvents.VILLAGER_YES, 1.5f, 0.8f);
        return true;
    }

    @Override
    public void onDeath(ServerPlayer p, MutationData d) {
        Kit.setMeter(p, REVENUE, ledger(p) ? Kit.meter(d, REVENUE) / 2 : 0);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        switch (a) {
            case LEVY -> {
                LivingEntity t = Kit.look(p, 24);
                if (t == null) return false;
                Marks.set(t, "levy", now + 300, p.getId());
                Kit.glow(t, 300);
                Powers.sound(p, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.5f, 1f);
                return true;
            }
            case AUDIT -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), e -> e != p && e.isAlive())) Kit.glow(e, 200);
                Powers.setTimer(p, "audit", 200);
                Powers.sound(p, SoundEvents.BOOK_PAGE_TURN, 2f, 0.8f);
                return true;
            }
            case GARNISH -> {
                List<LivingEntity> foes = Kit.hostiles(p, 8);
                if (foes.isEmpty() || !pay(p, d, 20)) return false;
                for (LivingEntity e : foes) {
                    Powers.hurt(p, e, 4f);
                    p.heal(1.5f);
                    Powers.line(level, e.getBoundingBox().getCenter(), p.getEyePosition(), ParticleTypes.HAPPY_VILLAGER, 0.6);
                }
                Powers.sound(p, SoundEvents.VILLAGER_TRADE, 1.5f, 0.7f);
                return true;
            }
            case BRIBE -> {
                LivingEntity t = Kit.look(p, 16);
                if (!(t instanceof Mob m) || !pay(p, d, 30)) return false;
                Marks.set(m, "bribed", now + 400, p.getId());
                if (m.getTarget() == p) m.setTarget(null);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 15, 0.3, 0.3, 0.3, 0);
                Powers.sound(p, SoundEvents.VILLAGER_YES, 1.5f, 1f);
                return true;
            }
            case REPOSSESS -> {
                LivingEntity t = Kit.look(p, 12);
                if (!(t instanceof Mob m)) return false;
                boolean any = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE) continue;
                    ItemStack s = m.getItemBySlot(slot);
                    if (s.isEmpty()) continue;
                    drop(level, m.position(), s.copy());
                    m.setItemSlot(slot, ItemStack.EMPTY);
                    any = true;
                }
                if (!any) { p.sendOverlayMessage(Component.literal("Nothing to repossess.").withStyle(ChatFormatting.GRAY)); return false; }
                Powers.sound(p, SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.5f, 0.6f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "tax_season", 400);
        Kit.setMeter(p, REVENUE, MAX);
        Powers.particles(p, ParticleTypes.HAPPY_VILLAGER, 60, 3, 0.1);
        Powers.sound(p, SoundEvents.VILLAGER_CELEBRATE, 2f, 0.7f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (p.getRandom().nextFloat() < 0.15f) drop((ServerLevel) p.level(), target.position(), new ItemStack(Items.GOLD_NUGGET, 1 + p.getRandom().nextInt(2)));
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        if (Powers.timerActive(d, p.level(), "gavel_cd") || !pay(p, d, 10)) return false;
        for (LivingEntity e : Kit.foes(p, 4)) Kit.root(e, 40);
        Powers.setTimer(p, "gavel_cd", 100);
        Powers.particles(p, ParticleTypes.CRIT, 30, 2, 0.1);
        Powers.sound(p, SoundEvents.ANVIL_PLACE, 1f, 1.4f);
        return true;
    }

    /** Villagers overcharge the tax man (unless he runs a Tax Haven). */
    public static void onVillagerInteract(ServerPlayer p, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof net.minecraft.world.entity.npc.villager.Villager v)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.TAX_MAN) || d.has(Secondary.TAX_HAVEN) || com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) return;
        long now = p.level().getGameTime();
        if (Marks.has(v, "taxman_gossip")) return;
        Marks.set(v, "taxman_gossip", now + 6000, 0);
        v.getGossips().add(p.getUUID(), net.minecraft.world.entity.ai.gossip.GossipType.MINOR_NEGATIVE, 25);
    }
}
