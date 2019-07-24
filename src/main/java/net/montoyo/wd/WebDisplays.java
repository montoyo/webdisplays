/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd;

import com.google.gson.Gson;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.montoyo.wd.block.BlockKeyboardRight;
import net.montoyo.wd.block.BlockPeripheral;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.*;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.item.*;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.client.CMessageServerInfo;
import net.montoyo.wd.net.Messages;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

@Mod(modid = "webdisplays", version = WebDisplays.MOD_VERSION, dependencies = "required-after:mcef@[1.0,2.0);after:opencomputers;after:computercraft;")
public class WebDisplays {

    public static final String MOD_VERSION = "1.1";

    @Mod.Instance(owner = "webdisplays")
    public static WebDisplays INSTANCE;

    @SidedProxy(serverSide = "net.montoyo.wd.SharedProxy", clientSide = "net.montoyo.wd.client.ClientProxy")
    public static SharedProxy PROXY;

    public static SimpleNetworkWrapper NET_HANDLER;
    public static WDCreativeTab CREATIVE_TAB;
    public static final ResourceLocation ADV_PAD_BREAK = new ResourceLocation("webdisplays", "webdisplays/pad_break");
    public static final String BLACKLIST_URL = "mod://webdisplays/blacklisted.html";
    public static final Gson GSON = new Gson();
    public static final ResourceLocation CAPABILITY = new ResourceLocation("webdisplays", "customdatacap");

    //Blocks
    public BlockScreen blockScreen;
    public BlockPeripheral blockPeripheral;
    public BlockKeyboardRight blockKbRight;

    //Items
    public ItemScreenConfigurator itemScreenCfg;
    public ItemOwnershipThief itemOwnerThief;
    public ItemLinker itemLinker;
    public ItemMinePad2 itemMinePad;
    public ItemUpgrade itemUpgrade;
    public ItemLaserPointer itemLaserPointer;
    public ItemCraftComponent itemCraftComp;
    public ItemMulti itemAdvIcon;

    //Sounds
    public SoundEvent soundTyping;
    public SoundEvent soundUpgradeAdd;
    public SoundEvent soundUpgradeDel;
    public SoundEvent soundScreenCfg;
    public SoundEvent soundServer;
    public SoundEvent soundIronic;

    //Criterions
    public Criterion criterionPadBreak;
    public Criterion criterionUpgradeScreen;
    public Criterion criterionLinkPeripheral;
    public Criterion criterionKeyboardCat;

