package infrastructure.adapter.ebike;

import application.ports.EbikeCommunicationPort;
import application.ports.EventPublisher;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class EBikeCommunicationAdapter implements EbikeCommunicationPort {
  private final Vertx vertx;
  private Producer<String, String> producer;
  private final KafkaProperties kafkaProperties;

  public EBikeCommunicationAdapter(Vertx vertx, KafkaProperties kafkaProperties) {
    this.vertx = vertx;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_EBIKE,
            message -> {
              if (message.body() instanceof JsonObject) {
                JsonObject update = (JsonObject) message.body();
                if (update.containsKey("id")) {
                  sendUpdate(update);
                }
              }
            });
    producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(JsonObject ebike) {
    String topicName = Topics.EBIKE_RIDE_UPDATE.getTopicName();
    System.out.println("Sending EBike update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, ebike.getString("id"), ebike.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println("EBike update sent successfully");
          } else {
            System.err.println("Failed to send EBike update: " + exception.getMessage());
          }
        });
  }
}
