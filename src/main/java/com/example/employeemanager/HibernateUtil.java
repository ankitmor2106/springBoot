package com.example.employeemanager;

import com.example.employeemanager.model.Employee;
import com.example.employeemanager.model.Project;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public final class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory buildSessionFactory() {
        String database = env("MYSQL_DATABASE", "college_enrollment_db");
        String host = env("MYSQL_HOST", "localhost");
        String port = env("MYSQL_PORT", "3306");
        String password = env("MYSQL_PASSWORD", "");
        if (password.isBlank()) {
            throw new IllegalStateException("MYSQL_PASSWORD must be set");
        }
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver")
                .applySetting("hibernate.connection.url", env("DB_URL",
                        "jdbc:mysql://" + host + ":" + port + "/" + database
                                + "?createDatabaseIfNotExist=true&serverTimezone=UTC"))
                .applySetting("hibernate.connection.username", env("MYSQL_USER", "ankitmor"))
                .applySetting("hibernate.connection.password", password)
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .applySetting("hibernate.show_sql", "false")
                .applySetting("hibernate.format_sql", "true")
                .build();
        try {
            return new MetadataSources(registry)
                    .addAnnotatedClass(Employee.class)
                    .addAnnotatedClass(Project.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (RuntimeException exception) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw exception;
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static SessionFactory sessionFactory() {
        return SESSION_FACTORY;
    }

    public static void close() {
        SESSION_FACTORY.close();
    }
}
