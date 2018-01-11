package thut.tech.client;

import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import thut.api.ThutBlocks;
import thut.tech.client.render.RenderLift;
import thut.tech.client.render.RenderLiftController;
import thut.tech.common.CommonProxy;
import thut.tech.common.blocks.lift.BlockLift;
import thut.tech.common.blocks.lift.TileEntityLiftAccess;
import thut.tech.common.entity.EntityLift;
import thut.tech.common.items.ItemLinker;

public class ClientProxy extends CommonProxy
{

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        return null;
    }

    @Override
    public EntityPlayer getPlayer()
    {
        return getPlayer(null);
    }

    @Override
    public EntityPlayer getPlayer(String playerName)
    {
        if (isOnClientSide())
        {
            if (playerName != null)
            {
                return getWorld().getPlayerEntityByName(playerName);
            }
            else
            {
                return Minecraft.getMinecraft().player;
            }
        }
        else
        {
            return super.getPlayer(playerName);
        }
    }

    @Override
    public World getWorld()
    {
        if (isOnClientSide())
        {
            return Minecraft.getMinecraft().world;
        }
        else
        {
            return super.getWorld();
        }
    }

    @Override
    public void registerItemModels()
    {
        // TODO Auto-generated method stub
        super.registerItemModels();
        Item lift = Item.getItemFromBlock(ThutBlocks.lift);
        ModelBakery.registerItemVariants(lift, new ModelResourceLocation("thuttech:liftcontroller", "inventory"),
                new ModelResourceLocation("thuttech:lift", "inventory"));
    }

    @Override
    public void registerBlockModels()
    {
        // TODO Auto-generated method stub
        super.registerBlockModels();
        ModelLoader.setCustomStateMapper(ThutBlocks.lift, (new StateMap.Builder()).withName(BlockLift.VARIANT)
                .ignore(new IProperty[] { BlockLift.CALLED, BlockLift.CURRENT }).build());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLiftAccess.class, new RenderLiftController<>());
    }

    @Override
    public void initClient()
    {
        Item lift = Item.getItemFromBlock(ThutBlocks.lift);
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(lift, 0,
                new ModelResourceLocation("thuttech:lift", "inventory"));
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(lift, 1,
                new ModelResourceLocation("thuttech:liftcontroller", "inventory"));
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ItemLinker.instance, 0,
                new ModelResourceLocation("thuttech:devicelinker", "inventory"));
    }

    @Override
    public boolean isOnClientSide()
    {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    @Override
    public void loadSounds()
    {
    }

    @Override
    public void preinit(FMLPreInitializationEvent event)
    {
        super.preinit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityLift.class, new IRenderFactory<EntityLivingBase>()
        {
            @Override
            public Render<? super EntityLivingBase> createRenderFor(RenderManager manager)
            {
                return new RenderLift(manager);
            }
        });
    }

}
