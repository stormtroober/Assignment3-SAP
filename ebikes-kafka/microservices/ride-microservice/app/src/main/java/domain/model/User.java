package domain.model;

import ddd.Entity;

public class User implements Entity<String> {

    private final String id;
    private volatile int credit;

    public User(String id, int credit) {
        this.id = id;
        this.credit = credit;
    }
    @Override
    public String getId() { return id; }

    public int getCredit() { return credit; }

    public void decreaseCredit(int amount) {
        this.credit = Math.max(this.credit - amount, 0);
    }


    @Override
    public String toString() {
        return String.format("User{id='%s', credit=%d}", id, credit);
    }
}