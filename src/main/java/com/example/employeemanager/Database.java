package com.example.employeemanager;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Function;

public final class Database {
    private Database() {
    }

    public static <T> T transaction(Function<Session, T> operation) {
        try (Session session = HibernateUtil.sessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = operation.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
}
