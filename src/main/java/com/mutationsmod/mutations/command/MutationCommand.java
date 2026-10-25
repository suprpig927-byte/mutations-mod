package com.mutationsmod.mutations.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.AbilityHandler;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.mutation.Stat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /mutation admin commands (operators only). See README.md for the full list. */
public final class MutationCommand {
    private MutationCommand() {}

    public static final int MAX_MUTATIONS = 4;

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown id: " + id + ". Use /mutation list"));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mutation")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                .then(Commands.literal("crater").executes(ctx -> {
                    var src = ctx.getSource();
                    var level = src.getLevel();
                    var pos = net.minecraft.core.BlockPos.containing(src.getPosition());
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
                    com.mutationsmod.mutations.world.MeteorCraterFeature.placeAt(level, level.getRandom(), new net.minecraft.core.BlockPos(pos.getX(), y, pos.getZ()), net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE);
                    src.sendSuccess(() -> Component.literal("A meteor strikes!"), true);
                    return 1;
                }))

                .then(Commands.literal("lab").then(mutationArg().executes(ctx -> {
                    var src = ctx.getSource();
                    var level = src.getLevel();
                    Mutation m = mutation(ctx);
                    var pos = net.minecraft.core.BlockPos.containing(src.getPosition());
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
                    com.mutationsmod.mutations.world.GeneLabFeature.build(level, level.getRandom(), new net.minecraft.core.BlockPos(pos.getX(), y, pos.getZ()), m);
                    src.sendSuccess(() -> Component.literal("Built a " + m.displayName() + " Gene Lab (surface y " + y + ")."), true);
                    return 1;
                })))

                // Debug: dump a box of blocks to the server log (used for automated previews).
                .then(Commands.literal("dump").then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                        .then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(ctx -> {
                            var level = ctx.getSource().getLevel();
                            var a = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx, "from");
                            var b = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx, "to");
                            StringBuilder out = new StringBuilder("BLOCKDUMP ");
                            for (var pos : net.minecraft.core.BlockPos.betweenClosed(a, b)) {
                                var st = level.getBlockState(pos);
                                if (st.isAir()) continue;
                                out.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append('=')
                                        .append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath()).append(';');
                            }
                            com.mutationsmod.mutations.MutationsMod.LOGGER.info(out.toString());
                            return 1;
                        }))))

                .then(Commands.literal("haven").executes(ctx -> {
                    var src = ctx.getSource();
                    var level = src.getLevel();
                    var pos = net.minecraft.core.BlockPos.containing(src.getPosition());
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
                    com.mutationsmod.mutations.world.HavenBuilder.build(level, level.getRandom().nextLong(), new net.minecraft.core.BlockPos(pos.getX(), y, pos.getZ()));
                    src.sendSuccess(() -> Component.literal("Built a Mutant Haven (surface y " + y + ")."), true);
                    return 1;
                }))

                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource()))
                        .then(mutationArg().executes(ctx -> listSecondaries(ctx.getSource(), mutation(ctx)))))

                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

                // ---- base mutations ----
                .then(Commands.literal("set").then(players().then(mutationArg()
                        .executes(ctx -> forEach(ctx, (p, src) -> setBase(p, src, mutation(ctx)))))))
                .then(Commands.literal("add").then(players().then(mutationArg()
                        .executes(ctx -> forEach(ctx, (p, src) -> add(p, src, mutation(ctx)))))))
                .then(Commands.literal("remove").then(players().then(mutationArg()
                        .executes(ctx -> forEach(ctx, (p, src) -> remove(p, src, mutation(ctx)))))))
                .then(Commands.literal("clear").then(players()
                        .executes(ctx -> forEach(ctx, MutationCommand::clear))))
                .then(Commands.literal("reroll").then(players()
                        .executes(ctx -> forEach(ctx, MutationCommand::reroll))))

                // ---- secondary mutations ----
                .then(Commands.literal("secondary")
                        .then(Commands.literal("add").then(players().then(secondaryArg()
                                .executes(ctx -> forEach(ctx, (p, src) -> addSecondary(p, src, secondary(ctx)))))))
                        .then(Commands.literal("remove").then(players().then(secondaryArg()
                                .executes(ctx -> forEach(ctx, (p, src) -> removeSecondary(p, src, secondary(ctx)))))))
                        .then(Commands.literal("clear").then(players()
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    AbilityHandler.resetState(p);
                                    ModAttachments.set(p, ModAttachments.get(p).withSecondaries(List.of()).withRollOptions(List.of()));
                                    src.sendSuccess(() -> Component.literal("Cleared " + name(p) + "'s secondary mutations"), true);
                                })))))
                .then(Commands.literal("roll").then(players()
                        .executes(ctx -> forEach(ctx, (p, src) -> {
                            Mastery.startRoll(p, true);
                            boolean offered = !ModAttachments.get(p).rollOptions().isEmpty();
                            if (offered) src.sendSuccess(() -> Component.literal("Offered " + name(p) + " a secondary roll (open the mutation screen)"), true);
                            else src.sendFailure(Component.literal(name(p) + " has no secondaries left to roll"));
                        }))))

                .then(Commands.literal("cooldowns").then(Commands.literal("reset").then(players()
                        .executes(ctx -> forEach(ctx, (p, src) -> {
                            ModAttachments.set(p, ModAttachments.get(p).withoutCooldowns());
                            src.sendSuccess(() -> Component.literal("Reset cooldowns for " + name(p)), true);
                        })))))

                // ---- mastery ----
                .then(Commands.literal("mastery")
                        .then(Commands.literal("set").then(players().then(Commands.argument("level", IntegerArgumentType.integer(1))
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                    Mastery.setLevel(p, lvl);
                                    src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s mastery level to " + lvl), true);
                                })))))
                        .then(Commands.literal("add").then(players().then(Commands.argument("levels", IntegerArgumentType.integer())
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    int add = IntegerArgumentType.getInteger(ctx, "levels");
                                    Mastery.setLevel(p, ModAttachments.get(p).level() + add);
                                    src.sendSuccess(() -> Component.literal(name(p) + " is now mastery level " + ModAttachments.get(p).level()), true);
                                })))))
                        .then(Commands.literal("xp").then(players().then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    Mastery.addXp(p, amount);
                                    src.sendSuccess(() -> Component.literal("Gave " + name(p) + " " + amount + " mastery XP"), true);
                                })))))
                        .then(Commands.literal("reset").then(players()
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    MutationData d = ModAttachments.get(p);
                                    ModAttachments.set(p, d.withMastery(1, 0, 0).withSpeedTier(0).withStats(Map.of()));
                                    src.sendSuccess(() -> Component.literal("Reset " + name(p) + "'s mastery and stats"), true);
                                })))))

                // ---- stats ----
                .then(Commands.literal("statpoints").then(Commands.literal("set").then(players()
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    Mastery.setStatPoints(p, amount);
                                    src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s stat points to " + amount), true);
                                }))))))
                .then(Commands.literal("stats")
                        .then(Commands.literal("set").then(players().then(statArg()
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> forEach(ctx, (p, src) -> {
                                            Stat stat = stat(ctx);
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            Map<String, Integer> stats = new HashMap<>(ModAttachments.get(p).stats());
                                            stats.put(stat.id(), amount);
                                            ModAttachments.set(p, ModAttachments.get(p).withStats(stats));
                                            src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s " + stat.displayName() + " to " + amount), true);
                                        }))))))
                        .then(Commands.literal("respec").then(players()
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    if (Mastery.respec(p, true)) src.sendSuccess(() -> Component.literal("Respecced " + name(p) + " for free"), true);
                                }))))));
    }

    // ------------------------------------------------------------------ argument helpers

    private static RequiredArgumentBuilder<CommandSourceStack, ?> players() {
        return Commands.argument("players", EntityArgument.players());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> mutationArg() {
        return Commands.argument("mutation", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Mutation.values()).map(Mutation::id), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> secondaryArg() {
        return Commands.argument("secondary", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Secondary.values()).map(Secondary::id), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> statArg() {
        return Commands.argument("stat", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Stat.values()).map(Stat::id), builder));
    }

    private static Mutation mutation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "mutation");
        return Mutation.byId(id).orElseThrow(() -> UNKNOWN.create(id));
    }

    private static Secondary secondary(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "secondary");
        return Secondary.byId(id).orElseThrow(() -> UNKNOWN.create(id));
    }

    private static Stat stat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "stat");
        return Stat.byId(id).orElseThrow(() -> UNKNOWN.create(id));
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run(ServerPlayer player, CommandSourceStack source) throws CommandSyntaxException;
    }

    private static int forEach(CommandContext<CommandSourceStack> ctx, PlayerAction action) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer p : targets) action.run(p, ctx.getSource());
        return targets.size();
    }

    private static String name(ServerPlayer p) {
        return p.getName().getString();
    }

    // ------------------------------------------------------------------ actions

    private static void setBase(ServerPlayer p, CommandSourceStack src, Mutation m) {
        AbilityHandler.resetState(p);
        ModAttachments.set(p, ModAttachments.get(p).withMutations(List.of(m.id())).withRolled(true).withoutCooldowns()
                .withSecondaries(List.of()).withRollOptions(List.of()).withCharge(0f));
        AbilityHandler.announceNewMutation(p, m);
        src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s mutation to " + m.displayName() + " (secondaries cleared)"), true);
    }

    private static void add(ServerPlayer p, CommandSourceStack src, Mutation m) {
        MutationData d = ModAttachments.get(p);
        if (d.has(m)) {
            src.sendFailure(Component.literal(name(p) + " already has " + m.displayName()));
            return;
        }
        if (d.mutations().size() >= MAX_MUTATIONS) {
            src.sendFailure(Component.literal(name(p) + " already has the maximum of " + MAX_MUTATIONS + " base mutations"));
            return;
        }
        List<String> list = new ArrayList<>(d.mutations());
        list.add(m.id());
        ModAttachments.set(p, d.withMutations(list).withRolled(true));
        src.sendSuccess(() -> Component.literal("Gave " + name(p) + " " + m.displayName()), true);
    }

    private static void remove(ServerPlayer p, CommandSourceStack src, Mutation m) {
        MutationData d = ModAttachments.get(p);
        if (!d.has(m)) {
            src.sendFailure(Component.literal(name(p) + " doesn't have " + m.displayName()));
            return;
        }
        List<String> list = new ArrayList<>(d.mutations());
        list.remove(m.id());
        AbilityHandler.resetState(p);
        ModAttachments.set(p, ModAttachments.get(p).withMutations(list));
        src.sendSuccess(() -> Component.literal("Removed " + m.displayName() + " from " + name(p)), true);
    }

    private static void clear(ServerPlayer p, CommandSourceStack src) {
        AbilityHandler.resetState(p);
        ModAttachments.set(p, ModAttachments.get(p).withMutations(List.of()).withoutCooldowns()
                .withSecondaries(List.of()).withRollOptions(List.of()).withCharge(0f));
        src.sendSuccess(() -> Component.literal("Cleared all mutations from " + name(p)), true);
    }

    private static void reroll(ServerPlayer p, CommandSourceStack src) {
        List<Mutation> pool = new ArrayList<>(Arrays.asList(Mutation.values()));
        Mutation current = ModAttachments.get(p).base();
        if (current != null) pool.remove(current); // always something new
        setBase(p, src, pool.get(p.getRandom().nextInt(pool.size())));
    }

    private static void addSecondary(ServerPlayer p, CommandSourceStack src, Secondary s) {
        MutationData d = ModAttachments.get(p);
        if (d.secondaries().contains(s.id())) {
            src.sendFailure(Component.literal(name(p) + " already has " + s.displayName()));
            return;
        }
        List<String> list = new ArrayList<>(d.secondaries());
        list.add(s.id());
        ModAttachments.set(p, d.withSecondaries(list));
        String note = d.has(s.base()) ? "" : " (it only works with the " + s.base().displayName() + " base mutation)";
        src.sendSuccess(() -> Component.literal("Gave " + name(p) + " " + s.displayName() + note), true);
    }

    private static void removeSecondary(ServerPlayer p, CommandSourceStack src, Secondary s) {
        MutationData d = ModAttachments.get(p);
        if (!d.secondaries().contains(s.id())) {
            src.sendFailure(Component.literal(name(p) + " doesn't have " + s.displayName()));
            return;
        }
        List<String> list = new ArrayList<>(d.secondaries());
        list.remove(s.id());
        AbilityHandler.resetState(p);
        ModAttachments.set(p, ModAttachments.get(p).withSecondaries(list));
        src.sendSuccess(() -> Component.literal("Removed " + s.displayName() + " from " + name(p)), true);
    }

    private static int list(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Mutations (use /mutation list <mutation> for its secondaries):").withStyle(ChatFormatting.GOLD), false);
        for (Mutation m : Mutation.values()) {
            MutableComponent line = Component.literal(" - " + m.id()).withStyle(s -> s.withColor(m.color() & 0xFFFFFF));
            src.sendSuccess(() -> line, false);
        }
        return Mutation.values().length;
    }

    private static int listSecondaries(CommandSourceStack src, Mutation m) {
        src.sendSuccess(() -> Component.literal(m.displayName() + " secondaries:").withStyle(s -> s.withColor(m.color() & 0xFFFFFF)), false);
        for (Secondary s : m.secondaries()) {
            src.sendSuccess(() -> Component.literal(" - " + s.id()).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  " + s.description()).withStyle(ChatFormatting.GRAY)), false);
        }
        return m.secondaries().size();
    }

    private static int info(CommandSourceStack src, ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        StringBuilder muts = new StringBuilder();
        for (Mutation m : d.mutationList()) muts.append(muts.isEmpty() ? "" : ", ").append(m.displayName());
        StringBuilder secs = new StringBuilder();
        for (Secondary s : d.secondaryList()) secs.append(secs.isEmpty() ? "" : ", ").append(s.displayName());
        StringBuilder stats = new StringBuilder();
        for (Stat s : Stat.values()) stats.append(stats.isEmpty() ? "" : ", ").append(s.displayName()).append(" ").append(d.stat(s));
        long now = p.level().getGameTime();
        StringBuilder cds = new StringBuilder();
        for (ActiveAbility a : d.actives()) {
            long left = Math.max(0, d.readyAt(a) - now) / 20;
            cds.append(cds.isEmpty() ? "" : ", ").append(a.displayName()).append(left > 0 ? " (" + left + "s)" : " (ready)");
        }
        src.sendSuccess(() -> Component.literal(name(p) + ": " + (muts.isEmpty() ? "no mutation" : muts)).withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(() -> Component.literal("Secondaries: " + (secs.isEmpty() ? "none" : secs)), false);
        src.sendSuccess(() -> Component.literal("Mastery " + d.level() + " (" + d.xp() + "/" + Mastery.xpToNext(d.level()) + " XP), "
                + d.statPoints() + " unspent points"), false);
        src.sendSuccess(() -> Component.literal("Stats: " + stats), false);
        if (!cds.isEmpty()) src.sendSuccess(() -> Component.literal("Abilities: " + cds), false);
        return 1;
    }
}
