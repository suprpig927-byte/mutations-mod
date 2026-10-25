package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.registries.Registries;
import org.jspecify.annotations.Nullable;

/** A mutant living in the hidden Haven. Trades depend on their role; Keepers guard one mutation's Catalyst. */
public class HavenMutant extends AbstractVillager {
    public enum Role {
        QUARTERMASTER, ARCHIVIST, KEEPER, CARTOGRAPHER, COOK;
        public static Role byId(int i) { return values()[Math.floorMod(i, values().length)]; }
        public String id() { return name().toLowerCase(); }
        public String title() { return name().charAt(0) + name().substring(1).toLowerCase(); }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(HavenMutant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MUTATION = SynchedEntityData.defineId(HavenMutant.class, EntityDataSerializers.INT);
    private static final TagKey<Structure> LAB_MAPS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("mutations", "on_lab_maps"));
    private static final TagKey<Structure> PRISON_MAPS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("mutations", "on_prison_maps"));
    private static final TagKey<Structure> CRATER_MAPS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("mutations", "on_crater_maps"));

    public HavenMutant(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(MUTATION, 0);
    }

    public Role role() { return Role.byId(this.entityData.get(ROLE)); }
    public Mutation mutation() { return Mutation.values()[Math.floorMod(this.entityData.get(MUTATION), Mutation.values().length)]; }

    public void setup(Role role, Mutation mutation, BlockPos home) {
        this.entityData.set(ROLE, role.ordinal());
        this.entityData.set(MUTATION, mutation.ordinal());
        setHomeTo(home, 6);
        setPersistenceRequired();
        setCustomName(Component.literal(role == Role.KEEPER ? mutation.displayName() + " Keeper" : "Haven " + role.title())
                .withStyle(role == Role.KEEPER ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN));
        setCustomNameVisible(false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.3));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading()) {
            if (!this.level().isClientSide()) {
                if (this.getOffers().isEmpty()) return InteractionResult.CONSUME;
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    private static MerchantOffer sell(ItemStack result, ItemCost a, @Nullable ItemCost b, int maxUses) {
        return new MerchantOffer(a, Optional.ofNullable(b), result, maxUses, 5, 0.05f);
    }

    private static ItemCost em(int n) { return new ItemCost(Items.EMERALD, n); }
    private static ItemCost crystals(int n) { return new ItemCost(ModContent.MUTAGEN_CRYSTAL.get(), n); }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers o = this.getOffers();
        switch (role()) {
            case QUARTERMASTER -> {
                o.add(sell(new ItemStack(ModContent.MUTAGEN_CRYSTAL.get(), 4), em(5), null, 12));
                o.add(sell(new ItemStack(ModContent.MUTAGEN_INGOT.get()), em(6), null, 12));
                o.add(sell(new ItemStack(ModContent.NULLITE_SHARD.get()), em(16), crystals(4), 4));
                o.add(sell(new ItemStack(ModContent.CONTAINMENT_ROD.get()), em(24), crystals(6), 2));
                o.add(sell(new ItemStack(Items.EMERALD, 2), crystals(3), null, 16));
            }
            case ARCHIVIST -> {
                o.add(sell(new ItemStack(ModContent.MASTERY_TOME.get()), em(32), crystals(8), 2));
                o.add(sell(new ItemStack(Items.EMERALD), new ItemCost(Items.PAPER, 24), null, 16));
                o.add(sell(new ItemStack(Items.EMERALD, 2), new ItemCost(Items.BOOK, 4), null, 12));
            }
            case KEEPER -> {
                o.add(sell(new ItemStack(Catalysts.BY_MUTATION.get(mutation()).get()), em(64), crystals(32), 1));
                o.add(sell(new ItemStack(ModContent.SPECIMEN_CHAMBER_ITEM.get()), em(12), crystals(2), 4));
            }
            case CARTOGRAPHER -> {
                map(level, o, LAB_MAPS, "Gene Lab Map", 20);
                map(level, o, CRATER_MAPS, "Meteor Crater Map", 28);
                map(level, o, PRISON_MAPS, "Containment Prison Map", 36);
                o.add(sell(new ItemStack(Items.EMERALD), new ItemCost(Items.PAPER, 20), null, 16));
            }
            case COOK -> {
                o.add(sell(new ItemStack(ModContent.GLOWCAP_STEW.get()), em(2), null, 12));
                o.add(sell(new ItemStack(ModContent.CAVE_LOAF.get(), 3), em(1), null, 16));
                o.add(sell(new ItemStack(ModContent.CRYSTAL_CANDY.get(), 6), em(1), null, 16));
                o.add(sell(new ItemStack(ModContent.SMOKED_BLINDFISH.get(), 2), em(2), null, 12));
                o.add(sell(new ItemStack(Items.EMERALD), new ItemCost(Items.BROWN_MUSHROOM, 12), null, 16));
            }
        }
    }

    /** An explorer-style map to the nearest structure of this kind (if one exists within range). */
    private void map(ServerLevel level, MerchantOffers o, TagKey<Structure> tag, String name, int price) {
        BlockPos target = level.findNearestMapStructure(tag, blockPosition(), 100, true);
        if (target == null) return;
        ItemStack stack = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, stack);
        MapItemSavedData.addTargetDecoration(stack, target, "+", MapDecorationTypes.RED_X);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        o.add(sell(stack, em(price), new ItemCost(Items.COMPASS), 1));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(),
                    3 + this.random.nextInt(4)));
        }
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public boolean removeWhenFarAway(double distSqr) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WANDERING_TRADER_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WANDERING_TRADER_DEATH; }
    @Override
    protected SoundEvent getTradeUpdatedSound(boolean valid) { return valid ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO; }
    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.WANDERING_TRADER_YES; }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Role", role().ordinal());
        output.putInt("Mutation", mutation().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(ROLE, input.getIntOr("Role", 0));
        this.entityData.set(MUTATION, input.getIntOr("Mutation", 0));
    }
}
