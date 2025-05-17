import application.RestRideServiceAPIImpl;
import application.ports.*;
import domain.model.repository.EBikeRepository;
import domain.model.repository.InMemoryEBikeRepository;
import domain.model.repository.InMemoryUserRepository;
import domain.model.repository.UserRepository;
import infrastructure.adapter.ebike.BikeConsumerAdapter;
import infrastructure.adapter.ebike.EBikeCommunicationAdapter;
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
              EbikeCommunicationPort ebikeCommunicationAdapter =
                  new EBikeCommunicationAdapter(vertx);
              ebikeCommunicationAdapter.init();
              MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter();
                mapCommunicationAdapter.init();
              UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx);
              userCommunicationAdapter.init();

                RestRideServiceAPI service = getRestRideServiceAPI(vertx, ebikeCommunicationAdapter, mapCommunicationAdapter);
                RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, vertx);
              rideServiceVerticle.init();
            });
  }

    private static RestRideServiceAPI getRestRideServiceAPI(Vertx vertx, EbikeCommunicationPort ebikeCommunicationAdapter, MapCommunicationPort mapCommunicationAdapter) {
        EBikeRepository ebikeRepository = new InMemoryEBikeRepository();

        BikeConsumerAdapter bikeConsumerAdapter =
            new BikeConsumerAdapter(ebikeRepository);
        bikeConsumerAdapter.init();

        UserRepository userRepository = new InMemoryUserRepository();

        UserConsumerAdapter userConsumerAdapter =
            new UserConsumerAdapter(userRepository);
        userConsumerAdapter.init();

        return new RestRideServiceAPIImpl(
            new EventPublisherImpl(vertx),
                vertx,
            ebikeRepository,
              userRepository,
                mapCommunicationAdapter,
                ebikeCommunicationAdapter);
    }
}
