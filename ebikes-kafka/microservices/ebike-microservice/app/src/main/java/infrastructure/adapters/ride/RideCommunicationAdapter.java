package infrastructure.adapters.ride;

import application.ports.EBikeServiceAPI;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import infrastructure.utils.MetricsManager;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;

public class RideCommunicationAdapter extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
    private final EBikeServiceAPI ebikeService;
    private final int port;
    private final Vertx vertx;
    private final MetricsManager metricsManager;
    private KafkaConsumer<String, String> consumer;

    public RideCommunicationAdapter(EBikeServiceAPI ebikeService, Vertx vertx) {
        this.ebikeService = ebikeService;
        this.port = ServiceConfiguration.getInstance(vertx).getRideAdapterConfig().getInteger("port");
        this.vertx = vertx;
        this.metricsManager = MetricsManager.getInstance();
        consumer = new KafkaConsumer<>(KafkaProperties.getConsumerProperties());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());


        router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));
        router.get("/metrics").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "text/plain")
                    .end(metricsManager.getMetrics());
        });

        router.get("/api/ebikes/:id").handler(this::getEBike);
        router.put("/api/ebikes/:id/update").handler(this::updateEBike);

        consumer.subscribe(Collections.singleton(Topics.EBIKE_RIDE_UPDATE.getTopicName()));

        vertx.setPeriodic(1000, timerId -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            records.forEach(record -> {
                logger.info("Received update from Kafka topic {}: {}", record.topic(), record.value());
                try {
                    JsonObject updateJson = new JsonObject(record.value());
                    // Call the service to update the EBike using the received JsonObject.
                    ebikeService.updateEBike(updateJson)
                            .thenAccept(updated -> logger.info("EBike {} updated successfully via Kafka consumer", updateJson.getString("id")))
                            .exceptionally(e -> {
                                logger.error("Failed to update EBike {}: {}", updateJson.getString("id"), e.getMessage());
                                return null;
                            });
                } catch (Exception e) {
                    logger.error("Error parsing Kafka message to Json: {}", e.getMessage());
                }
            });
        });

        router.get("/health").handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> {
                    logger.info("HTTP server started on port {}", port);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            logger.info("RideCommunicationAdapter deployed successfully with ID: " + id);
        }).onFailure(err -> {
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

        ebikeService.getEBike(id)
                .thenAccept(optionalEBike -> {
                    if (optionalEBike.isPresent()) {
                        System.out.println("EBike found with id: " + id);
                        System.out.println("Sending response to rides-microservice -> " + optionalEBike.get());
                        sendResponse(ctx, 200, optionalEBike.get());
                    } else {
                        System.out.println("EBike not found with id: " + id);
                        ctx.response().setStatusCode(404).end();
                    }
                })
                .whenComplete((result, throwable) -> {
                    metricsManager.recordTimer(timer, "getEBike");
                })
                .exceptionally(e -> {
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

            ebikeService.updateEBike(body)
                    .thenAccept(result -> {
                        if (result != null) {
                            sendResponse(ctx, 200, result);
                        } else {
                            ctx.response().setStatusCode(404).end();
                        }
                    })
                    .whenComplete((result, throwable) -> {
                        metricsManager.recordTimer(timer, "updateEBike");
                    })
                    .exceptionally(e -> {
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
                .end(new JsonObject()
                        .put("error", e.getMessage())
                        .encode());
    }
}