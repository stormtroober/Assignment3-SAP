package org.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import org.models.CallABikeStatus;

public class UserVerticle extends AbstractVerticle {

    private final WebClient webClient;
    private final HttpClient httpClient;
    private WebSocket userWebSocket;
    private WebSocket bikeWebSocket;
    private WebSocket stationWebSocket;
    private final Vertx vertx;
    private final String username;
    private static final int PORT = 8080;
    private static final String ADDRESS = "localhost";

    public UserVerticle(Vertx vertx, String username) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.httpClient = vertx.createHttpClient();
        this.username = username;
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("UserVerticle deployed successfully with ID: " + id);
            setupWebSocketConnections();
        }).onFailure(err -> {
            System.out.println("Failed to deploy UserVerticle: " + err.getMessage());
        });
    }

    private void setupWebSocketConnections() {
        httpClient.webSocket(PORT, ADDRESS, "/USER-MICROSERVICE/observeUser/" + username)
            .onSuccess(ws -> {
                System.out.println("Connected to user updates WebSocket: " + username);
                userWebSocket = ws;
                ws.textMessageHandler(this::handleUpdateFromUser);
                ws.exceptionHandler(err -> {
                    System.out.println("WebSocket error: " + err.getMessage());
                });
            }).onFailure(err -> {
                System.out.println("Failed to connect to user updates WebSocket: " + err.getMessage());
            });

        httpClient.webSocket(PORT, ADDRESS, "/MAP-MICROSERVICE/observeUserBikes?username=" + username)
            .onSuccess(ws -> {
                System.out.println("Connected to user bikes updates WebSocket");
                bikeWebSocket = ws;
                ws.textMessageHandler(message ->{
                    System.out.println("bike upd USER: " + message);
                        if(!message.contains("rideStatus")) {
                            vertx.eventBus().publish("user.bike.update." + username, new JsonArray(message));
                        }
                        else{
                        vertx.eventBus().publish("user.ride.update." + username, new JsonObject(message));
                    }
                });

            });

        WebSocketUtils.connectToStationsWebSocket(
                vertx, httpClient, PORT, ADDRESS, ws -> stationWebSocket = ws
        );
    }

    private void handleUpdateFromUser(String message){
        JsonObject update = new JsonObject(message);
        if (update.containsKey("positionX")) {
            handleBikeDispatch(message);
        }
        else {
            handleUserUpdate(message);
        }
    }

    private void handleBikeDispatch(String message) {
        JsonObject dispatch = new JsonObject(message);
        // publish on the user’s address so their view sees it
        vertx.eventBus().publish(
                "user.bike.dispatch." + username,
                dispatch
        );

        // Check if this is an "arrived" status and update the CallABikeStatus
        if (dispatch.getString("status", "").equals("arrived")) {
            // Publish a specific message to update the CallABikeStatus
            vertx.eventBus().publish(
                    "user.bike.statusUpdate." + username,
                    new JsonObject().put("callABikeStatus", CallABikeStatus.STOP_RIDE_ABIKE)
            );
        }
        if (dispatch.getString("status", "").equals("notArrived")) {
            // Publish a specific message to update the CallABikeStatus
            vertx.eventBus().publish(
                    "user.bike.statusUpdate." + username,
                    new JsonObject().put("callABikeStatus", CallABikeStatus.CALL_ABIKE)
            );
        }
    }

    //{"username":"ale","type":"USER","credit":38398}
    private void handleUserUpdate(String message) {
        JsonObject update = new JsonObject(message);
        String username = update.getString("username");
        vertx.eventBus().publish("user.update." + username, update);
    }


    @Override
    public void start() {

        vertx.eventBus().consumer("user.update.recharge" + username, message -> {
            JsonObject creditDetails = (JsonObject) message.body();
            webClient.patch(PORT, ADDRESS, "/USER-MICROSERVICE/api/users/" + username + "/recharge")
                .sendJsonObject(creditDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsJsonObject());
                    } else {
                        message.fail(500, "Failed to recharge credit: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("user.ride.start." + username, message -> {
            JsonObject rideDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/startRide")
                    .sendJsonObject(rideDetails, ar -> {
                        if (ar.succeeded()) {
                            if (ar.result().statusCode() == 200) {
                                message.reply(ar.result().bodyAsString());
                            } else {
                                JsonObject errorResponse = ar.result().bodyAsJsonObject();
                                String errorMessage = errorResponse != null ?
                                        errorResponse.getString("error") : "Unknown error";
                                message.fail(ar.result().statusCode(), errorMessage);
                            }
                        } else {
                            message.fail(500, "Failed to start ride: " +
                                    (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
        });

        vertx.eventBus().consumer("user.ride.stop." + username, message -> {
            JsonObject rideDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/stopRide")
                .sendJsonObject(rideDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsString());
                    } else {
                        message.fail(500, "Failed to stop ride: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("user.ride.callBike." + username, message -> {
            JsonObject callDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/rideToUser")
                    .sendJsonObject(callDetails, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 200) {
                            message.reply(ar.result().bodyAsString());
                        } else {
                            String errorMsg = ar.succeeded() && ar.result() != null
                                    ? ar.result().bodyAsString()
                                    : (ar.cause() != null ? ar.cause().getMessage() : "Unknown error");
                            message.fail(500, "Failed to call bike: " + errorMsg);
                        }
                    });
        });

        vertx.eventBus().consumer("user.ride.cancelCall." + username, message -> {
            JsonObject cancelDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/stopRideToUser")
                    .sendJsonObject(cancelDetails, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 200) {
                            message.reply(ar.result().bodyAsString());

                            // Publish a status update to reset the UI state
                            vertx.eventBus().publish(
                                    "user.bike.statusUpdate." + username,
                                    new JsonObject().put("callABikeStatus", CallABikeStatus.CALL_ABIKE)
                            );
                        } else {
                            String errorMsg = ar.succeeded() && ar.result() != null
                                    ? ar.result().bodyAsString()
                                    : (ar.cause() != null ? ar.cause().getMessage() : "Unknown error");
                            message.fail(500, "Failed to cancel bike call: " + errorMsg);
                        }
                    });
        });

        vertx.eventBus().consumer("user.ride.stopARide." + username, message -> {
            JsonObject cancelDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/stopRideToUser")
                    .sendJsonObject(cancelDetails, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 200) {
                            message.reply(ar.result().bodyAsString());

                            // Publish a status update to reset the UI state
                            vertx.eventBus().publish(
                                    "user.bike.statusUpdate." + username,
                                    new JsonObject().put("callABikeStatus", CallABikeStatus.CALL_ABIKE)
                            );
                        } else {
                            String errorMsg = ar.succeeded() && ar.result() != null
                                    ? ar.result().bodyAsString()
                                    : (ar.cause() != null ? ar.cause().getMessage() : "Unknown error");
                            message.fail(500, "Failed to cancel bike call: " + errorMsg);
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
        if (stationWebSocket != null) {
            stationWebSocket.close();
        }
    }
}
