package infrastructure.adapters.outbound;

import application.ports.CommunicationPort;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class BikeCommunicationAdapter implements CommunicationPort {
  private final Producer<String, String> producer;
  private final String topicName = Topics.ABIKE_UPDATES.getTopicName();

  public BikeCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(JsonObject abike) {
    System.out.println("Sending ABike update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, "abike:" + abike.getString("id"), abike.encode()));
  }

  public void sendAllUpdates(JsonArray abikes) {
    System.out.println("Sending all ABike updates to Kafka topic: " + topicName);
    for (int i = 0; i < abikes.size(); i++) {
      JsonObject ebike = abikes.getJsonObject(i);
      producer.send(
          new ProducerRecord<>(topicName, "abike:" + ebike.getString("id"), ebike.encode()));
    }
  }
}
