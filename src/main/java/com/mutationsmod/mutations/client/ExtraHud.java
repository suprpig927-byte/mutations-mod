package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ability.extra.LandlordPower;
import com.mutationsmod.mutations.ability.extra.MicroPower;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** HUD lines for the v0.19 mutations: meters, running ultimates, Audit, Ore Sense and Compass Sense. */
public final class ExtraHud {
    private ExtraHud() {}

    private static long lastScan = -100;
    private static List<String> oreLines = List.of();
    private static String compassLine = null;

    static int meter(MutationData d, String name) { return (int) d.timer("m_" + name); }

    public static void lines(Minecraft mc, MutationData d, long now, List<String> out) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (d.has(Mutation.SHADOW) && d.timer("merged") > 0) out.add("Merged with the shadows");
        if (d.has(Mutation.LUMINARY)) out.add("Radiance " + meter(d, "radiance") + " / 100");
        if (d.has(Mutation.SOULBINDER)) out.add("Souls " + meter(d, "souls") + " / " + (Catalysts.holding(p, Mutation.SOULBINDER) ? 20 : 10));
        if (d.has(Mutation.TAX_MAN)) out.add("Revenue " + meter(d, "revenue") + " / 100");
        if (d.has(Mutation.MICRO)) {
            int s = MicroPower.size(d);
            out.add("Size: " + (s == MicroPower.TINY ? "tiny" : s == MicroPower.BIG ? "big" : "normal"));
        }
        if (d.has(Mutation.LANDLORD)) {
            var claims = LandlordPower.claims(d);
            int max = Catalysts.holding(p, Mutation.LANDLORD) ? 6 : 3;
            boolean home = d.timer("foreclosure") > now;
            if (p.level().dimension() == Level.OVERWORLD) {
                int cx = p.blockPosition().getX() >> 4, cz = p.blockPosition().getZ() >> 4;
                for (long[] c : claims) if (c[0] == cx && c[1] == cz) home = true;
            }
            out.add("Property: " + claims.size() + " / " + max + " chunks" + (home ? "  (home)" : ""));
            if (d.timer("rent_next") > now) out.add("Rent in " + MutationsClient.formatTime(d.timer("rent_next") - now));
        }
        if (d.has(Mutation.SWARM) && d.timer("swarm_scattered") > now) out.add("Swarm scattered: " + MutationsClient.formatTime(d.timer("swarm_scattered") - now));
        if (d.has(Mutation.MAGNETAR) && d.timer("maglift") > 0) out.add("Mag Lift: ON");
        if (d.has(Mutation.SOULBINDER) && d.timer("spirit_walk_end") > now) out.add("Spirit Walk: " + MutationsClient.formatTime(d.timer("spirit_walk_end") - now));
        timed(d, now, out, "eclipse", "Eclipse");
        timed(d, now, out, "solar_flare", "Solar Flare");
        timed(d, now, out, "foreclosure", "Foreclosure");
        timed(d, now, out, "tax_season", "Tax Season");
        timed(d, now, out, "rampage", "Rubber Rampage");
        timed(d, now, out, "giant_slayer", "Giant Slayer");
        timed(d, now, out, "army", "Army of the Dead");
        timed(d, now, out, "rent_due", "Rent Due");
        timed(d, now, out, "facet_shield", "Facet Shield");
        timed(d, now, out, "metal_shell", "Metal Shell");
        timed(d, now, out, "soul_shield", "Soul Shield");
        timed(d, now, out, "shattered", "Shattered");
        timed(d, now, out, "borrowed_time", "Borrowed Time");
        timed(d, now, out, "bear_form", "Bear Form");

        // Audit: the health and armor of the mob you look at.
        if (d.has(Mutation.TAX_MAN) && d.timer("audit") > now && mc.hitResult instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le) {
            out.add(String.format("Audit: %s  %.0f/%.0f HP  %d armor", le.getName().getString(), le.getHealth(), le.getMaxHealth(),
                    (int) le.getAttributeValue(Attributes.ARMOR)));
        }

        boolean ores = d.has(Secondary.ORE_SENSE) && d.has(Mutation.EARTHSHAPER);
        boolean compass = d.has(Secondary.COMPASS_SENSE) && d.has(Mutation.MAGNETAR);
        if (ores || compass) {
            if (now - lastScan >= 20 || now < lastScan) { scan(p, ores, compass); lastScan = now; }
            if (ores) out.addAll(oreLines);
            if (compass && compassLine != null) out.add(compassLine);
        }
    }

    private static void timed(MutationData d, long now, List<String> out, String timer, String label) {
        long t = d.timer(timer);
        if (t > now && t - now < 20 * 60 * 10) out.add(label + ": " + MutationsClient.formatTime(t - now));
    }

    private static boolean isOre(BlockState s) {
        return s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES) || s.is(BlockTags.GOLD_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES);
    }

    private static void scan(LocalPlayer p, boolean ores, boolean compass) {
        Level level = p.level();
        BlockPos c = p.blockPosition();
        Map<String, Integer> counts = new LinkedHashMap<>();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        int r = compass ? 16 : 8;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            q.set(c.getX() + x, c.getY() + y, c.getZ() + z);
            BlockState s = level.getBlockState(q);
            if (!isOre(s)) continue;
            double dsq = x * x + y * y + z * z;
            if (ores && Math.abs(x) <= 8 && Math.abs(y) <= 8 && Math.abs(z) <= 8) counts.merge(s.getBlock().getName().getString(), 1, Integer::sum);
            if (dsq < best) { best = dsq; nearest = q.immutable(); }
        }
        List<String> lines = new ArrayList<>();
        if (ores) {
            if (counts.isEmpty()) lines.add("Ore Sense: nothing nearby");
            else counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(5)
                    .forEach(e -> lines.add("Ore Sense: " + e.getKey() + " x" + e.getValue()));
        }
        oreLines = lines;
        if (compass) {
            if (nearest == null) compassLine = "Compass Sense: no ore within 16";
            else {
                double dx = nearest.getX() + 0.5 - p.getX(), dz = nearest.getZ() + 0.5 - p.getZ(), dy = nearest.getY() - p.getY();
                double angle = Math.toDegrees(Math.atan2(-dx, dz)) - p.getYRot();
                angle = ((angle % 360) + 540) % 360 - 180;
                String arrow = Math.abs(angle) <= 45 ? "↑" : Math.abs(angle) >= 135 ? "↓" : angle > 0 ? "→" : "←";
                compassLine = "Compass Sense: " + arrow + " " + level.getBlockState(nearest).getBlock().getName().getString()
                        + " " + (int) Math.sqrt(best) + "m" + (dy > 1 ? " (above)" : dy < -1 ? " (below)" : "");
            }
        }
    }
}
