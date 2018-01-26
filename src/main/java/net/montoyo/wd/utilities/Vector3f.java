/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

public final class Vector3f {

    public float x;
    public float y;
    public float z;

    public Vector3f() {
        x = 0.f;
        y = 0.f;
        z = 0.f;
    }

    public Vector3f(float val) {
        x = val;
        y = val;
        z = val;
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vector3f clone() {
        return new Vector3f(x, y, z);
    }

    @Override
    public int hashCode() {
        return ((37 + Float.floatToRawIntBits(x)) * 31 + Float.floatToRawIntBits(y)) * 43 + Float.floatToRawIntBits(z);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Vector3f) {
            Vector3f src = (Vector3f) o;

            return (src.x == x && src.y == y && src.z == z);
        } else
            return false;
    }

    //Add
    public Vector3f add(Vector3f src) {
        x += src.x;
        y += src.y;
        z += src.z;

        return this;
    }

    public Vector3f addMul(Vector3f src, float m) {
        x += src.x * m;
        y += src.y * m;
        z += src.z * m;

        return this;
    }

    public Vector3f add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vector3f add(float xyz) {
        x += xyz;
        y += xyz;
        z += xyz;

        return this;
    }

    //Sub
    public Vector3f sub(Vector3f src) {
        x -= src.x;
        y -= src.y;
        z -= src.z;

        return this;
    }

    public Vector3f sub(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vector3f sub(float xyz) {
        x -= xyz;
        y -= xyz;
        z -= xyz;

        return this;
    }

    //Mul
    public Vector3f neg() {
        x = -x;
        y = -y;
        z = -z;

        return this;
    }

    public Vector3f mul(float val) {
        x *= val;
        y *= val;
        z *= val;

        return this;
    }

    public Vector3f div(float val) {
        x /= val;
        y /= val;
        z /= val;

        return this;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    public Vector3f set(float xyz) {
        x = xyz;
        y = xyz;
        z = xyz;

        return this;
    }

    public float dot(Vector3f vec) {
        return x * vec.x + y * vec.y + z * vec.z;
    }

    public Vector3f set(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;

        return this;
    }

    public Vector3f set(int x, int y, int z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;

        return this;
    }

    public Vector3f set(Vector3f vec) {
        x = vec.x;
        y = vec.y;
        z = vec.z;

        return this;
    }

    public Vector3f set(Vector3i vec) {
        x = (float) vec.x;
        y = (float) vec.y;
        z = (float) vec.z;

        return this;
    }

}