    //Config
    public static final double PAD_RATIO = 59.0 / 30.0;
    public String homePage;
    public double padResX;
    public double padResY;
    private int lastPadId = 0;
    public boolean doHardRecipe;
    private boolean hasOC;
    private boolean hasCC;
    private String[] blacklist;
    public boolean disableOwnershipThief;
    public double unloadDistance2;
    public double loadDistance2;
    public int maxResX;
    public int maxResY;
    public int maxScreenX;
    public int maxScreenY;
    public int miniservPort;
    public long miniservQuota;
    public boolean enableSoundDistance;
    public float ytVolume;
    public float avDist100;
    public float avDist0;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent ev) {
        //Load config
        Configuration cfg = new Configuration(ev.getSuggestedConfigurationFile());
        cfg.load();

        //CAT: Main
        Property blacklist = cfg.get("main", "blacklist", new String[0]);
        Property padHeight = cfg.get("main", "padHeight", 480);
        Property hardRecipe = cfg.get("main", "hardRecipes", true);
        Property homePage = cfg.get("main", "homepage", "mod://webdisplays/main.html");
        Property disableOT = cfg.get("main", "disableOwnershipThief", false);
        Property maxResX = cfg.get("main", "maxResolutionX", 1920);
        Property maxResY = cfg.get("main", "maxResolutionY", 1080);
        Property miniservPort = cfg.get("main", "miniservPort", 25566);
        Property miniservQuota = cfg.get("main", "miniservQuota", 1024); //It's stored as a string anyway
        Property maxScreenX = cfg.get("main", "maxScreenSizeX", 16);
        Property maxScreenY = cfg.get("main", "maxScreenSizeY", 16);

        //CAT: Client options
        Property loadDistance = cfg.get("client", "loadDistance", 30.0);
        Property unloadDistance = cfg.get("client", "unloadDistance", 32.0);

        //CAT: Auto volume config (client-side)
        Property enableAutoVol = cfg.get("clientAutoVolume", "enableAutoVolume", true);
        Property ytVolume = cfg.get("clientAutoVolume", "ytVolume", 100.0);
        Property dist100 = cfg.get("clientAutoVolume", "dist100", 10.0);
        Property dist0 = cfg.get("clientAutoVolume", "dist0", 30.0);


        //Comments & shit
        blacklist.setComment("An array of domain names you don't want to load.");
        padHeight.setComment("The minePad Y resolution in pixels. padWidth = padHeight * " + PAD_RATIO);
        hardRecipe.setComment("If true, breaking the minePad is required to craft upgrades.");
        homePage.setComment("The URL that will be loaded each time you create a screen");
        disableOT.setComment("If true, the ownership thief item will be disabled");
        loadDistance.setComment("All screens outside this range will be unloaded");
        unloadDistance.setComment("All unloaded screens inside this range will be loaded");
        maxResX.setComment("Maximum horizontal screen resolution, in pixels");
        maxResY.setComment("Maximum vertical screen resolution, in pixels");
        miniservPort.setComment("The port used by miniserv. 0 to disable.");
        miniservPort.setMaxValue(Short.MAX_VALUE);
        miniservQuota.setComment("The amount of data that can be uploaded to miniserv, in KiB (so 1024 = 1 MiO)");
        maxScreenX.setComment("Maximum screen width, in blocks. Resolution will be clamped by maxResolutionX.");
        maxScreenY.setComment("Maximum screen height, in blocks. Resolution will be clamped by maxResolutionY.");
        enableAutoVol.setComment("If true, the volume of YouTube videos will change depending on how far you are");
        ytVolume.setComment("Volume for YouTube videos. This will have no effect if enableSoundDistance is set to false");
        ytVolume.setMinValue(0.0);
        ytVolume.setMaxValue(100.0);
        dist100.setComment("Distance after which the sound starts dropping (in blocks)");
        dist100.setMinValue(0.0);
        dist0.setComment("Distance after which you can't hear anything (in blocks)");
        dist0.setMinValue(0.0);

        if(unloadDistance.getDouble() < loadDistance.getDouble() + 2.0)
            unloadDistance.set(loadDistance.getDouble() + 2.0);

        if(dist0.getDouble() < dist100.getDouble() + 0.1)
            dist0.set(dist100.getDouble() + 0.1);

        cfg.save();

        this.blacklist = blacklist.getStringList();
        doHardRecipe = hardRecipe.getBoolean();
        this.homePage = homePage.getString();
        disableOwnershipThief = disableOT.getBoolean();
        unloadDistance2 = unloadDistance.getDouble() * unloadDistance.getDouble();
        loadDistance2 = loadDistance.getDouble() * loadDistance.getDouble();
        this.maxResX = maxResX.getInt();
        this.maxResY = maxResY.getInt();
        this.miniservPort = miniservPort.getInt();
        this.miniservQuota = miniservQuota.getLong() * 1024L;
        this.maxScreenX = maxScreenX.getInt();
        this.maxScreenY = maxScreenY.getInt();
        enableSoundDistance = enableAutoVol.getBoolean();
        this.ytVolume = (float) ytVolume.getDouble();
        avDist100 = (float) dist100.getDouble();
        avDist0 = (float) dist0.getDouble();

        CREATIVE_TAB = new WDCreativeTab();

        //Criterions
        criterionPadBreak = new Criterion("pad_break");
        criterionUpgradeScreen = new Criterion("upgrade_screen");
        criterionLinkPeripheral = new Criterion("link_peripheral");
        criterionKeyboardCat = new Criterion("keyboard_cat");
        registerTrigger(criterionPadBreak, criterionUpgradeScreen, criterionLinkPeripheral, criterionKeyboardCat);

        //Read configuration
        padResY = (double) padHeight.getInt();
        padResX = padResY * PAD_RATIO;

        //Init blocks
        blockScreen = new BlockScreen();
        blockScreen.makeItemBlock();

        blockPeripheral = new BlockPeripheral();
        blockPeripheral.makeItemBlock();

        blockKbRight = new BlockKeyboardRight();

        //Init items
        itemScreenCfg = new ItemScreenConfigurator();
        itemOwnerThief = new ItemOwnershipThief();
        itemLinker = new ItemLinker();
        itemMinePad = new ItemMinePad2();
        itemUpgrade = new ItemUpgrade();
        itemLaserPointer = new ItemLaserPointer();
        itemCraftComp = new ItemCraftComponent();

        itemAdvIcon = new ItemMulti(AdvancementIcon.class);
        itemAdvIcon.setUnlocalizedName("webdisplays.advicon");
        itemAdvIcon.setRegistryName("advicon");

        PROXY.preInit();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent ev) {
        //Register tile entities
        GameRegistry.registerTileEntity(TileEntityScreen.class, new ResourceLocation("webdisplays", "screen"));
        for(DefaultPeripheral dp: DefaultPeripheral.values()) {
            if(dp.getTEClass() != null)
                GameRegistry.registerTileEntity(dp.getTEClass(), new ResourceLocation("webdisplays", dp.getName()));
        }

        //Other things
        CapabilityManager.INSTANCE.register(IWDDCapability.class, new WDDCapability.Storage(), new WDDCapability.Factory());
        PROXY.init();
        NET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("webdisplays");
        Messages.registerAll(NET_HANDLER);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent ev) {
        PROXY.postInit();
        hasOC = Loader.isModLoaded("opencomputers");
        hasCC = Loader.isModLoaded("computercraft");

        if(hasCC) {
            try {
                //We have to do this because the "register" method might be stripped out if CC isn't loaded
                CCPeripheralProvider.class.getMethod("register").invoke(null);
            } catch(Throwable t) {
                Log.error("ComputerCraft was found, but WebDisplays wasn't able to register its CC Interface Peripheral");
                t.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> ev) {
        ev.getRegistry().registerAll(blockScreen, blockPeripheral, blockKbRight);
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> ev) {
        ev.getRegistry().registerAll(blockScreen.getItem(), blockPeripheral.getItem());
        ev.getRegistry().registerAll(itemScreenCfg, itemOwnerThief, itemLinker, itemMinePad, itemUpgrade, itemLaserPointer, itemCraftComp, itemAdvIcon);
    }

    @SubscribeEvent
    public void onRegisterSounds(RegistryEvent.Register<SoundEvent> ev) {
        soundTyping = registerSound(ev, "keyboardType");
        soundUpgradeAdd = registerSound(ev, "upgradeAdd");
        soundUpgradeDel = registerSound(ev, "upgradeDel");
        soundScreenCfg = registerSound(ev, "screencfgOpen");
        soundServer = registerSound(ev, "server");
        soundIronic = registerSound(ev, "ironic");
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load ev) {
        if(ev.getWorld().isRemote || ev.getWorld().provider.getDimension() != 0)
            return;

        File worldDir = ev.getWorld().getSaveHandler().getWorldDirectory();
        File f = new File(worldDir, "wd_next.txt");

        if(f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String idx = br.readLine();
                Util.silentClose(br);

                if(idx == null)
                    throw new RuntimeException("Seems like the file is empty (1)");

                idx = idx.trim();
                if(idx.isEmpty())
                    throw new RuntimeException("Seems like the file is empty (2)");

                lastPadId = Integer.parseInt(idx); //This will throw NumberFormatException if it goes wrong
            } catch(Throwable t) {
                Log.warningEx("Could not read last minePad ID from %s. I'm afraid this might break all minePads.", t, f.getAbsolutePath());
            }
        }

        if(miniservPort != 0) {
            Server sv = Server.getInstance();
            sv.setPort(miniservPort);
            sv.setDirectory(new File(worldDir, "wd_filehost"));
            sv.start();
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save ev) {
        if(ev.getWorld().isRemote || ev.getWorld().provider.getDimension() != 0)
            return;

        File f = new File(ev.getWorld().getSaveHandler().getWorldDirectory(), "wd_next.txt");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("" + lastPadId + "\n");
            Util.silentClose(bw);
        } catch(Throwable t) {
            Log.warningEx("Could not save last minePad ID (%d) to %s. I'm afraid this might break all minePads.", t, lastPadId, f.getAbsolutePath());
        }
    }

    @SubscribeEvent
    public void onToss(ItemTossEvent ev) {
        if(!ev.getEntityItem().world.isRemote) {
            ItemStack is = ev.getEntityItem().getItem();

            if(is.getItem() == itemMinePad) {
                NBTTagCompound tag = is.getTagCompound();

                if(tag == null) {
                    tag = new NBTTagCompound();
                    is.setTagCompound(tag);
                }

                UUID thrower = ev.getPlayer().getGameProfile().getId();
                tag.setLong("ThrowerMSB", thrower.getMostSignificantBits());
                tag.setLong("ThrowerLSB", thrower.getLeastSignificantBits());
                tag.setDouble("ThrowHeight", ev.getPlayer().posY + ev.getPlayer().getEyeHeight());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerCraft(PlayerEvent.ItemCraftedEvent ev) {
        if(doHardRecipe && ev.crafting.getItem() == itemCraftComp && ev.crafting.getMetadata() == CraftComponent.EXTENSION_CARD.ordinal()) {
            if((ev.player instanceof EntityPlayerMP && !hasPlayerAdvancement((EntityPlayerMP) ev.player, ADV_PAD_BREAK)) || PROXY.hasClientPlayerAdvancement(ADV_PAD_BREAK) != HasAdvancement.YES) {
                ev.crafting.setItemDamage(CraftComponent.BAD_EXTENSION_CARD.ordinal());

                if(!ev.player.world.isRemote)
                    ev.player.world.playSound(null, ev.player.posX, ev.player.posY, ev.player.posZ, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @Mod.EventHandler
    public void onServerStop(FMLServerStoppingEvent ev) {
        Server.getInstance().stopServer();
    }

    @SubscribeEvent
    public void onLogIn(PlayerEvent.PlayerLoggedInEvent ev) {
        if(!ev.player.world.isRemote && ev.player instanceof EntityPlayerMP) {
            WebDisplays.NET_HANDLER.sendTo(new CMessageServerInfo(miniservPort), (EntityPlayerMP) ev.player);
            IWDDCapability cap = ev.player.getCapability(WDDCapability.INSTANCE, null);

            if(cap == null)
                Log.warning("Player %s (%s) has null IWDDCapability!", ev.player.getName(), ev.player.getGameProfile().getId().toString());
            else if(cap.isFirstRun()) {
                Util.toast(ev.player, TextFormatting.LIGHT_PURPLE, "welcome1");
                Util.toast(ev.player, TextFormatting.LIGHT_PURPLE, "welcome2");
                Util.toast(ev.player, TextFormatting.LIGHT_PURPLE, "welcome3");

                cap.clearFirstRun();
            }
        }
    }

    @SubscribeEvent
    public void onLogOut(PlayerEvent.PlayerLoggedOutEvent ev) {
        if(!ev.player.world.isRemote)
            Server.getInstance().getClientManager().revokeClientKey(ev.player.getGameProfile().getId());
    }

    @SubscribeEvent
    public void attachEntityCaps(AttachCapabilitiesEvent<Entity> ev) {
        if(ev.getObject() instanceof EntityPlayer)
            ev.addCapability(CAPABILITY, new WDDCapability.Provider());
    }

    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone ev) {
        IWDDCapability src = ev.getOriginal().getCapability(WDDCapability.INSTANCE, null);
        IWDDCapability dst = ev.getEntityPlayer().getCapability(WDDCapability.INSTANCE, null);

        if(src == null) {
            Log.error("src is null");
            return;
        }

        if(dst == null) {
            Log.error("dst is null");
            return;
        }

        src.cloneTo(dst);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent ev) {
        String msg = ev.getMessage().trim().replaceAll("\\s+", " ").toLowerCase();
        StringBuilder sb = new StringBuilder(msg.length());
        for(int i = 0; i < msg.length(); i++) {
            char chr = msg.charAt(i);

            if(chr != '.' && chr != ',' && chr != ';' && chr != '!' && chr != '?' && chr != ':' && chr != '\'' && chr != '\"' && chr != '`')
                sb.append(chr);
        }

        if(sb.toString().equals("ironic he could save others from death but not himself")) {
            EntityPlayer ply = ev.getPlayer();
            ply.world.playSound(null, ply.posX, ply.posY, ply.posZ, soundIronic, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent ev) {
        if(ev.getMessage().equals("!WD render recipes"))
            PROXY.renderRecipes();
    }

    private boolean hasPlayerAdvancement(EntityPlayerMP ply, ResourceLocation rl) {
        MinecraftServer server = PROXY.getServer();
        if(server == null)
            return false;

        Advancement adv = server.getAdvancementManager().getAdvancement(rl);
        return adv != null && ply.getAdvancements().getProgress(adv).isDone();
    }

    public static int getNextAvailablePadID() {
        return INSTANCE.lastPadId++;
    }

    private static SoundEvent registerSound(RegistryEvent.Register<SoundEvent> ev, String resName) {
        ResourceLocation resLoc = new ResourceLocation("webdisplays", resName);
        SoundEvent ret = new SoundEvent(resLoc);
        ret.setRegistryName(resLoc);

        ev.getRegistry().register(ret);
        return ret;
    }

    private static void registerTrigger(Criterion ... criteria) {
        for(Criterion c: criteria)
            CriteriaTriggers.register(c);
    }

    public static boolean isOpenComputersAvailable() {
        return INSTANCE.hasOC;
    }

    public static boolean isComputerCraftAvailable() {
        return INSTANCE.hasCC;
    }

    public static boolean isSiteBlacklisted(String url) {
        try {
            URL url2 = new URL(Util.addProtocol(url));
            return Arrays.stream(INSTANCE.blacklist).anyMatch(str -> str.equalsIgnoreCase(url2.getHost()));
        } catch(MalformedURLException ex) {
            return false;
        }
    }

    public static String applyBlacklist(String url) {
        return isSiteBlacklisted(url) ? BLACKLIST_URL : url;
    }

}

