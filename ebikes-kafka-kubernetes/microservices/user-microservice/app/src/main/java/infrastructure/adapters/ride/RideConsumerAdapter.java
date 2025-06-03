package infrastructure.adapters.ride;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideConsumerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideConsumerAdapter.class);
  private final UserServiceAPI userService;
    private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final KafkaProperties kafkaProperties;

  public RideConsumerAdapter(UserServiceAPI userService, KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
    this.userService = userService;
  }

  public void init() {
    startKafkaConsumer();
  }

  private void startKafkaConsumer() {
    consumerExecutor = Executors.newSingleThreadExecutor();
    running.set(true);
    consumerExecutor.submit(this::runKafkaConsumer);
  }

  private void runKafkaConsumer() {
    KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaProperties.getConsumerProperties());

    try (consumer) {
      // Subscribe to the ride-user-update topic
      consumer.subscribe(List.of(RIDE_USER_UPDATE.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", RIDE_USER_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject user = new JsonObject(record.value());
              logger.info("Received user update from Kafka: {}", user);

              // Process the user update using the existing updateUser method
              processUserUpdate(user);
            } catch (Exception e) {
              logger.error("Invalid user data from Kafka: {}", e.getMessage());
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

  private void processUserUpdate(JsonObject user) {

    try {
      userService
          .updateUser(user)
          .thenAccept(
              updatedUser -> {
                if (updatedUser != null) {
                  logger.info("User updated via Kafka: {}", updatedUser);
                } else {
                  logger.error("User not found via Kafka update: {}", user.getString("username"));
                }
              })
          .exceptionally(
              e -> {
                logger.error("Error processing user update from Kafka", e);
                return null;
              });
    } catch (Exception e) {
      logger.error("Invalid JSON format in Kafka message", e);
    }
  }

  public void stop() {
    running.set(false);
    if (consumerExecutor != null) {
      consumerExecutor.shutdownNow();
    }
    logger.info("RideCommunicationAdapter Kafka consumer executor shut down.");
  }
}
