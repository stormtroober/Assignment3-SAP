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
import domain.events.UserUpdate;

public class RideProducerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(RideProducerAdapter.class);
  private final Producer<String, Object> producer;
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
                JsonObject json = new JsonObject(message.body().toString());

                // Build the Avro record from JSON
                UserUpdate userUpdate = UserUpdate.newBuilder()
                        .setUsername(json.getString("username"))
                        .setType(json.getString("type"))
                        .setCredit(json.getInteger("credit"))
                        .build();

                String key = json.getString("username", "default-user-key");

                logger.info("Sending Avro message to topic: {}", topicName);
                producer.send(new ProducerRecord<>(topicName, key, userUpdate));
              } catch (Exception e) {
                logger.error("Failed to send Avro message", e);
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
