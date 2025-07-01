package infrastructure.adapters.web;

import application.ports.UserServiceAPI;
import domain.model.User;
import domain.model.UserType;
import infrastructure.utils.MetricsManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTUserAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RESTUserAdapter.class);
  private final UserServiceAPI userService;
  private final Vertx vertx;
  private final MetricsManager metricsManager;

  public RESTUserAdapter(UserServiceAPI userService, Vertx vertx) {
    this.userService = userService;
    this.vertx = vertx;
    this.metricsManager = MetricsManager.getInstance();
  }

  public void configureRoutes(Router router) {
    router.post("/api/users/signin").handler(this::signIn);
    router.post("/api/users/signup").handler(this::signUp);
    router.patch("/api/users/:username/recharge").handler(this::rechargeCredit);
    router.get("/health").handler(this::healthCheck);
    router.route("/observeAllUsers").handler(this::observeAllUsers);
    router.route("/observeUser/:username").handler(this::observeUser);
    router.get("/metrics").handler(this::metrics);
  }

  private void metrics(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader("Content-Type", "text/plain")
        .end(metricsManager.getMetrics());
  }

  private void signIn(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("signIn");
    var timer = metricsManager.startTimer();
    try {
      JsonObject body = ctx.body().asJsonObject();
      String username = body.getString("username");

      if (username == null || username.trim().isEmpty()) {
        sendError(ctx, 400, "Invalid username");
        metricsManager.recordError(timer, "signIn", new RuntimeException("Invalid username"));
        return;
      }

      userService
          .signIn(username)
          .thenAccept(
              result -> {
                sendResponse(ctx, 200, result.toJson());
                metricsManager.recordTimer(timer, "signIn");
              })
          .exceptionally(
              e -> {
                if (e.getCause() instanceof RuntimeException
                    && e.getCause().getMessage().equals("User not found")) {
                  sendError(ctx, 404, "User not found");
                } else {
                  handleError(ctx, e);
                }
                metricsManager.recordError(timer, "signIn", e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
      metricsManager.recordError(timer, "signIn", e);
    }
  }

  private void signUp(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("signUp");
    var timer = metricsManager.startTimer();
    try {
      System.out.println("Sign up received");
      System.out.println(ctx.body().toString());
      JsonObject body = ctx.body().asJsonObject();
      String username = body.getString("username");
      String type = body.getString("type");

      userService
          .signUp(username, UserType.valueOf(type))
          .thenAccept(
              result -> {
                sendResponse(ctx, 201, result.toJson());
                metricsManager.recordTimer(timer, "signUp");
              })
          .exceptionally(
              e -> {
                if (e.getCause() instanceof RuntimeException
                    && e.getCause().getMessage().equals("User already exists")) {
                  sendError(ctx, 409, "User already exists");
                } else {
                  handleError(ctx, e);
                }
                metricsManager.recordError(timer, "signUp", e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
      metricsManager.recordError(timer, "signUp", e);
    }
  }

  private void rechargeCredit(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("rechargeCredit");
    var timer = metricsManager.startTimer();
    JsonObject body = ctx.body().asJsonObject();
    String username = ctx.pathParam("username");
    if (username == null || username.trim().isEmpty()) {
      sendError(ctx, 400, "Invalid username");
      metricsManager.recordError(timer, "rechargeCredit", new RuntimeException("Invalid username"));
      return;
    }
    int creditToAdd = body.getInteger("creditToAdd");

    userService
        .rechargeCredit(username, creditToAdd)
        .thenAccept(
            result -> {
              if (result != null) {
                sendResponse(ctx, 200, result.toJson());
                metricsManager.recordTimer(timer, "rechargeCredit");
              } else {
                ctx.response().setStatusCode(404).end();
                metricsManager.recordError(
                    timer, "rechargeCredit", new RuntimeException("User not found"));
              }
            })
        .exceptionally(
            e -> {
              handleError(ctx, e);
              metricsManager.recordError(timer, "rechargeCredit", e);
              return null;
            });
  }

  private void healthCheck(RoutingContext ctx) {
    JsonObject health =
        new JsonObject().put("status", "UP").put("timestamp", System.currentTimeMillis());
    sendResponse(ctx, 200, health);
  }

  private void observeAllUsers(RoutingContext ctx) {
    ctx.request()
        .toWebSocket()
        .onComplete(
            webSocketAsyncResult -> {
              if (webSocketAsyncResult.succeeded()) {
                var webSocket = webSocketAsyncResult.result();
                userService
                    .getAllUsers()
                    .thenAccept(
                        users -> {
                          for (User user : users) {
                            JsonObject userJson = user.toJson();
                            webSocket.writeTextMessage(userJson.encode());
                          }
                        });
                var consumer =
                    vertx
                        .eventBus()
                        .consumer(
                            "users.update",
                            message -> {
                              webSocket.writeTextMessage(message.body().toString());
                            });
                webSocket.closeHandler(v -> consumer.unregister());
                webSocket.exceptionHandler(err -> consumer.unregister());
              } else {
                ctx.response().setStatusCode(500).end("WebSocket Upgrade Failed");
              }
            });
  }

  private void observeUser(RoutingContext ctx) {
    String username = ctx.pathParam("username");
    ctx.request()
        .toWebSocket()
        .onComplete(
            webSocketAsyncResult -> {
              if (webSocketAsyncResult.succeeded()) {
                var webSocket = webSocketAsyncResult.result();
                var consumer =
                    vertx
                        .eventBus()
                        .consumer(
                            username,
                            message -> webSocket.writeTextMessage(message.body().toString()));

                webSocket.closeHandler(v -> consumer.unregister());
                webSocket.exceptionHandler(err -> consumer.unregister());
              } else {
                ctx.response().setStatusCode(500).end("WebSocket Upgrade Failed");
              }
            });
  }

  private void sendResponse(RoutingContext ctx, int statusCode, Object result) {
    ctx.response()
        .setStatusCode(statusCode)
        .putHeader("content-type", "application/json")
        .end(result instanceof String ? (String) result : result.toString());
  }

  private void sendError(RoutingContext ctx, int statusCode, String message) {
    JsonObject error = new JsonObject().put("error", message);
    ctx.response()
        .setStatusCode(statusCode)
        .putHeader("content-type", "application/json")
        .end(error.encode());
  }

  private void handleError(RoutingContext ctx, Throwable e) {
    logger.error("Error processing request", e);
    ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", e.getMessage()).encode());
  }
}
