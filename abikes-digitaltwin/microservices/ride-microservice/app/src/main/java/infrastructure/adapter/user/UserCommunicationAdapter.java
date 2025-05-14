package infrastructure.adapter.user;

import application.ports.EventPublisher;
import application.ports.UserCommunicationPort;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class UserCommunicationAdapter extends AbstractVerticle implements UserCommunicationPort {
  private final WebClient webClient;
  private final String userServiceUrl;
  private final Vertx vertx;
  private Producer<String, String> producer;

  public UserCommunicationAdapter(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject userConfig = config.getUserAdapterAddress();
    this.userServiceUrl =
        "http://" + userConfig.getString("name") + ":" + userConfig.getInteger("port");
    this.vertx = vertx;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_USER,
            message -> {
              if (message.body() instanceof JsonObject) {
                JsonObject update = (JsonObject) message.body();
                if (update.containsKey("username")) {
                  sendUpdate(update);
                }
              }
            });

    startPromise.complete();
  }

  public void init() {
    vertx
        .deployVerticle(this)
        .onSuccess(
            id -> {
              System.out.println("UserCommunicationAdapter deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              System.err.println("Failed to deploy UserCommunicationAdapter: " + err.getMessage());
            });
    producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
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

  //{
  //
  //  "userId" : "ale",
  //
  //  "positionX" : 1.0,
  //
  //  "positionY" : 50.0
  //
  //}
  @Override
  public CompletableFuture<Void> sendDispatchToRide(JsonObject userPosition) {
    String topicName = Topics.RIDE_BIKE_DISPATCH.getTopicName();
    CompletableFuture<Void> result = new CompletableFuture<>();

    producer.send(
        new ProducerRecord<>(
                topicName, userPosition.getString("userId"), userPosition.encode()),
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println("Dispatch message sent successfully");
            result.complete(null);
          } else {
            System.err.println("Failed to send dispatch message: " + exception.getMessage());
            result.completeExceptionally(exception);
          }
        });

    return result;
  }

  @Override
  public CompletableFuture<JsonObject> getUser(String id) {
    System.out.println("Sending request to user-microservice -> getUser(" + id + ")");
    CompletableFuture<JsonObject> future = new CompletableFuture<>();

    webClient
        .getAbs(userServiceUrl + "/api/users/" + id)
        .send()
        .onSuccess(
            response -> {
              if (response.statusCode() == 200) {
                System.out.println("User received successfully");
                future.complete(response.bodyAsJsonObject());
              } else {
                System.err.println("Failed to get User: " + response.statusCode());
                future.completeExceptionally(
                    new RuntimeException("Failed to get User: " + response.statusCode()));
              }
            })
        .onFailure(
            err -> {
              System.err.println("Failed to get User: " + err.getMessage());
              future.completeExceptionally(err);
            });

    return future;
  }
}
