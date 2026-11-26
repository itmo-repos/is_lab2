package ru.itmo.is.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.is.model.Location;

@ApplicationScoped
public class LocationRepository extends BaseRepository<Location, Long> {

    public LocationRepository() {
        super(Location.class);
    }

    public boolean hasOtherUnnamed(Long excludedId, Integer x, Long y) {
        String jpql = "SELECT l.id FROM Location l WHERE l.x = :x AND l.y = :y AND l.name IS NULL";
        var query = em.createQuery(jpql + (excludedId == null ? "" : " AND l.id <> :excludedId"), Long.class)
                .setParameter("x", x)
                .setParameter("y", y);
        if (excludedId != null) {
            query.setParameter("excludedId", excludedId);
        }
        return !query.setMaxResults(1).getResultList().isEmpty();
    }
}
