package infrastructure.adapters.outbound;

import application.ports.BikeCommunicationPort;
import domain.model.EBike;
import domain.model.EBikeMapper;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

public class BikeUpdateAdapter implements BikeCommunicationPort {
  private final Producer<String, String> producer;
  private final String topicName = Topics.EBIKE_UPDATES.getTopicName();

  public BikeUpdateAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(EBike ebike) {
    System.out.println("Sending EBike update to Kafka topic: " + topicName);
    var ebikeJson = EBikeMapper.toJson(ebike);
    producer.send(new ProducerRecord<>(topicName, ebike.getId(), ebikeJson.encode()));
  }

  public void sendAllUpdates(List<EBike> ebikes) {
    System.out.println("Sending all EBike updates to Kafka topic: " + topicName);
    for(EBike ebike : ebikes) {
        var ebikeJson = EBikeMapper.toJson(ebike);
        producer.send(new ProducerRecord<>(topicName, ebike.getId(), ebikeJson.encode()));
    }
  }
}
