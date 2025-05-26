import application.ABikeServiceImpl;
import application.StationServiceImpl;
import application.ports.ABikeServiceAPI;
import application.ports.CommunicationPort;
import application.ports.StationRepository;
import application.ports.StationServiceAPI;
import infrastructure.adapters.map.BikeMapCommunicationAdapter;
import infrastructure.adapters.map.StationMapCommunicationAdapter;
import infrastructure.adapters.ride.RideCommunicationAdapter;
import infrastructure.adapters.web.ABikeVerticle;
import infrastructure.adapters.web.RESTABikeAdapter;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoABikeRepository;
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
              MongoABikeRepository repository = new MongoABikeRepository(mongoClient);
              StationRepository repositoryStation = new MongoStationRepository(mongoClient);

              CommunicationPort bikeMapCommunicationAdapter = new BikeMapCommunicationAdapter();
              CommunicationPort stationMapCommunicationAdapter =
                  new StationMapCommunicationAdapter();
              // Services
              StationServiceAPI stationService =
                  new StationServiceImpl(repositoryStation, stationMapCommunicationAdapter);
              ABikeServiceAPI aBikeService =
                  new ABikeServiceImpl(repository, bikeMapCommunicationAdapter, stationService);

              stationService
                  .createStation("station1")
                  .thenAccept(
                      station ->
                          System.out.println("Station1 created: " + station.encodePrettily()))
                  .exceptionally(
                      ex -> {
                        System.err.println("Failed to create station1: " + ex.getMessage());
                        return null;
                      });

              RESTABikeAdapter restABikeAdapter = new RESTABikeAdapter(aBikeService);
              RideCommunicationAdapter rideCommunicationAdapter =
                  new RideCommunicationAdapter(aBikeService, stationService, vertx);

              ABikeVerticle aBikeVerticle = new ABikeVerticle(restABikeAdapter, vertx);
              rideCommunicationAdapter.init();
              aBikeVerticle.init();
            });
  }
}
