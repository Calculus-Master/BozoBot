package com.calculusmaster.bozo.events;

import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimeManager
{
    private static final Map<String, Integer> INTERVALS = new HashMap<>();
    private static final Map<String, Long> TIMES = new HashMap<>();

    public static void init()
    {
        TimeManager.readIntervals();
        TimeManager.readTimes();

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(TimeManager::checkTime, 0, 3, TimeUnit.SECONDS);
    }

    public static void readTimes()
    {
        TIMES.clear();

        Document data = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "time_data")).first());

        for(TimeEntry te : TimeEntry.values()) TIMES.put(te.key, data.get(te.key, Document.class).getLong("next_time"));
    }

    public static void readIntervals()
    {
        INTERVALS.clear();

        Document data = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "time_data")).projection(Projections.excludeId()).first());

        for(TimeEntry te : TimeEntry.values()) INTERVALS.put(te.key, data.get(te.key, Document.class).getInteger("interval"));
    }

    private static void checkTime()
    {
        for(TimeEntry te : TimeEntry.values())
        {
            long now = Instant.now().getEpochSecond();
            long target = TIMES.get(te.key);

            if(now >= target)
            {
                te.action.run();

                long newTime = now + INTERVALS.get(te.key) * (60 * 60);
                TIMES.put(te.key, newTime);

                Mongo.Misc.updateOne(Filters.eq("type", "time_data"), Updates.set(te.key + ".next_time", newTime));
            }
        }
    }

    enum TimeEntry
    {
        IDIOT_LIST("idiot_list", IdiotListEvent::triggerIdiotListPing),
        NAME_CHANGER("name_changer", NameChangeRoleEvent::cycleNameChangeRole),

        ;
        final String key;
        final Runnable action;

        TimeEntry(String key, Runnable action)
        {
            this.key = key;
            this.action = action;
        }
    }
}
