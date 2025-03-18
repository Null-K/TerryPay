package com.puddingkc.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class JsonUtils {

    // 感谢 人间工作

    public static Integer optInt(@Nullable JsonObject obj, @NotNull String key, Integer def) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isNumber() ? Integer.valueOf(primitive.getAsInt()) : def;
        }
        return def;
    }

    public static String optString(@Nullable JsonObject obj, @NotNull String key, String def) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isString() ? primitive.getAsString() : def;
        }
        return def;
    }

    public static JsonObject optObject(@Nullable JsonObject obj, @NotNull String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    public static JsonArray optArray(@Nullable JsonObject obj, @NotNull String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return new JsonArray();
    }

}
