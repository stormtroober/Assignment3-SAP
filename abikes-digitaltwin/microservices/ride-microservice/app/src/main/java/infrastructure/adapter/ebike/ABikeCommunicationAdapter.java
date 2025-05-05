package infrastructure.adapter.ebike;

import application.ports.BikeCommunicationPort;
import application.ports.EventPublisher;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.concurrent.CompletableFuture;

public class ABikeCommunicationAdapter extends AbstractVerticle implements BikeCommunicationPort {
    private final WebClient webClient;
    private final String ebikeServiceUrl;
    private final Vertx vertx;
    private Producer<String, String> producer;

    public ABikeCommunicationAdapter(Vertx vertx) {
        this.webClient = WebClient.create(vertx);
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        JsonObject ebikeConfig = config.getEBikeAdapterAddress();
        //da updatare con l'indirizzo microservizio ABikes
        this.ebikeServiceUrl =
                "http://" + ebikeConfig.getString("name") + ":" + ebikeConfig.getInteger("port");
        this.vertx = vertx;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        vertx
                .eventBus()
                .consumer(
                        //Sostituire con evento di dominio nuova simulazione
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
                            System.out.println("ABikeCommunicationAdapter deployed successfully with ID: " + id);
                        })
                .onFailure(
                        err -> {
                            System.err.println("Failed to deploy ABikeCommunicationAdapter: " + err.getMessage());
                        });
        producer = new KafkaProducer<>(KafkaProperties.getProducerProperties());
    }

    //Manda update alla A-Bike. Forse anche alla stazione (?)
    @Override
    public void sendUpdate(JsonObject ebike) {

    }

    @Override
    public CompletableFuture<JsonObject> getBike(String id) {
        System.out.println("Sending request to abike-microservice -> getBike(" + id + ")");
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
