package org.models;
public record UserViewModel(String username, int credit, boolean admin) {
    public UserViewModel updateCredit(int credit) {
        return new UserViewModel(username, credit, admin);
    }
}