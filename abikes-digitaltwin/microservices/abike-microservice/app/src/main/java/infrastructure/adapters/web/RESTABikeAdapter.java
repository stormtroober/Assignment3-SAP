package infrastructure.adapters.web;

import application.ports.ABikeServiceAPI;
import infrastructure.utils.MetricsManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTABikeAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RESTABikeAdapter.class);
  private final ABikeServiceAPI abikeService;
  private final MetricsManager metricsManager;

  public RESTABikeAdapter(ABikeServiceAPI abikeService) {
    this.abikeService = abikeService;
    this.metricsManager = MetricsManager.getInstance();
  }

  public void configureRoutes(Router router) {
    router.post("/api/abikes/create").handler(this::createABike);
    router.put("/api/abikes/:id/recharge").handler(this::rechargeABike);
    router.get("/health").handler(this::healthCheck);
    router.get("/metrics").handler(this::metrics);
  }

  private void metrics(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader("Content-Type", "text/plain")
        .end(metricsManager.getMetrics());
  }

  private void createABike(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("createABike");
    var timer = metricsManager.startTimer();

    try {
      JsonObject body = ctx.body().asJsonObject();
      String id = body.getString("id");

      if (id == null || id.trim().isEmpty()) {
        metricsManager.recordError(timer, "createABike", new RuntimeException("Invalid id"));
        sendError(ctx, 400, "Invalid id");
        return;
      }

      abikeService
          .createABike(id)
          .thenAccept(
              result -> {
                sendResponse(ctx, 201, result);
                metricsManager.recordTimer(timer, "createABike");
              })
          .exceptionally(
              e -> {
                metricsManager.recordError(timer, "createABike", e);
                handleError(ctx, e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
    }
  }

  private void rechargeABike(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("rechargeABike");
    var timer = metricsManager.startTimer();

    String id = ctx.pathParam("id");
    if (id == null || id.trim().isEmpty()) {
      metricsManager.recordError(timer, "rechargeABike", new RuntimeException("Invalid id"));
      sendError(ctx, 400, "Invalid id");
      return;
    }

    abikeService
        .rechargeABike(id)
        .thenAccept(
            result -> {
              if (result != null) {
                sendResponse(ctx, 200, result);
                metricsManager.recordTimer(timer, "rechargeABike");
              } else {
                ctx.response().setStatusCode(404).end();
                metricsManager.recordError(
                    timer, "rechargeABike", new RuntimeException("ABike not found"));
              }
            })
        .exceptionally(
            e -> {
              metricsManager.recordError(timer, "rechargeABike", e);
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
