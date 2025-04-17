import application.UserServiceImpl;
import application.ports.UserEventPublisher;
import application.ports.UserServiceAPI;
import infrastructure.utils.UserEventPublisherImpl;
import infrastructure.adapters.ride.RideCommunicationAdapter;
import infrastructure.adapters.web.RESTUserAdapter;
import infrastructure.adapters.web.UserVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoUserRepository;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            logger.info("Configuration loaded: " + conf.encodePrettily());
            MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
            MongoUserRepository repository = new MongoUserRepository(mongoClient);
            UserEventPublisher UserEventPublisher = new UserEventPublisherImpl(vertx);
            UserServiceAPI service = new UserServiceImpl(repository, UserEventPublisher);
            RESTUserAdapter controller = new RESTUserAdapter(service, vertx);
            UserVerticle userVerticle = new UserVerticle(controller, vertx);
            RideCommunicationAdapter rideAdapter = new RideCommunicationAdapter(service, vertx);
            userVerticle.init();
            rideAdapter.init();
        });
    }
}