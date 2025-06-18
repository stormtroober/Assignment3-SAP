package infrastructure.adapter.inbound;

import application.ports.BikeMapServiceAPI;
import domain.model.*;
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
  private final BikeMapServiceAPI bikeMapServiceAPI;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;

  public BikeUpdateAdapter(BikeMapServiceAPI bikeMapServiceAPI, KafkaProperties kafkaProperties) {
    this.bikeMapServiceAPI = bikeMapServiceAPI;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaProperties.getConsumerProperties());

    try (consumer) {
      consumer.subscribe(
          List.of(Topics.EBIKE_UPDATES.getTopicName(), Topics.ABIKE_UPDATES.getTopicName()));

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {

            JsonObject body = new JsonObject(record.value());
            String topic = record.topic();

            if (Topics.EBIKE_UPDATES.getTopicName().equals(topic)) {
              EBike bike = createEBikeFromJson(body);
              bikeMapServiceAPI
                  .updateEBike(bike)
                  .thenAccept(v -> logger.info("EBike {} updated successfully", bike.getId()))
                  .exceptionally(
                      ex -> {
                        logger.error(
                            "Failed to update EBike {}: {}", bike.getId(), ex.getMessage());
                        return null;
                      });
            } else if (Topics.ABIKE_UPDATES.getTopicName().equals(topic)) {
              ABike bike = createABikeFromJson(body);
              bikeMapServiceAPI
                  .updateABike(bike)
                  .thenAccept(v -> logger.info("ABike {} updated successfully", bike.getId()))
                  .exceptionally(
                      ex -> {
                        logger.error(
                            "Failed to update ABike {}: {}", bike.getId(), ex.getMessage());
                        return null;
                      });
            } else {
              logger.warn("Received message from unknown topic: {}", topic);
            }
          }

          consumer.commitAsync(
              (offsets, exception) -> {
                if (exception != null) {
                  logger.error("Failed to commit offsets: {}", exception.getMessage());
                }
              });

        } catch (Exception e) {
          logger.error("Error during Kafka polling: {}", (Object) e.getStackTrace());
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

  private ABike createABikeFromJson(JsonObject body) {
    String id = body.getString("id");
    JsonObject loc = body.getJsonObject("location");
    float x = loc.getFloat("x");
    float y = loc.getFloat("y");
    ABikeState state = ABikeState.valueOf(body.getString("state"));
    int batteryLevel = body.getInteger("batteryLevel");
    BikeType type = BikeType.AUTONOMOUS;
    if (body.containsKey("type")) {
      type = BikeType.valueOf(body.getString("type"));
    }
    return ABikeFactory.getInstance().create(id, new P2d(x, y), state, batteryLevel, type);
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("BikeUpdateAdapter stopped and Kafka consumer executor shut down.");
  }
}
