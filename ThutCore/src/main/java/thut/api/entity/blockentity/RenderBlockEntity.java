package thut.api.entity.blockentity;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.client.FMLClientHandler;
import thut.api.entity.IMultiplePassengerEntity;
import thut.core.common.ThutCore;

@OnlyIn(Dist.CLIENT)
public class RenderBlockEntity<T extends LivingEntity> extends RenderLivingBase<T>
{
    private static IBakedModel crate_model;
    float                      pitch = 0.0f;
    float                      yaw   = 0.0f;
    long                       time  = 0;
    boolean                    up    = true;

    static final Tessellator   t     = new Tessellator(2097152);
    BufferBuilder              b     = t.getBuffer();

    ResourceLocation           texture;

    public RenderBlockEntity(RenderManager manager)
    {
        super(manager, null, 0);
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        try
        {
            b = Tessellator.getInstance().getBuffer();
            IBlockEntity blockEntity = (IBlockEntity) entity;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            if (entity instanceof IMultiplePassengerEntity)
            {
                IMultiplePassengerEntity multi = (IMultiplePassengerEntity) entity;
                float yaw = -(multi.getPrevYaw() + (multi.getYaw() - multi.getPrevYaw()) * partialTicks);
                float pitch = -(multi.getPrevPitch() + (multi.getPitch() - multi.getPrevPitch()) * partialTicks);
                GL11.glRotatef(yaw, 0, 1, 0);
                GL11.glRotatef(pitch, 0, 0, 1);
            }
            MutableBlockPos pos = new MutableBlockPos();
            int xMin = MathHelper.floor(blockEntity.getMin().getX() + entity.posX);
            int zMin = MathHelper.floor(blockEntity.getMin().getZ() + entity.posZ);
            int xMax = MathHelper.floor(blockEntity.getMax().getX() + entity.posX);
            int zMax = MathHelper.floor(blockEntity.getMax().getZ() + entity.posZ);
            int yMin = (int) Math.round(blockEntity.getMin().getY() + entity.posY);
            int yMax = (int) Math.round(blockEntity.getMax().getY() + entity.posY);

            for (int i = xMin; i <= xMax; i++)
                for (int j = yMin; j <= yMax; j++)
                    for (int k = zMin; k <= zMax; k++)
                    {
                        pos.setPos(i, j, k);
                        if (!blockEntity.shouldHide(pos)) drawBlockAt(pos, blockEntity);
                        else drawCrateAt(pos, blockEntity);
                    }

            for (int i = xMin; i <= xMax; i++)
                for (int j = yMin; j <= yMax; j++)
                    for (int k = zMin; k <= zMax; k++)
                    {
                        pos.setPos(i, j, k);
                        if (!blockEntity.shouldHide(pos)) drawTileAt(pos, blockEntity, partialTicks);
                    }
            GL11.glPopMatrix();

            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void drawCrateAt(MutableBlockPos pos, IBlockEntity blockEntity)
    {
        BlockPos origin = ((Entity) blockEntity).getPosition();
        GlStateManager.pushMatrix();
        GL11.glTranslated(-origin.getX(), 0.5 - origin.getY(), -origin.getZ());
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        RenderHelper.disableStandardItemLighting();
        boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();

        GlStateManager.disableCull();

        if (Minecraft.isAmbientOcclusionEnabled())
        {
            GlStateManager.shadeModel(7425);
        }
        else
        {
            GlStateManager.shadeModel(7424);
        }
        float f7 = 1.0F;
        GlStateManager.scale(-f7, -f7, f7);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        FMLClientHandler.instance().getClient().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        IBakedModel model = getCrateModel();
        renderBakedBlockModel(blockEntity, model, Blocks.STONE.getDefaultState(), blockEntity.getFakeWorld(), pos);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private IBakedModel getCrateModel()
    {
        if (crate_model == null)
        {
            IModel model = ModelLoaderRegistry
                    .getModelOrLogError(new ResourceLocation(ThutCore.modid, "block/craft_crate"), "derp?");
            crate_model = model.bake(model.getDefaultState(), DefaultVertexFormats.BLOCK,
                    ModelLoader.defaultTextureGetter());
        }

        return crate_model;
    }

    private void drawBlockAt(BlockPos pos, IBlockEntity entity)
    {
        BlockState BlockState = entity.getFakeWorld().getBlockState(pos);
        if (BlockState.getMaterial() != Material.AIR)
        {
            BlockRendererDispatcher blockrendererdispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
            BlockState actualstate = BlockState.getActualState(entity.getFakeWorld(), pos);
            BlockState = actualstate.getBlock().getExtendedState(actualstate, entity.getFakeWorld(), pos);
            if (BlockState.getRenderType() == EnumBlockRenderType.MODEL)
            {
                BlockPos liftPos = ((Entity) entity).getPosition();
                GlStateManager.pushMatrix();
                GL11.glTranslated(-liftPos.getX(), 0.5 - liftPos.getY(), -liftPos.getZ());
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-180.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                RenderHelper.disableStandardItemLighting();

                boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.enableBlend();

                GlStateManager.disableCull();

                if (Minecraft.isAmbientOcclusionEnabled())
                {
                    GlStateManager.shadeModel(7425);
                }
                else
                {
                    GlStateManager.shadeModel(7424);
                }
                float f7 = 1.0F;
                GlStateManager.scale(-f7, -f7, f7);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                FMLClientHandler.instance().getClient().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                IBakedModel model = blockrendererdispatcher.getModelForState(actualstate);
                renderBakedBlockModel(entity, model, BlockState, entity.getFakeWorld(), pos);
                if (!blend) GL11.glDisable(GL11.GL_BLEND);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderBakedBlockModel(IBlockEntity entity, IBakedModel model, BlockState state, IBlockReader world,
            BlockPos pos)
    {
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelSmooth(entity.getFakeWorld().world, model, state, pos, buffer, false, 0);
        tessellator.draw();
        return;
    }

    private void drawTileAt(BlockPos pos, IBlockEntity entity, float partialTicks)
    {
        TileEntity tile = entity.getFakeWorld().getTileEntity(pos);
        if (tile != null)
        {
            GL11.glPushMatrix();
            BlockPos liftPos = ((Entity) entity).getPosition();
            GL11.glTranslated(pos.getX() - liftPos.getX(), pos.getY() + 0.5 - liftPos.getY(),
                    pos.getZ() - liftPos.getZ());
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(-180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(0.5F, 0.5F, 0.5F);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            float f7 = 1.0F;
            GlStateManager.scale(-f7, -f7, f7);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            boolean fast = tile.hasFastRenderer();
            if (fast)
            {
                TileEntityRendererDispatcher.instance.preDrawBatch();
                TileEntityRendererDispatcher.instance.render(tile, 0, 0, 0, partialTicks);
                TileEntityRendererDispatcher.instance.drawBatch(0);
                TileEntityRendererDispatcher.instance.preDrawBatch();
                TileEntityRendererDispatcher.instance.render(tile, 0, 0, 0, partialTicks);
                TileEntityRendererDispatcher.instance.drawBatch(1);
            }
            else TileEntityRendererDispatcher.instance.render(tile, 0, 0, 0, partialTicks);
            GlStateManager.popMatrix();
            GL11.glPopMatrix();
        }
    }

    @Override
    public boolean shouldRender(T entityIn, ICamera camera, double camX, double camY, double camZ)
    {
        return true;
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity)
    {
        return null;
    }
}
