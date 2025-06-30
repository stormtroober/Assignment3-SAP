package infrastructure.adapter.outbound;

import application.ports.MapCommunicationPort;
import domain.events.BikeActionUpdate;
import domain.events.RideUpdate;
import domain.model.bike.BikeType;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapCommunicationAdapter implements MapCommunicationPort {
  private static final Logger logger = LoggerFactory.getLogger(MapCommunicationAdapter.class);
  private Producer<String, RideUpdate> producer; // Avro producer
  private final KafkaProperties kafkaProperties;

  public MapCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    producer = new KafkaProducer<>(kafkaProperties.getAvroProducerProperties());
  }

  private void sendNotification(
          String bikeId, BikeType type, String userId, String action, String logMessagePrefix) {
    // Build BikeActionUpdate.avsc Avro object
    BikeActionUpdate bikeActionUpdate = BikeActionUpdate.newBuilder()
            .setUsername(userId)
            .setBikeName(bikeId)
            .setBikeType(type.toString())
            .setAction(action)
            .build();

    // Wrap in RideUpdate Avro object
    RideUpdate rideUpdate = RideUpdate.newBuilder()
            .setPayload(bikeActionUpdate)
            .build();

    String topicName = Topics.RIDE_UPDATE.getTopicName();
    logger.info(
            "Sending {} notification to Kafka topic: {} for user: {} and bike: {}, type: {}",
            logMessagePrefix,
            topicName,
            userId,
            bikeId,
            type);

    producer.send(
            new ProducerRecord<>(topicName, bikeId, rideUpdate),
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

  @Override
  public void notifyStartPublicRide(String bikeId, BikeType type) {
    sendPublicRideNotification(bikeId, type, "public_start");
  }

  @Override
  public void notifyEndPublicRide(String bikeId, BikeType type) {
    sendPublicRideNotification(bikeId, type, "public_end");
  }

  private void sendPublicRideNotification(String bikeId, BikeType type, String action) {
    // Build BikeActionUpdate.avsc Avro object (no username for public rides)
    BikeActionUpdate bikeActionUpdate = BikeActionUpdate.newBuilder()
            .setUsername(null)
            .setBikeName(bikeId)
            .setBikeType(type.toString())
            .setAction(action)
            .build();

    RideUpdate rideUpdate = RideUpdate.newBuilder()
            .setPayload(bikeActionUpdate)
            .build();

    String topicName = Topics.RIDE_UPDATE.getTopicName();

    producer.send(
            new ProducerRecord<>(topicName, bikeId, rideUpdate),
            (metadata, exception) -> {
              if (exception == null) {
                logger.info("Notification for {} public ride sent successfully.", action);
              } else {
                logger.error("Failed to send notification for {} public ride", action);
              }
            });
  }

  public void close() {
    if (producer != null) {
      producer.close();
      logger.info("Kafka producer closed successfully");
    }
  }
}