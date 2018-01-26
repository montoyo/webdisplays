/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.net.CMessageAddScreen;
import net.montoyo.wd.net.CMessageScreenUpdate;
import net.montoyo.wd.net.SMessageRequestTEData;
import net.montoyo.wd.utilities.*;

import java.util.ArrayList;
import java.util.UUID;

public class TileEntityScreen extends TileEntity {

    public static class Screen {

        private static final String YT_REGEX1 = "^https?\\://(?:www\\.)?youtube\\.com/watch.+$"; //TODO: Fix embedded videos sound/distance
        private static final String YT_REGEX2 = "^https?\\://(?:www\\.)?youtu\\.be/[a-zA-Z0-9_\\-]+.*$";

        public BlockSide side;
        public Vector2i size;
        public Vector2i resolution;
        public String url;
        public boolean isYouTube = false;
        public NameUUIDPair owner;
        public ArrayList<NameUUIDPair> friends;
        public int friendRights;
        public int otherRights;
        public IBrowser browser;

        public static boolean isYouTubeURL(String url) {
            return url.matches(YT_REGEX1) || url.matches(YT_REGEX2);
        }

        public static Screen deserialize(NBTTagCompound tag) {
            Screen ret = new Screen();
            ret.side = BlockSide.values()[tag.getByte("Side")];
            ret.size = new Vector2i(tag.getInteger("Width"), tag.getInteger("Height"));
            ret.resolution = new Vector2i(tag.getInteger("ResolutionX"), tag.getInteger("ResolutionY"));
            ret.url = tag.getString("URL");
            ret.isYouTube = isYouTubeURL(ret.url);

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
            return ret;
        }

        public NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Side", (byte) side.ordinal());
            tag.setInteger("Width", size.x);
            tag.setInteger("Height", size.y);
            tag.setInteger("ResolutionX", resolution.x);
            tag.setInteger("ResolutionY", resolution.y);
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
            return tag;
        }

        public void setOwner(EntityPlayer ply) {
            owner = new NameUUIDPair(ply.getGameProfile());
        }

        public int rightsFor(EntityPlayer ply) {
            UUID uuid = ply.getGameProfile().getId();
            if(owner.uuid.equals(uuid))
                return ScreenRights.ALL;

            for(NameUUIDPair f: friends) {
                if(f.uuid.equals(uuid))
                    return friendRights;
            }

            return otherRights;
        }

    }

    private ArrayList<Screen> screens = new ArrayList<>();
    private AxisAlignedBB renderBB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private boolean loaded = true;
    public float ytVolume = 100.0f;

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
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        NBTTagList list = new NBTTagList();
        for(Screen scr: screens)
            list.appendTag(scr.serialize());

        tag.setTag("WDScreens", list);
        return tag;
    }

    public NetworkRegistry.TargetPoint point() {
        return new NetworkRegistry.TargetPoint(world.provider.getDimension(), (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 64.0);
    }

    public Screen addScreen(BlockSide side, Vector2i size, Vector2i resolution, boolean sendUpdate) {
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

        if(resolution == null || resolution.x < 1 || resolution.y < 1) {
            float psx = ((float) size.x) * 16.f - 4.f;
            float psy = ((float) size.y) * 16.f - 4.f;
            psx *= 8.f; //TODO: Use ratio in config file
            psy *= 8.f;

            ret.resolution = new Vector2i((int) psx, (int) psy);
        } else
            ret.resolution = resolution;

        if(sendUpdate && !world.isRemote) {
            CMessageAddScreen msg = new CMessageAddScreen(this, ret);
            WebDisplays.NET_HANDLER.sendToAllAround(msg, point());
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

        scr.url = url;
        scr.isYouTube = Screen.isYouTubeURL(url);

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
                world.setBlockState(getPos(), WebDisplays.INSTANCE.blockScreen.getDefaultState().withProperty(BlockScreen.hasTE, false));
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

        if(world.isRemote) {
            WebDisplays.PROXY.screenUpdateResolutionInGui(new Vector3i(getPos()), side, res);

            if(scr.browser != null) {
                scr.browser.close();
                scr.browser = null; //Will be re-created by renderer
            }
        } else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.setResolution(this, side, res), point());
            markDirty();
        }
    }

    public void click(BlockSide side, Vector2i vec) {
        Screen scr = getScreen(side);
        if(scr == null) {
            Log.error("Attempt click non-existing screen of side %s", side.toString());
            return;
        }

        if(world.isRemote) {
            if(scr.browser != null) {
                scr.browser.injectMouseMove(vec.x, vec.y, 0, false);
                scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, true, 1);
                scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, false, 1);
            }
        } else
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.click(this, side, vec), point());
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
    public AxisAlignedBB getRenderBoundingBox() {
        return renderBB;
    }

    public void updateTrackDistance(double d) {
        boolean needsComputation = true;
        float vol = -1.f;
        String jsCode = null;

        for(Screen scr: screens) {
            if(scr.isYouTube && scr.browser != null && !scr.browser.isPageLoading()) {
                if(needsComputation) {
                    float dist = (float) Math.sqrt(d);
                    if(dist <= 10.f)
                        vol = 100.f;
                    else if(dist >= 30.f)
                        vol = 0.f;
                    else
                        vol = (1.f - (dist - 10.f) / 20.f) * 100.f;

                    if(Math.abs(ytVolume - vol) < 0.5f)
                        return; //Delta is too small

                    ytVolume = vol;
                    int intPart = (int) vol; //Manually convert to string, probably faster in that case...
                    int fracPart = ((int) (vol * 100.f)) - intPart * 100;
                    //jsCode = "yt.player.getPlayerByElement(document.getElementById(\"movie_player\")).setVolume(" + intPart + '.' + fracPart + ')';
                    //jsCode = "console.log(document.getElementById(\"movie_player\"))";
                    jsCode = "document.getElementById(\"movie_player\").setVolume(" + intPart + '.' + fracPart + ')';
                    //Log.info(jsCode);
                    needsComputation = false;
                }

                scr.browser.runJS(jsCode, "");
            }
        }
    }

    /*@Override
    public void validate() {
        super.validate();

        if(world.isRemote)
            Log.info("===> TES(  VALIDATE) %s", getPos().toString());
    }*/

    @Override
    public void invalidate() {
        super.invalidate();

        if(world.isRemote) {
            Log.info("===> TES(INVALIDATE) %s", getPos().toString());
            onChunkUnload();
        }
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
                    String[] events = text.split("" + ((char) 1));

                    for(String ev: events) {
                        char action = ev.charAt(0);

                        if(action == 'p')
                            scr.browser.injectKeyPressed(ev.charAt(1), 0);
                        else if(action == 'r')
                            scr.browser.injectKeyReleased(ev.charAt(1), 0);
                        else if(action == 't') {
                            for(int i = 1; i < ev.length(); i++)
                                scr.browser.injectKeyTyped(ev.charAt(i), 0);
                        } else
                            throw new RuntimeException("Invalid control key '" + action + '\'');
                    }
                } catch(Throwable t) {
                    Log.warningEx("Suspicious keyboard type packet received...", t);
                }
            }
        } else {
            WebDisplays.NET_HANDLER.sendToAllAround(CMessageScreenUpdate.type(this, side, text), point());

            if(soundPos != null) {
                double x = (double) soundPos.getX();
                double y = (double) soundPos.getY();
                double z = (double) soundPos.getZ();

                world.playSound(null, x + 0.5, y + 0.5, z + 0.5, WebDisplays.INSTANCE.soundTyping, SoundCategory.BLOCKS, 0.25f, 1.f);
            }
        }
    }

}
