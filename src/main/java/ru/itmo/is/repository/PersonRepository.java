package ru.itmo.is.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import ru.itmo.is.dto.PersonDto;
import ru.itmo.is.model.Location;
import ru.itmo.is.model.Person;

@ApplicationScoped
public class PersonRepository extends BaseRepository<Person, Long> {

    public PersonRepository() {
        super(Person.class);
    }

    public boolean hasDuplicate(Long excludedId, PersonDto dto, Location location) {
        String jpql = """
                SELECT p.id FROM Person p
                WHERE p.name = :name AND p.eyeColor = :eyeColor
                    AND p.height = :height AND p.nationality = :nationality
                """;
        jpql += dto.hairColor() == null ? " AND p.hairColor IS NULL" : " AND p.hairColor = :hairColor";
        jpql += location == null ? " AND p.location IS NULL" : " AND p.location = :location";
        jpql += excludedId == null ? "" : " AND p.id <> :excludedId";

        var query = em.createQuery(jpql, Long.class)
                .setParameter("name", dto.name())
                .setParameter("eyeColor", dto.eyeColor())
                .setParameter("height", dto.height())
                .setParameter("nationality", dto.nationality());
        if (dto.hairColor() != null) {
            query.setParameter("hairColor", dto.hairColor());
        }
        if (location != null) {
            query.setParameter("location", location);
        }
        if (excludedId != null) {
            query.setParameter("excludedId", excludedId);
        }
        return !query.setMaxResults(1).getResultList().isEmpty();
    }

    public List<Person> findByLocation(Location location) {
        return em.createQuery("SELECT p FROM Person p WHERE p.location = :location", Person.class)
                .setParameter("location", location)
                .getResultList();
    }

    public boolean hasDuplicateAfterLocationRemoval(Location location) {
        return !em.createQuery("""
                SELECT p.id FROM Person p, Person other
                WHERE p.location = :location AND (other.location IS NULL OR other.location = :location)
                    AND p.id <> other.id AND p.name = other.name AND p.eyeColor = other.eyeColor
                    AND (p.hairColor = other.hairColor OR (p.hairColor IS NULL AND other.hairColor IS NULL))
                    AND p.height = other.height AND p.nationality = other.nationality
                """, Long.class)
                .setParameter("location", location)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }
}
