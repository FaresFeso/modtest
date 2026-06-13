package com.bazaaralert.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

public final class HypixelSkyblockUtils {
	private HypixelSkyblockUtils() {
	}

	public static boolean isOnHypixel() {
		Minecraft client = Minecraft.getInstance();
		if (client.getCurrentServer() == null) {
			return false;
		}

		String address = client.getCurrentServer().ip.toLowerCase();
		return address.contains("hypixel");
	}

	public static boolean isLikelyInSkyblock() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return false;
		}

		Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) {
			return false;
		}

		String title = objective.getDisplayName().getString().toUpperCase();
		return title.contains("SKYBLOCK") || title.contains("SKIBLOCK");
	}
}
