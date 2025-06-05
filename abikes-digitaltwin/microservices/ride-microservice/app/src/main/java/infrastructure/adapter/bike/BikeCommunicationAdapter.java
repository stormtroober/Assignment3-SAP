package infrastructure.adapter.bike;

import application.ports.BikeCommunicationPort;
import application.ports.EventPublisher;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class BikeCommunicationAdapter implements BikeCommunicationPort {
  private final Vertx vertx;
  private Producer<String, String> producer;
  private final KafkaProperties kafkaProperties;

  public BikeCommunicationAdapter(Vertx vertx,
                                  KafkaProperties kafkaProperties) {
    this.vertx = vertx;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    producer = new KafkaProducer<>(kafkaProperties.getProducerProperties());
    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_ABIKE,
            message -> {
              if (message.body() instanceof JsonObject update) {
                if (update.containsKey("id")) {
                  sendUpdateABike(update);
                }
              }
            });

    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_ABIKE_STATION,
            message -> {
                if (message.body() instanceof JsonObject update) {
                    System.out.println("Received ABike station update: " + update.encode());
                    if (update.containsKey("bikeName") && update.containsKey("stationId")) {
                        String bikeId = update.getString("bikeName");
                        producer.send(
                            new ProducerRecord<>(Topics.RIDE_UPDATE.getTopicName(), bikeId, update.encode()),
                            (metadata, exception) -> {
                                if (exception == null) {
                                    System.out.println("ABike station update sent successfully");
                                } else {
                                    System.err.println("Failed to send ABike station update: " + exception.getMessage());
                                }
                            });
                    }
                }
            }
        );


    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_EBIKE,
            message -> {
              if (message.body() instanceof JsonObject update) {
                if (update.containsKey("id")) {
                  sendUpdateEBike(update);
                }
              }
            });
  }

  @Override
  public void sendUpdateEBike(JsonObject ebike) {
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

  @Override
  public void sendUpdateABike(JsonObject aBike) {
    String topicName = Topics.ABIKE_RIDE_UPDATE.getTopicName();
    //System.out.println("Sending ABike update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, aBike.getString("id"), aBike.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println("ABike update sent successfully");
          } else {
            System.err.println("Failed to send EBike update: " + exception.getMessage());
          }
        });
  }
}
