//package it.unibo;
//
//import application.RestMapServiceAPIImpl;
//import application.ports.EventPublisher;
//import application.ports.RestMapServiceAPI;
//import infrastructure.adapter.ebike.BikeUpdateAdapter;
//import infrastructure.utils.EventPublisherImpl;
//import infrastructure.adapter.web.MapServiceVerticle;
//import infrastructure.config.ServiceConfiguration;
//import io.vertx.core.Vertx;
//import io.vertx.core.http.HttpClient;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.web.client.WebClient;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@ExtendWith(VertxExtension.class)
//public class MapServiceComponentTest {
//
//    private Vertx vertx;
//    private HttpClient client;
//    private WebClient webClient;
//    private static final int BIKE_UPDATE_PORT = 8082;
//
//    @BeforeEach
//    void setUp(VertxTestContext testContext) {
//        vertx = Vertx.vertx();
//        client = vertx.createHttpClient();
//        webClient = WebClient.create(vertx);
//
//        // Initialize components
//        EventPublisher eventPublisher = new EventPublisherImpl(vertx);
//        RestMapServiceAPI mapService = new RestMapServiceAPIImpl(eventPublisher);
//
//        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
//        config.load().onSuccess(conf -> {
//            // Deploy verticles
//            vertx.deployVerticle(new MapServiceVerticle(mapService, vertx))
//                    .compose(id -> vertx.deployVerticle(new BikeUpdateAdapter(mapService, vertx)))
//                    .onComplete(ar -> {
//                        if (ar.succeeded()) {
//                            vertx.setTimer(1000, id -> testContext.completeNow());
//                        } else {
//                            testContext.failNow(ar.cause());
//                        }
//                    });
//        });
//
//    }
//
//    @AfterEach
//    void tearDown(VertxTestContext testContext) {
//        vertx.close().onComplete(testContext.succeeding(v -> testContext.completeNow()));
//    }
//
//    @Test
//    void testUpdateEBikeAndObserveAllBikes(VertxTestContext testContext) {
//        JsonObject bikeJson = new JsonObject()
//                .put("id", "bike1")
//                .put("location", new JsonObject()
//                        .put("x", 10.0)
//                        .put("y", 20.0))
//                .put("state", "AVAILABLE")
//                .put("batteryLevel", 100);
//
//        // Send update request
//        webClient.put(BIKE_UPDATE_PORT, "localhost", "/updateEBike")
//                .putHeader("Content-Type", "application/json")
//                .sendJsonObject(bikeJson)
//                .onComplete(ar -> {
//                    if (ar.failed()) {
//                        testContext.failNow(ar.cause());
//                    }
//                });
//
//        // Set up WebSocket client
//        client.webSocket(8080, "localhost", "/observeAllBikes")
//                .onComplete(testContext.succeeding(webSocket -> webSocket.handler(buffer -> {
//                    JsonArray receivedBike = buffer.toJsonArray();
//                    JsonObject bike = new JsonObject(receivedBike.getString(0));
//                    testContext.verify(() -> {
//                        assertEquals("bike1", bike.getString("bikeName"));
//                        assertEquals(10.0, bike.getJsonObject("position").getDouble("x"));
//                        assertEquals(20.0, bike.getJsonObject("position").getDouble("y"));
//                        assertEquals("AVAILABLE", bike.getString("state"));
//                        assertEquals(100, bike.getInteger("batteryLevel"));
//                        testContext.completeNow();
//                    });
//                })));
//    }
//}