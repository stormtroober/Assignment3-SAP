package infrastructure.adapters.outbound;

import application.ports.BikeCommunicationPort;
import domain.model.ABike;
import domain.model.ABikeMapper;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

public class BikeCommunicationAdapter implements BikeCommunicationPort {
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

  public void sendAllUpdates(List<ABike> abikes) {
    System.out.println("Sending all ABike updates to Kafka topic: " + topicName);
    for (ABike abike : abikes) {
      JsonObject abikeJson = ABikeMapper.toJson(abike);
      producer.send(
              new ProducerRecord<>(topicName, "abike:" + abikeJson.getString("id"), abikeJson.encode()));
    }
  }
}
