package infrastructure.adapter.kafkatopic;

public enum Topics {
  EBIKE_UPDATES("ebike-update"),
  ABIKE_UPDATES("abike-update"),
  EBIKE_RIDE_UPDATE("ebike-ride-update"),
  STATION_UPDATES("station-update"),
  USER_RIDE_CALL("user-ride-call"),
  RIDE_MAP_UPDATE("ride-map-update");

  private final String topicName;

  Topics(String topicName) {
    this.topicName = topicName;
  }

  public String getTopicName() {
    return topicName;
  }
}
