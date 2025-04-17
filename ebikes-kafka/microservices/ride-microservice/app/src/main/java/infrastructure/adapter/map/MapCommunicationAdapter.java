package infrastructure.adapter.map;

import application.ports.MapCommunicationPort;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;

public class MapCommunicationAdapter extends AbstractVerticle implements MapCommunicationPort {
    private final WebClient webClient;
    private final String mapServiceUrl;
    private final Vertx vertx;

    public MapCommunicationAdapter(Vertx vertx) {
        this.webClient = WebClient.create(vertx);
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        JsonObject mapConfig = config.getMapAdapterAddress();
        this.mapServiceUrl = "http://" + mapConfig.getString("name") + ":" + mapConfig.getInteger("port");
        this.vertx = vertx;
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("MapCommunicationAdapter deployed successfully with ID: " + id);
        }).onFailure(err -> {
            System.err.println("Failed to deploy MapCommunicationAdapter: " + err.getMessage());
        });
    }

    @Override
    public void notifyStartRide(String bikeId, String userId) {
        JsonObject request = new JsonObject()
                .put("username", userId)
                .put("bikeName", bikeId);

        webClient.postAbs(mapServiceUrl + "/notifyStartRide")
                .sendJsonObject(request, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("Start ride notification sent successfully");
                    } else {
                        System.err.println("Failed to send start ride notification: " + ar.cause().getMessage());
                    }
                });
    }

    @Override
    public void notifyEndRide(String bikeId, String userId) {
        JsonObject request = new JsonObject()
                .put("username", userId)
                .put("bikeName", bikeId);

        webClient.postAbs(mapServiceUrl + "/notifyStopRide")
                .sendJsonObject(request, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("End ride notification sent successfully");
                    } else {
                        System.err.println("Failed to send end ride notification: " + ar.cause().getMessage());
                    }
                });
    }
}