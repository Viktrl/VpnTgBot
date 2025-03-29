package me.viktrl.VpnTgBot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import me.viktrl.VpnTgBot.config.BotConfig;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Requests extends TelegramBot {

    public Requests(BotConfig config) {
        super(config);
    }

    static Map<String, Long> getUsedTrafficByUser() throws IOException {
        Map<String, Long> trafficData = new LinkedHashMap<>();

        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl + "/metrics/transfer").openConnection();
        connection.setRequestMethod("GET");
        removeSSLVerifier(connection);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(connection.getInputStream());

        JsonNode userTraffic = jsonResponse.get("bytesTransferredByUserId");
        if (userTraffic != null) {
            for (Iterator<String> it = userTraffic.fieldNames(); it.hasNext(); ) {
                String userId = it.next();
                long bytesTransferred = userTraffic.get(userId).asLong();
                trafficData.put(String.valueOf(userId), bytesTransferred);
            }
        }

        return trafficData;
    }

    static void deleteKey(String keyId) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl + "/access-keys/" + keyId).openConnection();
        connection.setRequestMethod("DELETE");
        removeSSLVerifier(connection);
    }

    static void registerKey(String requestBody) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl + "/access-keys").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        removeSSLVerifier(connection);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private static void removeSSLVerifier(@NonNull HttpsURLConnection connection) {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                    }
                }
        };

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            System.out.println(e.getMessage());
        }
        connection.setSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier validHosts = (arg0, arg1) -> true;

        connection.setHostnameVerifier(validHosts);
    }
}
