package infrastructure.adapters.kafkatopic;

public enum Topics {
  EBIKE_RIDE_UPDATE("ebike-ride-update"),
  USER_UPDATE("user-update"),
  RIDE_USER_UPDATE("ride-user-update");

  private final String topicName;

  Topics(String topicName) {
    this.topicName = topicName;
  }

  public String getTopicName() {
    return topicName;
  }
}
