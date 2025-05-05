package org.verticles;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;

public class WebSocketUtils {
    public static void connectToStationsWebSocket(
            Vertx vertx,
            HttpClient httpClient,
            int port,
            String address,
            java.util.function.Consumer<WebSocket> onConnect
    ) {
        httpClient.webSocket(port, address, "/MAP-MICROSERVICE/observeStations")
            .onSuccess(ws -> {
                System.out.println("Connected to stations updates WebSocket");
                ws.textMessageHandler(message -> {
                    vertx.eventBus().publish("stations.update", new JsonArray(message));
                });
                ws.exceptionHandler(err -> {
                    System.out.println("Stations WebSocket error: " + err.getMessage());
                });
                if (onConnect != null) onConnect.accept(ws);
            }).onFailure(err -> {
                System.out.println("Failed to connect to stations updates WebSocket: " + err.getMessage());
            });
    }
}