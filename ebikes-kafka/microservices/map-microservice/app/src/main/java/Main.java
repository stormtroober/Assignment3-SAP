import application.RestMapServiceAPIImpl;
import application.ports.EventPublisher;
import application.ports.RestMapServiceAPI;
import infrastructure.adapter.ebike.BikeUpdateAdapter;
import infrastructure.utils.EventPublisherImpl;
import infrastructure.adapter.web.MapServiceVerticle;
import infrastructure.adapter.ride.RideUpdateAdapter;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.Vertx;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());
            EventPublisher eventPublisher = new EventPublisherImpl(vertx);
            RestMapServiceAPI service = new RestMapServiceAPIImpl(eventPublisher);
            MapServiceVerticle mapServiceVerticle = new MapServiceVerticle(service, vertx);
            BikeUpdateAdapter bikeUpdateAdapter = new BikeUpdateAdapter(service);
            RideUpdateAdapter rideUpdateAdapter = new RideUpdateAdapter(service, vertx);
            mapServiceVerticle.init();
            bikeUpdateAdapter.init();
            rideUpdateAdapter.init();
        });








    }
}