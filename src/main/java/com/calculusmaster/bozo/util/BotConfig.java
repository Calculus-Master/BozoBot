package com.calculusmaster.bozo.util;

import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BotConfig
{
    public static List<String> REACTIONS_POOL = new ArrayList<>();
    public static List<String> VALID_LFG_CHANNELS = new ArrayList<>();

    public static void init()
    {
        Document config = Objects.requireNonNull(Mongo.Misc.find(Filters.eq("type", "config")).first());

        REACTIONS_POOL = new ArrayList<>(config.getList("reactions_pool", String.class));
        VALID_LFG_CHANNELS = new ArrayList<>(config.getList("lfg_channels", String.class));
    }
}
