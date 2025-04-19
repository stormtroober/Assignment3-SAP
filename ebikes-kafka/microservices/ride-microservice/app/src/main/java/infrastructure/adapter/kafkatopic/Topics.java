package infrastructure.adapter.kafkatopic;

public enum Topics {
  EBIKE_RIDE_UPDATE("ebike-ride-update"),
  RIDE_USER_UPDATE("ride-user-update"),
  RIDE_MAP_UPDATE("ride-map-update");

  private final String topicName;

  Topics(String topicName) {
    this.topicName = topicName;
  }

  public String getTopicName() {
    return topicName;
  }
}
