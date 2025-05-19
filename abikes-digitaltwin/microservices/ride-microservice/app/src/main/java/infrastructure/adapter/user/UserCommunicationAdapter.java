package infrastructure.adapter.user;

import application.ports.EventPublisher;
import application.ports.UserCommunicationPort;
import domain.model.P2d;
import domain.model.User;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class UserCommunicationAdapter implements UserCommunicationPort {
  private final Vertx vertx;
  private Producer<String, String> producer;

  public UserCommunicationAdapter(Vertx vertx) {
    this.vertx = vertx;
  }

  public void init() {
    producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
      vertx
              .eventBus()
              .consumer(
                      EventPublisher.RIDE_UPDATE_ADDRESS_USER,
                      message -> {
                          if (message.body() instanceof JsonObject update) {
                              if (update.containsKey("username")) {
                                  sendUpdate(update);
                              }
                          }
                      });
  }

  @Override
  public void sendUpdate(JsonObject user) {
    String topicName = Topics.RIDE_USER_UPDATE.getTopicName();
    System.out.println("Sending User update to Kafka topic: " + topicName);
    producer.send(
        new ProducerRecord<>(topicName, user.getString("username"), user.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println("User update sent successfully");
          } else {
            System.err.println("Failed to send User update: " + exception.getMessage());
          }
        });
  }

    @Override
    public void addDispatch(User user, String bikeId, P2d userPosition) {
        sendDispatchMessage(user, bikeId, userPosition, "dispatch");
    }

    @Override
    public void removeDispatch(User user, String bikeId, P2d userPosition) {
        sendDispatchMessage(user, bikeId, userPosition, "arrived");
    }

    private void sendDispatchMessage(User user, String bikeId, P2d userPosition, String status) {
        String topicName = Topics.RIDE_BIKE_DISPATCH.getTopicName();
        CompletableFuture<Void> result = new CompletableFuture<>();
        JsonObject message = new JsonObject();
        message.put("positionX", userPosition.x());
        message.put("positionY", userPosition.y());
        message.put("bikeId", bikeId);
        message.put("status", status);
        producer.send(
                new ProducerRecord<>(topicName, user.getId(), message.encode()),
                (metadata, exception) -> {
                    if (exception == null) {
                        System.out.println(status + " message sent successfully");
                        result.complete(null);
                    } else {
                        System.err.println("Failed to send " + status + " message: " + exception.getMessage());
                        result.completeExceptionally(exception);
                    }
                });
    }
}
