package thut.tech.common.blocks.lift;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import io.netty.buffer.Unpooled;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import thut.api.entity.blockentity.BlockEntityWorld;
import thut.api.entity.blockentity.IBlockEntity;
import thut.api.maths.Vector3;
import thut.api.network.PacketHandler;
import thut.lib.CompatWrapper;
import thut.tech.common.entity.EntityLift;
import thut.tech.common.network.PacketPipeline;
import thut.tech.common.network.PacketPipeline.ServerPacket;

@net.minecraftforge.fml.common.Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityLiftAccess extends TileEntity implements ITickable, SimpleComponent
{
    public int                          power        = 0;
    public int                          prevPower    = 1;
    public EntityLift                   lift;
    public BlockState                  copiedState  = null;
    boolean                             listNull     = false;
    List<Entity>                        list         = new ArrayList<Entity>();
    Vector3                             here;
    public TileEntityLiftAccess         rootNode;
    public Vector<TileEntityLiftAccess> connected    = new Vector<TileEntityLiftAccess>();
    Direction                          sourceSide;
    boolean                             loaded       = false;
    public int                          floor        = 0;
    public int                          calledYValue = -1;
    public int                          calledFloor  = 0;
    public int                          currentFloor = 0;
    public UUID                         liftID       = null;
    UUID                                empty        = new UUID(0, 0);
    private byte[]                      sides        = new byte[6];
    private byte[]                      sidePages    = new byte[6];
    int                                 tries        = 0;
    public boolean                      toClear      = false;
    public boolean                      first        = true;
    public boolean                      read         = false;
    public boolean                      redstone     = true;
    public boolean                      powered      = false;
    public boolean[]                    callFaces    = new boolean[6];
    public boolean[]                    editFace     = new boolean[6];
    public boolean[]                    floorDisplay = new boolean[6];
    public boolean                      callPanel    = false;

    // Used for limiting how often checks for connected controllers are done.
    private int                         tick         = 0;

    public TileEntityLiftAccess()
    {
    }

    public void buttonPress(int button, boolean callPanel)
    {
        if (callPanel && lift != null)
        {
            lift.call(floor);
        }
        else
        {
            if (button != 0 && button <= lift.floors.length && lift != null && lift.hasFloors[button - 1])
            {
                if (button == floor)
                {
                }
                else
                {
                    if (lift.getCurrentFloor() == floor) lift.setCurrentFloor(-1);
                }
                lift.call(button);
            }
        }
    }

    /** Sets the worldObj for this tileEntity. */
    public void setWorldObj(World worldIn)
    {
        this.world = worldIn;
        if (worldIn instanceof BlockEntityWorld)
        {
            IBlockEntity blockEntity = ((BlockEntityWorld) worldIn).getEntity();
            if (blockEntity instanceof EntityLift)
            {
                this.setLift((EntityLift) blockEntity);
            }
        }
    }

    public boolean checkSides()
    {
        List<EntityLift> check = world.getEntitiesWithinAABB(EntityLift.class,
                new AxisAlignedBB(getPos().getX() + 0.5 - 1, getPos().getY(), getPos().getZ() + 0.5 - 1,
                        getPos().getX() + 0.5 + 1, getPos().getY() + 1, getPos().getZ() + 0.5 + 1));
        if (check != null && check.size() > 0)
        {
            lift = check.get(0);
            liftID = lift.getPersistentID();
        }
        return !(check == null || check.isEmpty());
    }

    public String connectionInfo()
    {
        String ret = "";
        return ret;
    }

    public boolean doButtonClick(LivingEntity clicker, Direction side, float hitX, float hitY, float hitZ)
    {
        if (liftID != null && !liftID.equals(empty) && lift != EntityLift.getLiftFromUUID(liftID, world))
        {
            lift = EntityLift.getLiftFromUUID(liftID, world);
        }
        int button = getButtonFromClick(side, hitX, hitY, hitZ);
        boolean valid = lift != null && lift.hasFloors[button - 1];
        if (lift != null && isSideOn(side))
        {
            if (editFace[side.ordinal()])
            {
                if (!getWorld().isRemote)
                {
                    String message = "msg.callPanel.name";
                    switch (button)
                    {
                    case 1:
                        callFaces[side.ordinal()] = !callFaces[side.ordinal()];
                        floorDisplay[side.ordinal()] = false;
                        clicker.sendMessage(new TranslationTextComponent(message, callFaces[side.ordinal()]));
                        break;
                    case 2:
                        floorDisplay[side.ordinal()] = !floorDisplay[side.ordinal()];
                        callFaces[side.ordinal()] = false;
                        message = "msg.floorDisplay.name";
                        clicker.sendMessage(new TranslationTextComponent(message, floorDisplay[side.ordinal()]));
                        break;
                    case 16:
                        editFace[side.ordinal()] = false;
                        message = "msg.editMode.name";
                        clicker.sendMessage(new TranslationTextComponent(message, false));
                        break;
                    }
                    if (clicker instanceof ServerPlayerEntity) sendUpdate((ServerPlayerEntity) clicker);
                }
                return true;
            }
            else
            {
                if (floorDisplay[side.ordinal()]) return false;
                if (getWorld() instanceof BlockEntityWorld)
                {
                    this.buttonPress(button, callFaces[side.ordinal()]);
                    this.calledFloor = this.lift.getDestinationFloor();
                }
                else if (getWorld().isRemote)
                {
                    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(32));
                    buffer.writeBlockPos(getPos());
                    buffer.writeInt(button);
                    buffer.writeBoolean(callFaces[side.ordinal()]);
                    ServerPacket packet = new ServerPacket(buffer);
                    PacketPipeline.sendToServer(packet);
                }
            }
        }
        if (clicker instanceof ServerPlayerEntity) sendUpdate((ServerPlayerEntity) clicker);
        return valid;
    }

    public int getButtonFromClick(Direction side, float hitX, float hitY, float hitZ)
    {
        int ret = 0;
        int page = getSidePage(side);
        switch (side.getIndex())
        {
        case 0:
        {
            return 0 + 16 * page;
        }
        case 1:
        {
            ret = 1 + (int) (((1 - hitX) * 4) % 4) + 4 * (int) (((1 - hitZ) * 4) % 4);
            return ret + 16 * page;
        }
        case 2:
        {
            ret = 1 + (int) (((1 - hitX) * 4) % 4) + 4 * (int) (((1 - hitY) * 4) % 4);
            return ret + 16 * page;
        }
        case 3:
        {
            ret = 1 + (int) (((hitX) * 4) % 4) + 4 * (int) (((1 - hitY) * 4) % 4);
            return ret + 16 * page;
        }
        case 4:
        {
            ret = 1 + 4 * (int) (((1 - hitY) * 4) % 4) + (int) (((hitZ) * 4) % 4);
            return ret + 16 * page;
        }
        case 5:
        {
            ret = 1 + 4 * (int) (((1 - hitY) * 4) % 4) + (int) (((1 - hitZ) * 4) % 4);
            return ret + 16 * page;
        }
        default:
        {
            return 0 + 16 * page;
        }

        }

    }

    @Override
    public String getComponentName()
    {
        return "lift";
    }

    /** Overriden in a sign to provide the text. */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(this.getPos(), 3, getUpdateTag());
    }

    public int getSidePage(Direction side)
    {
        return sidePages[side.getIndex()];
    }

    /** Called from Chunk.setBlockIDWithMetadata and Chunk.fillChunk, determines
     * if this tile entity should be re-created when the ID, or Metadata
     * changes. Use with caution as this will leave straggler TileEntities, or
     * create conflicts with other TileEntities if not used properly.
     *
     * @param world
     *            Current world
     * @param pos
     *            Tile's world position
     * @param oldState
     *            The old ID of the block
     * @param newState
     *            The new ID of the block (May be the same)
     * @return true forcing the invalidation of the existing TE, false not to
     *         invalidate the existing TE */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, BlockState oldState, BlockState newSate)
    {
        return oldState.getBlock() != newSate.getBlock();
    }

    public boolean isSideOn(Direction side)
    {
        int state = 1;
        byte byte0 = sides[side.getIndex()];
        return (byte0 & state) != 0;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        CompoundNBT CompoundNBT = pkt.getNbtCompound();
        this.readFromNBT(CompoundNBT);
    }

    @Override
    public void readFromNBT(CompoundNBT par1)
    {
        super.readFromNBT(par1);
        floor = par1.getInt("floor");
        liftID = new UUID(par1.getLong("idMost"), par1.getLong("idLess"));
        sides = par1.getByteArray("sides");
        for (Direction face : Direction.HORIZONTALS)
        {
            callFaces[face.ordinal()] = par1.getBoolean(face + "Call");
        }
        for (Direction face : Direction.HORIZONTALS)
        {
            editFace[face.ordinal()] = par1.getBoolean(face + "Edit");
        }
        for (Direction face : Direction.HORIZONTALS)
        {
            floorDisplay[face.ordinal()] = par1.getBoolean(face + "Display");
        }
        if (sides.length != 6) sides = new byte[6];
        sidePages = par1.getByteArray("sidePages");
        if (sidePages.length != 6) sidePages = new byte[6];
        if (par1.hasKey("state"))
        {
            CompoundNBT state = par1.getCompound("state");
            String key = state.getString("K");
            int meta = state.getInt("M");
            Block block = Block.REGISTRY.getObject(new ResourceLocation(key));
            if (block != null) copiedState = CompatWrapper.getBlockStateFromMeta(block, meta);
        }
    }

    public void setFloor(int floor)
    {
        if (lift != null && floor <= lift.floors.length && floor > 0)
        {
            lift.setFoor(this, floor);
            this.floor = floor;
            this.markDirty();
        }
    }

    public void setLift(EntityLift lift)
    {
        this.lift = lift;
        this.liftID = lift.getUniqueID();
        if (world != null && !world.isRemote) PacketHandler.sendTileUpdate(this);
    }

    public void setSide(Direction side, boolean flag)
    {
        int state = 1;
        byte byte0 = sides[side.getIndex()];

        if (side.getIndex() < 2) return;

        if (flag)
        {
            sides[side.getIndex()] = (byte) (byte0 | state);
        }
        else
        {
            sides[side.getIndex()] = (byte) (byte0 & -state - 1);
        }
        markDirty();
    }

    public void setSidePage(Direction side, int page)
    {
        sidePages[side.getIndex()] = (byte) page;
    }

    @Override
    public void update()
    {
        if (here == null) here = Vector3.getNewVector();
        here.set(this);
        if (this.world instanceof BlockEntityWorld) { return; }

        if ((lift == null || lift.isDead))
        {
            calledYValue = -1;
            calledFloor = 0;
            currentFloor = 0;
        }

        if (lift != null && !world.isRemote)
        {
            // This is whether the lift is currently at this floor, so redstone
            // should be emitted.
            boolean check = lift.getCurrentFloor() == this.floor && (int) (lift.motionY * 16) == 0;

            BlockState state = world.getBlockState(getPos());
            boolean old = state.getValue(BlockLift.CURRENT);
            boolean callPanel = false;
            if (!old && !lift.getCalled()) for (Direction face : Direction.HORIZONTALS)
            {
                callPanel |= callFaces[face.ordinal()];
            }
            // Call panels should only respond to redstone signals if they are
            // not supposed to be emitting one themselves.
            if (callPanel && !old && !lift.getCalled() && !check)
            {
                if (world.isBlockPowered(getPos())) lift.call(floor);
            }

            // If state has changed, change the blockstate as well. only do this
            // if it has changed to prevent too many changes to state.
            if (check != old)
            {
                state = state.withProperty(BlockLift.CURRENT, check);
                world.setBlockState(getPos(), state);
            }

            // Check to see if the called state needs to be changed.
            if (lift.motionY == 0 || lift.getDestinationFloor() == floor)
            {
                old = state.getValue(BlockLift.CALLED);
                check = lift.getDestinationFloor() == floor;
                if (check != old)
                {
                    state = state.withProperty(BlockLift.CALLED, check);
                    world.setBlockState(getPos(), state);
                }
            }
            MinecraftForge.EVENT_BUS.post(new EventLiftUpdate(this));
        }

        if (lift != null && floor > 0)
        {
            // Set lifts current floor to this if it is in the area of the
            // floor.
            if ((int) (Math.round(lift.posY)) == lift.floors[floor - 1])
            {
                lift.setCurrentFloor(floor);
            }
            // If lift thinks it is currently on this floor, and is not, set it
            // otherwise.
            else if (lift.getCurrentFloor() == floor)
            {
                lift.setCurrentFloor(-1);
            }

            // Sets the values used for rendering colours over numbers on the
            // display.
            calledFloor = lift.getDestinationFloor();
            currentFloor = lift.getCurrentFloor();
        }

        if (lift == null && liftID != null)
        {
            // Find lift if existing lift isn't found.
            EntityLift tempLift = EntityLift.getLiftFromUUID(liftID, world);
            if (liftID != null && !liftID.equals(empty) && (lift == null || lift.isDead || tempLift != lift))
            {
                lift = tempLift;
                if (lift == null || lift.isDead) return;

                // Make sure that lift's floor is this one if it doesn't have
                // one defined.
                if (floor > 0 && !lift.hasFloors[floor - 1])
                {
                    this.setFloor(floor);
                }
            }
        }
        // Cleanup floor if the lift is gone.
        if (floor > 0 && (lift == null || lift.isDead))
        {
            lift = null;
            floor = 0;
        }

        // Scan sides for a controller which actually has a lift attached, and
        // attach self to that floor.
        if (lift == null && tick++ % 50 == 0)
        {
            for (Direction side : Direction.values())
            {
                TileEntity t = here.getTileEntity(world, side);
                Block b = here.getBlock(world, side);
                if (b == getBlockType() && t instanceof TileEntityLiftAccess)
                {
                    TileEntityLiftAccess te = (TileEntityLiftAccess) t;
                    if (te.lift != null)
                    {
                        lift = te.lift;
                        floor = te.floor;
                        markDirty();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public CompoundNBT writeToNBT(CompoundNBT par1)
    {
        super.writeToNBT(par1);
        par1.setInteger("floor", floor);
        par1.putByteArray("sides", sides);
        par1.putByteArray("sidePages", sidePages);
        for (Direction face : Direction.HORIZONTALS)
        {
            par1.putBoolean(face + "Call", callFaces[face.ordinal()]);
        }
        for (Direction face : Direction.HORIZONTALS)
        {
            par1.putBoolean(face + "Edit", editFace[face.ordinal()]);
        }
        for (Direction face : Direction.HORIZONTALS)
        {
            par1.putBoolean(face + "Display", floorDisplay[face.ordinal()]);
        }
        if (lift != null)
        {
            liftID = lift.getPersistentID();
        }
        if (liftID != null)
        {
            par1.putLong("idLess", liftID.getLeastSignificantBits());
            par1.putLong("idMost", liftID.getMostSignificantBits());
        }
        if (copiedState != null)
        {
            CompoundNBT state = new CompoundNBT();
            state.putString("K", copiedState.getBlock().getRegistryName().toString());
            state.setInteger("M", copiedState.getBlock().getMetaFromState(copiedState));
            par1.setTag("state", state);
        }
        return par1;
    }

    @OnlyIn(Dist.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        AxisAlignedBB bb = INFINITE_EXTENT_AABB;
        return bb;
    }

    public void sendUpdate(ServerPlayerEntity player)
    {
        if (world instanceof BlockEntityWorld) return;
        player.connection.sendPacket(getUpdatePacket());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        return writeToNBT(new CompoundNBT());
    }

    // Open Computers stuff here, possibly will move this to a compat class or
    // something?
    /*
     * Calls lift to specified Floor
     */
    @Callback(doc = "function(floor:number) -- Calls the Lift to the specified Floor")
    @Optional.Method(modid = "opencomputers")
    public Object[] callFloor(Context context, Arguments args) throws Exception
    {
        if (lift != null)
        {
            lift.call(args.checkInteger(0));
            return new Object[] {};
        }
        throw new Exception("no connected lift");
    }

    /*
     * Calls lift to specified Y value
     */
    @Callback(doc = "function(yValue:number) -- Calls the Lift to the specified Y level")

    @Optional.Method(modid = "opencomputers")
    public Object[] callYValue(Context context, Arguments args) throws Exception
    {
        if (lift != null)
        {
            lift.setDestY(args.checkInteger(0));
            return new Object[] {};
        }
        throw new Exception("no connected lift");
    }

    /*
     * Calls lift to specified Y value
     */
    @Callback(doc = "function(xValue:number) -- Calls the Lift to thespecified X location")

    @Optional.Method(modid = "opencomputers")
    public Object[] callXValue(Context context, Arguments args) throws Exception
    {
        if (lift != null)
        {
            // +0.5f as the elevator is in centre of blocks.
            lift.setDestX(args.checkInteger(0) + 0.5f);
            return new Object[] {};
        }
        throw new Exception("no connected lift");
    }

    /*
     * Calls lift to specified Y value
     */
    @Callback(doc = "function(zValue:number) -- Calls the Lift to thespecified Z location")

    @Optional.Method(modid = "opencomputers")
    public Object[] callZValue(Context context, Arguments args) throws Exception
    {
        if (lift != null)
        {
            // +0.5f as the elevator is in centre of blocks.
            lift.setDestZ(args.checkInteger(0) + 0.5f);
            return new Object[] {};
        }
        throw new Exception("no connected lift");
    }

    /*
     * Sets floor associated with this block
     */
    @Callback(doc = "function(floor:number) -- Sets the floor assosiated tothe Controller")
    @Optional.Method(modid = "opencomputers")
    public Object[] setFloor(Context context, Arguments args)
    {
        floor = args.checkInteger(0);
        return new Object[] { floor };
    }

    /*
     * Returns the Yvalue of the lift.
     */
    @Callback(doc = "returns the current Y value of the lift.")
    @Optional.Method(modid = "opencomputers")
    public Object[] getYValue(Context context, Arguments args) throws Exception
    {
        if (lift != null) return new Object[] { (float) lift.posY };

        throw new Exception("no connected lift");
    }

    /*
     * Returns the Yvalue of the lift.
     */
    @Callback(doc = "returns the current X value of the lift.")
    @Optional.Method(modid = "opencomputers")
    public Object[] getXValue(Context context, Arguments args) throws Exception
    {
        if (lift != null) return new Object[] { (float) lift.posX };

        throw new Exception("no connected lift");
    }

    /*
     * Returns the Yvalue of the lift.
     */
    @Callback(doc = "returns the current Z value of the lift.")
    @Optional.Method(modid = "opencomputers")
    public Object[] getZValue(Context context, Arguments args) throws Exception
    {
        if (lift != null) return new Object[] { (float) lift.posZ };

        throw new Exception("no connected lift");
    }

    /*
     * Returns floor associated with this block
     */
    @Callback(doc = "returns the Floor assigned to the Controller")
    @Optional.Method(modid = "opencomputers")
    public Object[] getFloor(Context context, Arguments args)
    {
        return new Object[] { floor };
    }

    /*
     * Returns the Y value of the controller for the specified floor
     */
    @Callback(doc = "function(floor:number) -- returns the y value of the specified floor")
    @Optional.Method(modid = "opencomputers")
    public Object[] getFloorYValue(Context context, Arguments args) throws Exception
    {
        if (lift != null)
        {
            int floor = args.checkInteger(0);

            if (floor > 0 && floor <= lift.floors.length)
            {
                int value = lift.floors[floor - 1];
                if (!lift.hasFloors[floor - 1]) throw new Exception("floor " + floor + " is not assigned");
                return new Object[] { value };
            }
            throw new Exception("floor out of bounds");
        }
        throw new Exception("no connected lift");
    }

    /*
     * Returns floor associated with this block
     */
    @Callback(doc = "returns if the elevator is not currently called to afloor")
    @Optional.Method(modid = "opencomputers")
    public Object[] isReady(Context context, Arguments args) throws Exception
    {
        if (lift != null) { return new Object[] { !lift.getCalled() }; }
        throw new Exception("no connected lift");
    }
}
