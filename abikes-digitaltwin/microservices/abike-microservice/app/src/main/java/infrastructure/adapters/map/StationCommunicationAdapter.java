package infrastructure.adapters.map;

import application.ports.CommunicationPort;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class StationCommunicationAdapter implements CommunicationPort {
  private final Producer<String, String> producer;
  private final String topicName = Topics.STATION_UPDATES.getTopicName();

  public StationCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(JsonObject station) {
    System.out.println("Sending Station update to Kafka topic: " + topicName);
    // System.out.println("Station: " + station);
    producer.send(new ProducerRecord<>(topicName, "station:" + station.getString("id"), station.encode()));
  }

  public void sendAllUpdates(JsonArray stations) {
    System.out.println("Sending all Station updates to Kafka topic: " + topicName);
    for (int i = 0; i < stations.size(); i++) {
      JsonObject station = stations.getJsonObject(i);
      producer.send(new ProducerRecord<>(topicName, "station:" + station.getString("id"), station.encode()));
    }
  }
}
