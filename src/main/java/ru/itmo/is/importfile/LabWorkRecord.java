package ru.itmo.is.importfile;

import java.io.Serializable;
import ru.itmo.is.model.Color;
import ru.itmo.is.model.Country;
import ru.itmo.is.model.Difficulty;

public record LabWorkRecord(String name, String description, Difficulty difficulty,
                            Float minimalPoint, Long personalQualitiesMinimum,
                            Coordinates coordinates, Discipline discipline, Person author)
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public record Coordinates(Long x, Double y) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record Discipline(String name, Integer practiceHours, Integer labsCount) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record Person(String name, Color eyeColor, Color hairColor, Location location,
                         Long height, Country nationality) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record Location(Integer x, Long y, String name) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
