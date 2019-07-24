/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.net.client.CMessageAddScreen;
import net.montoyo.wd.net.client.CMessageCloseGui;
import net.montoyo.wd.net.client.CMessageJSResponse;
import net.montoyo.wd.net.client.CMessageScreenUpdate;
import net.montoyo.wd.net.server.SMessageRequestTEData;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.Rotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class TileEntityScreen extends TileEntity {

    public static class Screen {

        public BlockSide side;
        public Vector2i size;
        public Vector2i resolution;
        public Rotation rotation = Rotation.ROT_0;
        public String url;
        private VideoType videoType;
        public NameUUIDPair owner;
        public ArrayList<NameUUIDPair> friends;
        public int friendRights;
        public int otherRights;
        public IBrowser browser;
        public ArrayList<ItemStack> upgrades;
        public boolean doTurnOnAnim;
        public long turnOnTime;
        public EntityPlayer laserUser;
        public final Vector2i lastMousePos = new Vector2i();
        public NibbleArray redstoneStatus; //null on client
        public boolean autoVolume = true;

        public static Screen deserialize(NBTTagCompound tag) {
            Screen ret = new Screen();
            ret.side = BlockSide.values()[tag.getByte("Side")];
            ret.size = new Vector2i(tag.getInteger("Width"), tag.getInteger("Height"));
            ret.resolution = new Vector2i(tag.getInteger("ResolutionX"), tag.getInteger("ResolutionY"));
            ret.rotation = Rotation.values()[tag.getByte("Rotation")];
            ret.url = tag.getString("URL");
            ret.videoType = VideoType.getTypeFromURL(ret.url);

            if(ret.resolution.x <= 0 || ret.resolution.y <= 0) {
                float psx = ((float) ret.size.x) * 16.f - 4.f;
                float psy = ((float) ret.size.y) * 16.f - 4.f;
                psx *= 8.f; //TODO: Use ratio in config file
                psy *= 8.f;

                ret.resolution.x = (int) psx;
                ret.resolution.y = (int) psy;
            }

            if(tag.hasKey("OwnerName")) {
                String name = tag.getString("OwnerName");
                UUID uuid = tag.getUniqueId("OwnerUUID");
                ret.owner = new NameUUIDPair(name, uuid);
            }

            NBTTagList friends = tag.getTagList("Friends", 10);
            ret.friends = new ArrayList<>(friends.tagCount());

            for(int i = 0; i < friends.tagCount(); i++) {
                NBTTagCompound nf = friends.getCompoundTagAt(i);
                NameUUIDPair pair = new NameUUIDPair(nf.getString("Name"), nf.getUniqueId("UUID"));
                ret.friends.add(pair);
            }

            ret.friendRights = tag.getByte("FriendRights");
            ret.otherRights = tag.getByte("OtherRights");

            NBTTagList upgrades = tag.getTagList("Upgrades", 10);
            ret.upgrades = new ArrayList<>();

            for(int i = 0; i < upgrades.tagCount(); i++)
                ret.upgrades.add(new ItemStack(upgrades.getCompoundTagAt(i)));

            if(tag.hasKey("AutoVolume"))
                ret.autoVolume = tag.getBoolean("AutoVolume");

            return ret;
        }

        public NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Side", (byte) side.ordinal());
            tag.setInteger("Width", size.x);
            tag.setInteger("Height", size.y);
            tag.setInteger("ResolutionX", resolution.x);
            tag.setInteger("ResolutionY", resolution.y);
            tag.setByte("Rotation", (byte) rotation.ordinal());
            tag.setString("URL", url);

            if(owner == null)
                Log.warning("Found TES with NO OWNER!!");
            else {
                tag.setString("OwnerName", owner.name);
                tag.setUniqueId("OwnerUUID", owner.uuid);
            }

            NBTTagList list = new NBTTagList();
            for(NameUUIDPair f: friends) {
                NBTTagCompound nf = new NBTTagCompound();
                nf.setString("Name", f.name);
                nf.setUniqueId("UUID", f.uuid);

                list.appendTag(nf);
            }

            tag.setTag("Friends", list);
            tag.setByte("FriendRights", (byte) friendRights);
            tag.setByte("OtherRights", (byte) otherRights);

            list = new NBTTagList();
            for(ItemStack is: upgrades)
                list.appendTag(is.writeToNBT(new NBTTagCompound()));

            tag.setTag("Upgrades", list);
            tag.setBoolean("AutoVolume", autoVolume);
            return tag;
        }

        public int rightsFor(EntityPlayer ply) {
            return rightsFor(ply.getGameProfile().getId());
        }

        public int rightsFor(UUID uuid) {
            if(owner.uuid.equals(uuid))
                return ScreenRights.ALL;

            return friends.stream().anyMatch(f -> f.uuid.equals(uuid)) ? friendRights : otherRights;
        }

        public void setupRedstoneStatus(World world, BlockPos start) {
            if(world.isRemote) {
                Log.warning("Called Screen.setupRedstoneStatus() on client.");
                return;
            }

            if(redstoneStatus != null) {
                Log.warning("Called Screen.setupRedstoneStatus() on server, but redstone status is non-null");
                return;
            }

            redstoneStatus = new NibbleArray(size.x * size.y);
            final EnumFacing facing = EnumFacing.VALUES[side.reverse().ordinal()];
            final ScreenIterator it = new ScreenIterator(start, side, size);

            while(it.hasNext()) {
                int idx = it.getIndex();
                redstoneStatus.set(idx, world.getRedstonePower(it.next(), facing));
            }
        }

        public void clampResolution() {
            if(resolution.x > WebDisplays.INSTANCE.maxResX) {
                float newY = ((float) resolution.y) * ((float) WebDisplays.INSTANCE.maxResX) / ((float) resolution.x);
                resolution.x = WebDisplays.INSTANCE.maxResX;
                resolution.y = (int) newY;
            }

            if(resolution.y > WebDisplays.INSTANCE.maxResY) {
                float newX = ((float) resolution.x) * ((float) WebDisplays.INSTANCE.maxResY) / ((float) resolution.y);
                resolution.x = (int) newX;
                resolution.y = WebDisplays.INSTANCE.maxResY;
            }
        }

    }

    public void forEachScreenBlocks(BlockSide side, Consumer<BlockPos> func) {
        Screen scr = getScreen(side);

        if(scr != null) {
            ScreenIterator it = new ScreenIterator(pos, side, scr.size);

            while(it.hasNext())
                func.accept(it.next());
        }
    }

    private final ArrayList<Screen> screens = new ArrayList<>();
    private AxisAlignedBB renderBB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private boolean loaded = true;
    public float ytVolume = Float.POSITIVE_INFINITY;

    public boolean isLoaded() {
        return loaded;
    }

    public void load() {
        loaded = true;
    }

    public void unload() {
        for(Screen scr: screens) {
            if(scr.browser != null) {
                scr.browser.close();
                scr.browser = null;
            }
        }

        loaded = false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        NBTTagList list = tag.getTagList("WDScreens", 10);
        if(list.hasNoTags())
            return;

        screens.clear();
        for(int i = 0; i < list.tagCount(); i++)
            screens.add(Screen.deserialize(list.getCompoundTagAt(i)));
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagList list = new NBTTagList();
        for(Screen scr: screens)
            list.appendTag(scr.serialize());

        tag.setTag("WDScreens", list);
        return tag;
    }

    private NetworkRegistry.TargetPoint point() {
        return new NetworkRegistry.TargetPoint(world.provider.getDimension(), (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 64.0);
    }

    public Screen addScreen(BlockSide side, Vector2i size, @Nullable Vector2i resolution, @Nullable EntityPlayer owner, boolean sendUpdate) {
        for(Screen scr: screens) {
            if(scr.side == side)
                return scr;
        }

        Screen ret = new Screen();
        ret.side = side;
        ret.size = size;
        ret.url = WebDisplays.INSTANCE.homePage;
        ret.friends = new ArrayList<>();
        ret.friendRights = ScreenRights.DEFAULTS;
        ret.otherRights = ScreenRights.DEFAULTS;
        ret.upgrades = new ArrayList<>();

        if(owner != null) {
            ret.owner = new NameUUIDPair(owner.getGameProfile());

            if(side == BlockSide.TOP || side == BlockSide.BOTTOM) {
                int rot = MathHelper.floor(((double) (owner.rotationYaw * 4.0f / 360.0f)) + 2.5) & 3;

                if(side == BlockSide.TOP) {
                    if(rot == 1)
                        rot = 3;
                    else if(rot == 3)
                        rot = 1;
                }

                ret.rotation = Rotation.values()[rot];
            }
        }

        if(resolution == null || resolution.x < 1 || resolution.y < 1) {
            float psx = ((float) size.x) * 16.f - 4.f;
            float psy = ((float) size.y) * 16.f - 4.f;
            psx *= 8.f; //TODO: Use ratio in config file
            psy *= 8.f;

            ret.resolution = new Vector2i((int) psx, (int) psy);
        } else
            ret.resolution = resolution;

        ret.clampResolution();

        if(!world.isRemote) {
            ret.setupRedstoneStatus(world, pos);

            if(sendUpdate)
                WebDisplays.NET_HANDLER.sendToAllAround(new CMessageAddScreen(this, ret), point());
        }

        screens.add(ret);

        if(world.isRemote)
            updateAABB();
        else
            markDirty();

        return ret;
    }

    public Screen getScreen(BlockSide side) {
        for(Screen scr: screens) {
            if(scr.side == side)
                return scr;
        }

        return null;
    }

    public int screenCount() {
        return screens.size();
    }

    public Screen getScreen(int idx) {
        return screens.get(idx);
    }

    public void clear() {
        screens.clear();

        if(!world.isRemote)
            markDirty();
    }

    public void requestData(EntityPlayerMP ep) {
        if(!world.isRemote)
            WebDisplays.NET_HANDLER.sendTo(new CMessageAddScreen(this), ep);
    }

    public void setScreenURL(BlockSide side, String url) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Attempt to change URL of non-existing screen on side %s", side.toString());
            return;
        }

        url = WebDisplays.applyBlacklist(url);
        scr.url = url;
        scr.videoType = VideoType.getTypeFromURL(url);

        if(world.isRemote) {
            if(scr.browser != null)
                scr.browser.loadURL(url);
        } else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.setURL(this, side, url), point());
            markDirty();
        }
    }

    public void removeScreen(BlockSide side) {
        int idx = -1;
        for(int i = 0; i < screens.size(); i++) {
            if(screens.get(i).side == side) {
                idx = i;
                break;
            }
        }

        if(idx < 0) {
            Log.error("Tried to delete non-existing screen on side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            if(screens.get(idx).browser != null) {
                screens.get(idx).browser.close();
                screens.get(idx).browser = null;
            }
        } else
            WebDisplays.NET_HANDLER.sendToAllAround(new CMessageScreenUpdate(this, side), point()); //Delete the screen

        screens.remove(idx);

        if(!world.isRemote) {
            if(screens.isEmpty()) //No more screens: remove tile entity
                world.setBlockState(pos, WebDisplays.INSTANCE.blockScreen.getDefaultState().withProperty(BlockScreen.hasTE, false));
            else
                markDirty();
        }
    }

    public void setResolution(BlockSide side, Vector2i res) {
        if(res.x < 1 || res.y < 1) {
            Log.warning("Call to TileEntityScreen.setResolution(%s) with suspicious values X=%d and Y=%d", side.toString(), res.x, res.y);
            return;
        }

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Tried to change resolution of non-existing screen on side %s", side.toString());
            return;
        }

        scr.resolution = res;
        scr.clampResolution();

        if(world.isRemote) {
            WebDisplays.PROXY.screenUpdateResolutionInGui(new Vector3i(pos), side, res);

            if(scr.browser != null) {
                scr.browser.close();
                scr.browser = null; //Will be re-created by renderer
            }
        } else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.setResolution(this, side, res), point());
            markDirty();
        }
    }

    private static EntityPlayer getLaserUser(Screen scr) {
        if(scr.laserUser != null) {
            if(scr.laserUser.isDead || scr.laserUser.getHeldItem(EnumHand.MAIN_HAND).getItem() != WebDisplays.INSTANCE.itemLaserPointer)
                scr.laserUser = null;
        }

        return scr.laserUser;
    }

    private static void checkLaserUserRights(Screen scr) {
        if(scr.laserUser != null && (scr.rightsFor(scr.laserUser) & ScreenRights.CLICK) == 0)
            scr.laserUser = null;
    }

    public void clearLaserUser(BlockSide side) {
        Screen scr = getScreen(side);

        if(scr != null)
            scr.laserUser = null;
    }

    public void click(BlockSide side, Vector2i vec) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Attempt click non-existing screen of side %s", side.toString());
            return;
        }

        if(world.isRemote)
            Log.warning("TileEntityScreen.click() from client side is useless...");
        else if(getLaserUser(scr) == null)
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, CMessageScreenUpdate.MOUSE_CLICK, vec), point());
    }

    void clickUnsafe(BlockSide side, int action, int x, int y) {
        if(world.isRemote) {
            Vector2i vec = (action == CMessageScreenUpdate.MOUSE_UP) ? null : new Vector2i(x, y);
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, action, vec), point());
        }
    }

    public void handleMouseEvent(BlockSide side, int event, @Nullable Vector2i vec) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Attempt inject mouse events on non-existing screen of side %s", side.toString());
            return;
        }

        if(scr.browser != null) {
            if(event == CMessageScreenUpdate.MOUSE_CLICK) {
                scr.browser.injectMouseMove(vec.x, vec.y, 0, false);                                            //Move to target
                scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, true, 1);                              //Press
                scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, false, 1);                             //Release
            } else if(event == CMessageScreenUpdate.MOUSE_DOWN) {
                scr.browser.injectMouseMove(vec.x, vec.y, 0, false);                                            //Move to target
                scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, true, 1);                              //Press
            } else if(event == CMessageScreenUpdate.MOUSE_MOVE)
                scr.browser.injectMouseMove(vec.x, vec.y, 0, false);                                            //Move
            else if(event == CMessageScreenUpdate.MOUSE_UP)
                scr.browser.injectMouseButton(scr.lastMousePos.x, scr.lastMousePos.y, 0, 1, false, 1);  //Release

            if(vec != null) {
                scr.lastMousePos.x = vec.x;
                scr.lastMousePos.y = vec.y;
            }
        }
    }

    public void updateJSRedstone(BlockSide side, Vector2i vec, int redstoneLevel) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Called updateJSRedstone on non-existing side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            if(scr.browser != null)
                scr.browser.runJS("if(typeof webdisplaysRedstoneCallback == \"function\") webdisplaysRedstoneCallback(" + vec.x + ", " + vec.y + ", " + redstoneLevel + ");", "");
        } else {
            boolean sendMsg = false;

            if(scr.redstoneStatus == null) {
                scr.setupRedstoneStatus(world, pos);
                sendMsg = true;
            } else {
                int idx = vec.y * scr.size.x + vec.x;

                if(scr.redstoneStatus.get(idx) != redstoneLevel) {
                    scr.redstoneStatus.set(idx, redstoneLevel);
                    sendMsg = true;
                }
            }

            if(sendMsg)
                WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.jsRedstone(this, side, vec, redstoneLevel), point());
        }
    }

    public void handleJSRequest(EntityPlayerMP src, BlockSide side, int reqId, JSServerRequest req, Object[] data) {
        if(world.isRemote) {
            Log.error("Called handleJSRequest client-side");
            return;
        }

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Called handleJSRequest on non-existing side %s", side.toString());
            WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, 403, "Invalid side"), src);
            return;
        }

        if(!scr.owner.uuid.equals(src.getGameProfile().getId())) {
            Log.warning("Player %s (UUID %s) tries to use the redstone output API on a screen he doesn't own!", src.getName(), src.getGameProfile().getId().toString());
            WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, 403, "Only the owner can do that"), src);
            return;
        }

        if(scr.upgrades.stream().noneMatch(DefaultUpgrade.REDSTONE_OUTPUT::matches)) {
            WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, 403, "Missing upgrade"), src);
            return;
        }

        if(req == JSServerRequest.CLEAR_REDSTONE) {
            final BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
            final Vector3i vec1 = new Vector3i(pos);
            final Vector3i vec2 = new Vector3i();

            for(int y = 0; y < scr.size.y; y++) {
                vec2.set(vec1);

                for(int x = 0; x < scr.size.x; x++) {
                    vec2.toBlock(mbp);

                    IBlockState bs = world.getBlockState(mbp);
                    if(bs.getValue(BlockScreen.emitting))
                        world.setBlockState(mbp, bs.withProperty(BlockScreen.emitting, false));

                    vec2.add(side.right.x, side.right.y, side.right.z);
                }

                vec1.add(side.up.x, side.up.y, side.up.z);
            }

            WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, new byte[0]), src);
        } else if(req == JSServerRequest.SET_REDSTONE_AT) {
            int x = (Integer) data[0];
            int y = (Integer) data[1];
            boolean state = (Boolean) data[2];

            if(x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y)
                WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, 403, "Out of range"), src);
            else {
                BlockPos bp = (new Vector3i(pos)).addMul(side.right, x).addMul(side.up, y).toBlock();
                IBlockState bs = world.getBlockState(bp);

                if(bs.getValue(BlockScreen.emitting) != state)
                    world.setBlockState(bp, bs.withProperty(BlockScreen.emitting, state));

                WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, new byte[0]), src);
            }
        } else
            WebDisplays.NET_HANDLER.sendTo(new CMessageJSResponse(reqId, req, 400, "Invalid request"), src);
    }

    @Override
    public void onLoad() {
        if(world.isRemote) {
            WebDisplays.NET_HANDLER.sendToServer(new SMessageRequestTEData(this));
            WebDisplays.PROXY.trackScreen(this, true);
        }
    }

    @Override
    public void onChunkUnload() {
        if(world.isRemote) {
            WebDisplays.PROXY.trackScreen(this, false);

            for(Screen scr: screens) {
                if(scr.browser != null) {
                    scr.browser.close();
                    scr.browser = null;
                }
            }
        }
    }

    private void updateAABB() {
        Vector3i origin = new Vector3i(pos);
        Vector3i tmp = new Vector3i();
        AABB aabb = new AABB(origin);

        for(Screen scr: screens) {
            tmp.set(origin);
            tmp.addMul(scr.side.right, scr.size.x);
            tmp.addMul(scr.side.up, scr.size.y);
            tmp.add(scr.side.forward);

            aabb.expand(tmp);
        }

        renderBB = aabb.toMc().expand(0.1, 0.1, 0.1);
    }

    @Override
    @Nonnull
    public AxisAlignedBB getRenderBoundingBox() {
        return renderBB;
    }

    //FIXME: Not called if enableSoundDistance is false
    public void updateTrackDistance(double d, float masterVolume) {
        final WebDisplays wd = WebDisplays.INSTANCE;
        boolean needsComputation = true;
        int intPart = 0; //Need to initialize those because the compiler is stupid
        int fracPart = 0;

        for(Screen scr: screens) {
            if(scr.autoVolume && scr.videoType != null && scr.browser != null && !scr.browser.isPageLoading()) {
                if(needsComputation) {
                    float dist = (float) Math.sqrt(d);
                    float vol;

                    if(dist <= wd.avDist100)
                        vol = masterVolume * wd.ytVolume;
                    else if(dist >= wd.avDist0)
                        vol = 0.0f;
                    else
                        vol = (1.0f - (dist - wd.avDist100) / (wd.avDist0 - wd.avDist100)) * masterVolume * wd.ytVolume;

                    if(Math.abs(ytVolume - vol) < 0.5f)
                        return; //Delta is too small

                    ytVolume = vol;
                    intPart = (int) vol; //Manually convert to string, probably faster in that case...
                    fracPart = ((int) (vol * 100.0f)) - intPart * 100;
                    needsComputation = false;
                }

                scr.browser.runJS(scr.videoType.getVolumeJSQuery(intPart, fracPart), "");
            }
        }
    }

    public void updateClientSideURL(IBrowser target, String url) {
        for(Screen scr: screens) {
            if(scr.browser == target) {
                boolean blacklisted = WebDisplays.isSiteBlacklisted(url);
                scr.url = blacklisted ? WebDisplays.BLACKLIST_URL : url; //FIXME: This is an invalid fix for something that CANNOT be fixed
                scr.videoType = VideoType.getTypeFromURL(scr.url);
                ytVolume = Float.POSITIVE_INFINITY; //Force volume update

                if(blacklisted && scr.browser != null)
                    scr.browser.loadURL(WebDisplays.BLACKLIST_URL);

                break;
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if(world.isRemote)
            onChunkUnload();
    }

    public void addFriend(EntityPlayerMP ply, BlockSide side, NameUUIDPair pair) {
        if(!world.isRemote) {
            Screen scr = getScreen(side);
            if(scr == null) {
                Log.error("Tried to add friend to invalid screen side %s", side.toString());
                return;
            }

            if(!scr.friends.contains(pair)) {
                scr.friends.add(pair);
                (new ScreenConfigData(new Vector3i(pos), side, scr)).updateOnly().sendTo(point());
                markDirty();
            }
        }
    }

    public void removeFriend(EntityPlayerMP ply, BlockSide side, NameUUIDPair pair) {
        if(!world.isRemote) {
            Screen scr = getScreen(side);
            if(scr == null) {
                Log.error("Tried to remove friend from invalid screen side %s", side.toString());
                return;
            }

            if(scr.friends.remove(pair)) {
                checkLaserUserRights(scr);
                (new ScreenConfigData(new Vector3i(pos), side, scr)).updateOnly().sendTo(point());
                markDirty();
            }
        }
    }

    public void setRights(EntityPlayerMP ply, BlockSide side, int fr, int or) {
        if(!world.isRemote) {
            Screen scr = getScreen(side);
            if(scr == null) {
                Log.error("Tried to change rights of invalid screen on side %s", side.toString());
                return;
            }

            scr.friendRights = fr;
            scr.otherRights = or;

            checkLaserUserRights(scr);
            (new ScreenConfigData(new Vector3i(pos), side, scr)).updateOnly().sendTo(point());
            markDirty();
        }
    }

    public void type(BlockSide side, String text, BlockPos soundPos) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Tried to type on invalid screen on side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            if(scr.browser != null) {
                try {
                    if(text.startsWith("t")) {
                        for(int i = 1; i < text.length(); i++) {
                            char chr = text.charAt(i);
                            if(chr == 1)
                                break;

                            scr.browser.injectKeyTyped(chr, 0);
                        }
                    } else {
                        TypeData[] data = WebDisplays.GSON.fromJson(text, TypeData[].class);

                        for(TypeData ev : data) {
                            switch(ev.getAction()) {
                                case PRESS:
                                    scr.browser.injectKeyPressedByKeyCode(ev.getKeyCode(), ev.getKeyChar(), 0);
                                    break;

                                case RELEASE:
                                    scr.browser.injectKeyReleasedByKeyCode(ev.getKeyCode(), ev.getKeyChar(), 0);
                                    break;

                                case TYPE:
                                    scr.browser.injectKeyTyped(ev.getKeyChar(), 0);
                                    break;

                                default:
                                    throw new RuntimeException("Invalid type action '" + ev.getAction() + '\'');
                            }
                        }
                    }
                } catch(Throwable t) {
                    Log.warningEx("Suspicious keyboard type packet received...", t);
                }
            }
        } else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.type(this, side, text), point());

            if(soundPos != null)
                playSoundAt(WebDisplays.INSTANCE.soundTyping, soundPos, 0.25f, 1.f);
        }
    }

    private void playSoundAt(SoundEvent snd, BlockPos at, float vol, float pitch) {
        double x = (double) at.getX();
        double y = (double) at.getY();
        double z = (double) at.getZ();

        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, snd, SoundCategory.BLOCKS, vol, pitch);
    }

    public void updateUpgrades(BlockSide side, ItemStack[] upgrades) {
        if(!world.isRemote) {
            Log.error("Tried to call TileEntityScreen.updateUpgrades() from server side...");
            return;
        }

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Tried to update upgrades on invalid screen on side %s", side.toString());
            return;
        }

        scr.upgrades.clear();
        Collections.addAll(scr.upgrades, upgrades);

        if(scr.browser != null)
            scr.browser.runJS("if(typeof webdisplaysUpgradesChanged == \"function\") webdisplaysUpgradesChanged();", "");
    }

    private static String safeName(ItemStack is) {
        ResourceLocation rl = is.getItem().getRegistryName();
        return (rl == null) ? "[NO NAME, WTF?!]" : rl.toString();
    }

    //If equal is null, no duplicate check is preformed
    public boolean addUpgrade(BlockSide side, ItemStack is, @Nullable EntityPlayer player, boolean abortIfExisting) {
        if(world.isRemote)
            return false;

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Tried to add an upgrade on invalid screen on side %s", side.toString());
            return false;
        }

        if(!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to add a non-upgrade item %s to screen (%s does not implement IUpgrade)", safeName(is), is.getItem().getClass().getCanonicalName());
            return false;
        }

        if(scr.upgrades.size() >= 16) {
            Log.error("Can't insert upgrade %s in screen %s at %s: too many upgrades already!", safeName(is), side.toString(), pos.toString());
            return false;
        }

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
        if(abortIfExisting && scr.upgrades.stream().anyMatch(otherStack -> itemAsUpgrade.isSameUpgrade(is, otherStack)))
            return false; //Upgrade already exists

        ItemStack isCopy = is.copy(); //FIXME: Duct tape fix, because the original stack will be shrinked
        isCopy.setCount(1);

        scr.upgrades.add(isCopy);
        WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.upgrade(this, side), point());
        itemAsUpgrade.onInstall(this, side, player, isCopy);
        playSoundAt(WebDisplays.INSTANCE.soundUpgradeAdd, pos, 1.0f, 1.0f);
        markDirty();
        return true;
    }

    public boolean hasUpgrade(BlockSide side, ItemStack is) {
        Screen scr = getScreen(side);
        if(scr == null)
            return false;

        if(!(is.getItem() instanceof IUpgrade))
            return false;

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
        return scr.upgrades.stream().anyMatch(otherStack -> itemAsUpgrade.isSameUpgrade(is, otherStack));
    }

    public boolean hasUpgrade(BlockSide side, DefaultUpgrade du) {
        Screen scr = getScreen(side);
        return scr != null && scr.upgrades.stream().anyMatch(du::matches);
    }

    public void removeUpgrade(BlockSide side, ItemStack is, @Nullable EntityPlayer player) {
        if(world.isRemote)
            return;

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Tried to remove an upgrade on invalid screen on side %s", side.toString());
            return;
        }

        if(!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to remove a non-upgrade item %s to screen (%s does not implement IUpgrade)", safeName(is), is.getItem().getClass().getCanonicalName());
            return;
        }

        int idxToRemove = -1;
        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();

        for(int i = 0; i < scr.upgrades.size(); i++) {
            if(itemAsUpgrade.isSameUpgrade(is, scr.upgrades.get(i))) {
                idxToRemove = i;
                break;
            }
        }

        if(idxToRemove >= 0) {
            dropUpgrade(scr.upgrades.get(idxToRemove), side, player);
            scr.upgrades.remove(idxToRemove);
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.upgrade(this, side), point());
            playSoundAt(WebDisplays.INSTANCE.soundUpgradeDel, pos, 1.0f, 1.0f);
            markDirty();
        } else
            Log.warning("Tried to remove non-existing upgrade %s to screen %s at %s", safeName(is), side.toString(), pos.toString());
    }

    private void dropUpgrade(ItemStack is, BlockSide side, @Nullable EntityPlayer ply) {
        if(!((IUpgrade) is.getItem()).onRemove(this, side, ply, is)) { //Drop upgrade item
            boolean spawnDrop = true;

            if(ply != null) {
                if(ply.isCreative() || ply.addItemStackToInventory(is))
                    spawnDrop = false; //If in creative or if the item was added to the player's inventory, don't spawn drop entity
            }

            if(spawnDrop) {
                Vector3f pos = new Vector3f((float) this.pos.getX(), (float) this.pos.getY(), (float) this.pos.getZ());
                pos.addMul(side.backward.toFloat(), 1.5f);

                world.spawnEntity(new EntityItem(world, (double) pos.x, (double) pos.y, (double) pos.z, is));
            }
        }
    }

    private Screen getScreenForLaserOp(BlockSide side, EntityPlayer ply) {
        if(world.isRemote)
            return null;

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Called laser operation on invalid screen on side %s", side.toString());
            return null;
        }

        if((scr.rightsFor(ply) & ScreenRights.CLICK) == 0)
            return null; //Don't output an error, it can 'legally' happen

        if(scr.upgrades.stream().noneMatch(DefaultUpgrade.LASER_MOUSE::matches)) {
            Log.error("Called laser operation on side %s, but it's missing the laser sensor upgrade", side.toString());
            return null;
        }

        return scr; //Okay, go for it...
    }

    public void laserDownMove(BlockSide side, EntityPlayer ply, Vector2i pos, boolean down) {
        Screen scr = getScreenForLaserOp(side, ply);

        if(scr != null) {
            if(down) {
                //Try to acquire laser lock
                if(getLaserUser(scr) == null) {
                    scr.laserUser = ply;
                    WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, CMessageScreenUpdate.MOUSE_DOWN, pos), point());
                }
            } else if(getLaserUser(scr) == ply)
                WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, CMessageScreenUpdate.MOUSE_MOVE, pos), point());
        }
    }

    public void laserUp(BlockSide side, EntityPlayer ply) {
        Screen scr = getScreenForLaserOp(side, ply);

        if(scr != null) {
            if(getLaserUser(scr) == ply) {
                scr.laserUser = null;
                WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, CMessageScreenUpdate.MOUSE_UP, null), point());
            }
        }
    }

    public void onDestroy(@Nullable EntityPlayer ply) {
        for(Screen scr: screens) {
            scr.upgrades.forEach(is -> dropUpgrade(is, scr.side, ply));
            scr.upgrades.clear();
        }

        WebDisplays.NET_HANDLER.sendToAllAround(new CMessageCloseGui(pos), point());
    }

    public void setOwner(BlockSide side, EntityPlayer newOwner) {
        if(world.isRemote) {
            Log.error("Called TileEntityScreen.setOwner() on client...");
            return;
        }

        if(newOwner == null) {
            Log.error("Called TileEntityScreen.setOwner() with null owner");
            return;
        }

        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Called TileEntityScreen.setOwner() on invalid screen on side %s", side.toString());
            return;
        }

        scr.owner = new NameUUIDPair(newOwner.getGameProfile());
        WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.owner(this, side, scr.owner), point());
        checkLaserUserRights(scr);
        markDirty();
    }

    public void setRotation(BlockSide side, Rotation rot) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Trying to change rotation of invalid screen on side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            boolean oldWasVertical = scr.rotation.isVertical;
            scr.rotation = rot;

            WebDisplays.PROXY.screenUpdateRotationInGui(new Vector3i(pos), side, rot);

            if(scr.browser != null && oldWasVertical != rot.isVertical) {
                scr.browser.close();
                scr.browser = null; //Will be re-created by renderer
            }
        } else {
            scr.rotation = rot;
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.rotation(this, side, rot), point());
            markDirty();
        }
    }

    public void evalJS(BlockSide side, String code) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Trying to run JS code on invalid screen on side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            if(scr.browser != null)
                scr.browser.runJS(code, "");
        } else
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.js(this, side, code), point());
    }

    public void setAutoVolume(BlockSide side, boolean av) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Trying to toggle auto-volume on invalid screen (side %s)", side.toString());
            return;
        }

        scr.autoVolume = av;

        if(world.isRemote)
            WebDisplays.PROXY.screenUpdateAutoVolumeInGui(new Vector3i(pos), side, av);
        else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.autoVolume(this, side, av), point());
            markDirty();
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState) {
        if(oldState.getBlock() != WebDisplays.INSTANCE.blockScreen || newState.getBlock() != WebDisplays.INSTANCE.blockScreen)
            return true;

        return oldState.getValue(BlockScreen.hasTE) != newState.getValue(BlockScreen.hasTE);
    }

}
