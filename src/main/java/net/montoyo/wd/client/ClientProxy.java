/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.montoyo.mcef.api.*;
import net.montoyo.wd.SharedProxy;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.client.gui.*;
import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.renderers.*;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.HasAdvancement;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.data.GuiData;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.item.ItemMulti;
import net.montoyo.wd.item.WDItem;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.net.server.SMessagePadCtrl;
import net.montoyo.wd.net.server.SMessageScreenCtrl;
import net.montoyo.wd.utilities.*;
import org.lwjgl.input.Keyboard;
import paulscode.sound.SoundSystemConfig;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.function.Predicate;

public class ClientProxy extends SharedProxy implements ISelectiveResourceReloadListener, IDisplayHandler, IJSQueryHandler {

    public class PadData {

        public IBrowser view;
        private boolean isInHotbar;
        private final int id;
        private long lastURLSent;

        private PadData(String url, int id) {
            view = mcef.createBrowser(WebDisplays.applyBlacklist(url));
            view.resize((int) WebDisplays.INSTANCE.padResX, (int) WebDisplays.INSTANCE.padResY);
            isInHotbar = true;
            this.id = id;
        }

    }

    private Minecraft mc;
    private final ArrayList<ResourceModelPair> modelBakers = new ArrayList<>();
    private net.montoyo.mcef.api.API mcef;
    private MinePadRenderer minePadRenderer;
    private JSQueryDispatcher jsDispatcher;
    private LaserPointerRenderer laserPointerRenderer;
    private GuiScreen nextScreen;
    private boolean isF1Down;

    //Miniserv handling
    private int miniservPort;
    private boolean msClientStarted;

    //Client-side advancement hack
    private final Field advancementToProgressField = findAdvancementToProgressField();
    private ClientAdvancementManager lastAdvMgr;
    private Map advancementToProgress;

    //Laser pointer
    private TileEntityScreen pointedScreen;
    private BlockSide pointedScreenSide;
    private long lastPointPacket;

    //Tracking
    private final ArrayList<TileEntityScreen> screenTracking = new ArrayList<>();
    private int lastTracked = 0;

    //MinePads Management
    private final HashMap<Integer, PadData> padMap = new HashMap<>();
    private final ArrayList<PadData> padList = new ArrayList<>();
    private int minePadTickCounter = 0;

    /**************************************** INHERITED METHODS ****************************************/

    @Override
    public void preInit() {
        mc = Minecraft.getMinecraft();
        MinecraftForge.EVENT_BUS.register(this);
        registerCustomBlockBaker(new ScreenBaker(), WebDisplays.INSTANCE.blockScreen);

        mcef = MCEFApi.getAPI();
        if(mcef != null)
            mcef.registerScheme("wd", WDScheme.class, true, false, false, true, true, false, false);
    }

