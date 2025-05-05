package infrastructure.adapter.web;

import application.ports.RestAutonomousRideService;
import application.ports.RestSimpleRideService;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.MetricsManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RideServiceVerticle extends AbstractVerticle {

  private final String eurekaApplicationName;
  private final String eurekaInstanceId;
  private final int port;
  private final int eurekaPort;
  private final String eurekaHost;
  private WebClient client;
  private final RestSimpleRideService rideService;
  private final RestAutonomousRideService autonomousRideService;
  private final MetricsManager metricsManager;
  private final Vertx vertx;

  public RideServiceVerticle(RestSimpleRideService rideService, RestAutonomousRideService autonomousRideService, Vertx vertx) {
    this.rideService = rideService;
    this.autonomousRideService = autonomousRideService;
    this.vertx = vertx;
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject eurekaConfig = config.getEurekaConfig();
    JsonObject serviceConfig = config.getServiceConfig();
    this.eurekaApplicationName = serviceConfig.getString("name");
    this.eurekaInstanceId = UUID.randomUUID().toString().substring(0, 5);
    this.port = serviceConfig.getInteger("port");
    this.eurekaPort = eurekaConfig.getInteger("port");
    this.eurekaHost = eurekaConfig.getString("host");
    this.metricsManager = MetricsManager.getInstance();
  }

  public void init() {
    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              System.out.println("RideServiceVerticle deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              System.err.println("Failed to deploy RideServiceVerticle: " + err.getMessage());
            });
  }

  @Override
  public void start() {
    client = WebClient.create(vertx);
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));
    router
        .get("/metrics")
        .handler(
            ctx -> {
              ctx.response()
                  .putHeader("Content-Type", "text/plain")
                  .end(metricsManager.getMetrics());
            });

    router
        .post("/startRide")
        .handler(
            ctx -> {
              metricsManager.incrementMethodCounter("startRide");
              var timer = metricsManager.startTimer();

              JsonObject body = ctx.body().asJsonObject();
              String user = body.getString("user");
              String bike = body.getString("bike");

              rideService
                  .startRide(user, bike)
                  .thenAccept(
                      v -> {
                        ctx.response().setStatusCode(200).end("Ride started");
                        metricsManager.recordTimer(timer, "startRide");
                      })
                  .exceptionally(
                      ex -> {
                        ctx.response()
                            .setStatusCode(400) // Use 400 for client errors
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", ex.getMessage()).encode());
                        metricsManager.recordError(timer, "startRide", ex);
                        return null;
                      });
            });

    router
        .post("/stopRide")
        .handler(
            ctx -> {
              metricsManager.incrementMethodCounter("stopRide");
              var timer = metricsManager.startTimer();

              JsonObject body = ctx.body().asJsonObject();
              String username = body.getString("username");

              rideService
                  .stopRide(username)
                  .thenAccept(
                      v -> {
                        ctx.response().setStatusCode(200).end("Ride stopped");
                        metricsManager.recordTimer(timer, "stopRide");
                      })
                  .exceptionally(
                      ex -> {
                        ctx.response().setStatusCode(500).end(ex.getMessage());
                        metricsManager.recordError(timer, "stopRide", ex);
                        return null;
                      });
            });
    router.post("/rideToUser").handler(ctx -> {
      metricsManager.incrementMethodCounter("rideToUser");
      //var timer = metricsManager.startTimer();

      JsonObject body = ctx.body().asJsonObject();
      System.out.println("Ride to user request: " + body.encodePrettily());
        String user = body.getString("user");
        String bike = body.getString("bike");
        Double x = body.getDouble("posX");
        Double y = body.getDouble("posY");
      //autonomousRideService.dispatchBikeToUser

    });
    server
        .requestHandler(router)
        .listen(
            port,
            result -> {
              if (result.succeeded()) {
                System.out.println("HTTP server started on port " + port);
                registerWithEureka();
                vertx.setPeriodic(TimeUnit.SECONDS.toMillis(30), id -> sendHeartbeat());
              } else {
                System.err.println("Failed to start HTTP server: " + result.cause().getMessage());
              }
            });
  }

  private void registerWithEureka() {
    JsonObject instance =
        new JsonObject()
            .put(
                "instance",
                new JsonObject()
                    .put("hostName", eurekaApplicationName)
                    .put("app", eurekaApplicationName)
                    .put("instanceId", eurekaInstanceId)
                    .put("ipAddr", "127.0.0.1")
                    .put("vipAddress", eurekaApplicationName)
                    .put("port", new JsonObject().put("$", port).put("@enabled", true))
                    .put("status", "UP")
                    .put(
                        "healthCheckUrl",
                        "http://" + eurekaApplicationName + ":" + port + "/health")
                    .put("statusPageUrl", "http://" + eurekaApplicationName + ":" + port + "/info")
                    .put("homePageUrl", "http://" + eurekaApplicationName + ":" + port + "/")
                    .put(
                        "dataCenterInfo",
                        new JsonObject()
                            .put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                            .put("name", "MyOwn")));
    System.out.println("Registering with Eureka: " + instance.encodePrettily());
    System.out.println("Eureka host: " + eurekaHost + " Eureka port: " + eurekaPort);
    System.out.println("Eureka app name: " + eurekaApplicationName);

    client
        .post(eurekaPort, eurekaHost, "/eureka/apps/" + eurekaApplicationName)
        .sendJsonObject(
            instance,
            res -> {
              if (res.succeeded()) {
                System.out.println("Successfully registered with Eureka");
              } else {
                System.err.println("Failed to register with Eureka: " + res.cause());
              }
            });
  }

  private void sendHeartbeat() {
    client
        .put(
            eurekaPort,
            eurekaHost,
            "/eureka/apps/" + eurekaApplicationName + "/" + eurekaInstanceId)
        .send(
            res -> {
              if (res.succeeded()) {
                System.out.println("Heartbeat sent successfully");
              } else {
                System.err.println("Failed to send heartbeat: " + res.cause());
              }
            });
  }
}
