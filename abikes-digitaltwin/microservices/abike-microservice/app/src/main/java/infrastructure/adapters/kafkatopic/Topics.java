package infrastructure.adapters.kafkatopic;

public enum Topics {
  EBIKE_UPDATES("ebike-update"),
  ABIKE_UPDATES("abike-update"),
  STATION_UPDATES("station-update"),
  EBIKE_RIDE_UPDATE("ebike-ride-update");

  private final String topicName;

  Topics(String topicName) {
    this.topicName = topicName;
  }

  public String getTopicName() {
    return topicName;
  }
}
