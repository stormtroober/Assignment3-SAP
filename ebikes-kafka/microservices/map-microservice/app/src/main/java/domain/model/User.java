package domain.model;

import ddd.Entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable, Entity<String> {

    private final String username;
    private final Set<EBike> userBikes;

    public User(String username) {
        this.username = username;
        this.userBikes = new HashSet<>();
    }


    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", userBikes=" + userBikes +
                '}';
    }

    @Override
    public String getId() {
        return username;
    }
}
