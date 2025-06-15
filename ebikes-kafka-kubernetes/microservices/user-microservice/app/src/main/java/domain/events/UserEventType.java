package domain.events;

public enum UserEventType {
    USER_CREATED("UserCreated"),
    CREDIT_UPDATED("CreditUpdated"),
    CREDIT_RECHARGED("CreditRecharged");

    private final String value;

    UserEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserEventType fromString(String value) {
        for (UserEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + value);
    }
}
