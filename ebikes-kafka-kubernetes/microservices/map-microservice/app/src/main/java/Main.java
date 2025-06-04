import application.RestMapServiceAPIImpl;
import application.ports.EventPublisher;
import application.ports.RestMapServiceAPI;
import infrastructure.adapter.ebike.BikeUpdateAdapter;
import infrastructure.adapter.ride.RideUpdateAdapter;
import infrastructure.adapter.web.MapServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.EventPublisherImpl;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;

public class Main {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    config
        .load()
        .onSuccess(
            conf -> {
              System.out.println("Configuration loaded: " + conf.encodePrettily());
                KafkaProperties properties = new KafkaProperties(config);

              EventPublisher eventPublisher = new EventPublisherImpl(vertx);
              RestMapServiceAPI service = new RestMapServiceAPIImpl(eventPublisher);
              MapServiceVerticle mapServiceVerticle = new MapServiceVerticle(service, vertx);
              BikeUpdateAdapter bikeUpdateAdapter = new BikeUpdateAdapter(service, properties);
              RideUpdateAdapter rideUpdateAdapter = new RideUpdateAdapter(service, properties);
              mapServiceVerticle.init();
              bikeUpdateAdapter.init();
              rideUpdateAdapter.init();
            });
  }
}
