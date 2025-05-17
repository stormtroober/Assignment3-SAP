import application.RestAutonomousRideServiceImpl;
import application.RestSimpleRideServiceImpl;
import application.ports.*;
import domain.model.repository.*;
import infrastructure.adapter.bike.BikeCommunicationAdapter;
import infrastructure.adapter.bike.BikeConsumerAdapter;
import infrastructure.adapter.map.MapCommunicationAdapter;
import infrastructure.adapter.user.UserCommunicationAdapter;
import infrastructure.adapter.user.UserConsumerAdapter;
import infrastructure.adapter.web.RideServiceVerticle;
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
              // Initialize the adapters
//              BikeCommunicationPort ebikeCommunicationAdapter =
//                  new EBikeCommunicationAdapter(vertx);
//              BikeCommunicationPort abikeCommunicationAdapter =
//                  new ABikeCommunicationAdapter(vertx);
              MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter();
              UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx);

//              ebikeCommunicationAdapter.init();
//              abikeCommunicationAdapter.init();

                BikeCommunicationPort bikeCommunicationAdapter = new BikeCommunicationAdapter(vertx);
                bikeCommunicationAdapter.init();

              mapCommunicationAdapter.init();
              userCommunicationAdapter.init();

              UserRepository userRepository = new InMemoryUserRepository();
              ABikeRepository abikeRepository = new InMemoryABikeRepository();
              EBikeRepository ebikeRepository = new InMemoryEBikeRepository();
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
                        bikeCommunicationAdapter,
                      mapCommunicationAdapter,
                      userCommunicationAdapter,
                        abikeRepository,
                        userRepository);

              RideServiceVerticle rideServiceVerticle =
                  new RideServiceVerticle(service, autonomousRideService, vertx);
              rideServiceVerticle.init();

              // TODO: we need the port here
              UserConsumerAdapter userConsumerAdapter = new UserConsumerAdapter(userRepository);
              userConsumerAdapter.init();

              BikeConsumerAdapter bikeConsumerAdapter =
                  new BikeConsumerAdapter(abikeRepository, ebikeRepository);
              bikeConsumerAdapter.init();
            });
  }
}
