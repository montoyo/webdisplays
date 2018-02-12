/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.utilities.Log;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTBgra;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;

@SideOnly(Side.CLIENT)
public class RenderRecipe extends GuiScreen {

    private static class NameRecipePair {

        private final String name;
        private final ShapedRecipes recipe;

        private NameRecipePair(String n, ShapedRecipes r) {
            this.name = n;
            this.recipe = r;
        }

    }

    private static final ResourceLocation CRAFTING_TABLE_GUI_TEXTURES = new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 166;
    private int x;
    private int y;
    private RenderItem renderItem;
    private final ItemStack[] recipe = new ItemStack[3 * 3];
    private ItemStack recipeResult;
    private String recipeName;
    private final ArrayList<NameRecipePair> recipes = new ArrayList<>();
    private IntBuffer buffer;
    private int[] array;

    @Override
    public void initGui() {
        x = (width - SIZE_X) / 2;
        y = (height - SIZE_Y) / 2;
        renderItem = mc.getRenderItem();

        for(IRecipe recipe: CraftingManager.REGISTRY) {
            ResourceLocation regName = recipe.getRegistryName();

            if(regName != null && regName.getResourceDomain().equals("webdisplays")) {
                if(recipe instanceof ShapedRecipes)
                    recipes.add(new NameRecipePair(regName.getResourcePath(), (ShapedRecipes) recipe));
                else
                    Log.warning("Found non-shaped recipe %s", regName.toString());
            }
        }

        Log.info("Loaded %d recipes", recipes.size());
        nextRecipe();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(CRAFTING_TABLE_GUI_TEXTURES);
        drawTexturedModalRect(x, y, 0, 0, SIZE_X, SIZE_Y);
        fontRenderer.drawString(I18n.format("container.crafting"), x + 28, y + 6, 0x404040);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();

        for(int sy = 0; sy < 3; sy++) {
            for(int sx = 0; sx < 3; sx++) {
                ItemStack is = recipe[sy * 3 + sx];

                if(is != null) {
                    int x = this.x + 30 + sx * 18;
                    int y = this.y + 17 + sy * 18;

                    renderItem.renderItemAndEffectIntoGUI(mc.player, is, x, y);
                    renderItem.renderItemOverlayIntoGUI(fontRenderer, is, x, y, null);
                }
            }
        }

        if(recipeResult != null) {
            renderItem.renderItemAndEffectIntoGUI(mc.player, recipeResult, x + 124, y + 35);
            renderItem.renderItemOverlayIntoGUI(fontRenderer, recipeResult, x + 124, y + 35, null);
        }

        GlStateManager.enableLighting();
        RenderHelper.disableStandardItemLighting();
    }

    private void setRecipe(ShapedRecipes recipe) {
        IntStream.range(0, this.recipe.length).forEach(i -> this.recipe[i] = null);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int pos = 0;

        for(int y = 0; y < recipe.getRecipeHeight(); y++) {
            for(int x = 0; x < recipe.getRecipeWidth(); x++) {
                ItemStack[] stacks = ingredients.get(pos++).getMatchingStacks();

                if(stacks.length > 0)
                    this.recipe[y * 3 + x] = stacks[0];
            }
        }

        recipeResult = recipe.getRecipeOutput();
    }

    private void nextRecipe() {
        if(recipes.isEmpty())
            mc.displayGuiScreen(null);
        else {
            NameRecipePair pair = recipes.remove(0);
            setRecipe(pair.recipe);
            recipeName = pair.name;
        }
    }

    private int screen2DisplayX(int x) {
        double ret = ((double) x) / ((double) width) * ((double) mc.displayWidth);
        return (int) ret;
    }

    private int screen2DisplayY(int y) {
        double ret = ((double) y) / ((double) height) * ((double) mc.displayHeight);
        return (int) ret;
    }

    private void takeScreenshot() throws Throwable {
        int x = screen2DisplayX(this.x + 27);
        int y = mc.displayHeight - screen2DisplayY(this.y + 4);
        int w = screen2DisplayX(120);
        int h = screen2DisplayY(68);
        y -= h;

        if(buffer == null)
            buffer = BufferUtils.createIntBuffer(w * h);

        int oldPack = glGetInteger(GL_PACK_ALIGNMENT);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        buffer.clear();
        glReadPixels(x, y, w, h, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
        glPixelStorei(GL_PACK_ALIGNMENT, oldPack);

        if(array == null)
            array = new int[w * h];

        buffer.clear();
        buffer.get(array);
        TextureUtil.processPixelValues(array, w, h);

        File f = new File(mc.mcDataDir, "wd_recipes");
        if(!f.exists())
            f.mkdir();

        f = new File(f, recipeName + ".png");

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, w, h, array, 0, w);
        ImageIO.write(bi, "PNG", f);
    }

    @Override
    public void updateScreen() {
        if(recipeName != null) {
            try {
                takeScreenshot();
                nextRecipe();
            } catch(Throwable t) {
                t.printStackTrace();
                mc.displayGuiScreen(null);
            }
        }
    }

}
