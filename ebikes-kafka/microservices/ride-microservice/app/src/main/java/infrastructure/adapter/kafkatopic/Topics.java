package infrastructure.adapter.kafkatopic;

public enum Topics {
    EBIKE_UPDATES("ebike-updates");

    private final String topicName;

    Topics(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}