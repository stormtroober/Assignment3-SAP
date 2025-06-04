package infrastructure.adapter.map;

import application.ports.MapCommunicationPort;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapCommunicationAdapter implements MapCommunicationPort {
  private static final Logger logger = LoggerFactory.getLogger(MapCommunicationAdapter.class);
  private Producer<String, String> producer;
    private final KafkaProperties kafkaProperties;

  public MapCommunicationAdapter(
        KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

  public void init() {
    // Initialize Kafka producer
    producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void notifyStartRide(String bikeId, String userId) {
    JsonObject message =
        new JsonObject().put("username", userId).put("bikeName", bikeId).put("action", "start");

    String topicName = Topics.RIDE_MAP_UPDATE.getTopicName();
    logger.info(
        "Sending start ride notification to Kafka topic: {} for user: {} and bike: {}",
        topicName,
        userId,
        bikeId);

    producer.send(
        new ProducerRecord<>(topicName, bikeId, message.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            logger.info(
                "Start ride notification sent successfully to topic: {}, partition: {}, offset: {}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
          } else {
            logger.error("Failed to send start ride notification: {}", exception.getMessage());
          }
        });
  }

  @Override
  public void notifyEndRide(String bikeId, String userId) {
    JsonObject message =
        new JsonObject().put("username", userId).put("bikeName", bikeId).put("action", "stop");

    String topicName = Topics.RIDE_MAP_UPDATE.getTopicName();
    logger.info(
        "Sending end ride notification to Kafka topic: {} for user: {} and bike: {}",
        topicName,
        userId,
        bikeId);

    producer.send(
        new ProducerRecord<>(topicName, bikeId, message.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            logger.info(
                "End ride notification sent successfully to topic: {}, partition: {}, offset: {}",
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
          } else {
            logger.error("Failed to send end ride notification: {}", exception.getMessage());
          }
        });
  }

  // Method to close the producer when shutting down
  public void close() {
    if (producer != null) {
      producer.close();
      logger.info("Kafka producer closed successfully");
    }
  }
}
