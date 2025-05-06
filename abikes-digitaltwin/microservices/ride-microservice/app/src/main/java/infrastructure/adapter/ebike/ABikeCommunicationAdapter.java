package infrastructure.adapter.ebike;

import application.ports.BikeCommunicationPort;
import application.ports.EventPublisher;
import infrastructure.adapter.kafkatopic.Topics;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.CompletableFuture;

public class ABikeCommunicationAdapter extends AbstractVerticle implements BikeCommunicationPort {
    private final WebClient webClient;
    private final String abikeServiceUrl;
    private final Vertx vertx;
    private Producer<String, String> producer;

    public ABikeCommunicationAdapter(Vertx vertx) {
        this.webClient = WebClient.create(vertx);
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        JsonObject abikeConfig = config.getABikeAdapterAddress();
        this.abikeServiceUrl =
                "http://" + abikeConfig.getString("name") + ":" + abikeConfig.getInteger("port");
        this.vertx = vertx;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        vertx
                .eventBus()
                .consumer(
                        //Sostituire con evento di dominio nuova simulazione
                        EventPublisher.RIDE_UPDATE_ADDRESS_ABIKE,
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
                            System.out.println("ABikeCommunicationAdapter deployed successfully with ID: " + id);
                        })
                .onFailure(
                        err -> {
                            System.err.println("Failed to deploy ABikeCommunicationAdapter: " + err.getMessage());
                        });
        producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
    }

    //Manda update alla A-Bike
    @Override
    public void sendUpdate(JsonObject aBike) {
        String topicName = Topics.USER_RIDE_CALL.getTopicName();
        System.out.println("Sending ABike update to Kafka topic: " + topicName);
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

    @Override
    public CompletableFuture<JsonObject> getBike(String id) {
        System.out.println("Sending request to abike-microservice -> getBike(" + id + ")");
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        webClient
                .getAbs(abikeServiceUrl + "/api/abikes/" + id)
                .send()
                .onSuccess(
                        response -> {
                            if (response.statusCode() == 200) {
                                System.out.println("ABike received successfully");
                                future.complete(response.bodyAsJsonObject());
                            } else {
                                System.err.println("Failed to get ABike: " + response.statusCode());
                                future.completeExceptionally(
                                        new RuntimeException("Failed to get Abike: " + response.statusCode()));
                            }
                        })
                .onFailure(
                        err -> {
                            System.err.println("Failed to get ABike: " + err.getMessage());
                            future.completeExceptionally(err);
                        });

        return future;
    }

}
