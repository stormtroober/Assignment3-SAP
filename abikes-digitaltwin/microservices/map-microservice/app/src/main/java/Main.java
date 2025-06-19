import application.BikeMapServiceAPIImpl;
import application.StationMapServiceAPIImpl;
import application.ports.BikeMapServiceAPI;
import application.ports.EventPublisher;
import application.ports.StationMapServiceAPI;
import infrastructure.adapter.inbound.BikeUpdateAdapter;
import infrastructure.adapter.inbound.RideUpdateAdapter;
import infrastructure.adapter.inbound.StationUpdateAdapter;
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
              KafkaProperties kafkaProperties = new KafkaProperties(config);

              EventPublisher eventPublisher = new EventPublisherImpl(vertx);
              // Services
              StationMapServiceAPI stationMapService = new StationMapServiceAPIImpl(eventPublisher);
              BikeMapServiceAPI bikeService = new BikeMapServiceAPIImpl(eventPublisher);

              MapServiceVerticle mapServiceVerticle =
                  new MapServiceVerticle(bikeService, stationMapService, vertx);
              BikeUpdateAdapter bikeUpdateAdapter =
                  new BikeUpdateAdapter(bikeService, kafkaProperties);
              StationUpdateAdapter stationUpdateAdapter =
                  new StationUpdateAdapter(stationMapService, kafkaProperties);
                stationUpdateAdapter.init();
              RideUpdateAdapter rideUpdateAdapter =
                  new RideUpdateAdapter(bikeService, kafkaProperties);
              mapServiceVerticle.init();
              bikeUpdateAdapter.init();
              rideUpdateAdapter.init();
            });
  }
}
