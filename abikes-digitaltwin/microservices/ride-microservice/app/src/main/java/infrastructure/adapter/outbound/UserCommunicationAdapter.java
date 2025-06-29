package infrastructure.adapter.outbound;

import application.ports.EventPublisher;
import application.ports.UserCommunicationPort;
import domain.model.P2d;
import domain.model.User;
import domain.model.repository.UserRepository;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.repository.DispatchRepository;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import ride.RideUser;

public class UserCommunicationAdapter implements UserCommunicationPort {
  private final Vertx vertx;
  private Producer<String, byte[]> producer;
  private final UserRepository userRepository;
  private final DispatchRepository dispatchRepository;
  private final KafkaProperties kafkaProperties;

  public UserCommunicationAdapter(
      Vertx vertx,
      UserRepository userRepository,
      DispatchRepository dispatchRepository,
      KafkaProperties kafkaProperties) {
    this.vertx = vertx;
    this.userRepository = userRepository;
    this.dispatchRepository = dispatchRepository;
    this.kafkaProperties = kafkaProperties;
  }

  public void init() {
    producer = new KafkaProducer<>(kafkaProperties.getProducerProtobufProperties());
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
    try {
      RideUser.RideUserUpdate userUpdate =
          RideUser.RideUserUpdate.newBuilder()
              .setUsername(user.getString("username"))
              .setCredit(user.getInteger("credit"))
              .build();

      producer.send(
          new ProducerRecord<>(topicName, userUpdate.getUsername(), userUpdate.toByteArray()),
          (metadata, exception) -> {
            if (exception == null) {
              System.out.println("User update sent successfully");
            } else {
              System.err.println("Failed to send User update: " + exception.getMessage());
            }
          });
    } catch (Exception e) {
      System.err.println("Error building protobuf UserUpdate: " + e.getMessage());
    }
  }

  @Override
  public void addDispatch(String userId, String bikeId, P2d userPosition) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      dispatchRepository.saveDispatchPosition(userId, bikeId, userPosition); // Save dispatch
      sendDispatchMessage(user, bikeId, userPosition, "dispatch");
    } else {
      System.err.println("User not found for dispatch: " + userId);
    }
  }

  @Override
  public void removeDispatch(String userId, String bikeId, boolean arrived) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      P2d userPosition = dispatchRepository.getDispatchPosition(userId, bikeId);
      dispatchRepository.removeDispatchPosition(userId, bikeId);
      if (userPosition != null) {
        if (arrived) {
          sendDispatchMessage(user, bikeId, userPosition, "arrived");
        } else {
          sendDispatchMessage(user, bikeId, userPosition, "notArrived");
        }
      }
    } else {
      System.err.println("User not found for removing dispatch: " + userId);
    }
  }

  private void sendDispatchMessage(User user, String bikeId, P2d userPosition, String status) {
    String topicName = Topics.RIDE_BIKE_DISPATCH.getTopicName();
    CompletableFuture<Void> result = new CompletableFuture<>();
    try {
      RideUser.BikeDispatch dispatchMessage =
          RideUser.BikeDispatch.newBuilder()
              .setPositionX(userPosition.x())
              .setPositionY(userPosition.y())
              .setBikeId(bikeId)
              .setStatus(status)
              .build();

      producer.send(
          new ProducerRecord<>(topicName, user.getId(), dispatchMessage.toByteArray()),
          (metadata, exception) -> {
            if (exception == null) {
              System.out.println(status + " message sent successfully");
              result.complete(null);
            } else {
              System.err.println(
                  "Failed to send " + status + " message: " + exception.getMessage());
              result.completeExceptionally(exception);
            }
          });
    } catch (Exception e) {
      System.err.println("Error building protobuf Dispatch: " + e.getMessage());
      result.completeExceptionally(e);
    }
  }
}
