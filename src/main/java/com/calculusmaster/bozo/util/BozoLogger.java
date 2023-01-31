package com.calculusmaster.bozo.util;

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
}
