import application.RestAutonomousRideServiceImpl;
import application.RestSimpleRideServiceImpl;
import application.ports.*;
import infrastructure.adapter.ebike.EBikeCommunicationAdapter;
import infrastructure.adapter.map.MapCommunicationAdapter;
import infrastructure.adapter.user.UserCommunicationAdapter;
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
              BikeCommunicationPort ebikeCommunicationAdapter =
                  new EBikeCommunicationAdapter(vertx);
              MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter();
              UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx);
              RestSimpleRideService service =
                  new RestSimpleRideServiceImpl(
                      new EventPublisherImpl(vertx),
                      vertx,
                      ebikeCommunicationAdapter,
                      mapCommunicationAdapter,
                      userCommunicationAdapter);
                RestAutonomousRideService autonomousRideService =
                    new RestAutonomousRideServiceImpl(mapCommunicationAdapter, userCommunicationAdapter);
              RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, autonomousRideService, vertx);
              rideServiceVerticle.init();
            });
  }
}
