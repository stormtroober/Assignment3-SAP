package domain.model;

import domain.events.*;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class UserAggregate {
    private String username;
    private String userType;
    private int    credit;
    private long   version;

    public UserAggregate(List<UserEvent> history) {
        this.version = 0;
        history.forEach(this::applyEvent);
    }

    public void applyEvent(UserEvent evt) {
        switch (evt.getType()) {
            case USER_CREATED:
                UserCreated uc = (UserCreated) evt;
                this.username  = uc.getAggregateId();
                this.userType  = uc.getUserType();
                this.credit    = uc.getInitialCredit();
                break;
            case CREDIT_RECHARGED:
                this.credit += ((CreditRecharged) evt).getAmount();
                break;
            case CREDIT_UPDATED:
                this.credit = ((CreditUpdated) evt).getNewCredit();
                break;
            default:
                throw new IllegalStateException("Unhandled event: " + evt.getType());
        }
        this.version = evt.getSequence();
    }

    public UserCreated create(String username, String userType, int initialCredit) {
        if (version != 0) throw new IllegalStateException("Already created");
        return new UserCreated(username, version + 1, userType, initialCredit);
    }
    public CreditRecharged recharge(int amount) {
        if (version == 0) throw new IllegalStateException("Not created");
        return new CreditRecharged(username, version + 1, amount);
    }

    public CreditUpdated updateCredit(int newCredit) {
        if (version == 0) throw new IllegalStateException("Not created");
        return new CreditUpdated(username, version + 1, newCredit);
    }


    public String   getUsername() { return username; }
    public String   getUserType() { return userType; }
    public int      getCredit()   { return credit;   }
    public long     getVersion()  { return version;  }
    public JsonObject toJson() {
        return new JsonObject()
                .put("username", username)
                .put("type",     userType)
                .put("credit",   credit);
    }
}
