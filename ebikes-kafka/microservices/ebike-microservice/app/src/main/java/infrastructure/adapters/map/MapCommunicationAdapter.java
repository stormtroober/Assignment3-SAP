package infrastructure.adapters.map;

import application.ports.MapCommunicationPort;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MapCommunicationAdapter extends AbstractVerticle implements MapCommunicationPort {
    private final HttpClient httpClient;
    private final String microserviceUrl;
    private Vertx vertx;

    public MapCommunicationAdapter(Vertx vertx) {
        this.httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(30)
        );
        JsonObject configuration = ServiceConfiguration.getInstance(vertx).getMapAdapterConfig();
        this.microserviceUrl = "http://"+configuration.getString("name")+":"+configuration.getInteger("port");
        this.vertx = vertx;
    }

    @Override
    public void sendUpdate(JsonObject ebike) {
        System.out.println("Sending EBike update to Map microservice");
        System.out.println(ebike.encodePrettily());
        System.out.println("to -> " + microserviceUrl);

        String[] urlParts = microserviceUrl.replace("http://", "").split(":");
        String host = urlParts[0];
        int port = Integer.parseInt(urlParts[1].split("/")[0]);

        httpClient.request(HttpMethod.PUT, port, host, "/updateEBike")
                .compose(req -> req
                        .putHeader("content-type", "application/json")
                        .send(Buffer.buffer(ebike.encode())))
                .onSuccess(response -> {
                    System.out.println("EBike update sent successfully with status code: " + response.statusCode());
                })
                .onFailure(err -> {
                    System.err.println("Failed to send EBike update: " + err.getMessage());
                    err.printStackTrace();
                });
    }

    public void sendAllUpdates(JsonArray ebikes) {
        System.out.println("Sending all EBike updates to Map microservice");
        System.out.println(ebikes.encodePrettily());
        System.out.println("to -> " + microserviceUrl);

        String[] urlParts = microserviceUrl.replace("http://", "").split(":");
        String host = urlParts[0];
        int port = Integer.parseInt(urlParts[1].split("/")[0]);

        httpClient.request(HttpMethod.PUT, port, host, "/updateEBikes")
                .compose(req -> req
                        .putHeader("content-type", "application/json")
                        .send(Buffer.buffer(ebikes.encode())))
                .onSuccess(response -> {
                    System.out.println("All EBike updates sent successfully with status code: " + response.statusCode());
                })
                .onFailure(err -> {
                    System.err.println("Failed to send all EBike updates: " + err.getMessage());
                    err.printStackTrace();
                });
    }

    @Override
    public void start() {
        System.out.println("MapCommunicationAdapter verticle started");
    }

    public void init() {
        this.vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("MapCommunicationAdapter deployed successfully with ID: " + id);

        }).onFailure(err -> {
            System.err.println("Failed to deploy MapCommunicationAdapter: " + err.getMessage());
        });
    }
}