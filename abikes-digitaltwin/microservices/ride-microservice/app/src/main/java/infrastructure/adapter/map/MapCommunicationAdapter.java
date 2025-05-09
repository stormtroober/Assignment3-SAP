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

  @Override
  public void notifyStartRide(String bikeId, BikeType type, String userId) {
    JsonObject message =
        new JsonObject()
            .put("username", userId)
            .put("bikeName", bikeId)
            .put("bikeType", type)
            .put("action", "start");

    String topicName = Topics.RIDE_MAP_UPDATE.getTopicName();
    logger.info(
        "Sending start ride notification to Kafka topic: {} for user: {} and bike: {}, type: {}",
        topicName,
        userId,
        type,
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
  public void notifyEndRide(String bikeId, BikeType type, String userId) {
    JsonObject message =
        new JsonObject()
            .put("username", userId)
            .put("bikeName", bikeId)
            .put("bikeType", type)
            .put("action", "stop");

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
