package infrastructure.adapter.user;

import application.ports.EventPublisher;
import application.ports.UserCommunicationPort;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class UserCommunicationAdapter implements UserCommunicationPort {
  private final Vertx vertx;
  private final Producer<String, String> producer;

  public UserCommunicationAdapter(Vertx vertx, KafkaProperties kafkaProperties) {
    producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
    this.vertx = vertx;
  }

  public void init() {
    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_USER,
            message -> {
              if (message.body() instanceof JsonObject update) {
                if (update.containsKey("username")) {
                  sendUpdate(update);
                }
              }
            });
  }

  @Override
  public void sendUpdate(JsonObject user) {
    String topicName = Topics.RIDE_USER_UPDATE.getTopicName();
    System.out.println("Sending User update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, user.getString("username"), user.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println("User update sent successfully");
          } else {
            System.err.println("Failed to send User update: " + exception.getMessage());
          }
        });
  }
}
