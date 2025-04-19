package infrastructure.adapters.web;

import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBikeVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(EBikeVerticle.class);

  private final RESTEBikeAdapter controller;
  private final String eurekaApplicationName;
  private final int port;
  private final String eurekaInstanceId;
  private final String eurekaHost;
  private final Vertx vertx;
  private final int eurekaPort;
  private final WebClient client;

  public EBikeVerticle(RESTEBikeAdapter controller, Vertx vertx) {
    this.controller = controller;
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject eurekaConfig = config.getEurekaConfig();
    JsonObject serviceConfig = config.getServiceConfig();
    this.eurekaInstanceId = UUID.randomUUID().toString();
    this.eurekaHost = eurekaConfig.getString("host");
    this.eurekaPort = eurekaConfig.getInteger("port");
    WebClientOptions options = new WebClientOptions().setConnectTimeout(2000).setIdleTimeout(30);
    this.vertx = vertx;
    this.client = WebClient.create(vertx, options);
    this.eurekaApplicationName = serviceConfig.getString("name");
    this.port = serviceConfig.getInteger("port");
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    controller.configureRoutes(router);

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(
            server -> {
              registerWithEureka();
              logger.info("HTTP server started on port {}", port);
              startPromise.complete();
            })
        .onFailure(startPromise::fail);
  }

  public void init() {
    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              logger.info("EBikeVerticle deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy EBikeVerticle: " + err.getMessage());
            });
  }

  private void registerWithEureka() {
    JsonObject instance =
        new JsonObject()
            .put(
                "instance",
                new JsonObject()
                    .put("instanceId", eurekaInstanceId)
                    .put("hostName", eurekaApplicationName)
                    .put("app", eurekaApplicationName)
                    .put("ipAddr", "127.0.0.1")
                    .put("vipAddress", eurekaApplicationName)
                    .put("status", "UP")
                    .put("port", new JsonObject().put("$", port).put("@enabled", true))
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
    System.out.println("Eureka application name: " + eurekaApplicationName);
    client
        .post(eurekaPort, eurekaHost, "/eureka/apps/" + eurekaApplicationName)
        .putHeader("Content-Type", "application/json")
        .sendJsonObject(instance)
        .map(
            response -> {
              if (response.statusCode() == 204) {
                logger.info("Successfully registered with Eureka");
                startHeartbeat(eurekaApplicationName);
                return null;
              } else {
                throw new RuntimeException(
                    "Failed to register with Eureka: " + response.statusCode());
              }
            });
  }

  private void startHeartbeat(String eurekaApplicationName) {
    this.vertx.setPeriodic(
        30000,
        id -> {
          sendHeartbeat(eurekaApplicationName, eurekaInstanceId)
              .onFailure(err -> logger.warn("Failed to send heartbeat: {}", err.getMessage()));
        });
  }

  private Future<Void> sendHeartbeat(String eurekaApplicationName, String eurekaInstanceId) {
    return client
        .put(
            eurekaPort,
            eurekaHost,
            "/eureka/apps/" + eurekaApplicationName + "/" + eurekaInstanceId)
        .send()
        .map(
            response -> {
              if (response.statusCode() == 200) {
                logger.info("Successfully sent heartbeat to Eureka");
                return null;
              } else {
                throw new RuntimeException(
                    "Failed to send heartbeat to Eureka: " + response.statusCode());
              }
            });
  }
}
