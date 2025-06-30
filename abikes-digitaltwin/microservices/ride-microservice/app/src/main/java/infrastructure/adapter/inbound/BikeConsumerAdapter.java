package infrastructure.adapter.inbound;

import domain.events.EBikeUpdate;
import domain.model.ABikeMapper;
import domain.model.EBikeMapper;
import domain.model.repository.ABikeRepository;
import domain.model.repository.EBikeRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
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

public class BikeConsumerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BikeConsumerAdapter.class);
    private final ABikeRepository abikeRepository;
    private final EBikeRepository ebikeRepository;
    private ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final KafkaProperties kafkaProperties;

    public BikeConsumerAdapter(
            ABikeRepository abikeRepository,
            EBikeRepository ebikeRepository,
            KafkaProperties kafkaProperties) {
        this.abikeRepository = abikeRepository;
        this.ebikeRepository = ebikeRepository;
        this.kafkaProperties = kafkaProperties;
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
        // ABike consumer (JSON)
        KafkaConsumer<String, String> abikeConsumer =
                new KafkaConsumer<>(kafkaProperties.getConsumerProperties());
        abikeConsumer.subscribe(List.of(Topics.ABIKE_UPDATES.getTopicName()));

        // EBike consumer (Avro)
        KafkaConsumer<String, EBikeUpdate> ebikeConsumer =
                new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties());
        ebikeConsumer.subscribe(List.of(Topics.EBIKE_UPDATES.getTopicName()));

        try (abikeConsumer; ebikeConsumer) {
            while (running.get()) {
                // ABike updates
                ConsumerRecords<String, String> abikeRecords = abikeConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : abikeRecords) {
                    try {
                        JsonObject bikeJson = new JsonObject(record.value());
                        processABikeUpdate(bikeJson);
                    } catch (Exception e) {
                        logger.error("Error processing ABike update from Kafka: {}", e.getMessage(), e);
                    }
                }
                abikeConsumer.commitAsync(
                        (offsets, exception) -> {
                            if (exception != null) {
                                logger.error("Failed to commit ABike offsets: {}", exception.getMessage());
                            }
                        });

                // EBike updates
                ConsumerRecords<String, EBikeUpdate> ebikeRecords = ebikeConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, EBikeUpdate> record : ebikeRecords) {
                    try {
                        EBikeUpdate avroUpdate = record.value();
                        processEBikeUpdate(avroUpdate);
                    } catch (Exception e) {
                        logger.error("Error processing EBike update from Kafka: {}", e.getMessage(), e);
                    }
                }
                ebikeConsumer.commitAsync(
                        (offsets, exception) -> {
                            if (exception != null) {
                                logger.error("Failed to commit EBike offsets: {}", exception.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            logger.error("Error setting up Kafka consumers: {}", e.getMessage(), e);
        }
    }

    private void processABikeUpdate(JsonObject abikeJson) {
        try {
            String id = abikeJson.getString("id");
            abikeRepository
                    .findById(id)
                    .thenAccept(
                            existingBike -> {
                                if (existingBike.isPresent()) {
                                    abikeRepository
                                            .update(ABikeMapper.fromJson(abikeJson))
                                            .exceptionally(
                                                    ex -> {
                                                        logger.error("Failed to update ABike: {}", ex.getMessage(), ex);
                                                        return null;
                                                    });
                                } else {
                                    abikeRepository
                                            .save(ABikeMapper.fromJson(abikeJson))
                                            .exceptionally(
                                                    ex -> {
                                                        logger.error("Failed to save new ABike: {}", ex.getMessage(), ex);
                                                        return null;
                                                    });
                                }
                            })
                    .exceptionally(
                            ex -> {
                                logger.error("Failed to check if ABike exists: {}", ex.getMessage(), ex);
                                return null;
                            });
        } catch (Exception e) {
            logger.error("Failed to process ABike update: {}", e.getMessage(), e);
        }
    }

    private void processEBikeUpdate(EBikeUpdate avroUpdate) {
        try {
            String id = avroUpdate.getId().toString();
            logger.info("Received EBike update: id={}", id);

            ebikeRepository
                    .findById(id)
                    .thenAccept(
                            existingBike -> {
                                if (existingBike.isPresent()) {
                                    ebikeRepository
                                            .update(EBikeMapper.fromAvro(avroUpdate))
                                            .exceptionally(
                                                    ex -> {
                                                        logger.error("Failed to update EBike: {}", ex.getMessage(), ex);
                                                        return null;
                                                    });
                                } else {
                                    ebikeRepository
                                            .save(EBikeMapper.fromAvro(avroUpdate))
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