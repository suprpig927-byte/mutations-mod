package com.mutationsmod.mutations.world;

import com.mojang.serialization.Codec;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A small vein of Mutagen Ore (deepslate variant in deepslate), about as big as a gold vein. */
public class MutagenOreFeature extends Feature<NoneFeatureConfiguration> {
    public MutagenOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos.MutableBlockPos pos = ctx.origin().mutable();
        int size = 4 + rnd.nextInt(6);
        boolean placed = false;
        for (int i = 0; i < size; i++) {
            BlockState here = level.getBlockState(pos);
            if (here.is(BlockTags.STONE_ORE_REPLACEABLES)) {
                setBlock(level, pos, ModContent.MUTAGEN_ORE.get().defaultBlockState());
                placed = true;
            } else if (here.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                setBlock(level, pos, ModContent.DEEPSLATE_MUTAGEN_ORE.get().defaultBlockState());
                placed = true;
            }
            pos.move(rnd.nextInt(3) - 1, rnd.nextInt(3) - 1, rnd.nextInt(3) - 1);
        }
        return placed;
    }
}
