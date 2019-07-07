package thut.api.entity.blockentity.render;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import thut.api.entity.IMultiplePassengerEntity;
import thut.api.entity.blockentity.IBlockEntity;

@OnlyIn(Dist.CLIENT)
public class RenderBlockEntity<T extends LivingEntity> extends LivingRenderer<T, ModelBlockEntity<T>>
{
    private static IBakedModel crate_model;

    static final Tessellator t = new Tessellator(2097152);

    float         pitch = 0.0f;
    float         yaw   = 0.0f;
    long          time  = 0;
    boolean       up    = true;
    BufferBuilder b     = RenderBlockEntity.t.getBuffer();

    ResourceLocation texture;

    public RenderBlockEntity(final EntityRendererManager manager)
    {
        super(manager, new ModelBlockEntity<>(), 0);
    }

    @Override
    public void doRender(final T entity, final double x, final double y, final double z, final float entityYaw,
            final float partialTicks)
    {
        // Incase some other mod tries to render as us.
        if (!(entity instanceof IBlockEntity)) return;
        try
        {
            this.b = Tessellator.getInstance().getBuffer();
            final IBlockEntity blockEntity = (IBlockEntity) entity;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y + 0.5, z);
            if (entity instanceof IMultiplePassengerEntity)
            {
                final IMultiplePassengerEntity multi = (IMultiplePassengerEntity) entity;
                final float yaw = -(multi.getPrevYaw() + (multi.getYaw() - multi.getPrevYaw()) * partialTicks);
                final float pitch = -(multi.getPrevPitch() + (multi.getPitch() - multi.getPrevPitch()) * partialTicks);
                GL11.glRotatef(yaw, 0, 1, 0);
                GL11.glRotatef(pitch, 0, 0, 1);
            }
            final MutableBlockPos pos = new MutableBlockPos();

            final BlockPos liftPos = ((Entity) entity).getPosition();
            liftPos.getX();
            // GL11.glTranslated(-liftPos.getX(), 0.5 - liftPos.getY(),
            // -liftPos.getZ());

            // final int xMin = MathHelper.floor(blockEntity.getMin().getX() +
            // liftPos.getX());
            // final int xMax = MathHelper.floor(blockEntity.getMax().getX() +
            // liftPos.getX());
            // final int zMin = MathHelper.floor(blockEntity.getMin().getZ() +
            // liftPos.getZ());
            // final int zMax = MathHelper.floor(blockEntity.getMax().getZ() +
            // liftPos.getZ());
            // final int yMin = MathHelper.floor(blockEntity.getMin().getY() +
            // liftPos.getY());
            // final int yMax = MathHelper.floor(blockEntity.getMax().getY() +
            // liftPos.getY());

            final int xMin = MathHelper.floor(blockEntity.getMin().getX());
            final int xMax = MathHelper.floor(blockEntity.getMax().getX());
            final int zMin = MathHelper.floor(blockEntity.getMin().getZ());
            final int zMax = MathHelper.floor(blockEntity.getMax().getZ());
            final int yMin = MathHelper.floor(blockEntity.getMin().getY());
            final int yMax = MathHelper.floor(blockEntity.getMax().getY());

            for (int i = xMin; i <= xMax; i++)
                for (int j = yMin; j <= yMax; j++)
                    for (int k = zMin; k <= zMax; k++)
                    {
                        pos.setPos(i - xMin, j - yMin, k - zMin);
                        if (!blockEntity.shouldHide(pos)) this.drawBlockAt(pos, blockEntity);
                        else this.drawCrateAt(pos, blockEntity);
                    }

            for (int i = xMin; i <= xMax; i++)
                for (int j = yMin; j <= yMax; j++)
                    for (int k = zMin; k <= zMax; k++)
                    {
                        pos.setPos(i, j, k);
                        if (!blockEntity.shouldHide(pos)) this.drawTileAt(pos, blockEntity, partialTicks);
                    }
            GL11.glPopMatrix();

            GlStateManager.enableTexture();
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    private void drawBlockAt(BlockPos pos, final IBlockEntity entity)
    {
        if (entity.getBlocks() == null) return;
        BlockState BlockState = entity.getBlocks()[pos.getX()][pos.getY()][pos.getZ()];
        final BlockPos mobPos = entity.getMin();
        pos = pos.add(mobPos);
        if (BlockState == null) BlockState = Blocks.AIR.getDefaultState();
        if (BlockState.getMaterial() != Material.AIR)
        {
            final BlockRendererDispatcher blockrendererdispatcher = Minecraft.getInstance()
                    .getBlockRendererDispatcher();
            final BlockState actualstate = BlockState.getExtendedState((IBlockReader) entity.getFakeWorld(), pos);
            BlockState = actualstate.getBlock().getExtendedState(actualstate, (IBlockReader) entity.getFakeWorld(),
                    pos);
            if (BlockState.getRenderType() == BlockRenderType.MODEL)
            {
                GlStateManager.pushMatrix();
                GlStateManager.rotatef(90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotatef(-180.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translatef(0.5F, 0.5F, 0.5F);
                RenderHelper.disableStandardItemLighting();

                final boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.enableBlend();

                GlStateManager.disableCull();

                if (Minecraft.isAmbientOcclusionEnabled()) GlStateManager.shadeModel(7425);
                else GlStateManager.shadeModel(7424);
                final float f7 = 1.0F;
                GlStateManager.scalef(-f7, -f7, f7);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                final IBakedModel model = blockrendererdispatcher.getModelForState(actualstate);
                this.renderBakedBlockModel(entity, model, BlockState, (IBlockReader) entity.getFakeWorld(), pos);
                if (!blend) GL11.glDisable(GL11.GL_BLEND);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.popMatrix();
            }
        }
    }

    private void drawCrateAt(final MutableBlockPos pos, final IBlockEntity blockEntity)
    {
        GlStateManager.pushMatrix();
        GlStateManager.rotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(-180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translatef(0.5F, 0.5F, 0.5F);
        RenderHelper.disableStandardItemLighting();
        final boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();

        GlStateManager.disableCull();

        if (Minecraft.isAmbientOcclusionEnabled()) GlStateManager.shadeModel(7425);
        else GlStateManager.shadeModel(7424);
        final float f7 = 1.0F;
        GlStateManager.scalef(-f7, -f7, f7);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        this.getCrateModel();
        // renderBakedBlockModel(blockEntity, model,
        // Blocks.STONE.getDefaultState(), blockEntity.getFakeWorld(), pos);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void drawTileAt(final BlockPos pos, final IBlockEntity entity, final float partialTicks)
    {
        final TileEntity tile = entity.getFakeWorld().getTile(pos);
        if (tile != null)
        {
            GL11.glPushMatrix();
            GlStateManager.rotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.pushMatrix();
            GlStateManager.rotatef(-180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.translatef(0.5F, 0.5F, 0.5F);
            GlStateManager.rotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            final float f7 = 1.0F;
            GlStateManager.scalef(-f7, -f7, f7);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            final boolean fast = tile.hasFastRenderer();
            if (fast)
            {
                TileEntityRendererDispatcher.instance.preDrawBatch();
                TileEntityRendererDispatcher.instance.render(tile, 0, 0, 0, partialTicks);
                TileEntityRendererDispatcher.instance.drawBatch();
            }
            else TileEntityRendererDispatcher.instance.render(tile, 0, 0, 0, partialTicks);
            GlStateManager.popMatrix();
            GL11.glPopMatrix();
        }
    }

    private IBakedModel getCrateModel()
    {
        if (RenderBlockEntity.crate_model == null)
        {
            // IModel<?> model = ModelLoaderRegistry
            // .getModelOrLogError(new ResourceLocation(ThutCore.MODID,
            // "block/craft_crate"), "derp?");
            // crate_model = model.bake(model.getDefaultState(),
            // DefaultVertexFormats.BLOCK,
            // ModelLoader.defaultTextureGetter());
        }
        return RenderBlockEntity.crate_model;
    }

    @Override
    protected ResourceLocation getEntityTexture(final T entity)
    {
        return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
    }

    private void renderBakedBlockModel(final IBlockEntity entity, final IBakedModel model, final BlockState state,
            final IBlockReader world, final BlockPos pos)
    {
        GlStateManager.rotatef(90.0F, 0.0F, 1.0F, 0.0F);
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer().renderModelSmooth(entity
                .getFakeWorld().getWrapped(), model, state, pos, buffer, false, new Random(), 0,
                EmptyModelData.INSTANCE);
        tessellator.draw();
        return;
    }

    @Override
    public boolean shouldRender(final T entityIn, final ICamera camera, final double camX, final double camY,
            final double camZ)
    {
        return true;
    }
}