    @Override
    public void init() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityScreen.class, new ScreenRenderer());
        jsDispatcher = new JSQueryDispatcher(this);
        minePadRenderer = new MinePadRenderer();
        laserPointerRenderer = new LaserPointerRenderer();
    }

    @Override
    public void postInit() {
        ((SimpleReloadableResourceManager) mc.getResourceManager()).registerReloadListener(this);

        if(mcef == null)
            throw new RuntimeException("MCEF is missing");

        mcef.registerDisplayHandler(this);
        mcef.registerJSQueryHandler(this);
        findAdvancementToProgressField();
    }

    @Override
    public World getWorld(int dim) {
        World ret = mc.world;
        if(dim == CURRENT_DIMENSION)
            return ret;

        if(ret.provider.getDimension() != dim)
            throw new RuntimeException("Can't get non-current dimension " + dim + " from client.");

        return ret;
    }

    @Override
    public void enqueue(Runnable r) {
        mc.addScheduledTask(r);
    }

    @Override
    public void displayGui(GuiData data) {
        GuiScreen gui = data.createGui(mc.currentScreen, mc.world);
        if(gui != null)
            mc.displayGuiScreen(gui);
    }

    @Override
    public void trackScreen(TileEntityScreen tes, boolean track) {
        int idx = -1;
        for(int i = 0; i < screenTracking.size(); i++) {
            if(screenTracking.get(i) == tes) {
                idx = i;
                break;
            }
        }

        if(track) {
            if(idx < 0)
                screenTracking.add(tes);
        } else if(idx >= 0)
            screenTracking.remove(idx);
    }

    @Override
    public void onAutocompleteResult(NameUUIDPair[] pairs) {
        if(mc.currentScreen != null && mc.currentScreen instanceof WDScreen) {
            if(pairs.length == 0)
                ((WDScreen) mc.currentScreen).onAutocompleteFailure();
            else
                ((WDScreen) mc.currentScreen).onAutocompleteResult(pairs);
        }
    }

    @Override
    public GameProfile[] getOnlineGameProfiles() {
        return new GameProfile[] { mc.player.getGameProfile() };
    }

    @Override
    public void screenUpdateResolutionInGui(Vector3i pos, BlockSide side, Vector2i res) {
        if(mc.currentScreen != null && mc.currentScreen instanceof GuiScreenConfig) {
            GuiScreenConfig gsc = (GuiScreenConfig) mc.currentScreen;

            if(gsc.isForBlock(pos.toBlock(), side))
                gsc.updateResolution(res);
        }
    }

    @Override
    public void screenUpdateRotationInGui(Vector3i pos, BlockSide side, Rotation rot) {
        if(mc.currentScreen != null && mc.currentScreen instanceof GuiScreenConfig) {
            GuiScreenConfig gsc = (GuiScreenConfig) mc.currentScreen;

            if(gsc.isForBlock(pos.toBlock(), side))
                gsc.updateRotation(rot);
        }
    }

    @Override
    public void screenUpdateAutoVolumeInGui(Vector3i pos, BlockSide side, boolean av) {
        if(mc.currentScreen != null && mc.currentScreen instanceof GuiScreenConfig) {
            GuiScreenConfig gsc = (GuiScreenConfig) mc.currentScreen;

            if(gsc.isForBlock(pos.toBlock(), side))
                gsc.updateAutoVolume(av);
        }
    }

    @Override
    public void displaySetPadURLGui(String padURL) {
        mc.displayGuiScreen(new GuiSetURL2(padURL));
    }

    @Override
    public void openMinePadGui(int padId) {
        PadData pd = padMap.get(padId);

        if(pd != null && pd.view != null)
            mc.displayGuiScreen(new GuiMinePad(pd));
    }

    @Override
    @Nonnull
    public HasAdvancement hasClientPlayerAdvancement(@Nonnull ResourceLocation rl) {
        if(advancementToProgressField != null && mc.player != null && mc.player.connection != null) {
            ClientAdvancementManager cam = mc.player.connection.getAdvancementManager();
            Advancement adv = cam.getAdvancementList().getAdvancement(rl);

            if(adv == null)
                return HasAdvancement.DONT_KNOW;

            if(lastAdvMgr != cam) {
                lastAdvMgr = cam;

                try {
                    advancementToProgress = (Map) advancementToProgressField.get(cam);
                } catch(Throwable t) {
                    Log.warningEx("Could not get ClientAdvancementManager.advancementToProgress field", t);
                    advancementToProgress = null;
                    return HasAdvancement.DONT_KNOW;
                }
            }

            if(advancementToProgress == null)
                return HasAdvancement.DONT_KNOW;

            Object progress = advancementToProgress.get(adv);
            if(progress == null)
                return HasAdvancement.NO;

            if(!(progress instanceof AdvancementProgress)) {
                Log.warning("The ClientAdvancementManager.advancementToProgress map does not contain AdvancementProgress instances");
                advancementToProgress = null; //Invalidate this: it's wrong
                return HasAdvancement.DONT_KNOW;
            }

            return ((AdvancementProgress) progress).isDone() ? HasAdvancement.YES : HasAdvancement.NO;
        }

        return HasAdvancement.DONT_KNOW;
    }

    @Override
    public MinecraftServer getServer() {
        return mc.getIntegratedServer();
    }

    @Override
    public void handleJSResponseSuccess(int reqId, JSServerRequest type, byte[] data) {
        JSQueryDispatcher.ServerQuery q = jsDispatcher.fulfillQuery(reqId);

        if(q == null)
            Log.warning("Received success response for invalid query ID %d of type %s", reqId, type.toString());
        else {
            if(type == JSServerRequest.CLEAR_REDSTONE || type == JSServerRequest.SET_REDSTONE_AT)
                q.success("{\"status\":\"success\"}");
            else
                Log.warning("Received success response for query ID %d, but type is invalid", reqId);
        }
    }

    @Override
    public void handleJSResponseError(int reqId, JSServerRequest type, int errCode, String err) {
        JSQueryDispatcher.ServerQuery q = jsDispatcher.fulfillQuery(reqId);

        if(q == null)
            Log.warning("Received error response for invalid query ID %d of type %s", reqId, type.toString());
        else
            q.error(errCode, err);
    }

    @Override
    public void setMiniservClientPort(int port) {
        miniservPort = port;
    }

    @Override
    public void startMiniservClient() {
        if(miniservPort <= 0) {
            Log.warning("Can't start miniserv client: miniserv is disabled");
            return;
        }

        if(mc.player == null) {
            Log.warning("Can't start miniserv client: player is null");
            return;
        }

        SocketAddress saddr = mc.player.connection.getNetworkManager().channel().remoteAddress();
        if(saddr == null || !(saddr instanceof InetSocketAddress)) {
            Log.warning("Miniserv client: remote address is not inet, assuming local address");
            saddr = new InetSocketAddress("127.0.0.1", 1234);
        }

        InetSocketAddress msAddr = new InetSocketAddress(((InetSocketAddress) saddr).getAddress(), miniservPort);
        Client.getInstance().start(msAddr);
        msClientStarted = true;
    }

    @Override
    public boolean isMiniservDisabled() {
        return miniservPort <= 0;
    }

    @Override
    public void closeGui(BlockPos bp, BlockSide bs) {
        if(mc.currentScreen instanceof WDScreen) {
            WDScreen scr = (WDScreen) mc.currentScreen;

            if(scr.isForBlock(bp, bs))
                mc.displayGuiScreen(null);
        }
    }

    @Override
    public void renderRecipes() {
        nextScreen = new RenderRecipe();
    }

    @Override
    public boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    /**************************************** RESOURCE MANAGER METHODS ****************************************/

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        Log.info("Resource manager reload: clearing GUI cache...");
        GuiLoader.clearCache();
    }

    /**************************************** DISPLAY HANDLER METHODS ****************************************/

    @Override
    public void onAddressChange(IBrowser browser, String url) {
        if(browser != null) {
            long t = System.currentTimeMillis();

            for(PadData pd: padList) {
                if(pd.view == browser && t - pd.lastURLSent >= 1000) {
                    if(WebDisplays.isSiteBlacklisted(url))
                        pd.view.loadURL(WebDisplays.BLACKLIST_URL);
                    else {
                        pd.lastURLSent = t; //Avoid spamming the server with porn URLs
                        WebDisplays.NET_HANDLER.sendToServer(new SMessagePadCtrl(pd.id, url));
                    }

                    break;
                }
            }

            for(TileEntityScreen tes: screenTracking)
                tes.updateClientSideURL(browser, url);
        }
    }

    @Override
    public void onTitleChange(IBrowser browser, String title) {
    }

    @Override
    public void onTooltip(IBrowser browser, String text) {
    }

    @Override
    public void onStatusMessage(IBrowser browser, String value) {
    }

    /**************************************** JS HANDLER METHODS ****************************************/

    @Override
    public boolean handleQuery(IBrowser browser, long queryId, String query, boolean persistent, IJSQueryCallback cb) {
        if(browser != null && persistent && query != null && cb != null) {
            query = query.toLowerCase();

            if(query.startsWith("webdisplays_")) {
                query = query.substring(12);

                String args;
                int parenthesis = query.indexOf('(');
                if(parenthesis < 0)
                    args = null;
                else {
                    if(query.indexOf(')') != query.length() - 1) {
                        cb.failure(400, "Malformed request");
                        return true;
                    }

                    args = query.substring(parenthesis + 1, query.length() - 1);
                    query = query.substring(0, parenthesis);
                }

                if(jsDispatcher.canHandleQuery(query))
                    jsDispatcher.enqueueQuery(browser, query, args, cb);
                else
                    cb.failure(404, "Unknown WebDisplays query");

                return true;
            }
        }

        return false;
    }

    @Override
    public void cancelQuery(IBrowser browser, long queryId) {
    }

    /**************************************** EVENT METHODS ****************************************/

    @SubscribeEvent
    public void onStitchTextures(TextureStitchEvent.Pre ev) {
        TextureMap texMap = ev.getMap();

        if(texMap == mc.getTextureMapBlocks()) {
            for(ResourceModelPair pair : modelBakers)
                pair.getModel().loadTextures(texMap);
        }
    }

    @SubscribeEvent
    public void onBakeModel(ModelBakeEvent ev) {
        for(ResourceModelPair pair : modelBakers)
            ev.getModelRegistry().putObject(pair.getResourceLocation(), pair.getModel());
    }

    @SubscribeEvent
    public void onRegisterModels(ModelRegistryEvent ev) {
        final WebDisplays wd = WebDisplays.INSTANCE;

        //I hope I'm doing this right because it doesn't seem like it...
        registerItemModel(wd.blockScreen.getItem(), 0, "inventory");
        ModelLoader.setCustomModelResourceLocation(wd.blockPeripheral.getItem(), 0, new ModelResourceLocation("webdisplays:kb_inv", "normal"));
        registerItemModel(wd.blockPeripheral.getItem(), 1, "facing=2,type=ccinterface");
        registerItemModel(wd.blockPeripheral.getItem(), 2, "facing=2,type=cointerface");
        registerItemModel(wd.blockPeripheral.getItem(), 3, "facing=0,type=remotectrl");
        registerItemModel(wd.blockPeripheral.getItem(), 7, "facing=0,type=redstonectrl");
        registerItemModel(wd.blockPeripheral.getItem(), 11, "facing=0,type=server");
        registerItemModel(wd.itemScreenCfg, 0, "normal");
        registerItemModel(wd.itemOwnerThief, 0, "normal");
        registerItemModel(wd.itemLinker, 0, "normal");
        registerItemModel(wd.itemMinePad, 0, "normal");
        registerItemModel(wd.itemMinePad, 1, "normal");
        registerItemModel(wd.itemLaserPointer, 0, "normal");
        registerItemMultiModels(wd.itemUpgrade);
        registerItemMultiModels(wd.itemCraftComp);
        registerItemMultiModels(wd.itemAdvIcon);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent ev) {
        if(ev.phase == TickEvent.Phase.END) {
            //Help
            if(Keyboard.isKeyDown(Keyboard.KEY_F1)) {
                if(!isF1Down) {
                    isF1Down = true;

                    String wikiName = null;
                    if(mc.currentScreen instanceof WDScreen)
                        wikiName = ((WDScreen) mc.currentScreen).getWikiPageName();
                    else if(mc.currentScreen instanceof GuiContainer) {
                        Slot slot = ((GuiContainer) mc.currentScreen).getSlotUnderMouse();

                        if(slot != null && slot.getHasStack() && slot.getStack().getItem() instanceof WDItem)
                            wikiName = ((WDItem) slot.getStack().getItem()).getWikiName(slot.getStack());
                    }

                    if(wikiName != null)
                        mcef.openExampleBrowser("https://montoyo.net/wdwiki/index.php/" + wikiName);
                }
            } else if(isF1Down)
                isF1Down = false;

            //Workaround cuz chat sux
            if(nextScreen != null && mc.currentScreen == null) {
                mc.displayGuiScreen(nextScreen);
                nextScreen = null;
            }

            //Unload/load screens depending on client player distance
            if(mc.player != null && !screenTracking.isEmpty()) {
                int id = lastTracked % screenTracking.size();
                lastTracked++;

                TileEntityScreen tes = screenTracking.get(id);
                double dist2 = mc.player.getDistanceSq(tes.getPos());

                if(tes.isLoaded()) {
                    if(dist2 > WebDisplays.INSTANCE.unloadDistance2)
                        tes.unload();
                    else if(WebDisplays.INSTANCE.enableSoundDistance)
                        tes.updateTrackDistance(dist2, SoundSystemConfig.getMasterGain());
                } else if(dist2 <= WebDisplays.INSTANCE.loadDistance2)
                    tes.load();
            }

            //Load/unload minePads depending on which item is in the player's hand
            if(++minePadTickCounter >= 10) {
                minePadTickCounter = 0;
                EntityPlayer ep = mc.player;

                for(PadData pd: padList)
                    pd.isInHotbar = false;

                if(ep != null) {
                    updateInventory(ep.inventory.mainInventory, ep.getHeldItem(EnumHand.MAIN_HAND), 9);
                    updateInventory(ep.inventory.offHandInventory, ep.getHeldItem(EnumHand.OFF_HAND), 1); //Is this okay?
                }

                //TODO: Check for GuiContainer.draggedStack

                for(int i = padList.size() - 1; i >= 0; i--) {
                    PadData pd = padList.get(i);

                    if(!pd.isInHotbar) {
                        pd.view.close();
                        pd.view = null; //This is for GuiMinePad, in case the player dies with the GUI open
                        padList.remove(i);
                        padMap.remove(pd.id);
                    }
                }
            }

            //Laser pointer raycast
            boolean raycastHit = false;

            if(mc.player != null && mc.world != null && mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == WebDisplays.INSTANCE.itemLaserPointer
                                                     && mc.gameSettings.keyBindUseItem.isKeyDown()
                                                     && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK)) {
                laserPointerRenderer.isOn = true;
                RayTraceResult result = raycast(64.0); //TODO: Make that distance configurable

                if(result != null) {
                    BlockPos bpos = result.getBlockPos();

                    if(result.typeOfHit == RayTraceResult.Type.BLOCK && mc.world.getBlockState(bpos).getBlock() == WebDisplays.INSTANCE.blockScreen) {
                        Vector3i pos = new Vector3i(result.getBlockPos());
                        BlockSide side = BlockSide.values()[result.sideHit.ordinal()];

                        Multiblock.findOrigin(mc.world, pos, side, null);
                        TileEntityScreen te = (TileEntityScreen) mc.world.getTileEntity(pos.toBlock());

                        if(te != null && te.hasUpgrade(side, DefaultUpgrade.LASER_MOUSE)) { //hasUpgrade returns false is there's no screen on side 'side'
                            //Since rights aren't synchronized, let the server check them for us...
                            TileEntityScreen.Screen scr = te.getScreen(side);

                            if(scr.browser != null) {
                                float hitX = ((float) result.hitVec.x) - (float) bpos.getX();
                                float hitY = ((float) result.hitVec.y) - (float) bpos.getY();
                                float hitZ = ((float) result.hitVec.z) - (float) bpos.getZ();
                                Vector2i tmp = new Vector2i();

                                if(BlockScreen.hit2pixels(side, bpos, pos, scr, hitX, hitY, hitZ, tmp)) {
                                    laserClick(te, side, scr, tmp);
                                    raycastHit = true;
                                }
                            }
                        }
                    }
                }
            } else
                laserPointerRenderer.isOn = false;

            if(!raycastHit)
                deselectScreen();

            //Handle JS queries
            jsDispatcher.handleQueries();

            //Miniserv
            if(msClientStarted && mc.player == null) {
                msClientStarted = false;
                Client.getInstance().stop();
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerHand(RenderSpecificHandEvent ev) {
        Item item = ev.getItemStack().getItem();
        IItemRenderer renderer;

        if(item == WebDisplays.INSTANCE.itemMinePad)
            renderer = minePadRenderer;
        else if(item == WebDisplays.INSTANCE.itemLaserPointer)
            renderer = laserPointerRenderer;
        else
            return;

        EnumHandSide handSide = mc.player.getPrimaryHand();
        if(ev.getHand() == EnumHand.OFF_HAND)
            handSide = handSide.opposite();

        renderer.render(ev.getItemStack(), (handSide == EnumHandSide.RIGHT) ? 1.0f : -1.0f, ev.getSwingProgress(), ev.getEquipProgress());
        ev.setCanceled(true);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload ev) {
        Log.info("World unloaded; killing screens...");
        int dim = ev.getWorld().provider.getDimension();

        for(int i = screenTracking.size() - 1; i >= 0; i--) {
            if(screenTracking.get(i).getWorld().provider.getDimension() == dim) //Could be world == ev.getWorld()
                screenTracking.remove(i).unload();
        }
    }

    /**************************************** OTHER METHODS ****************************************/

    private void laserClick(TileEntityScreen tes, BlockSide side, TileEntityScreen.Screen scr, Vector2i hit) {
        if(pointedScreen == tes && pointedScreenSide == side) {
            long t = System.currentTimeMillis();

            if(t - lastPointPacket >= 100) {
                lastPointPacket = t;
                WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.vec2(tes, side, SMessageScreenCtrl.CTRL_LASER_MOVE, hit));
            }
        } else {
            deselectScreen();
            pointedScreen = tes;
            pointedScreenSide = side;
            WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.vec2(tes, side, SMessageScreenCtrl.CTRL_LASER_DOWN, hit));
        }
    }

    private void deselectScreen() {
        if(pointedScreen != null && pointedScreenSide != null) {
            WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.laserUp(pointedScreen, pointedScreenSide));
            pointedScreen = null;
            pointedScreenSide = null;
        }
    }

    private RayTraceResult raycast(double dist) {
        Vec3d start = mc.player.getPositionEyes(1.0f);
        Vec3d lookVec = mc.player.getLook(1.0f);
        Vec3d end = start.addVector(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist);

        return mc.world.rayTraceBlocks(start, end, true, true, false);
    }

    private void updateInventory(NonNullList<ItemStack> inv, ItemStack heldStack, int cnt) {
        for(int i = 0; i < cnt; i++) {
            ItemStack item = inv.get(i);

            if(item.getItem() == WebDisplays.INSTANCE.itemMinePad) {
                NBTTagCompound tag = item.getTagCompound();

                if(tag != null && tag.hasKey("PadID"))
                    updatePad(tag.getInteger("PadID"), tag, item == heldStack);
            }
        }
    }

    private void registerCustomBlockBaker(IModelBaker baker, Block block0) {
        ModelResourceLocation normalLoc = new ModelResourceLocation(block0.getRegistryName(), "normal");
        ResourceModelPair pair = new ResourceModelPair(normalLoc, baker);
        modelBakers.add(pair);
        ModelLoader.setCustomStateMapper(block0, new StaticStateMapper(normalLoc));
    }

    private void registerItemModel(Item item, int meta, String variant) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), variant));
    }

    private void registerItemMultiModels(ItemMulti item) {
        Enum[] values = item.getEnumValues();

        for(int i = 0; i < values.length; i++)
            ModelLoader.setCustomModelResourceLocation(item, i, new ModelResourceLocation(item.getRegistryName().toString() + '_' + values[i], "normal"));
    }

    private void updatePad(int id, NBTTagCompound tag, boolean isSelected) {
        PadData pd = padMap.get(id);

        if(pd != null)
            pd.isInHotbar = true;
        else if(isSelected && tag.hasKey("PadURL")) {
            pd = new PadData(tag.getString("PadURL"), id);
            padMap.put(id, pd);
            padList.add(pd);
        }
    }

    public MinePadRenderer getMinePadRenderer() {
        return minePadRenderer;
    }

    public PadData getPadByID(int id) {
        return padMap.get(id);
    }

    public net.montoyo.mcef.api.API getMCEF() {
        return mcef;
    }

    public static final class ScreenSidePair {

        public TileEntityScreen tes;
        public BlockSide side;

    }

    public boolean findScreenFromBrowser(IBrowser browser, ScreenSidePair pair) {
        for(TileEntityScreen tes: screenTracking) {
            for(int i = 0; i < tes.screenCount(); i++) {
                TileEntityScreen.Screen scr = tes.getScreen(i);

                if(scr.browser == browser) {
                    pair.tes = tes;
                    pair.side = scr.side;
                    return true;
                }
            }
        }

        return false;
    }

    private static Field findAdvancementToProgressField() {
        Field[] fields = ClientAdvancementManager.class.getDeclaredFields();
        Optional<Field> result = Arrays.stream(fields).filter(f -> f.getType() == Map.class).findAny();

        if(result.isPresent()) {
            try {
                Field ret = result.get();
                ret.setAccessible(true);
                return ret;
            } catch(Throwable t) {
                t.printStackTrace();
            }
        }

        Log.warning("ClientAdvancementManager.advancementToProgress field could not be found");
        return null;
    }

}
