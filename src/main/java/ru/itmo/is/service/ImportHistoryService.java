package ru.itmo.is.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import ru.itmo.is.auth.Account;
import ru.itmo.is.dto.ImportOperationDto;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ImportHistoryService {

    @PersistenceContext(unitName = "lab1")
    EntityManager em;

    public List<ImportOperationDto> list(Account account) {
        boolean admin = account.role() == Account.Role.ADMIN;
        String sql = """
                SELECT operation.id, operation.status, account.username, operation.added_count
                FROM is_lab2_import_operation operation
                JOIN is_lab2_account account ON account.id = operation.account_id
                """;
        if (!admin) {
            sql += "WHERE operation.account_id = ?1\n";
        }

        Query query = em.createNativeQuery(sql + "ORDER BY operation.id DESC");
        if (!admin) {
            query.setParameter(1, account.id());
        }

        List<ImportOperationDto> result = new ArrayList<>();
        List<?> rows = query.getResultList();
        for (Object row : rows) {
            Object[] fields = (Object[]) row;
            result.add(new ImportOperationDto(
                    ((Number) fields[0]).longValue(),
                    ImportOperationDto.Status.valueOf((String) fields[1]),
                    (String) fields[2],
                    (Integer) fields[3]
            ));
        }
        return result;
    }
}
