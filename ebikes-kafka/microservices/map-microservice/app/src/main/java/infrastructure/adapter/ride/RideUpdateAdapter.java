package infrastructure.adapter.ride;

import application.ports.RestMapServiceAPI;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.MetricsManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class RideUpdateAdapter extends AbstractVerticle {

  private final RestMapServiceAPI mapService;
  private final int port;
  private final MetricsManager metricsManager;
  private final Vertx vertx;

  public RideUpdateAdapter(RestMapServiceAPI mapService, Vertx vertx) {
    this.vertx = vertx;
    this.mapService = mapService;
    this.port = ServiceConfiguration.getInstance(vertx).getRideAdapterConfig().getInteger("port");
    this.metricsManager = MetricsManager.getInstance();
  }

  public void init() {
    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              System.out.println("RideUpdateAdapter deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              System.err.println("Failed to deploy RideUpdateAdapter: " + err.getMessage());
            });
  }

  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router
        .get("/metrics")
        .handler(
            ctx -> {
              ctx.response()
                  .putHeader("Content-Type", "text/plain")
                  .end(metricsManager.getMetrics());
            });
    router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

    router
        .post("/notifyStartRide")
        .handler(
            ctx -> {
              metricsManager.incrementMethodCounter("notifyStartRide");
              var timer = metricsManager.startTimer();

              JsonObject body = ctx.body().asJsonObject();
              String username = body.getString("username");
              String bikeName = body.getString("bikeName");

              mapService
                  .notifyStartRide(username, bikeName)
                  .thenAccept(v -> ctx.response().setStatusCode(200).end("OK"))
                  .whenComplete(
                      (result, throwable) -> {
                        metricsManager.recordTimer(timer, "notifyStartRide");
                      })
                  .exceptionally(
                      ex -> {
                        ctx.response().setStatusCode(500).end(ex.getMessage());
                        return null;
                      });
            });

    router
        .post("/notifyStopRide")
        .handler(
            ctx -> {
              metricsManager.incrementMethodCounter("notifyStopRide");
              var timer = metricsManager.startTimer();

              JsonObject body = ctx.body().asJsonObject();
              String username = body.getString("username");
              String bikeName = body.getString("bikeName");

              mapService
                  .notifyStopRide(username, bikeName)
                  .thenAccept(v -> ctx.response().setStatusCode(200).end("OK"))
                  .whenComplete(
                      (result, throwable) -> {
                        metricsManager.recordTimer(timer, "notifyStopRide");
                      })
                  .exceptionally(
                      ex -> {
                        ctx.response().setStatusCode(500).end(ex.getMessage());
                        return null;
                      });
            });

    server
        .requestHandler(router)
        .listen(
            port,
            result -> {
              if (result.succeeded()) {
                System.out.println("RideUpdateVerticle is running on port " + port);
              } else {
                System.err.println(
                    "Failed to start RideUpdateVerticle: " + result.cause().getMessage());
              }
            });
  }
}
