/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.montoyo.wd.block.BlockKeyboardRight;
import net.montoyo.wd.block.BlockPeripheral;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.core.WDCreativeTab;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.item.*;
import net.montoyo.wd.net.Messages;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.*;

@Mod(modid = "webdisplays", version = WebDisplays.MOD_VERSION, dependencies = "required-after:mcef;")
public class WebDisplays {

    public static final String MOD_VERSION = "1.0";

    @Mod.Instance(owner = "webdisplays")
    public static WebDisplays INSTANCE;

    @SidedProxy(serverSide = "net.montoyo.wd.SharedProxy", clientSide = "net.montoyo.wd.client.ClientProxy")
    public static SharedProxy PROXY;

    public static SimpleNetworkWrapper NET_HANDLER;
    public static WDCreativeTab CREATIVE_TAB;

    //Blocks
    public BlockScreen blockScreen;
    public BlockPeripheral blockPeripheral;
    public BlockKeyboardRight blockKbRight;

    //Items
    public ItemScreenConfigurator itemScreenCfg;
    public ItemOwnershipThief itemOwnerThief;
    public ItemLinker itemLinker;
    public Item itemStoneKey;
    public ItemMinePad2 itemMinePad;
    public ItemUpgrade itemUpgrade;

    //Sounds
    public SoundEvent soundTyping;

    //Config
    public static final double PAD_RATIO = 59.0 / 30.0;
    public String homePage = "https://google.com"; //TODO: Read from config
    public double padResX;
    public double padResY;
    private int lastPadId = 0;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent ev) {
        CREATIVE_TAB = new WDCreativeTab();

        //TODO: Read configuration
        final int padHeight = 480;
        padResY = (double) padHeight;
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

        itemStoneKey = new Item();
        itemStoneKey.setCreativeTab(CREATIVE_TAB);
        itemStoneKey.setUnlocalizedName("webdisplays.stonekey");
        itemStoneKey.setRegistryName("stonekey");

        PROXY.preInit();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent ev) {
        //Register tile entities
        GameRegistry.registerTileEntity(TileEntityScreen.class, "webdisplays:screen");
        for(DefaultPeripheral dp: DefaultPeripheral.values()) {
            if(dp.getTEClass() != null)
                GameRegistry.registerTileEntity(dp.getTEClass(), "webdisplays:" + dp.getName());
        }

        //Other things
        PROXY.init();
        NET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("webdisplays");
        Messages.registerAll(NET_HANDLER);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent ev) {
        PROXY.postInit();
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> ev) {
        ev.getRegistry().registerAll(blockScreen, blockPeripheral, blockKbRight);
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> ev) {
        ev.getRegistry().registerAll(blockScreen.getItem(), blockPeripheral.getItem());
        ev.getRegistry().registerAll(itemScreenCfg, itemOwnerThief, itemLinker, itemStoneKey, itemMinePad, itemUpgrade);
    }

    @SubscribeEvent
    public void onRegisterSounds(RegistryEvent.Register<SoundEvent> ev) {
        soundTyping = new SoundEvent(new ResourceLocation("webdisplays", "keyboardType"));
        soundTyping.setRegistryName(soundTyping.getSoundName());
        ev.getRegistry().register(soundTyping);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load ev) {
        if(ev.getWorld().isRemote || ev.getWorld().provider.getDimension() != 0)
            return;

        File f = new File(ev.getWorld().getSaveHandler().getWorldDirectory(), "wd_next.txt");

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

    public static int getNextAvailablePadID() {
        return INSTANCE.lastPadId++;
    }

}
