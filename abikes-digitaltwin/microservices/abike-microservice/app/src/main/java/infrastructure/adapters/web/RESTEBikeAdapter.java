package infrastructure.adapters.web;

import application.ports.EBikeServiceAPI;
import infrastructure.utils.MetricsManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTEBikeAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RESTEBikeAdapter.class);
  private final EBikeServiceAPI ebikeService;
  private final MetricsManager metricsManager;

  public RESTEBikeAdapter(EBikeServiceAPI ebikeService) {
    this.ebikeService = ebikeService;
    this.metricsManager = MetricsManager.getInstance();
  }

  public void configureRoutes(Router router) {
    router.post("/api/ebikes/create").handler(this::createEBike);
    router.put("/api/ebikes/:id/recharge").handler(this::rechargeEBike);
    router.get("/api/ebikes").handler(this::getAllEBikes);
    router.get("/health").handler(this::healthCheck);
    router.get("/metrics").handler(this::metrics);
  }

  private void metrics(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader("Content-Type", "text/plain")
        .end(metricsManager.getMetrics());
  }

  private void createEBike(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("createEBike");
    var timer = metricsManager.startTimer();

    try {
      JsonObject body = ctx.body().asJsonObject();
      String id = body.getString("id");
      float x = body.getFloat("x", 0.0f);
      float y = body.getFloat("y", 0.0f);

      if (id == null || id.trim().isEmpty()) {
        metricsManager.recordError(timer, "createEBike", new RuntimeException("Invalid id"));
        sendError(ctx, 400, "Invalid id");
        return;
      }

      ebikeService
          .createEBike(id, x, y)
          .thenAccept(
              result -> {
                sendResponse(ctx, 201, result);
                metricsManager.recordTimer(timer, "createEBike");
              })
          .exceptionally(
              e -> {
                metricsManager.recordError(timer, "createEBike", e);
                handleError(ctx, e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
    }
  }

  private void rechargeEBike(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("rechargeEBike");
    var timer = metricsManager.startTimer();

    String id = ctx.pathParam("id");
    if (id == null || id.trim().isEmpty()) {
      metricsManager.recordError(timer, "rechargeEBike", new RuntimeException("Invalid id"));
      sendError(ctx, 400, "Invalid id");
      return;
    }

    ebikeService
        .rechargeEBike(id)
        .thenAccept(
            result -> {
              if (result != null) {
                sendResponse(ctx, 200, result);
                metricsManager.recordTimer(timer, "rechargeEBike");
              } else {
                ctx.response().setStatusCode(404).end();
                metricsManager.recordError(
                    timer, "rechargeEBike", new RuntimeException("EBike not found"));
              }
            })
        .exceptionally(
            e -> {
              metricsManager.recordError(timer, "rechargeEBike", e);
              handleError(ctx, e);
              return null;
            });
  }

  private void getAllEBikes(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("getAllEBikes");
    var timer = metricsManager.startTimer();
    ebikeService
        .getAllEBikes()
        .thenAccept(
            result -> {
              sendResponse(ctx, 200, result);
              metricsManager.recordTimer(timer, "getAllEBikes");
            })
        .exceptionally(
            e -> {
              metricsManager.recordError(timer, "getAllEBikes", e);
              handleError(ctx, e);
              return null;
            });
  }

  private void healthCheck(RoutingContext ctx) {
    JsonObject health =
        new JsonObject().put("status", "UP").put("timestamp", System.currentTimeMillis());
    sendResponse(ctx, 200, health);
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
