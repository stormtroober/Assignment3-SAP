package domain.events;

/** Enum representing possible bike action types. */
public enum ActionType {
  START("start"),
  STOP("stop"),
  PUBLIC_START("public_start"),
  PUBLIC_END("public_end");

  private final String value;

  ActionType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ActionType fromString(String action) {
    for (ActionType type : ActionType.values()) {
      if (type.value.equalsIgnoreCase(action)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown action type: " + action);
  }
}
