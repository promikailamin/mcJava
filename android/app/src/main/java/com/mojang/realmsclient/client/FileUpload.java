package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileUpload implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_RETRIES = 5;
   private static final String UPLOAD_PATH = "/upload";
   private final File file;
   private final long realmId;
   private final int slotId;
   private final UploadInfo uploadInfo;
   private final String sessionId;
   private final String username;
   private final String clientVersion;
   private final String worldVersion;
   private final UploadStatus uploadStatus;

   public FileUpload(
      final File file,
      final long realmId,
      final int slotId,
      final UploadInfo uploadInfo,
      final User user,
      final String clientVersion,
      final String worldVersion,
      final UploadStatus uploadStatus
   ) {
      this.file = file;
      this.realmId = realmId;
      this.slotId = slotId;
      this.uploadInfo = uploadInfo;
      this.sessionId = user.getSessionId();
      this.username = user.getName();
      this.clientVersion = clientVersion;
      this.worldVersion = worldVersion;
      this.uploadStatus = uploadStatus;
   }

   @Override
   public void close() {
   }

   public CompletableFuture<UploadResult> startUpload() {
      long fileSize = this.file.length();
      this.uploadStatus.setTotalBytes(fileSize);
      return CompletableFuture.supplyAsync(() -> {
         for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
               connection = (HttpURLConnection) this.uploadInfo
                  .uploadEndpoint()
                  .resolve("/upload/" + this.realmId + "/" + this.slotId)
                  .toURL()
                  .openConnection();
               connection.setRequestMethod("POST");
               connection.setConnectTimeout((int) Duration.ofSeconds(15L).toMillis());
               connection.setReadTimeout((int) Duration.ofMinutes(10L).toMillis());
               connection.setInstanceFollowRedirects(true);
               connection.setDoOutput(true);
               connection.setRequestProperty("Cookie", this.uploadCookie());
               connection.setRequestProperty("Content-Type", "application/octet-stream");
               connection.setRequestProperty("Content-Length", Long.toString(fileSize));
               try (
                  InputStream in = new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
                  OutputStream out = connection.getOutputStream();
               ) {
                  in.transferTo(out);
               }

               int statusCode = connection.getResponseCode();
               String body = readBody(connection);
               long retryDelaySeconds = connection.getHeaderFieldLong("Retry-After", 0L);
               connection.disconnect();
               connection = null;
               if (this.shouldRetry(retryDelaySeconds, attempt)) {
                  this.uploadStatus.restart();
                  Thread.sleep(Duration.ofSeconds(retryDelaySeconds));
               } else {
                  return this.handleResponse(statusCode, body);
               }
            } catch (Exception e) {
               LOGGER.error("Failed to upload world to Realms", e);
               return new UploadResult(0, e.getMessage());
            } finally {
               if (connection != null) {
                  connection.disconnect();
               }
            }
         }

         return new UploadResult(0, "upload_failed");
      });
   }

   private static String readBody(final HttpURLConnection connection) throws IOException {
      try (InputStream is = connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream()) {
         if (is == null) {
            return "";
         }

         return new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
   }

   private String uploadCookie() {
      return "sid="
         + this.sessionId
         + ";token="
         + this.uploadInfo.token()
         + ";user="
         + this.username
         + ";version="
         + this.clientVersion
         + ";worldVersion="
         + this.worldVersion;
   }

   private UploadResult handleResponse(final int statusCode, final String body) {
      if (statusCode == 401) {
         LOGGER.debug("Realms server returned 401");
      }

      String errorMessage = null;
      if (body != null && !body.isBlank()) {
         try {
            JsonElement errorMsgElement = LenientJsonParser.parse(body).getAsJsonObject().get("errorMsg");
            if (errorMsgElement != null) {
               errorMessage = errorMsgElement.getAsString();
            }
         } catch (Exception e) {
            LOGGER.warn("Failed to parse response {}", body, e);
         }
      }

      return new UploadResult(statusCode, errorMessage);
   }

   private boolean shouldRetry(final long retryDelaySeconds, final int currentAttempt) {
      return retryDelaySeconds > 0L && currentAttempt + 1 < 5;
   }

   private static class UploadCountingInputStream extends CountingInputStream {
      private final UploadStatus uploadStatus;

      private UploadCountingInputStream(final InputStream proxy, final UploadStatus uploadStatus) {
         super(proxy);
         this.uploadStatus = uploadStatus;
      }

      protected void afterRead(final int n) throws IOException {
         super.afterRead(n);
         this.uploadStatus.onWrite(this.getByteCount());
      }
   }
}
