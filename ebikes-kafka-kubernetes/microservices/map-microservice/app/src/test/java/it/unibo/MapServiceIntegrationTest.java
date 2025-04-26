// package it.unibo;
//
//
// import application.RestMapServiceAPIImpl;
// import application.ports.EventPublisher;
// import application.ports.RestMapServiceAPI;
// import domain.model.EBike;
// import infrastructure.config.ServiceConfiguration;
// import io.vertx.core.Vertx;
// import io.vertx.core.json.JsonObject;
// import io.vertx.ext.web.client.WebClient;
// import io.vertx.junit5.VertxExtension;
// import io.vertx.junit5.VertxTestContext;
// import infrastructure.adapter.ebike.BikeUpdateAdapter;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
//
// import java.util.List;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
//
// @ExtendWith(VertxExtension.class)
// public class MapServiceIntegrationTest {
//
//    private Vertx vertx;
//    private WebClient client;
//    private RestMapServiceAPI mapService;
//    private EventPublisher eventPublisher;
//    private static final int PORT = 8082;
//
//    @BeforeEach
//    void setUp(VertxTestContext testContext) {
//        vertx = Vertx.vertx();
//        client = WebClient.create(vertx);
//
//        // Initialize components
//        eventPublisher = new TestEventPublisher();
//        mapService = new RestMapServiceAPIImpl(eventPublisher);
//
//        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
//        config.load().onSuccess(conf -> vertx.deployVerticle(new BikeUpdateAdapter(mapService,
// vertx))
//                .onComplete(ar -> {
//                    if (ar.succeeded()) {
//                        vertx.setTimer(1000, id -> testContext.completeNow());
//                    } else {
//                        testContext.failNow(ar.cause());
//                    }
//                }));
//    }
//
//    @AfterEach
//    void tearDown(VertxTestContext testContext) {
//        vertx.close().onComplete(testContext.succeeding(v -> testContext.completeNow()));
//    }
//
//    @Test
//    void testUpdateEBike(VertxTestContext testContext) {
//        JsonObject bikeJson = new JsonObject()
//                .put("id", "bike1")
//                .put("location", new JsonObject()
//                        .put("x", 10.0)
//                        .put("y", 20.0))
//                .put("state", "AVAILABLE")
//                .put("batteryLevel", 100);
//
//        client.put(PORT, "localhost", "/updateEBike")
//                .sendJsonObject(bikeJson)
//                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
//                    assertEquals(200, response.statusCode());
//
//                    // Verify the published bikes
//                    List<EBike> publishedBikes = ((TestEventPublisher)
// eventPublisher).getPublishedBikes();
//                    assertEquals(1, publishedBikes.size());
//                    EBike bike = publishedBikes.getFirst();
//                    assertEquals("bike1", bike.getBikeName());
//                    assertEquals(10.0, bike.getPosition().x());
//                    assertEquals(20.0, bike.getPosition().y());
//                    assertEquals("AVAILABLE", bike.getState().toString());
//                    assertEquals(100, bike.getBatteryLevel());
//
//                    testContext.completeNow();
//                })));
//    }
//
//    // Test implementation of EventPublisher for integration tests
//    private static class TestEventPublisher implements EventPublisher {
//        private List<EBike> publishedBikes;
//        @Override
//        public void publishBikesUpdate(List<EBike> bikes) {}
//
//        @Override
//        public void publishUserBikesUpdate(List<EBike> bikes, String username) {}
//
//        @Override
//        public void publishUserAvailableBikesUpdate(List<EBike> bikes) {
//            this.publishedBikes = bikes;
//            System.out.println("Available bikes: " + bikes);
//        }
//
//        @Override
//        public void publishStopRide(String username) {}
//
//        public List<EBike> getPublishedBikes() {
//            return publishedBikes;
//        }
//    }
// }
