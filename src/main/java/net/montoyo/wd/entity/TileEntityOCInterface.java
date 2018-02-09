/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Optional;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.net.client.CMessageScreenUpdate;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Rotation;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector2i;

import javax.annotation.Nonnull;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityOCInterface extends TileEntityPeripheralBase implements SimpleComponent {

    private NameUUIDPair owner;
    private static final Object[] TRUE = new Object[] { true };
    private static final Object[] FALSE = new Object[] { false };

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        owner = Util.readOwnerFromNBT(tag);
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        return Util.writeOwnerToNBT(tag, owner);
    }

    public void setOwner(EntityPlayer ep) {
        owner = new NameUUIDPair(ep.getGameProfile());
        markDirty();
    }

    @Override
    public String getComponentName() {
        return "webdisplays";
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] isLinked(Context ctx, Arguments args) {
        return new Object[] { isLinked() };
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] isScreenChunkLoaded(Context ctx, Arguments args) {
        return new Object[] { isScreenChunkLoaded() };
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getScreenPos(Context ctx, Arguments args) {
        return isLinked() ? new Object[] { screenPos.x, screenPos.y, screenPos.z } : null;
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getScreenSide(Context ctx, Arguments args) {
        return isLinked() ? new Object[] { screenSide.toString().toLowerCase() } : null;
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getOwner(Context ctx, Arguments args) {
        if(owner == null)
            return null;
        else
            return new Object[] { owner.name, owner.uuid.toString() };
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] can(Context ctx, Arguments args) {
        String what = args.checkString(0).toLowerCase();
        int right;
        switch(what) {
            case "click":
            case "type":
            case "js":
            case "javascript":
            case "runjs":
                right = ScreenRights.CLICK;
                break;

            case "seturl":
                right = ScreenRights.CHANGE_URL;
                break;

            case "setresolution":
            case "setrotation":
                right = ScreenRights.CHANGE_RESOLUTION;
                break;

            default:
                throw new IllegalArgumentException("invalid right name");
        }

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else
            return ((getConnectedScreen().getScreen(screenSide).rightsFor(owner.uuid) & right) == 0) ? FALSE : TRUE;
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] hasUpgrade(Context ctx, Arguments args) {
        String name = args.checkString(0);

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else
            return getConnectedScreen().getScreen(screenSide).upgrades.stream().anyMatch(is -> ((IUpgrade) is.getItem()).getJSName(is).equalsIgnoreCase(name)) ? TRUE : FALSE;
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getSize(Context ctx, Arguments args) {
        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else {
            Vector2i sz = getConnectedScreen().getScreen(screenSide).size;
            return new Object[] { sz.x, sz.y };
        }
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getResolution(Context ctx, Arguments args) {
        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else {
            Vector2i res = getConnectedScreen().getScreen(screenSide).resolution;
            return new Object[] { res.x, res.y };
        }
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getRotation(Context ctx, Arguments args) {
        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else
            return new Object[] { getConnectedScreen().getScreen(screenSide).rotation.angle };
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getURL(Context ctx, Arguments args) {
        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return null;
        else
            return new Object[] { getConnectedScreen().getScreen(screenSide).url };
    }

    private static Object[] err(String str) {
        return new Object[] { false, str };
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] click(Context ctx, Arguments args) {
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        String action = "click";
        if(args.count() > 2)
            action = args.checkString(2).toLowerCase();

        int actionId;
        switch(action) {
            case "click":
                actionId = CMessageScreenUpdate.MOUSE_CLICK;
                break;

            case "up":
            case "release":
                actionId = CMessageScreenUpdate.MOUSE_UP;
                break;

            case "down":
            case "press":
                actionId = CMessageScreenUpdate.MOUSE_DOWN;
                break;

            case "move":
                actionId = CMessageScreenUpdate.MOUSE_MOVE;
                break;

            default:
                throw new IllegalArgumentException("bad action name");
        }

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();
            TileEntityScreen.Screen scrscr = scr.getScreen(screenSide);

            if((scrscr.rightsFor(owner.uuid) & ScreenRights.CLICK) == 0)
                return err("restrictions");
            else {
                switch(scrscr.rotation) {
                    case ROT_90:
                        y = scrscr.resolution.y - y;
                        break;

                    case ROT_180:
                        x = scrscr.resolution.x - x;
                        y = scrscr.resolution.y - y;
                        break;

                    case ROT_270:
                        x = scrscr.resolution.x - x;
                        break;

                    default:
                        break;
                }

                if(scrscr.rotation.isVertical)
                    scr.clickUnsafe(screenSide, actionId, y, x);
                else
                    scr.clickUnsafe(screenSide, actionId, x, y);

                return TRUE;
            }
        }
    }

    private Object[] realType(String what) {
        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();

            if((scr.getScreen(screenSide).rightsFor(owner.uuid) & ScreenRights.CLICK) == 0)
                return err("restrictions");
            else {
                scr.type(screenSide, what, null);
                return TRUE;
            }
        }
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] type(Context ctx, Arguments args) {
        String text = args.checkString(0);
        if(text.length() > 64)
            return err("toolong");

        if(text.indexOf((char) 1) >= 0)
            return err("badchar");

        return realType("t" + text);
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] typeAdvanced(Context ctx, Arguments args) {
        String text = args.checkString(0);
        if(text.length() > 64)
            return err("toolong");

        String[] ctrl = text.split("" + ((char) 1));
        for(String c: ctrl) {
            if(c.length() < 2)
                return err("badformat");

            if(c.charAt(0) != 't' && c.charAt(0) != 'p' && c.charAt(0) != 'r')
                return err("badformat");
        }

        return realType(text);
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setURL(Context ctx, Arguments args) {
        String url = args.checkString(0);

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();

            if((scr.getScreen(screenSide).rightsFor(owner.uuid) & ScreenRights.CHANGE_URL) == 0)
                return err("restrictions");
            else {
                scr.setScreenURL(screenSide, url);
                return TRUE;
            }
        }
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setResolution(Context ctx, Arguments args) {
        int rx = args.checkInteger(0);
        int ry = args.checkInteger(1);

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();

            if((scr.getScreen(screenSide).rightsFor(owner.uuid) & ScreenRights.CHANGE_RESOLUTION) == 0)
                return err("restrictions");
            else {
                scr.setResolution(screenSide, new Vector2i(rx, ry));
                return TRUE;
            }
        }
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setRotation(Context ctx, Arguments args) {
        int rot = args.checkInteger(0);
        if(rot < 0) {
            int toAdd = (rot / -360) + 1;
            rot += toAdd * 360;
        }

        rot /= 90;
        rot &= 3;

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();

            if((scr.getScreen(screenSide).rightsFor(owner.uuid) & ScreenRights.CHANGE_RESOLUTION) == 0)
                return err("restrictions");
            else {
                scr.setRotation(screenSide, Rotation.values()[rot]);
                return TRUE;
            }
        }
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] runJS(Context ctx, Arguments args) {
        String code = args.checkString(0);

        if(owner == null || !isLinked() || !isScreenChunkLoaded())
            return err("notlinked");
        else {
            TileEntityScreen scr = getConnectedScreen();

            if((scr.getScreen(screenSide).rightsFor(owner.uuid) & ScreenRights.CHANGE_URL) == 0)
                return err("restrictions");
            else {
                scr.evalJS(screenSide, code);
                return TRUE;
            }
        }
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] unlink(Context ctx, Arguments args) {
        if(isLinked()) {
            screenPos = null;
            screenSide = null;
            markDirty();
        }

        return null;
    }



}
