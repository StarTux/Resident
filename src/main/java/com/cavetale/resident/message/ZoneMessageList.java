package com.cavetale.resident.message;

import java.util.ArrayList;
import java.util.List;

public final class ZoneMessageList {
    protected final List<ZoneMessage> messages = new ArrayList<>();

    public static ZoneMessageList ofConfig(List<Object> configList) {
        ZoneMessageList result = new ZoneMessageList();
        result.loadConfig(configList);
        return result;
    }

    public void loadConfig(List<Object> configList) {
        if (configList == null) configList = List.of();
        for (Object item : configList) {
            if (item instanceof String) {
                messages.add(new ZoneMessage(List.of((String) item)));
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) item;
                if (list.isEmpty()) {
                    throw new IllegalArgumentException("message is empty!");
                }
                messages.add(new ZoneMessage(list));
            }
        }
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int size() {
        return messages.size();
    }

    public ZoneMessage get(int index) {
        return messages.get(index);
    }
}
