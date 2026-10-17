package dev.kissoo.mod.modules.impl.client.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class SkinFetcher {
   private static final String USER_AGENT = "ForeverClient/1.0";
   private static final Map<String, SkinCacheEntry> CACHE = new HashMap<>();

   public static SkinCacheEntry fetchSkin(String playerName) {
      String key = playerName.toLowerCase();
      SkinCacheEntry cached = CACHE.get(key);
      if (cached != null) {
         if (!cached.isExpired()) {
            return cached;
         }

         CACHE.remove(key);
      }

      try {
         String uuid = fetchUUID(playerName);
         if (uuid == null) {
            return null;
         }

         SkinInfo skinInfo = fetchSkinInfo(uuid);
         if (skinInfo == null) {
            return null;
         }

         NativeImage image = downloadSkin(skinInfo);
         if (image == null) {
            return null;
         }

         Identifier identifier = Identifier.of("forever", "skin/custom_" + sanitizePath(key));
         registerTexture(identifier, image);
         SkinCacheEntry entry = new SkinCacheEntry(identifier, skinInfo.slim(), playerName, System.currentTimeMillis());
         CACHE.put(key, entry);
         return entry;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static SkinCacheEntry getCached(String playerName) {
      String key = playerName.toLowerCase();
      SkinCacheEntry entry = CACHE.get(key);
      if (entry != null && !entry.isExpired()) {
         return entry;
      }

      if (entry != null) {
         CACHE.remove(key);
      }

      return null;
   }

   public static void clearCache(String playerName) {
      CACHE.remove(playerName.toLowerCase());
   }

   public static void clearAllCache() {
      CACHE.clear();
   }

   private static String fetchUUID(String playerName) throws Exception {
      URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName).toURL();
      HttpURLConnection connection = openConnection(url, 5000);

      try {
         if (connection.getResponseCode() != 200) {
            return null;
         }

         JsonObject object = readJson(connection);
         return object.has("id") ? object.get("id").getAsString() : null;
      } finally {
         connection.disconnect();
      }
   }

   private static SkinInfo fetchSkinInfo(String uuid) throws Exception {
      String formattedUUID = uuid.replace("-", "");
      URL url = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + formattedUUID).toURL();
      HttpURLConnection connection = openConnection(url, 5000);

      try {
         if (connection.getResponseCode() != 200) {
            return null;
         }

         JsonObject root = readJson(connection);
         JsonArray properties = root.getAsJsonArray("properties");
         if (properties == null) {
            return null;
         }

         String encodedValue = null;
         for (int i = 0; i < properties.size(); i++) {
            JsonObject object = properties.get(i).getAsJsonObject();
            if (object.has("name") && "textures".equals(object.get("name").getAsString())) {
               encodedValue = object.has("value") ? object.get("value").getAsString() : null;
               break;
            }
         }

         if (encodedValue == null) {
            return null;
         }

         String decoded = new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
         JsonObject textureJson = JsonParser.parseString(decoded).getAsJsonObject();
         JsonObject textures = textureJson.getAsJsonObject("textures");
         if (textures == null) {
            return null;
         }

         JsonObject skin = textures.getAsJsonObject("SKIN");
         if (skin == null || !skin.has("url")) {
            return null;
         }

         boolean slim = false;
         JsonObject metadata = skin.getAsJsonObject("metadata");
         if (metadata != null && metadata.has("model")) {
            slim = "slim".equals(metadata.get("model").getAsString());
         }

         return new SkinInfo(skin.get("url").getAsString(), slim);
      } finally {
         connection.disconnect();
      }
   }

   private static NativeImage downloadSkin(SkinInfo info) throws Exception {
      URL url = URI.create(info.url()).toURL();
      HttpURLConnection connection = openConnection(url, 10000);

      try {
         if (connection.getResponseCode() != 200) {
            return null;
         }

         return NativeImage.read(connection.getInputStream());
      } finally {
         connection.disconnect();
      }
   }

   private static HttpURLConnection openConnection(URL url, int readTimeout) throws Exception {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(readTimeout);
      connection.setRequestProperty("User-Agent", USER_AGENT);
      return connection;
   }

   private static JsonObject readJson(HttpURLConnection connection) throws Exception {
      try (InputStream inputStream = connection.getInputStream()) {
         String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
         return JsonParser.parseString(json).getAsJsonObject();
      }
   }

   private static void registerTexture(Identifier id, NativeImage image) {
      MinecraftClient mc = MinecraftClient.getInstance();
      mc.execute(() -> {
         try {
            mc.getTextureManager().destroyTexture(id);
         } catch (Exception ignored) {
         }

         mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
      });
   }

   private static String sanitizePath(String value) {
      return value.replaceAll("[^a-z0-9/._-]", "_");
   }

   private record SkinInfo(String url, boolean slim) {
   }
}
