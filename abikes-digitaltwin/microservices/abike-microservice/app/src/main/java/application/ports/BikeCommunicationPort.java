package application.ports;

import domain.model.ABike;

/** Port for sending updates to the map microservice adapter. */
public interface BikeCommunicationPort extends CommunicationPort<ABike> {}
