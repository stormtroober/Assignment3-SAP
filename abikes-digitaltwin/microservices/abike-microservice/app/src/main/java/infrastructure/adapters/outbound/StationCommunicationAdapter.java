package infrastructure.adapters.outbound;

import application.ports.StationCommunicationPort;
import domain.model.Station;
import domain.model.StationMapper;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

public class StationCommunicationAdapter implements StationCommunicationPort {
  private final Producer<String, String> producer;
  private final String topicName = Topics.STATION_UPDATES.getTopicName();

  public StationCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(Station station) {
    System.out.println("Sending Station update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, "station:" + station.getId(), StationMapper.toJson(station).encode()));
  }

  public void sendAllUpdates(List<Station> stations) {
    System.out.println("Sending all Station updates to Kafka topic: " + topicName);
    for(Station station : stations) {
      producer.send(
          new ProducerRecord<>(topicName, "station:" + station.getId(), StationMapper.toJson(station).encode()));
    }
  }
}
