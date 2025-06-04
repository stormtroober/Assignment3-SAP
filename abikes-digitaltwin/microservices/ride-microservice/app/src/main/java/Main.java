import application.RestAutonomousRideServiceImpl;
import application.RestSimpleRideServiceImpl;
import application.ports.*;
import domain.model.repository.*;
import infrastructure.adapter.bike.BikeCommunicationAdapter;
import infrastructure.adapter.bike.BikeConsumerAdapter;
import infrastructure.adapter.map.MapCommunicationAdapter;
import infrastructure.adapter.station.StationConsumerAdapter;
import infrastructure.adapter.user.UserCommunicationAdapter;
import infrastructure.adapter.user.UserConsumerAdapter;
import infrastructure.adapter.web.RideServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.repository.DispatchRepository;
import infrastructure.repository.InMemoryDispatchRepository;
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

              MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter(kafkaProperties);
              BikeCommunicationPort bikeCommunicationAdapter = new BikeCommunicationAdapter(vertx, kafkaProperties);

              bikeCommunicationAdapter.init();
              mapCommunicationAdapter.init();

              UserRepository userRepository = new InMemoryUserRepository();
              ABikeRepository abikeRepository = new InMemoryABikeRepository();
              EBikeRepository ebikeRepository = new InMemoryEBikeRepository();
              DispatchRepository dispatchRepository = new InMemoryDispatchRepository();
                StationRepository stationRepository = new InMemoryStationRepository();

              UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx, userRepository, dispatchRepository,
                      kafkaProperties);
              userCommunicationAdapter.init();

              EventPublisher eventPublisher = new EventPublisherImpl(vertx);

              RestSimpleRideService service =
                  new RestSimpleRideServiceImpl(
                      eventPublisher,
                      vertx,
                      userRepository,
                      ebikeRepository,
                      bikeCommunicationAdapter,
                      mapCommunicationAdapter);

              RestAutonomousRideService autonomousRideService =
                  new RestAutonomousRideServiceImpl(
                      eventPublisher,
                      vertx,
                          mapCommunicationAdapter,
                      userCommunicationAdapter,
                      abikeRepository,
                      userRepository,
                          stationRepository
                  );

              RideServiceVerticle rideServiceVerticle =
                  new RideServiceVerticle(service, autonomousRideService, vertx);
              rideServiceVerticle.init();

              // TODO: we need ports here
              UserConsumerAdapter userConsumerAdapter = new UserConsumerAdapter(userRepository, kafkaProperties);
              userConsumerAdapter.init();

              BikeConsumerAdapter bikeConsumerAdapter =
                  new BikeConsumerAdapter(abikeRepository, ebikeRepository, kafkaProperties);
              bikeConsumerAdapter.init();

                StationConsumerAdapter stationConsumerAdapter =
                    new StationConsumerAdapter(stationRepository, kafkaProperties);
                stationConsumerAdapter.init();
            });
  }
}
