package ru.itmo.is.dto;

public record ImportOperationDto(long id, Status status, String username, Integer addedCount) {
    public enum Status { SUCCEEDED, FAILED }
}
