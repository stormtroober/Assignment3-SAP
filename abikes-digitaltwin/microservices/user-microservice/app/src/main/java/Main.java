import application.UserEventPublisherImpl;
import application.UserServiceEventSourcedImpl;
import application.ports.EventStore;
import application.ports.UserEventPublisher;
import application.ports.UserServiceAPI;
import infrastructure.adapters.inbound.RideConsumerAdapter;
import infrastructure.adapters.outbound.RideProducerAdapter;
import infrastructure.adapters.web.RESTUserAdapter;
import infrastructure.adapters.web.UserVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoEventStore;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
    config
        .load()
        .onSuccess(
            conf -> {
              logger.info("Configuration loaded: " + conf.encodePrettily());
              KafkaProperties kafkaProperties = new KafkaProperties(config);

              MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());

              UserEventPublisher UserEventPublisher = new UserEventPublisherImpl(vertx);

              EventStore eventStore = new MongoEventStore(mongoClient);

              UserServiceAPI service =
                  new UserServiceEventSourcedImpl(eventStore, UserEventPublisher);
              RESTUserAdapter controller = new RESTUserAdapter(service, vertx);
              UserVerticle userVerticle = new UserVerticle(controller, vertx);
              RideConsumerAdapter rideAdapter =
                  new RideConsumerAdapter(service, vertx, kafkaProperties);
              userVerticle.init();
              rideAdapter.init();

              RideProducerAdapter rideProducerAdapter =
                  new RideProducerAdapter(vertx, kafkaProperties);
            });
  }
}
