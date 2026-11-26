package ru.itmo.is.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.Map;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class UniquenessLock {

    public enum Rule { DRAFT, UNNAMED_LOCATION }

    private final Map<Rule, Semaphore> permits = Map.of(
            Rule.DRAFT, new Semaphore(1, true),
            Rule.UNNAMED_LOCATION, new Semaphore(1, true)
    );

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry transactions;

    public void acquire(Rule rule) {
        if (transactions.getTransactionStatus() != Status.STATUS_ACTIVE) {
            throw new IllegalStateException("Для проверки уникальности нужна активная транзакция");
        }
        if (transactions.getResource(rule) != null) {
            return;
        }

        Semaphore permit = permits.get(rule);
        permit.acquireUninterruptibly();
        try {
            transactions.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                }

                @Override
                public void afterCompletion(int status) {
                    permit.release();
                }
            });
        } catch (RuntimeException e) {
            permit.release();
            throw e;
        }
        transactions.putResource(rule, Boolean.TRUE);
    }
}
