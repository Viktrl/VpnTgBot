package me.viktrl.VpnTgBot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.viktrl.VpnTgBot.config.BotConfig;
import me.viktrl.VpnTgBot.service.POJOs.CreateKeyRequest;
import me.viktrl.VpnTgBot.service.POJOs.KeyResponse;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Requests extends TelegramBot {

    static ObjectMapper objectMapper = new ObjectMapper();

    public Requests(BotConfig config) {
        super(config);
    }

    static Map<String, Long> getUsedTrafficByUser() throws IOException {
        Map<String, Long> trafficData = new LinkedHashMap<>();

        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl + "/metrics/transfer").openConnection();
        connection.setRequestMethod("GET");
        removeSSLVerifier(connection);

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

    static KeyResponse registerKey(String name) {
        return new GsonBuilder().setPrettyPrinting().create().fromJson(getResponse("access-keys", "POST", new GsonBuilder().setPrettyPrinting().create().toJson(new CreateKeyRequest(name))).responseBody, KeyResponse.class);
    }

    static boolean deleteKey(String keyId) {
        return getResponse("/access-keys/" + keyId, "DELETE", null).responseCode == 204;
    }

    static KeyResponse getAccessKey(String id) {
        return new GsonBuilder().setPrettyPrinting().create().fromJson(getResponse("access-keys" + id, "GET", null).responseBody, KeyResponse.class);
    }

    @SneakyThrows
    private static Response getResponse(@NonNull String endpoint, @NonNull String method, String requestBody) {
        try {
            URL url = new URL(apiUrl + endpoint);

            HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
            httpConn.setRequestMethod(method);
            removeSSLVerifier(httpConn);

            if (requestBody != null) {
                httpConn.setDoOutput(true);
                httpConn.setRequestProperty("Content-Type", "application/json");
                OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
                writer.write(requestBody);
                writer.flush();
                writer.close();
                httpConn.getOutputStream().close();
            }

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";

            return new Response(httpConn.getResponseCode(), response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
