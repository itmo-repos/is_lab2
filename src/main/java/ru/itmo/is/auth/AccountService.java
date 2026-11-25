package ru.itmo.is.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

@ApplicationScoped
public class AccountService {

    private final SecureRandom random = new SecureRandom();

    @PersistenceContext(unitName = "lab1")
    EntityManager em;

    @Transactional
    public boolean register(String username, String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return em.createNativeQuery("""
                INSERT INTO is_lab2_account (username, salt, password_hash)
                VALUES (?1, ?2, ?3)
                ON CONFLICT (username) DO NOTHING
                """)
                .setParameter(1, username)
                .setParameter(2, salt)
                .setParameter(3, hash(password, salt))
                .executeUpdate() == 1;
    }

    public Account authenticate(String username, String password) {
        List<?> accounts = em.createNativeQuery("""
                SELECT id, role, salt, password_hash
                FROM is_lab2_account WHERE username = ?1
                """)
                .setParameter(1, username)
                .getResultList();
        Object[] row = accounts.isEmpty() ? null : (Object[]) accounts.get(0);
        byte[] salt = row == null ? new byte[16] : (byte[]) row[2];
        byte[] expected = row == null ? new byte[32] : (byte[]) row[3];

        if (!MessageDigest.isEqual(hash(password, salt), expected) || row == null) {
            return null;
        }
        return new Account(((Number) row[0]).longValue(), username, Account.Role.valueOf((String) row[1]));
    }

    private byte[] hash(String password, byte[] salt) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withMemoryAsKB(19 * 1024)
                .withIterations(2)
                .withParallelism(1)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        byte[] result = new byte[32];
        generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), result);
        return result;
    }
}
