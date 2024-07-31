package com.cavetale.resident.message;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import static com.cavetale.core.font.Emoji.getEmoji;

public final class EmojiResolver implements TagResolver {
    private static EmojiResolver instance;

    public static EmojiResolver emojiResolver() {
        if (instance == null) {
            instance = new EmojiResolver();
        }
        return instance;
    }

    @Override
    public boolean has(String name) {
        return name.equals("emoji");
    }

    @Override
    public Tag resolve(String name, ArgumentQueue arguments, Context ctx) {
        if (!has(name)) return null;
        final var arg = arguments.pop();
        if (arg == null) return null;
        final var emoji = getEmoji(arg.value());
        return Tag.selfClosingInserting(emoji.getComponent());
    }
}
