import application.EBikeServiceImpl;
import infrastructure.adapters.inbound.RideCommunicationAdapter;
import infrastructure.adapters.outbound.BikeUpdateAdapter;
import infrastructure.adapters.web.EBikeVerticle;
import infrastructure.adapters.web.RESTEBikeAdapter;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoEBikeRepository;
import infrastructure.utils.KafkaProperties;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

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

              MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
              MongoEBikeRepository repository = new MongoEBikeRepository(mongoClient);
              EBikeVerticle eBikeVerticle = getEBikeVerticle(kafkaProperties, repository, vertx);
              eBikeVerticle.init();
            });
  }

  private static EBikeVerticle getEBikeVerticle(
      KafkaProperties kafkaProperties, MongoEBikeRepository repository, Vertx vertx) {
    BikeUpdateAdapter bikeUpdateAdapter = new BikeUpdateAdapter(kafkaProperties);
    EBikeServiceImpl service = new EBikeServiceImpl(repository, bikeUpdateAdapter);
    RESTEBikeAdapter restEBikeAdapter = new RESTEBikeAdapter(service);
    RideCommunicationAdapter rideCommunicationAdapter =
        new RideCommunicationAdapter(service, kafkaProperties);
    rideCommunicationAdapter.init();

    return new EBikeVerticle(restEBikeAdapter, vertx);
  }
}
