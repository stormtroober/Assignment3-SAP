package it.unibo;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EBikeSystemE2ETest {

    private static Vertx vertx;
    private static WebClient client;

    @BeforeAll
    static void setUp() {
        String command = "cd ../../ && docker compose up -d --build";
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        try {
            Process process = processBuilder.start();

            // Redirect the process's input and error streams to the console
            new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int exitCode = process.waitFor();
            System.out.println("Docker Compose Exit code: " + exitCode);

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        vertx = Vertx.vertx();
        client = WebClient.create(vertx);

        // Wait for health check to succeed
        waitForHealthCheck();
    }

    private static void waitForHealthCheck() {
        CompletableFuture<Void> healthCheckFuture = new CompletableFuture<>();

        vertx.setPeriodic(1000, id -> { // Poll every 1 second
            client.get(8080, "localhost", "/actuator/health")
                    .send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> response = ar.result();
                            String body = response.bodyAsString();
                            if (response.statusCode() == 200 && body.contains("\"status\":\"UP\"")) {
                                System.out.println("Health check SUCCEEDED: " + body);
                                healthCheckFuture.complete(null); // Complete the future on success
                                vertx.cancelTimer(id); // Cancel the periodic task
                            } else {
                                System.err.println("Health check response but not ready: " + body);
                            }
                        } else {
                            System.err.println("Health check failed: " + ar.cause().getMessage());
                        }
                    });
        });

    try {
        // Wait for the health check to succeed or timeout after 60 seconds
        healthCheckFuture.get(100, TimeUnit.SECONDS);
        System.out.println("Health check succeeded in time");
    } catch (Exception e) {
        throw new RuntimeException("Health check did not succeed in time", e);
    }
}

    @Test
    void testEBikeCreationAndArrive() {
        var httpClient = vertx.createHttpClient();

        // Register users after health check succeeds
        CompletableFuture<Void> adminFuture = registerUser("admin", "ADMIN");
        CompletableFuture<Void> userFuture = registerUser("user", "USER");

        // Wait for user registration to complete
        CompletableFuture.allOf(adminFuture, userFuture).thenCompose(v -> {
            // Create an eBike after user registration
            return createEBike("bike1", 10.0, 20.0, "AVAILABLE", 100);
        }).thenAccept(v -> {
            // Connect to WebSocket and verify the received message
            httpClient.webSocket(8080, "localhost", "/MAP-MICROSERVICE/observeUserBikes?username=user")
                    .onSuccess(ws -> {
                        ws.textMessageHandler(message -> {
                            System.out.print("Received WebSocket message: " + message);
                            JsonArray receivedArray = new JsonArray(message);
                            String bikeString = receivedArray.getString(0);
                            JsonObject receivedBike = new JsonObject(bikeString);

                            JsonObject expectedBike = new JsonObject()
                                .put("bikeName", "bike1")
                                .put("position", new JsonObject()
                                    .put("x", 10.0)
                                    .put("y", 20.0))
                                .put("state", "AVAILABLE")
                                .put("batteryLevel", 100);

                            assertEquals(expectedBike, receivedBike);
                        });
                    });
        }).join(); // Wait for all operations to complete
    }

    private static CompletableFuture<Void> registerUser(String username, String type) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject user = new JsonObject()
                .put("username", username)
                .put("type", type);

        client.post(8080, "localhost", "/USER-MICROSERVICE/api/users/signup")
                .sendJsonObject(user, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("User registration SUCCEEDED: " + ar.result().bodyAsString());
                        future.complete(null);
                    } else {
                        System.err.println("User registration failed: " + ar.cause().getMessage());
                        future.completeExceptionally(ar.cause());
                    }
                });
        return future;
    }

    private static CompletableFuture<Void> createEBike(String id, double x, double y, String status, int batteryLevel) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject ebike = new JsonObject()
                .put("id", id)
                .put("x", x)
                .put("y", y)
                .put("status", status)
                .put("batteryLevel", batteryLevel);

        client.post(8080, "localhost", "/EBIKE-MICROSERVICE/api/ebikes/create")
                .sendJsonObject(ebike, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("EBike creation SUCCEEDED: " + ar.result().bodyAsString());
                        future.complete(null);
                    } else {
                        System.err.println("EBike creation failed: " + ar.cause().getMessage());
                        future.completeExceptionally(ar.cause());
                    }
                });
        return future;
    }

    @AfterAll
    static void tearDown() {
        String command = "cd ../../ && docker-compose down -v";
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        try {

            Process process = processBuilder.start();

            // Redirect the process's input and output streams to the console
            new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}