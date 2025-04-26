package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class UserServiceIntegrationTest {
  private static Vertx vertx;
  private static WebClient client;

  @BeforeAll
  static void setUp() {
    String command = "cd src/test/java/it/unibo/ && docker compose up -d --build";
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("bash", "-c", command);

    try {
      Process process = processBuilder.start();

      // Redirect the process's input and error streams to the console
      new Thread(
              () -> {
                try (var reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                }
              })
          .start();

      new Thread(
              () -> {
                try (var reader =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                }
              })
          .start();

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

    vertx.setPeriodic(
        1000,
        id -> { // Poll every 1 second
          client
              .get(8084, "localhost", "/health")
              .send(
                  ar -> {
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
      healthCheckFuture.get(25, TimeUnit.SECONDS);
      System.out.println("Health check succeeded in time");
    } catch (Exception e) {
      throw new RuntimeException("Health check did not succeed in time", e);
    }
  }

  @Test
  void testSignUpAndSignIn() {
    System.out.println("Testing sign Up and sign In");
    registerUser("testuser", "USER").join();
    signIn("testuser").join();
  }

  private static CompletableFuture<Void> signIn(String username) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    JsonObject user = new JsonObject().put("username", username);

    client
        .post(8084, "localhost", "/api/users/signin")
        .sendJsonObject(
            user,
            ar -> {
              if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                System.out.println("User sign in SUCCEEDED: " + response.bodyAsString());
                assertEquals(200, response.statusCode());
                future.complete(null);
              } else {
                System.err.println("User sign in FAILED: " + ar.cause().getMessage());
                future.completeExceptionally(ar.cause());
              }
            });

    return future;
  }

  private static CompletableFuture<Void> registerUser(String username, String type) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    JsonObject user = new JsonObject().put("username", username).put("type", type);

    client
        .post(8084, "localhost", "/api/users/signup")
        .sendJsonObject(
            user,
            ar -> {
              if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                System.out.println("User sign up SUCCEEDED: " + response.bodyAsString());
                assertEquals(201, response.statusCode());
                future.complete(null);
              } else {
                System.err.println("User sign up FAILED: " + ar.cause().getMessage());
                future.completeExceptionally(ar.cause());
              }
            });

    return future;
  }

  @AfterAll
  static void tearDown() {
    String command = "cd src/test/java/it/unibo/ && docker compose down -v";
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("bash", "-c", command);

    try {

      Process process = processBuilder.start();

      // Redirect the process's input and output streams to the console
      new Thread(
              () -> {
                try (var reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                }
              })
          .start();

      new Thread(
              () -> {
                try (var reader =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                }
              })
          .start();

      int exitCode = process.waitFor();
      System.out.println("Exit code: " + exitCode);
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
