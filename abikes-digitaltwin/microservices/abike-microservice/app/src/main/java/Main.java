import application.ABikeServiceImpl;
import application.StationServiceImpl;
import application.ports.ABikeServiceAPI;
import application.ports.StationServiceAPI;
import infrastructure.adapters.map.MapCommunicationAdapter;
import infrastructure.adapters.web.ABikeVerticle;
import infrastructure.adapters.web.RESTABikeAdapter;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoEBikeRepository;
import infrastructure.persistence.MongoStationRepository;
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
              MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
              // Repository
              MongoEBikeRepository repository = new MongoEBikeRepository(mongoClient);
              MongoStationRepository repositoryStation = new MongoStationRepository(mongoClient);

              MapCommunicationAdapter mapCommunicationAdapter = new MapCommunicationAdapter();
              // Services
              StationServiceAPI stationService = new StationServiceImpl(repositoryStation);
              ABikeServiceAPI aBikeService =
                  new ABikeServiceImpl(repository, mapCommunicationAdapter, stationService);

              stationService
                  .createStation("station1", 10.0f, 10.0f)
                  .thenAccept(
                      station ->
                          System.out.println("Station1 created: " + station.encodePrettily()))
                  .exceptionally(
                      ex -> {
                        System.err.println("Failed to create station1: " + ex.getMessage());
                        return null;
                      });

              stationService
                  .createStation("station2", 100.0f, 100.0f)
                  .thenAccept(
                      station ->
                          System.out.println("Station2 created: " + station.encodePrettily()))
                  .exceptionally(
                      ex -> {
                        System.err.println("Failed to create station2: " + ex.getMessage());
                        return null;
                      });

              RESTABikeAdapter restABikeAdapter = new RESTABikeAdapter(aBikeService);
              // RideCommunicationAdapter rideCommunicationAdapter =
              // new RideCommunicationAdapter(eBikeService, vertx); // Port for
              // RideCommunicationAdapter
              ABikeVerticle aBikeVerticle = new ABikeVerticle(restABikeAdapter, vertx);
              // rideCommunicationAdapter.init();
              aBikeVerticle.init();
            });
  }
}
