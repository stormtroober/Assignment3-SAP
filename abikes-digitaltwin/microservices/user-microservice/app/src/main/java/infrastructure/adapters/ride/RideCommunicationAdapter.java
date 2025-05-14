package infrastructure.adapters.ride;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_BIKE_DISPATCH;
import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.KafkaProperties;
import infrastructure.utils.MetricsManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideCommunicationAdapter extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
  private final UserServiceAPI userService;
  private final int port;
  private final Vertx vertx;
  private final MetricsManager metricsManager;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

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
    // Removed PUT endpoint for user updates as it will now come from Kafka
    router.get("/metrics").handler(this::metrics);
    router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(
            server -> {
              logger.info("RideCommunicationAdapter HTTP server started on port {}", port);
              startPromise.complete();
            })
        .onFailure(startPromise::fail);
  }

  private void metrics(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader("Content-Type", "text/plain")
        .end(metricsManager.getMetrics());
  }

  public void init() {
    startKafkaConsumer();

    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              logger.info("RideCommunicationAdapter deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy RideCommunicationAdapter", err);
            });
  }

  private void startKafkaConsumer() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

    try (consumer) {
      // Subscribe to the ride-user-update topic
      consumer.subscribe(List.of(RIDE_USER_UPDATE.getTopicName(),
              RIDE_BIKE_DISPATCH.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", RIDE_USER_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject message = new JsonObject(record.value());
              if (record.topic().equals(Topics.RIDE_BIKE_DISPATCH.getTopicName())) {
                logger.info("Received dispatch message: {}", message);
                processBikeDispatch(message);
              } else if (record.topic().equals(RIDE_USER_UPDATE.getTopicName())) {
                logger.info("Received user update from Kafka: {}", message);
                processUserUpdate(message);
              }
            } catch (Exception e) {
              logger.error("Invalid message from Kafka: {}", e.getMessage());
            }
          }

          consumer.commitAsync(
              (offsets, exception) -> {
                if (exception != null) {
                  logger.error("Failed to commit offsets: {}", exception.getMessage());
                }
              });
        } catch (Exception e) {
          logger.error("Error during Kafka polling: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      logger.error("Error setting up Kafka consumer: {}", e.getMessage());
    }
  }

  private void processUserUpdate(JsonObject user) {
    metricsManager.incrementMethodCounter("updateUser");
    var timer = metricsManager.startTimer();

    try {
      userService
          .updateUser(user)
          .thenAccept(
              updatedUser -> {
                if (updatedUser != null) {
                  logger.info("User updated via Kafka: {}", updatedUser);
                  metricsManager.recordTimer(timer, "updateUser");
                } else {
                  logger.error("User not found via Kafka update: {}", user.getString("username"));
                  metricsManager.recordError(
                      timer, "updateUser", new RuntimeException("User not found"));
                }
              })
          .exceptionally(
              e -> {
                logger.error("Error processing user update from Kafka", e);
                metricsManager.recordError(timer, "updateUser", e);
                return null;
              });
    } catch (Exception e) {
      logger.error("Invalid JSON format in Kafka message", e);
      metricsManager.recordError(timer, "updateUser", e);
    }
  }

  //{
  //
  //  "userId" : "ale",
  //
  //  "positionX" : 1.0,
  //
  //  "positionY" : 50.0
  //
  //}
  private void processBikeDispatch(JsonObject message) {
    logger.info("Dispatching bike to user: {}", message);
    String username = message.getString("userId");
    if (username != null && !username.isBlank()) {
      vertx.eventBus().publish(username, message.encode());
    } else {
      logger.error("Dispatch message missing username: {}", message);
    }
  }



  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideCommunicationAdapter Kafka consumer executor shut down.");
  }

  private void getUser(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("getUser");
    var timer = metricsManager.startTimer();
    try {
      String username = ctx.pathParam("username");
      if (username == null || username.trim().isEmpty()) {
        sendError(ctx, 400, "Invalid username");
        metricsManager.recordError(timer, "getUser", new RuntimeException("Invalid username"));
        return;
      }
      userService
          .getUserByUsername(username)
          .thenAccept(
              optionalUser -> {
                if (optionalUser.isPresent()) {
                  logger.info("User found with username: " + username);
                  logger.info("Sending response to rides-microservice -> " + optionalUser.get());
                  sendResponse(ctx, 200, optionalUser.get());
                  metricsManager.recordTimer(timer, "getUser");
                } else {
                  logger.error("User not found with username: " + username);
                  ctx.response().setStatusCode(404).end();
                  metricsManager.recordError(
                      timer, "getUser", new RuntimeException("User not found"));
                }
              })
          .exceptionally(
              e -> {
                handleError(ctx, e);
                metricsManager.recordError(timer, "getUser", e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
      metricsManager.recordError(timer, "getUser", e);
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
        .end(new JsonObject().put("error", e.getMessage()).encode());
  }
}
