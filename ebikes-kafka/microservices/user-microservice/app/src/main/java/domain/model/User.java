package domain.model;

import java.io.Serializable;
import ddd.Entity;

public class User implements Entity<String>, Serializable {
    public enum UserType { ADMIN, USER }

    private final String username;
    private final UserType type;
    private volatile int credit;

    public User(String username, UserType type, int credit) {
        this.username = username;
        this.type = type;
        this.credit = credit;
    }

    @Override
    public String toString() {
        return String.format("User{username='%s', type='%s', credit=%d}", username, type, credit);
    }

    @Override
    public String getId() {
        return username;
    }
}
