package infrastructure.adapter.ebike;

import domain.model.repository.EBikeRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BikeConsumerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BikeConsumerAdapter.class);
    private final EBikeRepository ebikeRepository;
    private ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BikeConsumerAdapter(EBikeRepository ebikeRepository) {
        this.ebikeRepository = ebikeRepository;
    }

    public void init() {
        startKafkaConsumer();
        logger.info("BikeConsumerAdapter initialized");
    }

    private void startKafkaConsumer() {
        consumerExecutor = Executors.newSingleThreadExecutor();
        running.set(true);
        consumerExecutor.submit(this::runKafkaConsumer);
    }

    private void runKafkaConsumer() {
        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(KafkaProperties.getConsumerProperties())) {
            consumer.subscribe(
                    Collections.singletonList(Topics.EBIKE_UPDATES.getTopicName()));
            logger.info(
                    "Subscribed to Kafka topics: {}",
                    Topics.EBIKE_UPDATES.getTopicName());

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            JsonObject bikeJson = new JsonObject(record.value());
                            if (record.topic().equals(Topics.EBIKE_UPDATES.getTopicName())) {
                                processEBikeUpdate(bikeJson);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing bike update from Kafka: {}", e.getMessage(), e);
                        }
                    }

                    consumer.commitAsync(
                            (offsets, exception) -> {
                                if (exception != null) {
                                    logger.error("Failed to commit offsets: {}", exception.getMessage());
                                }
                            });
                } catch (Exception e) {
                    logger.error("Error during Kafka polling: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error setting up Kafka consumer: {}", e.getMessage(), e);
        }
    }

    private void processEBikeUpdate(JsonObject ebikeJson) {
        try {
            String id = ebikeJson.getString("id");
            logger.info("Received EBike update: id={}", id);

            ebikeRepository
                    .findById(id)
                    .thenAccept(
                            existingBike -> {
                                if (existingBike.isPresent()) {
                                    ebikeRepository
                                            .update(ebikeJson)
                                            .exceptionally(
                                                    ex -> {
                                                        logger.error("Failed to update EBike: {}", ex.getMessage(), ex);
                                                        return null;
                                                    });
                                } else {
                                    ebikeRepository
                                            .save(ebikeJson)
                                            .exceptionally(
                                                    ex -> {
                                                        logger.error("Failed to save new EBike: {}", ex.getMessage(), ex);
                                                        return null;
                                                    });
                                }
                            })
                    .exceptionally(
                            ex -> {
                                logger.error("Failed to check if EBike exists: {}", ex.getMessage(), ex);
                                return null;
                            });
        } catch (Exception e) {
            logger.error("Failed to process EBike update: {}", e.getMessage(), e);
        }
    }

    public void stop() {
        running.set(false);
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
        logger.info("BikeConsumerAdapter Kafka consumer executor shut down");
    }
}
