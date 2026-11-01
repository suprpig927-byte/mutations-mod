package com.mutationsmod.mutations.world;

import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/** Readable lore found in Gene Labs: the meteor, Mutagen, Nullite, and each lab's own experiments. */
public final class ResearchNotes {
    private ResearchNotes() {}

    private static final String[][] GENERAL = {
            {"Impact Report", "Dr. Vale",
                    "Day 1. Something came down in the night. The crater is thirty paces across and the rock at its heart is not from here.",
                    "The core is studded with green crystal. Our instruments go quiet near it. So does Ellis. He says he feels 'ordinary' for the first time in years."},
            {"On Mutagen", "Dr. Okafor",
                    "Mutagen forms wherever the meteor's dust settled: green ore underground, crystal in the deep geodes.",
                    "Exposed tissue changes. Not randomly - it chooses. Every subject expressed exactly one dominant trait.",
                    "We are calling them mutations. The board wants them weaponised."},
            {"Nullite", "Dr. Vale",
                    "The crystal at the crater's heart cancels mutation outright. Within a few paces, powers simply stop.",
                    "Ellis made a spear from it, bound to one of our containment rods. One touch and a subject is human again for a while.",
                    "Keep it locked up. If they ever turn on us, it is all we have."},
            {"Incident Log", "Security",
                    "Containment failure in the specimen hall. Subjects 7 through 19 are loose. The ones that didn't take have turned... wrong.",
                    "Evacuating to the surface. Seal the vault. Do NOT open the tanks."},
    };

