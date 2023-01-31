package com.calculusmaster.bozo.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.calculusmaster.bozo.BozoBot;
import org.slf4j.LoggerFactory;

public class BozoLogger
{
    public static void init(String name, Runnable init)
    {
        long i = System.currentTimeMillis();
        init.run();
        long f = System.currentTimeMillis();

        BozoLogger.info(BozoBot.class, "Initialized " + name + "! Time: " + (f - i) + " ms.");
    }

    public static void info(Class<?> clazz, String msg)
    {
        LoggerFactory.getLogger(clazz).info(msg);
    }

    public static void warn(Class<?> clazz, String msg)
    {
        LoggerFactory.getLogger(clazz).warn(msg);
    }

    public static void error(Class<?> clazz, String msg)
    {
        LoggerFactory.getLogger(clazz).error(msg);
    }

    public static void suppressMongo()
    {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.connection").setLevel(Level.OFF);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.management").setLevel(Level.OFF);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.cluster").setLevel(Level.OFF);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.protocol.insert").setLevel(Level.OFF);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.protocol.query").setLevel(Level.OFF);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver.protocol.update").setLevel(Level.OFF);
    }
}
