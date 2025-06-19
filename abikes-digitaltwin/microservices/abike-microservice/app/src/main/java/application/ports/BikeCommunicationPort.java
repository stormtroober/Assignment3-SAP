package application.ports;

import domain.model.ABike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/** Port for sending updates to the map microservice adapter. */
public interface BikeCommunicationPort extends CommunicationPort<ABike> {

}
