package com.calculusmaster.bozo.util;

import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LFGPost
{
    private String postID;

    //Location
    private String serverID;
    private String channelID;
    private String messageID;

    //Contents
    private String postUserID;
    private String activity;
    private String time;
    private int players;

    //Users
    private List<String> yes;
    private List<String> maybe;
    private List<String> no;
    private Map<String, String> usernames;

    {
        this.serverID = "";
        this.channelID = "";
        this.messageID = "";

        this.postUserID = "";
        this.activity = "";
        this.time = "";
        this.players = 0;

        this.yes = new ArrayList<>();
        this.maybe = new ArrayList<>();
        this.no = new ArrayList<>();
        this.usernames = new HashMap<>();
    }

    public LFGPost()
    {
        Random r = new Random();
        this.postID = IntStream.range(0, 8).mapToObj(i -> String.valueOf(r.nextInt(10))).collect(Collectors.joining(""));
    }

    private LFGPost(Document data)
    {
        this.postID = data.getString("postID");

        Document locationData = data.get("location", Document.class);
        this.serverID = locationData.getString("serverID");
        this.channelID = locationData.getString("channelID");
        this.messageID = locationData.getString("messageID");

        this.postUserID = data.getString("postUserID");
        this.activity = data.getString("activity");
        this.time = data.getString("time");
        this.players = data.getInteger("players", 0);

        Document userData = data.get("users", Document.class);
        this.yes = userData.getList("yes", String.class);
        this.maybe = userData.getList("maybe", String.class);
        this.no = userData.getList("no", String.class);

        data.get("usernames", Document.class).forEach((k, v) -> this.usernames.put(k, (String)v));
    }

    public static LFGPost get(String postID)
    {
        Document data = Mongo.LFGPostDB.find(Filters.eq("postID", postID)).first();

        return data == null ? null : new LFGPost(data);
    }

    //Builders
    public EmbedBuilder createEmbed()
    {
        return new EmbedBuilder()
                .setTitle(this.usernames.get(this.postUserID) + "'s LFG Post%s".formatted(this.players > 0 && this.players == this.yes.size() ? " (Full)" : ""))

                .addField("Activity", "***" + this.activity + "***", false)

                .addField("Time", this.time, true)
                .addField("Post ID", this.postID, true)
                .addBlankField(true)

                .addField("Yes%s".formatted(this.players > 0 ? " (" + this.yes.size() + " / " + this.players + ")" : ""), this.yes.isEmpty() ? "None" : this.yes.stream().map(userID -> this.usernames.get(userID)).collect(Collectors.joining("\n")), true)
                .addField("Maybe", this.maybe.isEmpty() ? "None" : this.maybe.stream().map(userID -> this.usernames.get(userID)).collect(Collectors.joining("\n")), true)
                .addField("No", this.no.isEmpty() ? "None" : this.no.stream().map(userID -> this.usernames.get(userID)).collect(Collectors.joining("\n")), true)
        ;
    }

    public Document serialize()
    {
        return new Document()
                .append("postID", this.postID)
                .append("location", new Document()
                        .append("serverID", this.serverID)
                        .append("channelID", this.channelID)
                        .append("messageID", this.messageID)
                )
                .append("postUserID", this.postUserID)
                .append("activity", this.activity)
                .append("time", this.time)
                .append("players", this.players)
                .append("users", new Document()
                        .append("yes", this.yes)
                        .append("maybe", this.maybe)
                        .append("no", this.no)
                )
                .append("usernames", new Document(this.usernames));
    }

    public void upload()
    {
        Mongo.LFGPostDB.insertOne(this.serialize());
    }

    public void update()
    {
        Mongo.LFGPostDB.replaceOne(Filters.eq("postID", this.postID), this.serialize());
    }

    //Setters

    //If the time input can be made into a Unix timestamp, it will be
    public boolean setTime(String timeInput)
    {
        //Best guess for it being a timestamp already
        if(timeInput.startsWith("<t:") && timeInput.lastIndexOf(":") != timeInput.indexOf(":")) this.time = timeInput;

        //<x>d<y>h
        else if(timeInput.matches("\\d*d\\d*h"))
        {
            int days = Integer.parseInt(timeInput.substring(0, timeInput.indexOf("d")));
            int hours = Integer.parseInt(timeInput.substring(timeInput.indexOf("d") + 1, timeInput.indexOf("h")));

            Instant time = Instant.now().plus(days, ChronoUnit.DAYS).plus(hours, ChronoUnit.HOURS);

            long epoch = time.getEpochSecond();

            epoch = (epoch / 900) * 900; //Rounded to nearest 15 minutes

            this.time = "<t:" + epoch + ":F>";
        }
        //<x>h
        else if(timeInput.matches("\\d*h"))
        {
            int hours = Integer.parseInt(timeInput.substring(0, timeInput.indexOf("h")));

            Instant time = Instant.now().plus(hours, ChronoUnit.HOURS);

            long epoch = time.getEpochSecond();

            epoch = (epoch / 900) * 900; //Rounded to nearest 15 minutes

            this.time = "<t:" + epoch + ":F>";
        }
        else this.time = timeInput;

        return !this.time.trim().isEmpty();
    }

    public void setActivity(String activity)
    {
        this.activity = activity;
    }

    public void setPostUser(User postUser)
    {
        this.postUserID = postUser.getId();
        this.addUser(postUser, "yes");
    }

    public void addUser(User user, String category)
    {
        switch(category)
        {
            case "yes" ->
            {
                this.yes.add(user.getId());

                this.maybe.remove(user.getId());
                this.no.remove(user.getId());
            }
            case "maybe" ->
            {
                this.maybe.add(user.getId());

                this.yes.remove(user.getId());
                this.no.remove(user.getId());
            }
            case "no" ->
            {
                this.no.add(user.getId());

                this.yes.remove(user.getId());
                this.maybe.remove(user.getId());
            }
        }

        this.usernames.put(user.getId(), user.getName());
    }

    public void setMessage(Message message)
    {
        this.serverID = message.getGuild().getId();
        this.channelID = message.getChannel().getId();
        this.messageID = message.getId();
    }

    public void setPlayers(int players)
    {
        this.players = players;
    }

    //Getters
    public String getPostID()
    {
        return this.postID;
    }

    public List<String> yes()
    {
        return this.yes;
    }

    public List<String> maybe()
    {
        return this.maybe;
    }

    public List<String> no()
    {
        return this.no;
    }

    public String getActivity()
    {
        return this.activity;
    }

    public int getPlayers()
    {
        return this.players;
    }

    public String getTime()
    {
        return this.time;
    }

    public String getChannelID()
    {
        return this.channelID;
    }

    public String getMessageID()
    {
        return this.messageID;
    }

    public String getPostUserID()
    {
        return this.postUserID;
    }

    public String getPostUserName()
    {
        return this.usernames.get(this.postUserID);
    }
}
