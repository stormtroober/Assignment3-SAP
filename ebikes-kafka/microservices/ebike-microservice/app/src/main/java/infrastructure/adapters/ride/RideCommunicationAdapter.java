package infrastructure.adapters.ride;

import application.ports.EBikeServiceAPI;
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
  private final EBikeServiceAPI ebikeService;
  private final int port;
  private final Vertx vertx;
  private final MetricsManager metricsManager;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public RideCommunicationAdapter(EBikeServiceAPI ebikeService, Vertx vertx) {
    this.ebikeService = ebikeService;
    this.port = ServiceConfiguration.getInstance(vertx).getRideAdapterConfig().getInteger("port");
    this.vertx = vertx;
    this.metricsManager = MetricsManager.getInstance();
  }

  @Override
  public void start(Promise<Void> startPromise) {
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

    router.get("/api/ebikes/:id").handler(this::getEBike);
    router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(
            server -> {
              logger.info("HTTP server started on port {}", port);
              startPromise.complete();
            })
        .onFailure(startPromise::fail);
    initKafkaConsumer();
  }

  private void initKafkaConsumer() {
    logger.info("Initializing Kafka consumer for EBike updates");
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());
    try (consumer) {
      consumer.subscribe(List.of(Topics.EBIKE_RIDE_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", Topics.EBIKE_RIDE_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject updateJson = new JsonObject(record.value());
              ebikeService
                  .updateEBike(updateJson)
                  .thenAccept(
                      updated ->
                          logger.info(
                              "EBike {} updated successfully via Kafka consumer",
                              updateJson.getString("id")))
                  .exceptionally(
                      e -> {
                        logger.error(
                            "Failed to update EBike {}: {}",
                            updateJson.getString("id"),
                            e.getMessage());
                        return null;
                      });
            } catch (Exception e) {
              logger.error("Invalid EBike data from Kafka: {}", e.getMessage());
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

  @Override
  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideCommunicationAdapter stopped and Kafka consumer executor shut down.");
  }

  public void init() {
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

  private void getEBike(RoutingContext ctx) {
    metricsManager.incrementMethodCounter("getEBike");
    var timer = metricsManager.startTimer();

    String id = ctx.pathParam("id");
    System.out.println("Receive request from rides-microservice -> getEBike(" + id + ")");
    if (id == null || id.trim().isEmpty()) {
      sendError(ctx, 400, "Invalid id");
      return;
    }

    ebikeService
        .getEBike(id)
        .thenAccept(
            optionalEBike -> {
              if (optionalEBike.isPresent()) {
                System.out.println("EBike found with id: " + id);
                System.out.println(
                    "Sending response to rides-microservice -> " + optionalEBike.get());
                sendResponse(ctx, 200, optionalEBike.get());
              } else {
                System.out.println("EBike not found with id: " + id);
                ctx.response().setStatusCode(404).end();
              }
            })
        .whenComplete(
            (result, throwable) -> {
              metricsManager.recordTimer(timer, "getEBike");
            })
        .exceptionally(
            e -> {
              handleError(ctx, e);
              return null;
            });
  }

  private void updateEBike(RoutingContext ctx) {
    try {
      metricsManager.incrementMethodCounter("updateEBike");
      var timer = metricsManager.startTimer();

      JsonObject body = ctx.body().asJsonObject();
      String id = ctx.pathParam("id");
      body.put("id", id);

      ebikeService
          .updateEBike(body)
          .thenAccept(
              result -> {
                if (result != null) {
                  sendResponse(ctx, 200, result);
                } else {
                  ctx.response().setStatusCode(404).end();
                }
              })
          .whenComplete(
              (result, throwable) -> {
                metricsManager.recordTimer(timer, "updateEBike");
              })
          .exceptionally(
              e -> {
                handleError(ctx, e);
                return null;
              });
    } catch (Exception e) {
      handleError(ctx, new RuntimeException("Invalid JSON format"));
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
