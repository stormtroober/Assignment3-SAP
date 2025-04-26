package org.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class UserManagementVerticle extends AbstractVerticle {

    private final WebClient webClient;
    private final Vertx vertx;

    public UserManagementVerticle(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("UserManagementVerticle deployed successfully with ID: " + id);
        }).onFailure(err -> {
            System.out.println("Failed to deploy UserManagementVerticle: " + err.getMessage());
        });
    }

    @Override
    public void start() {
        vertx.eventBus().consumer("user.login", message -> {
            String username = (String) message.body();
            JsonObject requestPayload = new JsonObject().put("username", username);

            webClient.post(8080, "localhost", "/USER-MICROSERVICE/api/users/signin")
                    .sendJsonObject(requestPayload, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 200) {
                            JsonObject response = ar.result().bodyAsJsonObject();
                            message.reply(response);
                        } else {
                            message.fail(500, "Failed to connect to API: " + (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
        });
        vertx.eventBus().consumer("user.register", message -> {
            JsonObject userDetails = (JsonObject) message.body();
            webClient.post(8080, "localhost", "/USER-MICROSERVICE/api/users/signup")
                    .sendJsonObject(userDetails, ar -> {
                        if (ar.succeeded() && ar.result().statusCode() == 201) {
                            message.reply(userDetails);
                        } else {
                            message.fail(500, "Registration failed: " + (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
        });
    }
}