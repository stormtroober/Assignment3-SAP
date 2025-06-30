package infrastructure.adapters.outbound;

import application.ports.BikeCommunicationPort;
import domain.events.EBikeUpdate;
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

import static domain.model.EBikeMapper.toAvro;

public class BikeUpdateAdapter implements BikeCommunicationPort {
  private final Producer<String, EBikeUpdate> producer;
  private final String topicName = Topics.EBIKE_UPDATES.getTopicName();

  public BikeUpdateAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
  }

  @Override
  public void sendUpdate(EBike ebike) {
    System.out.println("Sending EBike update to Kafka topic: " + topicName);
    EBikeUpdate avroUpdate = toAvro(ebike);
    producer.send(new ProducerRecord<>(topicName, ebike.getId(), avroUpdate));
  }

  public void sendAllUpdates(List<EBike> ebikes) {
    System.out.println("Sending all EBike updates to Kafka topic: " + topicName);
    for (EBike ebike : ebikes) {
      EBikeUpdate avroUpdate = toAvro(ebike);
      producer.send(new ProducerRecord<>(topicName, ebike.getId(), avroUpdate));
    }
  }
}
