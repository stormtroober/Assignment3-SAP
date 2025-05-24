package infrastructure.adapter.web;

import application.ports.BikeMapServiceAPI;
import application.ports.StationMapServiceAPI;
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

public class MapServiceVerticle extends AbstractVerticle {

  private final String eurekaApplicationName;
  private final String eurekaInstanceId;
  private final int port;
  private final int eurekaPort;
  private final String eurekaHost;
  private WebClient client;
  private final BikeMapServiceAPI bikeMapService;
  private final StationMapServiceAPI stationMapService;
  private final MetricsManager metricsManager;
  private final Vertx vertx;

  public MapServiceVerticle(
      BikeMapServiceAPI bikeMapService, StationMapServiceAPI stationMapService, Vertx vertx) {
    this.vertx = vertx;
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject eurekaConfig = config.getEurekaConfig();
    JsonObject serviceConfig = config.getServiceConfig();
    this.bikeMapService = bikeMapService;
    this.stationMapService = stationMapService;
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
              System.out.println("MapServiceVerticle deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              System.err.println("Failed to deploy MapServiceVerticle: " + err.getMessage());
            });
  }

  @Override
  public void start() {
    client = WebClient.create(vertx);
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
        .route("/observeAllBikes")
        .handler(
            ctx -> {
              metricsManager.incrementMethodCounter("observeAllBikes");

              ctx.request()
                  .toWebSocket()
                  .onComplete(
                      webSocketAsyncResult -> {
                        if (webSocketAsyncResult.succeeded()) {
                          var webSocket = webSocketAsyncResult.result();

                          metricsManager.incrementMethodCounter(
                              "observeAllBikes_connection_success");

                          var consumer =
                              vertx
                                  .eventBus()
                                  .consumer(
                                      "bikes.update",
                                      message -> {
                                        webSocket.writeTextMessage(message.body().toString());

                                        metricsManager.incrementMethodCounter(
                                            "observeAllBikes_message_sent");
                                      });
                            var abikeConsumer = vertx.eventBus().consumer("abikes.update", message -> {

                                webSocket.writeTextMessage(message.body().toString());
                                metricsManager.incrementMethodCounter("observeAllBikes_abike_message_sent");
                            });

                          bikeMapService.getAllBikes();

                          webSocket.closeHandler(
                              v -> {
                                consumer.unregister();
                                abikeConsumer.unregister();
                                metricsManager.incrementMethodCounter(
                                    "observeAllBikes_connection_closed");
                              });

                          webSocket.exceptionHandler(
                              err -> {
                                consumer.unregister();
                                abikeConsumer.unregister();
                                metricsManager.incrementMethodCounter(
                                    "observeAllBikes_connection_error");
                              });
                        } else {
                          ctx.response().setStatusCode(500).end("WebSocket Upgrade Failed");
                          metricsManager.incrementMethodCounter(
                              "observeAllBikes_connection_failed");
                        }
                      });
            });

    router
        .route("/observeUserBikes")
        .handler(
            ctx -> {
              String username = ctx.queryParam("username").stream().findFirst().orElse(null);

              if (username == null) {
                ctx.response().setStatusCode(400).end("Missing username parameter");
                return;
              }

              metricsManager.incrementMethodCounter("observeUserBikes");

              ctx.request()
                  .toWebSocket()
                  .onComplete(
                      webSocketAsyncResult -> {
                        if (webSocketAsyncResult.succeeded()) {
                          var webSocket = webSocketAsyncResult.result();
                          System.out.println("User " + username + " connected");

                          metricsManager.incrementMethodCounter(
                              "observeUserBikes_connection_success");

                          var availableEBikesConsumer =
                              vertx
                                  .eventBus()
                                  .consumer(
                                      "available_bikes",
                                      message -> {
                                        webSocket.writeTextMessage(message.body().toString());

                                        metricsManager.incrementMethodCounter(
                                            "observeUserBikes_message_sent");
                                      });
                          var availableABikesConsumer =
                              vertx
                                  .eventBus()
                                  .consumer(
                                      "available_abikes",
                                      message -> {
                                        webSocket.writeTextMessage(message.body().toString());

                                        metricsManager.incrementMethodCounter(
                                            "observeUserBikes_message_sent");
                                      });

                          var stopRideConsumer =
                              vertx
                                  .eventBus()
                                  .consumer(
                                      "ride.stop." + username,
                                      message -> {
                                        webSocket.writeTextMessage(message.body().toString());
                                      });

                          var userConsumer =
                              vertx
                                  .eventBus()
                                  .consumer(
                                      username,
                                      message -> {
                                        webSocket.writeTextMessage(message.body().toString());

                                        metricsManager.incrementMethodCounter(
                                            "observeUserBikes_message_sent");
                                      });
                            var userABikeConsumer =
                                vertx.eventBus().consumer(username + ".abikes", message -> {
                                    webSocket.writeTextMessage(message.body().toString());
                                    metricsManager.incrementMethodCounter("observeUserBikes_abike_message_sent");
                            });

                            var publicABikesConsumer =
                                vertx.eventBus().consumer("public_abikes", message -> {
                                    webSocket.writeTextMessage(message.body().toString());
                                    metricsManager.incrementMethodCounter("observeUserBikes_public_abike_message_sent");
                            });

                          bikeMapService.registerUser(username);
                          bikeMapService.getAllBikes(username);

                          webSocket.closeHandler(
                              v -> {

                                metricsManager.incrementMethodCounter(
                                    "observeUserBikes_connection_closed");
                                bikeMapService.deregisterUser(username);
                                availableEBikesConsumer.unregister();
                                userConsumer.unregister();
                                stopRideConsumer.unregister();
                                userABikeConsumer.unregister();
                                availableABikesConsumer.unregister();
                                publicABikesConsumer.unregister();
                              });

                          webSocket.exceptionHandler(
                              err -> {

                                metricsManager.incrementMethodCounter(
                                    "observeUserBikes_connection_error");
                                bikeMapService.deregisterUser(username);
                                availableEBikesConsumer.unregister();
                                userConsumer.unregister();
                                stopRideConsumer.unregister();
                                userABikeConsumer.unregister();
                                availableABikesConsumer.unregister();
                                publicABikesConsumer.unregister();
                              });
                        } else {
                          metricsManager.incrementMethodCounter(
                              "observeUserBikes_connection_failed");
                        }
                      });
            });
    router
        .route("/observeStations")
        .handler(
            ctx -> {
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
                                      "stations.update",
                                      message ->
                                          webSocket.writeTextMessage(message.body().toString()));

                          stationMapService.getAllStations();
                          webSocket.closeHandler(v -> consumer.unregister());
                          webSocket.exceptionHandler(err -> consumer.unregister());
                        } else {
                          ctx.response().setStatusCode(500).end("WebSocket Upgrade Failed");
                        }
                      });
            });
    server
        .requestHandler(router)
        .listen(
            this.port,
            result -> {
              if (result.succeeded()) {
                System.out.println("HTTP server started on port " + this.port);
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
