package thut.api.terrain;

import java.util.HashMap;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import thut.api.maths.Vector3;

public class BiomeDatabase
{
    public static HashMap<Biome, Type[]> biomeTypes = new HashMap<>();

    public static boolean contains(Biome b, Type type)
    {
        return BiomeDictionary.hasType(b, type);
    }

    public static BiomeType getBiome(Biome b)
    {
        if (b != null) if (BiomeDatabase.getBiomeName(b).toLowerCase(java.util.Locale.ENGLISH).contains("flower"))
            return BiomeType.FLOWER;
        return BiomeType.NONE;
    }

    public static String getBiome(World world, Vector3 v, boolean checkIndandVillage)
    {
        String ret = "";

        if (checkIndandVillage && world instanceof ServerWorld)
        {
            final ServerWorld server = (ServerWorld) world;
            if (server.func_217483_b_(v.getPos())) return "village";
        }
        final Biome biome = v.getBiome(world);
        ret = BiomeDatabase.getBiome(biome).name;

        return ret;
    }

    public static String getBiomeName(Biome biome)
    {
        return biome.getRegistryName().getNamespace();
    }

    public static String getUnlocalizedNameFromType(int type)
    {
        return BiomeType.getType(type).readableName;
    }

}