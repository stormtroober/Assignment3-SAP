import application.RestRideServiceAPIImpl;
import application.ports.*;
import infrastructure.utils.EventPublisherImpl;
import infrastructure.adapter.ebike.EBikeCommunicationAdapter;
import infrastructure.adapter.map.MapCommunicationAdapter;
import infrastructure.adapter.user.UserCommunicationAdapter;
import infrastructure.adapter.web.RideServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.Vertx;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());
            EbikeCommunicationPort ebikeCommunicationAdapter = new EBikeCommunicationAdapter(vertx);
            MapCommunicationPort mapCommunicationAdapter = new MapCommunicationAdapter(vertx);
            UserCommunicationPort userCommunicationAdapter = new UserCommunicationAdapter(vertx);
            RestRideServiceAPI service = new RestRideServiceAPIImpl(new EventPublisherImpl(vertx), vertx, ebikeCommunicationAdapter, mapCommunicationAdapter, userCommunicationAdapter);
            RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, vertx);
            rideServiceVerticle.init();
        });






    }
}