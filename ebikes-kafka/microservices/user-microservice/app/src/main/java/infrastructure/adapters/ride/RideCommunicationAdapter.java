package infrastructure.adapters.ride;

import application.ports.UserServiceAPI;
import infrastructure.utils.MetricsManager;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideCommunicationAdapter extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
    private final UserServiceAPI userService;
    private final int port;
    private final Vertx vertx;
    private final MetricsManager metricsManager;

    public RideCommunicationAdapter(UserServiceAPI userService, Vertx vertx) {
        this.userService = userService;
        this.port = ServiceConfiguration.getInstance(vertx).getRideAdapterConfig().getInteger("port");
        this.vertx = vertx;
        this.metricsManager = MetricsManager.getInstance();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/api/users/:username").handler(this::getUser);
        router.put("/api/users/:id/update").handler(this::updateUser);
        router.get("/metrics").handler(this::metrics);
        router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> {
                    logger.info("RideCommunicationAdapter HTTP server started on port {}", port);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    private void metrics(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("Content-Type", "text/plain")
                .end(metricsManager.getMetrics());
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            logger.info("RideCommunicationAdapter deployed successfully with ID: " + id);
        }).onFailure(err -> {
            logger.error("Failed to deploy RideCommunicationAdapter", err);
        });
    }

    private void getUser(RoutingContext ctx){
        metricsManager.incrementMethodCounter("getUser");
        var timer = metricsManager.startTimer();
        try{
            String username = ctx.pathParam("username");
            if (username == null || username.trim().isEmpty()) {
                sendError(ctx, 400, "Invalid username");
                metricsManager.recordError(timer, "getUser", new RuntimeException("Invalid username"));
                return;
            }
            userService.getUserByUsername(username)
                    .thenAccept(optionalUser -> {
                        if (optionalUser.isPresent()) {
                            logger.info("User found with username: " + username);
                            logger.info("Sending response to rides-microservice -> " + optionalUser.get());
                            sendResponse(ctx, 200, optionalUser.get());
                            metricsManager.recordTimer(timer, "getUser");
                        } else {
                            logger.error("User not found with username: " + username);
                            ctx.response().setStatusCode(404).end();
                            metricsManager.recordError(timer, "getUser", new RuntimeException("User not found"));
                        }
                    })
                    .exceptionally(e -> {
                        handleError(ctx, e);
                        metricsManager.recordError(timer, "getUser", e);
                        return null;
                    });
        } catch (Exception e) {
            handleError(ctx, new RuntimeException("Invalid JSON format"));
            metricsManager.recordError(timer, "getUser", e);
        }
    }

    private void updateUser(RoutingContext ctx) {
        metricsManager.incrementMethodCounter("updateUser");
        var timer = metricsManager.startTimer();
        try {
            JsonObject user = ctx.body().asJsonObject();
            userService.updateUser(user)
                    .thenAccept(updatedUser -> {
                        if (updatedUser != null) {
                            logger.info("User updated: " + updatedUser);
                            sendResponse(ctx, 200, updatedUser);
                            metricsManager.recordTimer(timer, "updateUser");
                        } else {
                            logger.error("User not found: " + user.getString("username"));
                            sendError(ctx, 404, "User not found");
                            metricsManager.recordError(timer, "updateUser", new RuntimeException("User not found"));
                        }
                    })
                    .exceptionally(e -> {
                        handleError(ctx, e);
                        metricsManager.recordError(timer, "updateUser", e);
                        return null;
                    });
        } catch (Exception e) {
            handleError(ctx, new RuntimeException("Invalid JSON format"));
            metricsManager.recordError(timer, "updateUser", e);
        }
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
                .end(new JsonObject()
                        .put("error", e.getMessage())
                        .encode());
    }
}