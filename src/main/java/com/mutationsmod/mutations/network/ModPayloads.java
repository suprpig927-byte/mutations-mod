package com.mutationsmod.mutations.network;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.AbilityHandler;
import com.mutationsmod.mutations.ability.power.Blinker;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.Stat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, path);
    }

    /** Client -> server: the player pressed "Ability N" (slot is 0-based). */
    public record UseAbility(int slot) implements CustomPacketPayload {
        public static final Type<UseAbility> TYPE = new Type<>(id("use_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseAbility> CODEC =
                ByteBufCodecs.VAR_INT.map(UseAbility::new, UseAbility::slot).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: Phaser speed tier up (+1) or down (-1). */
    public record ChangeSpeed(int delta) implements CustomPacketPayload {
        public static final Type<ChangeSpeed> TYPE = new Type<>(id("change_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeSpeed> CODEC =
                ByteBufCodecs.VAR_INT.map(ChangeSpeed::new, ChangeSpeed::delta).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: spend stat points. */
    public record SpendStat(String stat, int amount) implements CustomPacketPayload {
        public static final Type<SpendStat> TYPE = new Type<>(id("spend_stat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpendStat> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, SpendStat p) -> {
                    buf.writeUtf(p.stat);
                    buf.writeVarInt(p.amount);
                },
                buf -> new SpendStat(buf.readUtf(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Client -> server: a screen button. 0 = respec, 1 = start a secondary roll,
     * 2 = choose roll option {@code arg}, 3 = open Pocket Space.
     */
    public record ScreenAction(int action, String arg) implements CustomPacketPayload {
        public static final int RESPEC = 0, START_ROLL = 1, CHOOSE_ROLL = 2, OPEN_POCKET = 3, PHASE = 4,
                SPLICE_RESPEC = 5, SPLICE_REROLL = 6;
        public static final Type<ScreenAction> TYPE = new Type<>(id("screen_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScreenAction> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, ScreenAction p) -> {
                    buf.writeVarInt(p.action);
                    buf.writeUtf(p.arg);
                },
                buf -> new ScreenAction(buf.readVarInt(), buf.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: add velocity to the local player. Player movement is controlled by the
     * client, so the server has to ask the client to apply dashes and knockback to players.
     */
    public record Impulse(double x, double y, double z, boolean replace) implements CustomPacketPayload {
        public static final Type<Impulse> TYPE = new Type<>(id("impulse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Impulse> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Impulse p) -> {
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                    buf.writeBoolean(p.replace);
                },
                buf -> new Impulse(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> nearby clients: exact position of a telekinetic block, every tick.
     * {@code holderId} is the entity id of the player holding it (-1 while flying);
     * {@code slot} is which of their held blocks it is (0 = center, 1 = left, 2 = right).
     */
    public record KineticBlock(int entityId, int holderId, int slot, double x, double y, double z) implements CustomPacketPayload {
        public static final Type<KineticBlock> TYPE = new Type<>(id("kinetic_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, KineticBlock> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, KineticBlock p) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeVarInt(p.holderId);
                    buf.writeVarInt(p.slot);
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                },
                buf -> new KineticBlock(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> clients: a Host's tendril shot out and grabbed something (entity id, or -1 for a block position). */
    public record TendrilStrike(int playerId, int targetEntity, double x, double y, double z, int ticks) implements CustomPacketPayload {
        public static final Type<TendrilStrike> TYPE = new Type<>(id("tendril_strike"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TendrilStrike> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, TendrilStrike p) -> {
                    buf.writeVarInt(p.playerId);
                    buf.writeVarInt(p.targetEntity + 1);
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                    buf.writeVarInt(p.ticks);
                },
                buf -> new TendrilStrike(buf.readVarInt(), buf.readVarInt() - 1, buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: open the Gene Splicer screen. */
    public record OpenSplicer() implements CustomPacketPayload {
        public static final Type<OpenSplicer> TYPE = new Type<>(id("open_splicer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSplicer> CODEC = StreamCodec.unit(new OpenSplicer());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client-side: blocks the local player is holding. entity id -> slot, plus last update time. */
    public static final Map<Integer, Integer> LOCAL_HELD = new ConcurrentHashMap<>();
    public static final Map<Integer, Long> LOCAL_HELD_SEEN = new ConcurrentHashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");

        registrar.playToServer(UseAbility.TYPE, UseAbility.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) AbilityHandler.useSlot(sp, payload.slot());
        });

        registrar.playToServer(ChangeSpeed.TYPE, ChangeSpeed.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) AbilityHandler.changeSpeedTier(sp, payload.delta());
        });

        registrar.playToServer(SpendStat.TYPE, SpendStat.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Stat.byId(payload.stat()).ifPresent(s -> Mastery.spendStat(sp, s, Math.max(1, Math.min(100, payload.amount()))));
            }
        });

        registrar.playToServer(ScreenAction.TYPE, ScreenAction.CODEC, (payload, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            switch (payload.action()) {
                case ScreenAction.RESPEC -> Mastery.respec(sp, false);
                case ScreenAction.START_ROLL -> Mastery.startRoll(sp, false);
                case ScreenAction.CHOOSE_ROLL -> Mastery.chooseRoll(sp, payload.arg());
                case ScreenAction.OPEN_POCKET -> Blinker.openPocket(sp);
                case ScreenAction.PHASE -> com.mutationsmod.mutations.ability.power.Phaser.tryPhase(sp);
                case ScreenAction.SPLICE_RESPEC -> com.mutationsmod.mutations.mastery.GeneSplicer.respec(sp);
                case ScreenAction.SPLICE_REROLL -> com.mutationsmod.mutations.mastery.GeneSplicer.reroll(sp, payload.arg());
                default -> {}
            }
        });

        registrar.playToClient(OpenSplicer.TYPE, OpenSplicer.CODEC, (payload, ctx) ->
                com.mutationsmod.mutations.client.GeneSplicerScreen.open());

        registrar.playToClient(Impulse.TYPE, Impulse.CODEC, (payload, ctx) -> {
            var player = ctx.player();
            Vec3 v = new Vec3(payload.x(), payload.y(), payload.z());
            player.setDeltaMovement(payload.replace() ? v : player.getDeltaMovement().add(v));
        });

        registrar.playToClient(TendrilStrike.TYPE, TendrilStrike.CODEC, (payload, ctx) -> {
            long now = ctx.player().level().getGameTime();
            com.mutationsmod.mutations.client.render.HostRenderLogic.STRIKES.put(payload.playerId(),
                    new com.mutationsmod.mutations.client.render.HostRenderLogic.Strike(payload.targetEntity(),
                            new Vec3(payload.x(), payload.y(), payload.z()), now, payload.ticks()));
        });

        registrar.playToClient(KineticBlock.TYPE, KineticBlock.CODEC, (payload, ctx) -> {
            var player = ctx.player();
            if (payload.holderId() == player.getId()) {
                // We're holding it: our own client places it every frame (see MutationsClient).
                LOCAL_HELD.put(payload.entityId(), payload.slot());
                LOCAL_HELD_SEEN.put(payload.entityId(), System.currentTimeMillis());
                return;
            }
            LOCAL_HELD.remove(payload.entityId());
            LOCAL_HELD_SEEN.remove(payload.entityId());
            Entity e = player.level().getEntity(payload.entityId());
            if (e != null) {
                e.setPos(payload.x(), payload.y(), payload.z());
                e.setDeltaMovement(Vec3.ZERO);
            }
        });
    }
}
