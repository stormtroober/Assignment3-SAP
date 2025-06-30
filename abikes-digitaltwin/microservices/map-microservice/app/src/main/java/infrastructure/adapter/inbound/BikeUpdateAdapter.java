package infrastructure.adapter.inbound;

import application.ports.BikeMapServiceAPI;
import domain.events.EBikeUpdate;
import domain.model.*;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
    // EBike consumer (Avro)
    KafkaConsumer<String, EBikeUpdate> ebikeConsumer =
            new KafkaConsumer<>(kafkaProperties.getAvroConsumerProperties());
    ebikeConsumer.subscribe(List.of(Topics.EBIKE_UPDATES.getTopicName()));

    // ABike consumer (JSON)
    KafkaConsumer<String, String> abikeConsumer =
            new KafkaConsumer<>(kafkaProperties.getConsumerProperties());
    abikeConsumer.subscribe(List.of(Topics.ABIKE_UPDATES.getTopicName()));

    try (ebikeConsumer; abikeConsumer) {
      while (running.get()) {
        // EBike updates
        ConsumerRecords<String, EBikeUpdate> ebikeRecords =
                ebikeConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, EBikeUpdate> record : ebikeRecords) {
          EBikeUpdate avroUpdate = record.value();
          EBike bike = EBikeMapper.fromAvro(avroUpdate);
          bikeMapServiceAPI
                  .updateEBike(bike)
                  .thenAccept(v -> logger.info("EBike {} updated successfully", bike.getId()))
                  .exceptionally(
                          ex -> {
                            logger.error(
                                    "Failed to update EBike {}: {}", bike.getId(), ex.getMessage());
                            return null;
                          });
        }
        ebikeConsumer.commitAsync(
                (offsets, exception) -> {
                  if (exception != null) {
                    logger.error("Failed to commit EBike offsets: {}", exception.getMessage());
                  }
                });

        // ABike updates
        ConsumerRecords<String, String> abikeRecords =
                abikeConsumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : abikeRecords) {
          JsonObject body = new JsonObject(record.value());
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
        }
        abikeConsumer.commitAsync(
                (offsets, exception) -> {
                  if (exception != null) {
                    logger.error("Failed to commit ABike offsets: {}", exception.getMessage());
                  }
                });
      }
    } catch (Exception e) {
      logger.error("Error setting up Kafka consumers: {}", e.getMessage());
    }
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