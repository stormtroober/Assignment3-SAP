package application.ports;

import domain.model.Station;

/** Port for sending updates to the map microservice adapter. */
public interface StationCommunicationPort extends CommunicationPort<Station> {}
