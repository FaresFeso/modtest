package com.bazaaralert.client.util;

import java.lang.reflect.Method;




// Using reflection-friendly signatures to avoid compile-time dependency on ItemStack class
public final class HypixelItemUtils {
	private HypixelItemUtils() {
	}

	/**
	 * Extracts the SkyBlock item id from the item's NBT-like custom data.
	 *
	 * This code must compile against multiple Minecraft mappings/versions.
	 * Newer versions expose DataComponentTypes/CustomData; older ones use NBT directly.
	 */
	public static String getSkyblockItemId(Object stack) {
		if (stack == null) {
			return null;
		}

		// Try to call ItemStack#isEmpty via reflection if it exists.
		try {
			Method isEmpty = stack.getClass().getMethod("isEmpty");
			Object emptyResult = isEmpty.invoke(stack);
			if (emptyResult instanceof Boolean b && b) {
				return null;
			}
		} catch (Throwable ignored) {
			// If we can't determine emptiness, continue.
		}


		// Try the modern DataComponentTypes/CUSTOM_DATA path using reflection.
		try {
			Class<?> dataComponentTypes = Class.forName("net.minecraft.component.DataComponentTypes");
			Object customDataType = dataComponentTypes.getField("CUSTOM_DATA").get(null);

			Object customData = stack.getClass().getMethod("get", Class.forName("net.minecraft.component.type.DataComponentType"))
					.invoke(stack, customDataType);
			if (customData == null) {
				return null;
			}

			Method copyTag = customData.getClass().getMethod("copyTag");
			Object root = copyTag.invoke(customData);
			if (root == null) {
				return null;
			}

			Method contains = root.getClass().getMethod("contains", String.class);
			if (!(Boolean) contains.invoke(root, "ExtraAttributes")) {
				return null;
			}

			Method getCompoundOrEmpty = root.getClass().getMethod("getCompoundOrEmpty", String.class);
			Object extraAttributes = getCompoundOrEmpty.invoke(root, "ExtraAttributes");
			if (extraAttributes == null) {
				return null;
			}

			// getStringOr("id", "") exists on NbtCompound in newer mappings.
			try {
				Method getStringOr = extraAttributes.getClass().getMethod("getStringOr", String.class, String.class);
				String id = (String) getStringOr.invoke(extraAttributes, "id", "");
				return (id == null || id.isEmpty()) ? null : id;
			} catch (NoSuchMethodException ignored) {
				// Fallback for older NBT APIs: getString("id")
				Method getString = extraAttributes.getClass().getMethod("getString", String.class);
				String id = (String) getString.invoke(extraAttributes, "id");
				return (id == null || id.isEmpty()) ? null : id;
			}
		} catch (Throwable ignored) {
			// Continue to legacy NBT path.
		}

		// Legacy path: try stack.getNbt() / stack.getTag() via reflection.
		try {
			Object tag = null;
			try {
				Method getNbt = stack.getClass().getMethod("getNbt");
				tag = getNbt.invoke(stack);
			} catch (NoSuchMethodException e) {
				Method getTag = stack.getClass().getMethod("getTag");
				tag = getTag.invoke(stack);
			}

			if (tag == null) {
				return null;
			}

			Method contains = tag.getClass().getMethod("contains", String.class);
			if (!(Boolean) contains.invoke(tag, "ExtraAttributes")) {
				return null;
			}

			Method getCompoundOrEmpty = tag.getClass().getMethod("getCompoundOrEmpty", String.class);
			Object extraAttributes = getCompoundOrEmpty.invoke(tag, "ExtraAttributes");
			if (extraAttributes == null) {
				return null;
			}

			Method getStringOr = extraAttributes.getClass().getMethod("getStringOr", String.class, String.class);
			String id = (String) getStringOr.invoke(extraAttributes, "id", "");
			return (id == null || id.isEmpty()) ? null : id;
		} catch (Throwable ignored) {
			return null;
		}
	}
}

