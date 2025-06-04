package infrastructure.adapters.map;

import application.ports.MapCommunicationPort;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class MapCommunicationAdapter implements MapCommunicationPort {
  private final Producer<String, String> producer;
  private final String topicName = Topics.EBIKE_UPDATES.getTopicName();

  public MapCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(JsonObject ebike) {
    System.out.println("Sending EBike update to Kafka topic: " + topicName);
    producer.send(new ProducerRecord<>(topicName, ebike.getString("id"), ebike.encode()));
  }

  public void sendAllUpdates(JsonArray ebikes) {
    System.out.println("Sending all EBike updates to Kafka topic: " + topicName);
    for (int i = 0; i < ebikes.size(); i++) {
      JsonObject ebike = ebikes.getJsonObject(i);
      producer.send(new ProducerRecord<>(topicName, ebike.getString("id"), ebike.encode()));
    }
  }
}
