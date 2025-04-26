package infrastructure.adapters.web;

import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(UserVerticle.class);

  private final RESTUserAdapter userService;
  private final String eurekaApplicationName;
  private final String eurekaHost;
  private final int port;
  private final String eurekaInstanceId;
  private final int eurekaPort;
  private WebClient client;
  private Vertx vertx;

  public UserVerticle(RESTUserAdapter userService, Vertx vertx) {
    this.userService = userService;
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject eurekaConfig = config.getEurekaConfig();
    JsonObject serviceConfig = config.getServiceConfig();
    this.eurekaApplicationName = serviceConfig.getString("name");
    this.eurekaInstanceId = UUID.randomUUID().toString().substring(0, 5);
    this.eurekaHost = eurekaConfig.getString("host");
    this.eurekaPort = eurekaConfig.getInteger("port");
    this.port = serviceConfig.getInteger("port");
    this.vertx = vertx;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    userService.configureRoutes(router);

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(
            server -> {
              registerWithEureka();
              logger.info("RestUserAdapter HTTP server started on port {}", port);
              startPromise.complete();
            })
        .onFailure(startPromise::fail);
  }

  public void init() {
    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              logger.info("UserVerticle deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy UserVerticle: " + err.getMessage());
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
    client = WebClient.create(vertx);
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
