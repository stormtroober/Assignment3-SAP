package infrastructure.adapter.kafkatopic;

public enum Topics {
    EBIKE_RIDE_UPDATE("ebike-ride-update");

    private final String topicName;

    Topics(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}