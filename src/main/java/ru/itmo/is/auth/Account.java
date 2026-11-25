package ru.itmo.is.auth;

import java.io.Serializable;

public record Account(long id, String username, Role role) implements Serializable {
    public enum Role { USER, ADMIN }
}
