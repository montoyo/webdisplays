/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraft.util.math.AxisAlignedBB;

public final class AABB {

    public final Vector3i start;
    public final Vector3i end;

    public AABB() {
        start = new Vector3i();
        end = new Vector3i();
    }

    public AABB(Vector3i pos) {
        start = pos.clone();
        end = pos.clone();
    }

    public AABB(Vector3i a, Vector3i b) {
        start = new Vector3i();
        end = new Vector3i();

        start.x = Math.min(a.x, b.x);
        start.y = Math.min(a.y, b.y);
        start.z = Math.min(a.z, b.z);
        end.x = Math.max(a.x, b.x);
        end.y = Math.max(a.y, b.y);
        end.z = Math.max(a.z, b.z);
    }

    public AABB(AxisAlignedBB bb) {
        start = new Vector3i();
        end = new Vector3i();

        start.x = (int) bb.minX;
        start.y = (int) bb.minY;
        start.z = (int) bb.minZ;
        end.x = (int) Math.ceil(bb.maxX);
        end.y = (int) Math.ceil(bb.maxY);
        end.z = (int) Math.ceil(bb.maxZ);
    }

    public AABB expand(Vector3i vec) {
        if(vec.x > end.x)
            end.x = vec.x;
        else if(vec.x < start.x)
            start.x = vec.x;

        if(vec.y > end.y)
            end.y = vec.y;
        else if(vec.y < start.y)
            start.y = vec.y;

        if(vec.z > end.z)
            end.z = vec.z;
        else if(vec.z < start.z)
            start.z = vec.z;

        return this;
    }

    public AABB move(Vector3i start) {
        end.sub(this.start).add(start);
        this.start.set(start);
        return this;
    }

    public AxisAlignedBB toMc() {
        return new AxisAlignedBB((double) start.x, (double) start.y, (double) start.z, (double) end.x, (double) end.y, (double) end.z);
    }

}
