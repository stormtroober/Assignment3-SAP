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
              EbikeCommunicationPort ebikeCommunicationAdapter =
                  new EBikeCommunicationAdapter(vertx, kafkaProperties);
              ebikeCommunicationAdapter.init();
              MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter(kafkaProperties);
              mapCommunicationAdapter.init();
              UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx, kafkaProperties);
              userCommunicationAdapter.init();

              RestRideServiceAPI service =
                  getRestRideServiceAPI(vertx, ebikeCommunicationAdapter, mapCommunicationAdapter, kafkaProperties);
              RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, vertx);
              rideServiceVerticle.init();
            });
  }

  private static RestRideServiceAPI getRestRideServiceAPI(
      Vertx vertx,
      EbikeCommunicationPort ebikeCommunicationAdapter,
      MapCommunicationPort mapCommunicationAdapter,
      KafkaProperties kafkaProperties) {
    EBikeRepository ebikeRepository = new InMemoryEBikeRepository();

    BikeConsumerAdapter bikeConsumerAdapter = new BikeConsumerAdapter(ebikeRepository, kafkaProperties);
    bikeConsumerAdapter.init();

    UserRepository userRepository = new InMemoryUserRepository();

    UserConsumerAdapter userConsumerAdapter = new UserConsumerAdapter(userRepository, kafkaProperties);
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
