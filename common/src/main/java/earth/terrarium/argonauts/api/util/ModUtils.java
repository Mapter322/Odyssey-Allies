package earth.terrarium.argonauts.api.util;

import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

public class ModUtils {

    public static Color uuidToColor(UUID id) {
        return MinecraftColors.COLORS[Math.abs(id.hashCode()) % MinecraftColors.COLORS.length];
    }

    public static Component translatableWithStyle(String key, Object... args) {
        for (int i = 0; i < args.length; ++i) {
            if (!(args[i] instanceof MutableComponent component)) continue;
            if (component.getStyle().getColor() == null) continue;

            ChatFormatting color = ChatFormatting.getByName(component.getStyle().getColor().toString());
            if (color != null) {
                args[i] = "§" + color.getChar() + component.getString();
            }
        }

        return Component.literal(CommonUtils.serverTranslatable(key, args).getString());
    }
}
