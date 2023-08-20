package com.calculusmaster.bozo.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageLeaderboardHandler
{
    private static final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    public static final Map<String, Map<String, LeaderboardEntry>> LEADERBOARDS = new HashMap<>();

    public static void init()
    {
        String bozoServer = "983450314885713940";

        MessageLeaderboardHandler.createLeaderboard(Objects.requireNonNull(Mongo.Misc
                .find(Filters.and(Filters.eq("type", "message_leaderboard"), Filters.eq("server", bozoServer)))
                .first()));
    }

    public static void addUserMessage(String serverID, String userID, String username)
    {
        if(!LEADERBOARDS.get(serverID).containsKey(userID)) LEADERBOARDS.get(serverID).put(userID, new LeaderboardEntry(username));

        LeaderboardEntry entry = LEADERBOARDS.get(serverID).get(userID);

        entry.increment();

        THREAD_POOL.submit(() -> Mongo.Misc.updateOne(
                Filters.and(Filters.eq("type", "message_leaderboard"), Filters.eq("server", serverID)),
                Updates.set("data." + userID, entry.serialize())));
    }

    public static boolean hasServer(String serverID)
    {
        return LEADERBOARDS.containsKey(serverID);
    }

    private static void createLeaderboard(Document data)
    {
        String server = data.getString("server");
        LEADERBOARDS.put(server, new HashMap<>());

        data.get("data", Document.class).forEach((userID, userData) -> LEADERBOARDS.get(server).put(userID, new LeaderboardEntry((Document)userData)));
    }

    public static class LeaderboardEntry
    {
        private final String username;
        private int count;

        public LeaderboardEntry(String username)
        {
            this(username, 0);
        }

        public LeaderboardEntry(String username, int count)
        {
            this.username = username;
            this.count = count;
        }

        public LeaderboardEntry(Document data)
        {
            this(data.getString("username"), data.getInteger("count"));
        }

        public Document serialize()
        {
            return new Document("username", this.username).append("count", this.count);
        }

        public String getUsername()
        {
            return this.username;
        }

        public int getCount()
        {
            return this.count;
        }

        public void increment()
        {
            this.count++;
        }
    }
}
