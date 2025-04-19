package infrastructure.adapter.ebike;

import application.ports.EbikeCommunicationPort;
import application.ports.EventPublisher;
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

// TODO: I would change name, because now sends updates also to map
public class EBikeCommunicationAdapter extends AbstractVerticle implements EbikeCommunicationPort {
  private final WebClient webClient;
  private final String ebikeServiceUrl;
  private final Vertx vertx;
  private Producer<String, String> producer;

  public EBikeCommunicationAdapter(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    JsonObject ebikeConfig = config.getEBikeAdapterAddress();
    this.ebikeServiceUrl =
        "http://" + ebikeConfig.getString("name") + ":" + ebikeConfig.getInteger("port");
    this.vertx = vertx;
    producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx
        .eventBus()
        .consumer(
            EventPublisher.RIDE_UPDATE_ADDRESS_EBIKE,
            message -> {
              if (message.body() instanceof JsonObject) {
                JsonObject update = (JsonObject) message.body();
                if (update.containsKey("id")) {
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
              System.out.println("EBikeCommunicationAdapter deployed successfully with ID: " + id);
            })
        .onFailure(
            err -> {
              System.err.println("Failed to deploy EBikeCommunicationAdapter: " + err.getMessage());
            });
  }

  @Override
  public void sendUpdate(JsonObject ebike) {
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
  public CompletableFuture<JsonObject> getEbike(String id) {
    System.out.println("Sending request to ebike-microservice -> getEbike(" + id + ")");
    CompletableFuture<JsonObject> future = new CompletableFuture<>();

    webClient
        .getAbs(ebikeServiceUrl + "/api/ebikes/" + id)
        .send()
        .onSuccess(
            response -> {
              if (response.statusCode() == 200) {
                System.out.println("EBike received successfully");
                future.complete(response.bodyAsJsonObject());
              } else {
                System.err.println("Failed to get EBike: " + response.statusCode());
                future.completeExceptionally(
                    new RuntimeException("Failed to get Ebike: " + response.statusCode()));
              }
            })
        .onFailure(
            err -> {
              System.err.println("Failed to get EBike: " + err.getMessage());
              future.completeExceptionally(err);
            });

    return future;
  }
}
