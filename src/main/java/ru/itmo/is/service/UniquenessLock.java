package ru.itmo.is.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class UniquenessLock {

    private final Semaphore permit = new Semaphore(1, true);

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry transactions;

    public void acquire() {
        if (transactions.getTransactionStatus() != Status.STATUS_ACTIVE) {
            throw new IllegalStateException("Для проверки уникальности нужна активная транзакция");
        }
        if (transactions.getResource(UniquenessLock.class) != null) { // Если в этой транзакции уже захватили, выходим
            return;
        }

        // Ждем пока освободят или занимаем
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
            }); // Когда транзакция завершится отпускаем  permit
        } catch (RuntimeException e) {
            permit.release();
            throw e;
        }
        transactions.putResource(UniquenessLock.class, Boolean.TRUE);
    }
}
