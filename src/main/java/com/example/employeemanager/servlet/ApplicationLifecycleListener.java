package com.example.employeemanager.servlet;

import com.example.employeemanager.HibernateUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ApplicationLifecycleListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        HibernateUtil.sessionFactory();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        HibernateUtil.close();
    }
}
