package com.bazaaralert.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class DebugUtil {
	private DebugUtil() {}

	public static void sendChatOrLog(Logger logger, String message) {
		logger.warn("{}", message);
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				mc.player.displayClientMessage(Component.literal(message), false);
			}
		} catch (Throwable ignored) {
			// no-op
		}
	}
}

