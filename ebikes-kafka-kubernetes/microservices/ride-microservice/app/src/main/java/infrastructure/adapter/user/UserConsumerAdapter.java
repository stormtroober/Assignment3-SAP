package infrastructure.adapter.user;


import domain.model.User;
import domain.model.repository.UserRepository;
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

import static infrastructure.adapter.kafkatopic.Topics.USER_UPDATE;

public class UserConsumerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(UserConsumerAdapter.class);
    private final UserRepository userRepository;
    private ExecutorService consumerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public UserConsumerAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void init() {
        startKafkaConsumer();
        logger.info("UserConsumerAdapter initialized");
    }

    private void startKafkaConsumer() {
        consumerExecutor = Executors.newSingleThreadExecutor();
        running.set(true);
        consumerExecutor.submit(this::runKafkaConsumer);
    }

    private void runKafkaConsumer() {
        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(KafkaProperties.getConsumerProperties())) {
            consumer.subscribe(List.of(USER_UPDATE.getTopicName()));
            logger.info("Subscribed to Kafka topic: {}", USER_UPDATE.getTopicName());

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            JsonObject userJson = new JsonObject(record.value());
                            processUserUpdate(userJson);
                        } catch (Exception e) {
                            logger.error("Error processing user update from Kafka: {}", e.getMessage(), e);
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

    private void processUserUpdate(JsonObject userJson) {
        try {
            String username = userJson.getString("username");
            int credit = userJson.getInteger("credit", 0);

            logger.info("Received user update: username={}, credit={}", username, credit);

            User user = new User(username, credit);
            userRepository.save(user);
        } catch (Exception e) {
            logger.error("Failed to process user update: {}", e.getMessage(), e);
        }
    }

    public void stop() {
        running.set(false);
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
        logger.info("UserConsumerAdapter Kafka consumer executor shut down");
    }
}
