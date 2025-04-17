package infrastructure.adapter.ebike;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BikeUpdateAdapter {

    private final RestMapServiceAPI mapService;
    private ExecutorService executorService;
    private boolean running = false;

    public BikeUpdateAdapter(RestMapServiceAPI mapService) {
        this.mapService = mapService;
    }

    public void init() {
        System.out.println("Initializing BikeUpdateAdapter");
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::runKafkaConsumer);
        running = true;
    }

    private void runKafkaConsumer() {
        Properties props = getProps();
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        try (consumer) {
            consumer.subscribe(List.of("ebike-updates"));
            System.out.println("Subscribed to Kafka topic: ebike-updates");
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        JsonObject body = new JsonObject(record.value());
                        EBike bike = createEBikeFromJson(body);
                        mapService.updateEBike(bike)
                                .thenAccept(v -> System.out.printf("EBike %s updated successfully\n", bike.getId()))
                                .exceptionally(ex -> {
                                    System.err.printf("Failed to update EBike %s: %s\n", bike.getId(), ex.getMessage());
                                    return null;
                                });
                    } catch (Exception e) {
                        System.err.println("Invalid EBike data from Kafka: " + e.getMessage());
                    }
                }
                consumer.commitSync();
            }
        }
    }

    private static Properties getProps() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka:9092");
        props.put("group.id", "ebike-map-group");
        props.put("enable.auto.commit", "false"); // Changed to manual commit
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
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