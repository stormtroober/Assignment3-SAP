package infrastructure.adapter.outbound;

import application.ports.BikeCommunicationPort;
import application.ports.EventPublisher;
import domain.events.BikeRideUpdate;
import domain.events.Location;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class BikeCommunicationAdapter implements BikeCommunicationPort {
  private final Vertx vertx;
  private Producer<String, Object> producer; // Avro producer
  private final KafkaProperties kafkaProperties;

  public BikeCommunicationAdapter(Vertx vertx, KafkaProperties kafkaProperties) {
    this.vertx = vertx;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    producer = new KafkaProducer<>(kafkaProperties.getAvroProducerProperties());

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
                  // Build BikeStationUpdate Avro object
                  domain.events.BikeStationUpdate stationUpdate =
                      domain.events.BikeStationUpdate.newBuilder()
                          .setBikeName(update.getString("bikeName"))
                          .setStationId(update.getString("stationId"))
                          .build();

                  // Wrap in RideUpdate Avro object
                  domain.events.RideUpdate rideUpdate =
                      domain.events.RideUpdate.newBuilder().setPayload(stationUpdate).build();

                  producer.send(
                      new ProducerRecord<>(
                          Topics.RIDE_UPDATE.getTopicName(),
                          update.getString("bikeName"),
                          rideUpdate),
                      (metadata, exception) -> {
                        if (exception == null) {
                          System.out.println("ABike station update sent successfully (Avro)");
                        } else {
                          System.err.println(
                              "Failed to send ABike station update (Avro): "
                                  + exception.getMessage());
                        }
                      });
                }
              }
            });

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
    sendBikeRideUpdate(ebike, Topics.EBIKE_RIDE_UPDATE.getTopicName());
  }

  @Override
  public void sendUpdateABike(JsonObject aBike) {
    sendBikeRideUpdate(aBike, Topics.ABIKE_RIDE_UPDATE.getTopicName());
  }

  private void sendBikeRideUpdate(JsonObject bike, String topicName) {
    BikeRideUpdate.Builder builder =
        BikeRideUpdate.newBuilder().setId(bike.getString("id")).setState(bike.getString("state"));

    if (bike.containsKey("location") && bike.getValue("location") != null) {
      JsonObject loc = bike.getJsonObject("location");
      Location location =
          Location.newBuilder().setX(loc.getDouble("x")).setY(loc.getDouble("y")).build();
      builder.setLocation(location);
    } else {
      builder.setLocation(null);
    }

    if (bike.containsKey("batteryLevel")) {
      builder.setBatteryLevel(bike.getInteger("batteryLevel"));
    } else {
      builder.setBatteryLevel(null);
    }

    BikeRideUpdate avroUpdate = builder.build();

    producer.send(
        new ProducerRecord<>(topicName, bike.getString("id"), avroUpdate),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println(topicName + " Avro update sent successfully");
          } else {
            System.err.println(
                "Failed to send " + topicName + " Avro update: " + exception.getMessage());
          }
        });
  }
}
