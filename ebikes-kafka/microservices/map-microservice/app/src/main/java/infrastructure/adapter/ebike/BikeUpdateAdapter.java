package infrastructure.adapter.ebike;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import infrastructure.utils.MetricsManager;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

public class BikeUpdateAdapter extends AbstractVerticle {

    private final RestMapServiceAPI mapService;
    private final int port;
    private final MetricsManager metricsManager;
    private final Vertx vertx;

    public BikeUpdateAdapter(RestMapServiceAPI mapService, Vertx vertx) {
        this.vertx = vertx;
        this.mapService = mapService;
        this.port = ServiceConfiguration.getInstance(vertx).getEBikeAdapterConfig().getInteger("port");
        this.metricsManager = MetricsManager.getInstance();
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("BikeUpdateAdapter deployed successfully with ID: " + id);
        }).onFailure(err -> {
            System.err.println("Failed to deploy BikeUpdateAdapter: " + err.getMessage());
        });
    }

    @Override
    public void start() {
        // Kafka consumer setup
        new Thread(() -> {
            Properties props = new Properties();
            props.put("bootstrap.servers", "kafka:9092");
            props.put("group.id", "ebike-map-group");
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", "1000");
            props.put("session.timeout.ms", "30000");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(java.util.List.of("ebike-updates"));
                System.out.println("Subscribed to Kafka topic: ebike-updates");

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            JsonObject body = new JsonObject(record.value());
                            EBike bike = createEBikeFromJson(body);
                            mapService.updateEBike(bike)
                                    .thenAccept(v -> {
                                        System.out.printf("EBike %s updated successfully from Kafka message\n", bike.getId());
                                    })
                                    .exceptionally(ex -> {
                                        System.err.printf("Failed to update EBike %s: %s\n", bike.getId(), ex.getMessage());
                                        return null;
                                    });
                        } catch (Exception e) {
                            System.err.println("Invalid EBike data from Kafka: " + e.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    private EBike createEBikeFromJson(JsonObject body) {
        String bikeName = body.getString("id");
        JsonObject location = body.getJsonObject("location");
        double x = location.getDouble("x");
        double y = location.getDouble("y");
        EBikeState state = EBikeState.valueOf(body.getString("state"));
        int batteryLevel = body.getInteger("batteryLevel");

        EBikeFactory factory = EBikeFactory.getInstance();
        return factory.createEBike(bikeName, (float) x, (float) y, state, batteryLevel);
    }

}
