package infrastructure.adapter.ebike;

import application.ports.BikeMapServiceAPI;
import domain.model.BikeType;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
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

public class BikeUpdateAdapter {

  private static final Logger logger = LoggerFactory.getLogger(BikeUpdateAdapter.class);
  private final BikeMapServiceAPI mapService;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public BikeUpdateAdapter(BikeMapServiceAPI mapService) {
    this.mapService = mapService;
  }

  public void init() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

    try (consumer) {
      consumer.subscribe(
          List.of(Topics.EBIKE_UPDATES.getTopicName(), Topics.ABIKE_UPDATES.getTopicName()));

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject body = new JsonObject(record.value());
              EBike bike = createEBikeFromJson(body);
              mapService
                  .updateEBike(bike)
                  .thenAccept(v -> logger.info("Bike {} updated successfully", bike.getId()))
                  .exceptionally(
                      ex -> {
                        logger.error(
                            "Failed to update Bike {}: {}", bike.getId(), ex.getMessage());
                        return null;
                      });
            } catch (Exception e) {
              logger.error("Invalid Bike data from Kafka: {}", e.getMessage());
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

  private EBike createEBikeFromJson(JsonObject body) {
    String bikeName = body.getString("id");
    JsonObject location = body.getJsonObject("location");
    double x = location.getDouble("x");
    double y = location.getDouble("y");
    EBikeState state = EBikeState.valueOf(body.getString("state"));
    int batteryLevel = body.getInteger("batteryLevel");
    // Extract type, default to NORMAL if missing
    BikeType type = BikeType.NORMAL;
    if (body.containsKey("type")) {
      type = BikeType.valueOf(body.getString("type"));
    }

    EBikeFactory factory = EBikeFactory.getInstance();
    return factory.create(
        bikeName, new domain.model.P2d((float) x, (float) y), state, batteryLevel, type);
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("BikeUpdateAdapter stopped and Kafka consumer executor shut down.");
  }
}
