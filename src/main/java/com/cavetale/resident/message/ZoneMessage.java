package com.cavetale.resident.message;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * A ZoneMessage may have more than one line.
 */
@RequiredArgsConstructor
public final class ZoneMessage {
    private final List<String> lines;

    public String getLine(int index) {
        return lines.get(index);
    }

    public int size() {
        return lines.size();
    }
}
