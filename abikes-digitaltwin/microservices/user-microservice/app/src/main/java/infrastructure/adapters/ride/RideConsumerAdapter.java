package infrastructure.adapters.ride;

import static infrastructure.adapters.kafkatopic.Topics.RIDE_BIKE_DISPATCH;
import static infrastructure.adapters.kafkatopic.Topics.RIDE_USER_UPDATE;

import application.ports.UserServiceAPI;
import infrastructure.adapters.kafkatopic.Topics;
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
  private final Vertx vertx;
  private final MetricsManager metricsManager;
  private ExecutorService consumerExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public RideConsumerAdapter(UserServiceAPI userService, Vertx vertx) {
    this.userService = userService;
    this.vertx = vertx;
    this.metricsManager = MetricsManager.getInstance();
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
        new KafkaConsumer<>(KafkaProperties.getConsumerProperties());

    try (consumer) {
      //RIDE_USER_UPDATE is for updating coming from a Ride,
        //RIDE_BIKE_DISPATCH is for making appear the user position on the map
      consumer.subscribe(List.of(RIDE_USER_UPDATE.getTopicName(),
              RIDE_BIKE_DISPATCH.getTopicName()));
      logger.info("Subscribed to Kafka topic: {}", RIDE_USER_UPDATE.getTopicName());

      while (running.get()) {
        try {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
          for (ConsumerRecord<String, String> record : records) {
            try {
              JsonObject message = new JsonObject(record.value());
              if (record.topic().equals(Topics.RIDE_BIKE_DISPATCH.getTopicName())) {
                logger.info("Received dispatch message: {}", message);
                processBikeDispatch(message);
              } else if (record.topic().equals(RIDE_USER_UPDATE.getTopicName())) {
                logger.info("Received user update from Kafka: {}", message);
                processUserUpdate(message);
              }
            } catch (Exception e) {
              logger.error("Invalid message from Kafka: {}", e.getMessage());
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
    metricsManager.incrementMethodCounter("updateUser");
    var timer = metricsManager.startTimer();

    try {
      userService
          .updateUser(user)
          .thenAccept(
              updatedUser -> {
                if (updatedUser != null) {
                  logger.info("User updated via Kafka: {}", updatedUser);
                  metricsManager.recordTimer(timer, "updateUser");
                } else {
                  logger.error("User not found via Kafka update: {}", user.getString("username"));
                  metricsManager.recordError(
                      timer, "updateUser", new RuntimeException("User not found"));
                }
              })
          .exceptionally(
              e -> {
                logger.error("Error processing user update from Kafka", e);
                metricsManager.recordError(timer, "updateUser", e);
                return null;
              });
    } catch (Exception e) {
      logger.error("Invalid JSON format in Kafka message", e);
      metricsManager.recordError(timer, "updateUser", e);
    }
  }

  //{
  //
  //  "userId" : "ale",
  //
  //  "positionX" : 1.0,
  //
  //  "positionY" : 50.0
  //
  //}
  private void processBikeDispatch(JsonObject message) {
    logger.info("Dispatching bike to user: {}", message);
    String username = message.getString("userId");
    if (username != null && !username.isBlank()) {
      vertx.eventBus().publish(username, message.encode());
    } else {
      logger.error("Dispatch message missing username: {}", message);
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
