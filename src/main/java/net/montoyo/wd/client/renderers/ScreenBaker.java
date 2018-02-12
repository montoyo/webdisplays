/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Vector3f;
import net.montoyo.wd.utilities.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScreenBaker implements IModelBaker {

    private static final List<BakedQuad> noQuads = ImmutableList.of();
    private final TextureAtlasSprite[] texs = new TextureAtlasSprite[16];
    private final BlockSide[] blockSides = BlockSide.values();
    private final EnumFacing[] blockFacings = EnumFacing.values();

    @Override
    public void loadTextures(TextureMap texMap) {
        for(int i = 0; i < texs.length; i++)
            texs[i] = texMap.registerSprite(new ResourceLocation("webdisplays", "blocks/screen" + i));
    }

    private void putVertex(int[] buf, int pos, Vector3f vpos, TextureAtlasSprite tex, Vector3f uv, Vector3i normal) {
        buf[pos * 7 + 0] = Float.floatToRawIntBits(vpos.x);
        buf[pos * 7 + 1] = Float.floatToRawIntBits(vpos.y);
        buf[pos * 7 + 2] = Float.floatToRawIntBits(vpos.z);
        buf[pos * 7 + 3] = 0xFFFFFFFF; //Color, let this white...
        buf[pos * 7 + 4] = Float.floatToRawIntBits(tex.getInterpolatedU(uv.x));
        buf[pos * 7 + 5] = Float.floatToRawIntBits(tex.getInterpolatedV(uv.y));

        int nx = (normal.x * 127) & 0xFF;
        int ny = (normal.y * 127) & 0xFF;
        int nz = (normal.z * 127) & 0xFF;
        buf[pos * 7 + 6] = nx | (ny << 8) | (nz << 16);
    }

    private Vector3f rotateVec(Vector3f vec, BlockSide side) {
        switch(side) {
            case BOTTOM: return new Vector3f(vec.x,   1.0f,          1.0f - vec.z);
            case TOP:    return new Vector3f(vec.x,   0.0f,                 vec.z);
            case NORTH:  return new Vector3f(vec.x,   vec.z,                 1.0f);
            case SOUTH:  return new Vector3f(vec.x,   1.0f - vec.z,          0.0f);
            case WEST:   return new Vector3f(1.f ,    vec.x,                vec.z);
            case EAST:   return new Vector3f(0.0f,    1.0f - vec.x,         vec.z);
        }

        throw new RuntimeException("Unknown block side " + side);
    }

    private Vector3f rotateTex(BlockSide side, float u, float v) {
        switch(side) {
            case BOTTOM: return new Vector3f(u,        16.f - v, 0.0f);
            case TOP:    return new Vector3f(u,               v, 0.0f);
            case NORTH:  return new Vector3f(16.f - u, 16.f - v, 0.0f);
            case SOUTH:  return new Vector3f(u,               v, 0.0f);
            case WEST:   return new Vector3f(v,        16.f - u, 0.0f);
            case EAST:   return new Vector3f(16.f - v,        u, 0.0f);
        }

        throw new RuntimeException("Unknown block side " + side);
    }

    private BakedQuad bakeSide(BlockSide side, TextureAtlasSprite tex) {
        int[] data = new int[7 * 4];

        putVertex(data, 3, rotateVec(new Vector3f(0.0f, 0.0f, 0.0f), side), tex, rotateTex(side, 0.0f, 0.0f  ), side.backward);
        putVertex(data, 2, rotateVec(new Vector3f(0.0f, 0.0f, 1.0f), side), tex, rotateTex(side, 0.0f, 16.0f ), side.backward);
        putVertex(data, 1, rotateVec(new Vector3f(1.0f, 0.0f, 1.0f), side), tex, rotateTex(side, 16.0f, 16.0f), side.backward);
        putVertex(data, 0, rotateVec(new Vector3f(1.0f, 0.0f, 0.0f), side), tex, rotateTex(side, 16.0f, 0.0f ), side.backward);

        return new BakedQuad(data, 0xFFFFFFFF, blockFacings[side.ordinal()], tex, true, DefaultVertexFormats.ITEM);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if(side == null)
            return noQuads;

        IExtendedBlockState bs = (IExtendedBlockState) state;
        List<BakedQuad> ret = new ArrayList<>();

        int sid = BlockSide.reverse(side.ordinal());
        BlockSide s = blockSides[sid];
        TextureAtlasSprite tex = texs[15];
        if(bs != null)
            tex = texs[bs.getValue(BlockScreen.sideFlags[sid])];

        ret.add(bakeSide(s, tex));
        return ret;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    @Nonnull
    public TextureAtlasSprite getParticleTexture() {
        return texs[15];
    }

    @Override
    @Nonnull
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    @Nonnull
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

}
