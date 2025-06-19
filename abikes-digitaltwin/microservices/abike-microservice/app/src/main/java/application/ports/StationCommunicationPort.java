package application.ports;

import domain.model.Station;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/** Port for sending updates to the map microservice adapter. */
public interface StationCommunicationPort extends CommunicationPort<Station> {
}
