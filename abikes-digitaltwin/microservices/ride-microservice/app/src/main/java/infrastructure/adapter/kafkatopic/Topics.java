package infrastructure.adapter.kafkatopic;

public enum Topics {
  EBIKE_RIDE_UPDATE("ebike-ride-update"),
  RIDE_USER_UPDATE("ride-user-update"),
  ABIKE_RIDE_UPDATE("abike-ride-update"),
  RIDE_BIKE_DISPATCH("ride-bike-dispatch"),
  USER_UPDATE("user-update"),
  ABIKE_UPDATES("abike-update"),
  EBIKE_UPDATES("ebike-update"),
  STATION_UPDATES("station-update"),
  RIDE_UPDATE("ride-update");

  private final String topicName;

  Topics(String topicName) {
    this.topicName = topicName;
  }

  public String getTopicName() {
    return topicName;
  }
}
