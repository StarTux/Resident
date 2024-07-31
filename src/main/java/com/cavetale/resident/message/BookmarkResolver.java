package com.cavetale.resident.message;

import com.cavetale.core.font.DefaultFont;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import static net.kyori.adventure.text.Component.textOfChildren;

public final class BookmarkResolver implements TagResolver {
    private static BookmarkResolver instance;

    public static BookmarkResolver bookmarkResolver() {
        if (instance == null) {
            instance = new BookmarkResolver();
        }
        return instance;
    }

    @Override
    public boolean has(String name) {
        return name.equals("bookmark");
    }

    @Override
    public Tag resolve(String name, ArgumentQueue arguments, Context ctx) {
        if (!has(name)) return null;
        final var arg = arguments.pop();
        final TextColor color;
        if (arg.value() == null) {
            color = NamedTextColor.BLACK;
        } else if (arg.value().startsWith("#")) {
            color = TextColor.fromCSSHexString(arg.value());
        } else {
            color = NamedTextColor.NAMES.value(arg.value().toUpperCase());
        }
        return Tag.selfClosingInserting(textOfChildren(DefaultFont.BOOK_MARKER.component.color(color),
                                                       DefaultFont.BACKSPACE_MARKER.component));
    }
}
