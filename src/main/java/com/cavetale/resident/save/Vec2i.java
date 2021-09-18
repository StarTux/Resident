package com.cavetale.resident.save;

import lombok.Value;

@Value
public final class Vec2i {
    static final Vec2i ZERO = new Vec2i(0, 0);
    public final int x;
    public final int y;

    public static Vec2i of(int x, int y) {
        return new Vec2i(x, y);
    }
}
