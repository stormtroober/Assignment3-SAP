package infrastructure.adapter.map;

import application.ports.MapCommunicationPort;
import domain.model.bike.BikeType;
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

  public MapCommunicationAdapter() {}

  public void init() {
    producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
  }

  private void sendNotification(
      String bikeId, BikeType type, String userId, String action, String logMessagePrefix) {
    JsonObject message =
        new JsonObject()
            .put("username", userId)
            .put("bikeName", bikeId)
            .put("bikeType", type)
            .put("action", action);

    String topicName = Topics.RIDE_MAP_UPDATE.getTopicName();
    logger.info(
        "Sending {} notification to Kafka topic: {} for user: {} and bike: {}, type: {}",
        logMessagePrefix,
        topicName,
        userId,
        bikeId,
        type);

    producer.send(
        new ProducerRecord<>(topicName, bikeId, message.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            logger.info(
                "{} notification sent successfully to topic: {}, partition: {}, offset: {}",
                logMessagePrefix,
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
          } else {
            logger.error(
                "Failed to send {} notification: {}", logMessagePrefix, exception.getMessage());
          }
        });
  }

  @Override
  public void notifyStartRide(String bikeId, BikeType type, String userId) {
    sendNotification(bikeId, type, userId, "start", "start ride");
  }

  @Override
  public void notifyEndRide(String bikeId, BikeType type, String userId) {
    sendNotification(bikeId, type, userId, "stop", "end ride");
  }

  // Method to close the producer when shutting down
  public void close() {
    if (producer != null) {
      producer.close();
      logger.info("Kafka producer closed successfully");
    }
  }
}
