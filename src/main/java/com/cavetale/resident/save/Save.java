package com.cavetale.resident.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class Save {
    protected final List<Zone> zones = new ArrayList<>();
}
