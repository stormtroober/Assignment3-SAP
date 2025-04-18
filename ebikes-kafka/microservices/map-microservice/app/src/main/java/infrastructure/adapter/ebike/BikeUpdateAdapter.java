package infrastructure.adapter.ebike;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
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
    private boolean running = false;

    public BikeUpdateAdapter(RestMapServiceAPI mapService) {
        this.mapService = mapService;
    }

    public void init() {
        System.out.println("Initializing BikeUpdateAdapter");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::runKafkaConsumer);
        running = true;
    }

    private void runKafkaConsumer() {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

        try (consumer) {
            consumer.subscribe(List.of(Topics.EBIKE_UPDATES.getTopicName()));
            System.out.println("Subscribed to Kafka topic: " + Topics.EBIKE_UPDATES.getTopicName());
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