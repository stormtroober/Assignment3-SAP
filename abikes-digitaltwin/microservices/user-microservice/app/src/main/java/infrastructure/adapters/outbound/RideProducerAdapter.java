package infrastructure.adapters.outbound;

import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideProducerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideProducerAdapter.class);
  private final Producer<String, String> producer;
  private final String topicName = Topics.USER_UPDATE.getTopicName();
  private final Vertx vertx;

  public RideProducerAdapter(Vertx vertx, KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
    this.vertx = vertx;
    setupEventBusConsumer();
  }

  private void setupEventBusConsumer() {
    vertx
        .eventBus()
        .consumer(
            "users.update",
            message -> {
              try {
                String userUpdateJson = message.body().toString();
                JsonObject userUpdate = new JsonObject(userUpdateJson);

                String key =
                    userUpdate.containsKey("username")
                        ? userUpdate.getString("username")
                        : "default-user-key";

                logger.info("Forwarding user update to Kafka topic: {}", topicName);
                producer.send(new ProducerRecord<>(topicName, key, userUpdateJson));
              } catch (Exception e) {
                logger.error("Error publishing user update to Kafka", e);
              }
            });
    logger.info("User update Kafka producer started");
  }

  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}
