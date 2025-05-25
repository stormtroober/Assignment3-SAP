package infrastructure.adapters.ride;

import application.ports.ABikeServiceAPI;
import application.ports.StationServiceAPI;
import domain.model.BikeType;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideCommunicationAdapter extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
    private final ABikeServiceAPI aBikeService;
    private final StationServiceAPI stationService;
    private final int port;
    private final Vertx vertx;
    private final MetricsManager metricsManager;
    private ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RideCommunicationAdapter(ABikeServiceAPI aBikeService, StationServiceAPI stationService, Vertx vertx) {
        this.aBikeService = aBikeService;
        this.stationService = stationService;
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

        router.get("/api/abikes/:id").handler(this::getABike);
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
            consumer.subscribe(List.of(Topics.ABIKE_RIDE_UPDATE.getTopicName(), Topics.RIDE_MAP_UPDATE.getTopicName()));
            logger.info("Subscribed to Kafka topic: {}", Topics.ABIKE_RIDE_UPDATE.getTopicName());

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        if (record.topic().equals(Topics.RIDE_MAP_UPDATE.getTopicName())) {
                            logger.info("Received ride update from Kafka: {}", record.value());
                            JsonObject payload = new JsonObject(record.value());
                            processRideUpdate(payload);
                        }
                        else if(record.topic().equals(Topics.ABIKE_RIDE_UPDATE.getTopicName())) {
                            JsonObject updateJson = new JsonObject(record.value());
                                try {
                                    aBikeService
                                            .updateABike(updateJson)
                                            .thenAccept(
                                                    updated ->
                                                            logger.info(
                                                                    "ABike {} updated successfully via Kafka consumer",
                                                                    updateJson.getString("id")))
                                            .exceptionally(
                                                    e -> {
                                                        logger.error(
                                                                "Failed to update ABike {}: {}",
                                                                updateJson.getString("id"),
                                                                e.getMessage());
                                                        return null;
                                                    });
                                } catch (Exception e) {
                                    logger.error("Invalid ABike data from Kafka: {}", e.getMessage());
                                }

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

    private void processRideUpdate(JsonObject rideUpdate) {
        String bikeId = rideUpdate.getString("bikeName");

        if (bikeId == null) {
            logger.error("Incomplete ride update data: {}", rideUpdate);
            return;
        }

        //Here we should receive also the arrived station id and bikeid
        //so we can assign the bike to the station
        if(rideUpdate.containsKey("action")){
            String action = rideUpdate.getString("action");
            if(action.equals("start")){
                stationService.deassignBikeFromStation(bikeId);
            }
        }
        if(rideUpdate.containsKey("stationId")){
            String stationId = rideUpdate.getString("stationId");
            stationService.assignBikeToStation(stationId, bikeId);
        }
        }


    private void getABike(RoutingContext ctx) {
        metricsManager.incrementMethodCounter("getABike");
        var timer = metricsManager.startTimer();

        String id = ctx.pathParam("id");
        System.out.println("Receive request from rides-microservice -> getABike(" + id + ")");
        if (id == null || id.trim().isEmpty()) {
            sendError(ctx);
            return;
        }

        aBikeService
                .getABike(id)
                .thenAccept(
                        optionalABike -> {
                            if (optionalABike.isPresent()) {
                                System.out.println("ABike found with id: " + id);
                                System.out.println(
                                        "Sending response to rides-microservice -> " + optionalABike.get());
                                sendResponse(ctx, optionalABike.get());
                            } else {
                                System.out.println("ABike not found with id: " + id);
                                ctx.response().setStatusCode(404).end();
                            }
                        })
                .whenComplete(
                        (result, throwable) -> {
                            metricsManager.recordTimer(timer, "getABike");
                        })
                .exceptionally(
                        e -> {
                            handleError(ctx, e);
                            return null;
                        });
    }

    private void sendResponse(RoutingContext ctx, Object result) {
        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(result instanceof String ? (String) result : result.toString());
    }

    private void sendError(RoutingContext ctx) {
        JsonObject error = new JsonObject().put("error", "Invalid id");
        ctx.response()
                .setStatusCode(400)
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
