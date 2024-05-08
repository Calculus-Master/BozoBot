package com.calculusmaster.bozo.util;

import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.*;

public class BotConfig
{
    public static List<String> REACTIONS_POOL = new ArrayList<>();
    public static List<String> VALID_LFG_CHANNELS = new ArrayList<>();
    public static List<String> ONE_WORD_RESPONSES = new ArrayList<>();
    public static final Map<String, String> UNIQUE_RESPONSES = new HashMap<>();
    public static List<String> D2_RESPONSES = new ArrayList<>();
    public static String STARBOARD_REACTION = "";
    public static int STARBOARD_MIN_REACTIONS = 0;
    public static List<String> BANNED_CHANNELS = new ArrayList<>();
    public static double BOZOCORD_MESSAGE_DELAY = 0.0;
    public static int NAME_CHANGERS = 1;
    public static int MAX_PREVIOUS_NAME_CHANGERS = 5;
    public static int AI_MAX_TOKENS = 100;
    public static int AI_MAX_CONVO_MESSAGES = 5; //Multiplied by 2

    public static void init()
    {
        Document config = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "config")).first());

        REACTIONS_POOL = new ArrayList<>(config.getList("reactions_pool", String.class));
        VALID_LFG_CHANNELS = new ArrayList<>(config.getList("lfg_channels", String.class));
        ONE_WORD_RESPONSES = new ArrayList<>(config.getList("one_word_responses", String.class));
        UNIQUE_RESPONSES.clear(); config.get("unique_responses", Document.class).forEach((key, value) -> UNIQUE_RESPONSES.put(key, (String)value));
        D2_RESPONSES = new ArrayList<>(config.getList("d2_responses", String.class));
        STARBOARD_REACTION = config.getString("starboard_reaction");
        STARBOARD_MIN_REACTIONS = config.getInteger("starboard_min_reactions");
        BANNED_CHANNELS = new ArrayList<>(config.getList("banned_channels", String.class));
        BOZOCORD_MESSAGE_DELAY = config.getDouble("message_delay");
        NAME_CHANGERS = config.getInteger("name_changers");
        MAX_PREVIOUS_NAME_CHANGERS = config.getInteger("max_previous_name_changers");
        AI_MAX_TOKENS = config.getInteger("ai_max_tokens");
        AI_MAX_CONVO_MESSAGES = config.getInteger("ai_max_convo_messages");
    }
}
