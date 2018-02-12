/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;

public final class Vector3i {

    public int x;
    public int y;
    public int z;

    public Vector3i() {
        x = 0;
        y = 0;
        z = 0;
    }

    public Vector3i(int val) {
        x = val;
        y = val;
        z = val;
    }

    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3i(BlockPos bp) {
        x = bp.getX();
        y = bp.getY();
        z = bp.getZ();
    }

    public Vector3i(ByteBuf bb) {
        x = bb.readInt();
        y = bb.readInt();
        z = bb.readInt();
    }

    @Override
    public Vector3i clone() {
        return new Vector3i(x, y, z);
    }

    @Override
    public int hashCode() {
        return ((37 + x) * 31 + y) * 43 + z;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Vector3i) {
            Vector3i src = (Vector3i) o;

            return (src.x == x && src.y == y && src.z == z);
        } else
            return false;
    }

    public boolean equalsBlockPos(BlockPos bp) {
        return bp.getX() == x && bp.getY() == y && bp.getZ() == z;
    }

    @Override
    public String toString() {
        return "X: " + x + ", Y: " + y + ", Z: " + z;
    }

    //Add
    public Vector3i add(Vector3i src) {
        x += src.x;
        y += src.y;
        z += src.z;

        return this;
    }

    public Vector3i addMul(Vector3i src, int mul) {
        x += src.x * mul;
        y += src.y * mul;
        z += src.z * mul;

        return this;
    }

    public Vector3i add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector3i add(int xyz) {
        x += xyz;
        y += xyz;
        z += xyz;

        return this;
    }

    //Sub
    public Vector3i sub(Vector3i src) {
        x -= src.x;
        y -= src.y;
        z -= src.z;

        return this;
    }

    public Vector3i sub(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector3i sub(int xyz) {
        x -= xyz;
        y -= xyz;
        z -= xyz;

        return this;
    }

    //Mul
    public Vector3i neg() {
        x = -x;
        y = -y;
        z = -z;

        return this;
    }

    public Vector3i mul(int val) {
        x *= val;
        y *= val;
        z *= val;

        return this;
    }

    public Vector3i div(int val) {
        x /= val;
        y /= val;
        z /= val;

        return this;
    }

    public Vector3i set(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3i set(double x, double y, double z)
    {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        return this;
    }

    public Vector3i set(float x, float y, float z)
    {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        return this;
    }

    public Vector3i set(int val)
    {
        x = val;
        y = val;
        z = val;
        return this;
    }

    public Vector3i set(Vector3i val)
    {
        x = val.x;
        y = val.y;
        z = val.z;
        return this;
    }

    public Vector3i set(Vector3f vec)
    {
        this.x = (int) vec.x;
        this.y = (int) vec.y;
        this.z = (int) vec.z;
        return this;
    }

    public int dot(Vector3i vec) {
        return x * vec.x + y * vec.y + z * vec.z;
    }

    public Vector3f toFloat() {
        return new Vector3f((float) x, (float) y, (float) z);
    }
    public BlockPos toBlock() {
        return new BlockPos(x, y, z);
    }

    public void toBlock(BlockPos.MutableBlockPos bp) {
        bp.setPos(x, y, z);
    }

    public int getChunkLocalPos()
    {
        int lx = x & 15;
        int ly = y & 255;
        int lz = z & 15;

        return (ly << 8) | (lz << 4) | lx;
    }

    public void writeTo(ByteBuf bb) {
        bb.writeInt(x);
        bb.writeInt(y);
        bb.writeInt(z);
    }

}
