package com.calculusmaster.bozo.util;

import com.mongodb.client.model.Filters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderManager
{
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    public static final ExecutorService UPDATER = Executors.newFixedThreadPool(2);
    public static final Map<String, List<Reminder>> REMINDERS = new HashMap<>();

    public static void init()
    {
        Mongo.ReminderDB.find().forEach(d ->
        {
            if(!REMINDERS.containsKey(d.getString("authorID"))) REMINDERS.put(d.getString("authorID"), new ArrayList<>());
            REMINDERS.get(d.getString("authorID")).add(new Reminder(d));
        });

        SCHEDULER.scheduleAtFixedRate(ReminderManager::update, 5, 5, TimeUnit.SECONDS);
    }

    private static void update()
    {
        List<Reminder> toRemove = new ArrayList<>();
        long now = Instant.now().getEpochSecond();

        REMINDERS.forEach((authorID, reminders) ->
        {
            toRemove.clear();

            for(Reminder r : reminders)
                if(r.getEndTimestamp() <= now)
                {
                    r.sendReminder();
                    if(!r.isManualRemoval())
                    {
                        toRemove.add(r);
                        UPDATER.submit(() -> Mongo.ReminderDB.deleteOne(Filters.and(Filters.eq("author", r.getAuthorID()), Filters.eq("timestamp", String.valueOf(r.getEndTimestamp())))));
                    }
                }

            reminders.removeAll(toRemove);
        });
    }

    public static void addReminder(String authorID, Reminder r)
    {
        if(!REMINDERS.containsKey(authorID)) REMINDERS.put(authorID, new ArrayList<>());
        REMINDERS.get(authorID).add(r);
        Mongo.ReminderDB.insertOne(r.serialize());
    }
}
