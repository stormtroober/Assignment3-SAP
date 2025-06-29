package infrastructure.adapters.outbound;

import application.ports.StationCommunicationPort;
import domain.model.Station;
import infrastructure.adapters.kafkatopic.Topics;
import infrastructure.proto.StationProtos;
import infrastructure.utils.KafkaProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

import static domain.model.StationFactory.MAX_SLOTS;

public class StationCommunicationAdapter implements StationCommunicationPort {
  private final Producer<String, byte[]> producer;
  private final String topicName = Topics.STATION_UPDATES.getTopicName();

  public StationCommunicationAdapter(KafkaProperties kafkaProperties) {
    this.producer = new KafkaProducer<>(kafkaProperties.getProducerProtobufProperties());
  }

  @Override
  public void sendUpdate(Station station) {
    System.out.println("Sending Station update to Kafka topic: " + topicName);
    StationProtos.Station stationProto = buildStationProto(station);
    producer.send(
            new ProducerRecord<>(topicName, "station:" + station.getId(), stationProto.toByteArray()));
  }

  public void sendAllUpdates(List<Station> stations) {
    System.out.println("Sending all Station updates to Kafka topic: " + topicName);
    for(Station station : stations) {
      StationProtos.Station stationProto = buildStationProto(station);
      producer.send(
              new ProducerRecord<>(topicName, "station:" + station.getId(), stationProto.toByteArray()));
    }
  }

  private StationProtos.Station buildStationProto(Station station) {
    StationProtos.Location.Builder locationBuilder = StationProtos.Location.newBuilder()
            .setX(station.getPosition().getX())
            .setY(station.getPosition().getY());

    StationProtos.Station.Builder stationBuilder = StationProtos.Station.newBuilder()
            .setId(station.getId())
            .setLocation(locationBuilder)
            .setMaxSlots(MAX_SLOTS);

    station.getSlots().forEach(slot -> {
      StationProtos.Slot.Builder slotBuilder = StationProtos.Slot.newBuilder()
              .setId(slot.getId());

      if (slot.getAbikeId() != null) {
        slotBuilder.setAbikeId(slot.getAbikeId());
      }

      stationBuilder.addSlots(slotBuilder);
    });

    return stationBuilder.build();
  }
}