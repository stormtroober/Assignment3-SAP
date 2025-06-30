package infrastructure.adapters.inbound;

import application.ports.EBikeServiceAPI;
import domain.model.EBike;
import domain.model.EBikeMapper;
import domain.events.BikeRideUpdate;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
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

public class RideCommunicationAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideCommunicationAdapter.class);
  private final EBikeServiceAPI eBikeService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;

  public RideCommunicationAdapter(EBikeServiceAPI eBikeService, KafkaProperties kafkaProperties) {
    this.eBikeService = eBikeService;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    logger.info("Initializing Kafka consumer for EBike updates");
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, BikeRideUpdate> consumer =
            new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties());
    try (consumer) {
      consumer.subscribe(List.of(Topics.EBIKE_RIDE_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", Topics.EBIKE_RIDE_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, BikeRideUpdate> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, BikeRideUpdate> record : records) {
            handleRideUpdate(record);
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

  private void handleRideUpdate(ConsumerRecord<String, BikeRideUpdate> record) {
    try {
      BikeRideUpdate update = record.value();
      // Map Avro BikeRideUpdate to your EBike domain model as needed
      EBike bike = EBikeMapper.fromAvro(update);
      eBikeService
              .updateEBike(bike)
              .thenAccept(
                      updated ->
                              logger.info(
                                      "EBike {} updated successfully via Kafka consumer",
                                      update.getId()))
              .exceptionally(
                      e -> {
                        logger.error(
                                "Failed to update EBike {}: {}", update.getId(), e.getMessage());
                        return null;
                      });
    } catch (Exception e) {
      logger.error("Invalid EBike data from Kafka: {}", e.getMessage());
    }
  }
}