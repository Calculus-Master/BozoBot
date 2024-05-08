package com.calculusmaster.bozo.events;

import com.calculusmaster.bozo.util.BingoManager;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimeManager
{
    private static final Map<String, Integer> INTERVALS = new HashMap<>();
    private static final Map<String, Long> TIMES = new HashMap<>();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static void init()
    {
        TimeManager.readIntervals();
        TimeManager.readTimes();

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(TimeManager::checkTime, 1, 1, TimeUnit.SECONDS);
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
                if(te.parallel) EXECUTOR.submit(te.action);
                else te.action.run();

                long newTime = target + INTERVALS.get(te.key) * (60 * 60);
                TIMES.put(te.key, newTime);

                Mongo.Misc.updateOne(Filters.eq("type", "time_data"), Updates.set(te.key + ".next_time", newTime));
            }
        }
    }

    public enum TimeEntry
    {
        IDIOT_LIST("idiot_list", IdiotListEvent::triggerIdiotListPing, false),
        NAME_CHANGER("name_changer", NameChangeRoleEvent::cycleNameChangeRole, false),
        BINGO_BOARD("bingo_board", BingoManager::createBingoBoard, true),

        ;
        final String key;
        final Runnable action;
        final boolean parallel;

        TimeEntry(String key, Runnable action, boolean parallel)
        {
            this.key = key;
            this.action = action;
            this.parallel = parallel;
        }
    }
}
