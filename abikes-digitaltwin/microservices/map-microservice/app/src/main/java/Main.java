import application.BikeMapServiceAPIImpl;
import application.StationMapServiceAPIImpl;
import application.ports.EventPublisher;
import application.ports.BikeMapServiceAPI;
import application.ports.StationMapServiceAPI;
import infrastructure.adapter.ebike.BikeUpdateAdapter;
import infrastructure.adapter.ride.RideUpdateAdapter;
import infrastructure.adapter.station.StationUpdateAdapter;
import infrastructure.adapter.web.MapServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.utils.EventPublisherImpl;
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
              EventPublisher eventPublisher = new EventPublisherImpl(vertx);
              //Services
              BikeMapServiceAPI bikeService = new BikeMapServiceAPIImpl(eventPublisher);
              StationMapServiceAPI stationMapService = new StationMapServiceAPIImpl(eventPublisher);

              MapServiceVerticle mapServiceVerticle = new MapServiceVerticle(bikeService, vertx);
              BikeUpdateAdapter bikeUpdateAdapter = new BikeUpdateAdapter(bikeService);
              StationUpdateAdapter stationUpdateAdapter = new StationUpdateAdapter(stationMapService);
              RideUpdateAdapter rideUpdateAdapter = new RideUpdateAdapter(bikeService);
              mapServiceVerticle.init();
              bikeUpdateAdapter.init();
              stationUpdateAdapter.init();
              rideUpdateAdapter.init();
            });
  }
}
