package com.mutationsmod.mutations.mutation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The six base stats. Players get stat points every mastery level. No caps. */
public enum Stat {
    STRENGTH("Strength", "+0.5 melee damage"),
    VITALITY("Vitality", "+1 max health (half a heart)"),
    AGILITY("Agility", "+1% movement and attack speed"),
    ENDURANCE("Endurance", "+0.5 armor"),
    LUCK("Luck", "+0.1 luck (better loot and fishing)"),
    MUTATION("Mutation", "+2% ability power, -1% cooldowns (max -50%)");

    private final String displayName;
    private final String perPoint;

    Stat(String displayName, String perPoint) {
        this.displayName = displayName;
        this.perPoint = perPoint;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public String perPoint() {
        return perPoint;
    }

    public static Optional<Stat> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id().equals(id)).findFirst();
    }
}