    private static String[] forMutation(Mutation m) {
        return switch (m) {
            case REGENERATOR -> new String[]{"Subject R: Regeneration", "Dr. Okafor",
                    "Subject R closed a gash in four seconds. We removed a finger. It grew back by lunch.",
                    "The Troll Heart in the vault doubles the effect. We tried it on a rat. The rat is now very large."};
            case KINETIC_MIND -> new String[]{"Subject K: Telekinesis", "Dr. Lindqvist",
                    "K lifts stone with a look. Range and grip are limited by focus, not strength.",
                    "The Focus Prism sharpens that focus: farther, and one more block at a time. It hums when she holds it."};
            case STORMCALLER -> new String[]{"Subject S: Weather", "Dr. Marsh",
                    "S is weak under clear skies and terrifying in a storm. Lightning seeks her out and she welcomes it.",
                    "We built the Storm Rod to carry a storm with her. Under its charge, every sky is raining."};
            case PHASER -> new String[]{"Subject P: Velocity", "Dr. Ruiz",
                    "P outran the treadmill, then the car, then the camera. Each new speed tier costs him more food.",
                    "The Kinetic Soles let him find one gear past his limit. He says the world goes silent up there."};
            case CRYO -> new String[]{"Subject C: Cold", "Dr. Ivers",
                    "C freezes water by walking on it. Heat is her weakness - in the desert she could barely raise frost.",
                    "The Frost Core keeps her cold from the inside. Even in the Nether, the heat can't touch her now."};
            case BLINKER -> new String[]{"Subject B: Displacement", "Dr. Ashby",
                    "B is never where we left him. He blinks short distances and can throw others the same way.",
                    "Through the Ender Lens he sees farther, and blinks farther. Half again as far, by our measurements."};
            case COLOSSUS -> new String[]{"Subject T: Density", "Dr. Kowal",
                    "T turns to living steel at will. In that form he shatters stone with his fists and sinks like an anchor.",
                    "The Titan Gauntlet focuses the shockwave of his Ground Pound. The test range is gone."};
            case SIREN -> new String[]{"Subject V: Resonance", "Dr. Moreau",
                    "V's voice is a weapon. Her scream knocks subjects flat; her lullaby puts guards to sleep.",
                    "The Resonance Crystal carries her scream half again as far, and anything it hits is left reeling."};
            case ABSORBER -> new String[]{"Subject A: Absorption", "Dr. Nakamura",
                    "A stores the force of every blow and releases it later. The more we hit him, the more dangerous he gets.",
                    "The Capacitor Cell lets him soak it up faster. We stopped hitting him."};
            case CHAMELEON -> new String[]{"Subject X: Mimicry", "Dr. Hale",
                    "X blends into any surface and fools the creatures of the dark into ignoring her.",
                    "Wearing the Prism Cloak, her mimicry lasts twice as long. We lost her in the lab for a week."};
            case PYRO -> new String[]{"Subject F: Combustion", "Dr. Grant",
                    "F is fire. Lava does not burn him; water does. Rain leaves him shivering and steaming.",
                    "The Ember Heart keeps his flame lit through any storm. Only deep water can still hurt him."};
            case HOST -> new String[]{"Subject H: Symbiosis", "Dr. ???",
                    "H is not alone in his body. The organism makes him strong, but it is always hungry.",
                    "Sealed in the Symbiote Jar, a sample of it keeps the rest calm. It starves half as fast. It is watching me write this."};
            case BULLET -> new String[]{"Subject B: Ballistics", "Dr. Okafor",
                    "B swallowed a handful of iron nuggets and fired them back at us through his palms. Every ore makes a different round.",
                    "He burns hot when he fires too fast. The Heat Sink draws it off: twice the rounds before the fever takes him."};
            case FIERCE_WINGS -> new String[]{"Subject W: Flight", "Dr. Moreau",
                    "W grew wings in a week. Real feathers, hollow bones. She glides from the roof of the lab and beats her way back up.",
                    "Fire terrifies her, and with reason: it catches in the plumage. The Gale Feather lets her flap all day without tiring."};
            case SHADOW -> new String[]{"Subject S: Umbra", "Dr. Vance",
                    "S walks into a dark corner and is simply gone. Bring a torch and she stumbles out, blinking.",
                    "Carrying the Void Lantern she stayed hidden even under the lab lamps. We no longer know where she is."};
            case RADIATION -> new String[]{"Subject R: Isotope", "Dr. Petrov",
                    "R's cell counters click without stopping. Plants in his wing died within a week.",
                    "Behind the Lead-Lined Core his beam holds steady for twice as long. Wear the badge at all times."};
            case LUMINARY -> new String[]{"Subject L: Radiance", "Dr. Ilse",
                    "L drinks sunlight. After an hour outdoors she lit the corridor brighter than our lamps.",
                    "The Sunstone holds her light through the night. She has not slept in the dark since."};
            case MAGNETAR -> new String[]{"Subject M: Polarity", "Dr. Brandt",
                    "Every loose bolt in the room ends up stuck to M. Our rifles jammed when we raised them.",
                    "With the Lodestone Heart his pull reaches across the whole yard. Keep the vault doors closed."};
            case VENOM -> new String[]{"Subject V: Toxin", "Dr. Okoye",
                    "V's bite put a cow down in seconds. Her own blood neutralises any poison we try.",
                    "With the Venom Gland implanted the toxin lingers twice as long. She is sluggish in the cold store."};
            case VERDANT -> new String[]{"Subject G: Growth", "Dr. Arden",
                    "G turned down every meal we offered except salad. The seedlings around her cot are already waist high.",
                    "The Heartseed in her palm doubles her healing in the greenhouse, and she finally ate a steak."};
            case EARTHSHAPER -> new String[]{"Subject E: Bedrock", "Dr. Kaur",
                    "E pressed his palm to the floor and a pillar threw the guard into the ceiling.",
                    "The Geode Heart doubles everything he raises. We have moved his testing outdoors."};
            case BEASTCALLER -> new String[]{"Subject B: Pack", "Dr. Lowe",
                    "Every dog on the base followed B to breakfast. When a hen died in the yard, he clutched his chest.",
                    "With the Alpha Fang his spirit wolves stay twice as long. They will not let us near him."};
            case CHRONO -> new String[]{"Subject C: Loop", "Dr. Sato",
                    "C finished the maze before we started the clock. Then she did it again, from five seconds earlier.",
                    "The Broken Pocketwatch lets her slip twice as far back. I have written this entry three times."};
            case SWARM -> new String[]{"Subject W: Hive", "Dr. Moss",
                    "W hums. The sound comes from under his skin. When he is angry, it comes out.",
                    "On Queen's Jelly the swarm is twice as fierce and our smoke canisters do nothing."};
            case GRAVITON -> new String[]{"Subject N: Mass", "Dr. Ruiz",
                    "N drops a spoon and it falls sideways. She herself falls up, now and then.",
                    "The Neutron Core steadies her field. With it she hovers twice as long and stays on the floor."};
            case TIDECALLER -> new String[]{"Subject T: Current", "Dr. Nwosu",
                    "T sleeps in the tank. Left in the dry lab for a day she could barely stand.",
                    "The Tide Pearl keeps her damp in the desert. She flooded the canteen on purpose."};
            case CRYSTALLINE -> new String[]{"Subject Q: Lattice", "Dr. Fournier",
                    "Q's skin rings like a glass when you tap it. Our scalpels broke on it.",
                    "The Flawless Gem seals her cracks. The blast test that should have shattered her left a chip."};
            case ELASTIC -> new String[]{"Subject P: Plastic", "Dr. Hughes",
                    "P reached the top shelf from across the room. Dropped from the roof, he bounced back up to it.",
                    "The Rubber Core lengthens his reach again. Our knives still worry him."};
            case SOULBINDER -> new String[]{"Subject D: Threshold", "Dr. Hollis",
                    "D's heart stopped on the table for four minutes. He came back with company.",
                    "The Phylactery holds twice the souls, and he walks in the sun again. The company stays."};
            case MICRO -> new String[]{"Subject Z: Scale", "Dr. Amari",
                    "Z escaped through the air vent at the size of a mouse. We found her stuck in the doorway, ten feet tall.",
                    "The Size Crystal makes her changes instant. She hides in the coffee cups now."};
            case TAX_MAN -> new String[]{"Subject $: Audit", "Dr. Pell",
                    "$ collected a gold nugget from every rat in the lab. Nobody remembers agreeing to it.",
                    "With the Golden Ledger he takes twice as much, and death only halves the books."};
            case LANDLORD -> new String[]{"Subject K: Tenure", "Dr. Beaumont",
                    "K claimed the east wing on his first day. Inside it he is unbeatable; outside it he mopes.",
                    "The Golden Deed doubles his holdings and his rent. He has started charging us."};
        };
    }

    private static ItemStack book(String title, String author, String... pages) {
        List<Filterable<Component>> list = new ArrayList<>();
        for (String p : pages) list.add(Filterable.passThrough(Component.literal(p)));
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0, list, true));
        return stack;
    }

    private static ItemStack fromArray(String[] a) {
        String[] pages = new String[a.length - 2];
        System.arraycopy(a, 2, pages, 0, pages.length);
        return book(a[0], a[1], pages);
    }

    /** The note about this lab's own subject and Catalyst. */
    public static ItemStack labNote(Mutation m) {
        return fromArray(forMutation(m));
    }

    /** A random general note about the meteor and the experiments. */
    public static ItemStack randomGeneral(RandomSource rnd) {
        return fromArray(GENERAL[rnd.nextInt(GENERAL.length)]);
    }
}
