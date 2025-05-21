package org.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

public class AdminVerticle extends AbstractVerticle {
    private final WebClient webClient;
    private final HttpClient httpClient;
    private WebSocket userWebSocket;
    private WebSocket bikeWebSocket;
    private WebSocket stationWebSocket;
    private final Vertx vertx;
    private static final int PORT = 8080;
    private static final String ADDRESS = "localhost";

    public AdminVerticle(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.httpClient = vertx.createHttpClient();
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("AdminVerticle deployed successfully with ID: " + id);
            setupWebSocketConnections();
        }).onFailure(err -> {
            System.out.println("Failed to deploy AdminVerticle: " + err.getMessage());
        });
    }

    private void setupWebSocketConnections() {
        httpClient.webSocket(PORT, ADDRESS, "/USER-MICROSERVICE/observeAllUsers")
            .onSuccess(ws -> {
                System.out.println("Connected to user updates WebSocket");
                userWebSocket = ws;
                ws.textMessageHandler(this::handleUserUpdate);
            });

        // Connect to bike location updates WebSocket
        httpClient.webSocket(PORT, ADDRESS, "/MAP-MICROSERVICE/observeAllBikes")
            .onSuccess(ws -> {
                bikeWebSocket = ws;
                ws.textMessageHandler(message -> {
                    //System.out.println("bike Admin UPD: " + message);
                    vertx.eventBus().publish("admin.bike.update", new JsonArray(message));
                });
            });

        WebSocketUtils.connectToStationsWebSocket(
                vertx, httpClient, PORT, ADDRESS, ws -> stationWebSocket = ws
        );
    }

    private void handleUserUpdate(String message) {
        JsonObject update = new JsonObject(message);
        vertx.eventBus().publish("admin.user.update", update);
    }

    @Override
    public void start() {
        // Handle create bike requests
        vertx.eventBus().consumer("admin.bike.create", message -> {
            JsonObject bikeDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/EBIKE-MICROSERVICE/api/ebikes/create")
                .sendJsonObject(bikeDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 201) {
                        message.reply(ar.result().bodyAsJsonObject());
                    } else {
                        message.fail(500, "Failed to create bike: " + 
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("admin.abike.create", message -> {
            JsonObject bikeDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/ABIKE-MICROSERVICE/api/abikes/create")
                    .sendJsonObject(bikeDetails, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 201) {
                            message.reply(ar.result().bodyAsJsonObject());
                        } else {
                            message.fail(500, "Failed to create bike: " +
                                    (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
        });

        vertx.eventBus().consumer("admin.bike.recharge", message -> {
            JsonObject rechargeDetails = (JsonObject) message.body();
            String bikeId = rechargeDetails.getString("bikeId");
            webClient.put(PORT, ADDRESS, "/EBIKE-MICROSERVICE/api/ebikes/" + bikeId + "/recharge")
                .sendJsonObject(rechargeDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsJsonObject());
                    } else {
                        message.fail(500, "Failed to recharge bike: " + 
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });
    }

    @Override
    public void stop() {
        if (userWebSocket != null) {
            userWebSocket.close();
        }
        if (bikeWebSocket != null) {
            bikeWebSocket.close();
        }
    }
}
