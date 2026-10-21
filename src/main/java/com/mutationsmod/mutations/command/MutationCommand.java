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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /mutation admin commands (operators only). Examples:
 * <pre>
 *   /mutation info [player]
 *   /mutation list
 *   /mutation set &lt;players&gt; &lt;mutation&gt;
 *   /mutation add &lt;players&gt; &lt;mutation&gt;
 *   /mutation remove &lt;players&gt; &lt;mutation&gt;
 *   /mutation clear &lt;players&gt;
 *   /mutation reroll &lt;players&gt;
 *   /mutation cooldowns reset &lt;players&gt;
 *   /mutation mastery set|add &lt;players&gt; &lt;levels&gt;
 *   /mutation mastery xp &lt;players&gt; &lt;amount&gt;
 *   /mutation mastery reset &lt;players&gt;
 *   /mutation statpoints set &lt;players&gt; &lt;amount&gt;
 * </pre>
 */
public final class MutationCommand {
    private MutationCommand() {}

    public static final int MAX_MUTATIONS = 4;

    private static final DynamicCommandExceptionType UNKNOWN_MUTATION = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown or not-yet-available mutation: " + id + ". Use /mutation list"));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mutation")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))

                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

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

                .then(Commands.literal("cooldowns").then(Commands.literal("reset").then(players()
                        .executes(ctx -> forEach(ctx, (p, src) -> {
                            ModAttachments.set(p, ModAttachments.get(p).withoutCooldowns());
                            src.sendSuccess(() -> Component.literal("Reset cooldowns for " + name(p)), true);
                        })))))

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
                                    ModAttachments.set(p, d.withMastery(1, 0, 0).withSpeedTier(0));
                                    src.sendSuccess(() -> Component.literal("Reset " + name(p) + "'s mastery"), true);
                                })))))

                .then(Commands.literal("statpoints").then(Commands.literal("set").then(players()
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> forEach(ctx, (p, src) -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    Mastery.setStatPoints(p, amount);
                                    src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s stat points to " + amount), true);
                                })))))));
    }

    // ------------------------------------------------------------------ argument helpers

    private static RequiredArgumentBuilder<CommandSourceStack, ?> players() {
        return Commands.argument("players", EntityArgument.players());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> mutationArg() {
        return Commands.argument("mutation", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Mutation.implementedValues().stream().map(Mutation::id), builder));
    }

    private static Mutation mutation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "mutation");
        Mutation m = Mutation.byId(id).orElse(null);
        if (m == null || !m.implemented()) throw UNKNOWN_MUTATION.create(id);
        return m;
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
        ModAttachments.set(p, ModAttachments.get(p).withMutations(List.of(m.id())).withRolled(true).withoutCooldowns());
        AbilityHandler.announceNewMutation(p, m);
        src.sendSuccess(() -> Component.literal("Set " + name(p) + "'s mutation to " + m.displayName()), true);
    }

    private static void add(ServerPlayer p, CommandSourceStack src, Mutation m) {
        MutationData d = ModAttachments.get(p);
        if (d.has(m)) {
            src.sendFailure(Component.literal(name(p) + " already has " + m.displayName()));
            return;
        }
        if (d.mutations().size() >= MAX_MUTATIONS) {
            src.sendFailure(Component.literal(name(p) + " already has the maximum of " + MAX_MUTATIONS + " mutations"));
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
        ModAttachments.set(p, d.withMutations(list));
        src.sendSuccess(() -> Component.literal("Removed " + m.displayName() + " from " + name(p)), true);
    }

    private static void clear(ServerPlayer p, CommandSourceStack src) {
        AbilityHandler.resetState(p);
        ModAttachments.set(p, ModAttachments.get(p).withMutations(List.of()).withoutCooldowns());
        src.sendSuccess(() -> Component.literal("Cleared all mutations from " + name(p)), true);
    }

    private static void reroll(ServerPlayer p, CommandSourceStack src) {
        List<Mutation> pool = new ArrayList<>(Mutation.implementedValues());
        List<Mutation> current = ModAttachments.get(p).mutationList();
        if (!current.isEmpty() && pool.size() > 1) pool.remove(current.get(0)); // always something new
        Mutation m = pool.get(p.getRandom().nextInt(pool.size()));
        setBase(p, src, m);
    }

    private static int list(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Mutations:").withStyle(ChatFormatting.GOLD), false);
        for (Mutation m : Mutation.values()) {
            MutableComponent line = Component.literal(" - " + m.id())
                    .withStyle(s -> s.withColor(m.color() & 0xFFFFFF));
            if (!m.implemented()) line.append(Component.literal(" (coming soon)").withStyle(ChatFormatting.DARK_GRAY));
            src.sendSuccess(() -> line, false);
        }
        return Mutation.values().length;
    }

    private static int info(CommandSourceStack src, ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        StringBuilder muts = new StringBuilder();
        for (Mutation m : d.mutationList()) muts.append(muts.isEmpty() ? "" : ", ").append(m.displayName());
        long now = p.level().getGameTime();
        StringBuilder cds = new StringBuilder();
        for (ActiveAbility a : d.actives()) {
            long left = Math.max(0, d.readyAt(a) - now) / 20;
            cds.append(cds.isEmpty() ? "" : ", ").append(a.displayName()).append(left > 0 ? " (" + left + "s)" : " (ready)");
        }
        String text = name(p) + ": " + (muts.isEmpty() ? "no mutation" : muts)
                + " | Mastery " + d.level() + " (" + d.xp() + "/" + Mastery.xpToNext(d.level()) + " XP)"
                + " | " + d.statPoints() + " stat points"
                + (cds.isEmpty() ? "" : " | " + cds);
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
