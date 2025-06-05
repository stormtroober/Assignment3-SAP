package infrastructure.adapter.kafkatopic;

public enum Topics {
  EBIKE_UPDATES("ebike-update"),
  ABIKE_UPDATES("abike-update"),
  EBIKE_RIDE_UPDATE("ebike-ride-update"),
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
