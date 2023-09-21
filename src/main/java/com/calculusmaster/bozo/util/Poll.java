package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.BozoBot;
import com.mongodb.client.model.Filters;
import kotlin.Pair;
import kotlin.TuplesKt;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.bson.Document;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Poll
{
    //Manager
    public static final Map<String, Poll> POLLS = new HashMap<>();

    public static void init()
    {
        Mongo.PollDB.find().forEach(d -> POLLS.put(d.getString("messageID"), new Poll(d)));

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(Poll::checkPolls, 5, 5, TimeUnit.MINUTES);
    }

    private static void checkPolls()
    {
        List<String> queuedForDeletion = new ArrayList<>();
        POLLS.forEach((messageID, poll) ->
        {
            if(poll.isOver()) queuedForDeletion.add(messageID);
        });

        if(!queuedForDeletion.isEmpty()) Mongo.PollDB.deleteMany(Filters.in("messageID", queuedForDeletion));
    }

    public static Poll getPoll(String messageID)
    {
        return POLLS.getOrDefault(messageID, null);
    }

    //Class
    private String channelID;
    private String messageID;

    private final String question;
    private final List<PollOption> options;
    private final String time;
    private final boolean anonymous;

    public Poll(String question, List<Pair<String, String>> options, String time, boolean anonymous)
    {
        this.channelID = "";
        this.messageID = "";

        this.question = question;
        this.options = options.stream().map(p -> new PollOption(p.getFirst(), p.getSecond())).toList();
        this.time = time;
        this.anonymous = anonymous;
    }

    public Poll(Document data)
    {
        this.channelID = data.getString("channelID");
        this.messageID = data.getString("messageID");

        this.question = data.getString("question");

        this.options = new ArrayList<>();
        data.getList("options", Document.class).forEach(d -> this.options.add(new PollOption(d)));

        this.time = data.getString("time");
        this.anonymous = data.getBoolean("anonymous");
    }

    public Document serialize()
    {
        return new Document()
                .append("channelID", this.channelID)
                .append("messageID", this.messageID)
                .append("question", this.question)
                .append("options", this.options.stream().map(PollOption::serialize).toList())
                .append("time", this.time);
    }

    public boolean isOver()
    {
        if(!this.time.isEmpty())
        {
            long now = Instant.now().toEpochMilli();
            long time = Long.parseLong(this.time.substring(this.time.indexOf(":") + 1, this.time.lastIndexOf(":")));
            return now > time;
        }
        else return false;
    }

    public boolean isShowVotes()
    {
        return this.time.isEmpty() || this.isOver();
    }

    private EmbedBuilder createEmbed()
    {
        boolean showVotes = this.isShowVotes();
        String winning = this.options.stream().max(Comparator.comparingInt(o -> o.votes.size())).map(o -> o.reaction).orElse("");

        return new EmbedBuilder()
                .setDescription("## " + this.question)
                .addField("Choices", this.options.stream().map(p -> p.getAsLine(showVotes, winning)).collect(Collectors.joining("\n")), false)
                .addField("Options", """
                        Time: %s
                        Anonymous: %s
                        """.formatted(this.time.isEmpty() ? "No time limit." : "Ends " + this.time.substring(0, this.time.lastIndexOf(":") + 1) + "R>.", this.anonymous), false)
                .setTimestamp(Instant.now());
    }

    public void sendAndUpload(TextChannel channel)
    {
        channel.sendMessageEmbeds(this.createEmbed().build()).queue(m ->
        {
            this.channelID = channel.getId();
            this.messageID = m.getId();

            POLLS.put(this.messageID, this);
            Mongo.PollDB.insertOne(this.serialize());

            this.options.forEach(o -> m.addReaction(Emoji.fromFormatted(o.reaction)).queue());
        });
    }

    public void updateEmbed()
    {
        Objects.requireNonNull(Objects.requireNonNull(
                BozoBot.BOT_JDA.getGuildById("983450314885713940"))
                .getChannelById(TextChannel.class, this.channelID))
                .editMessageEmbedsById(this.messageID, this.createEmbed().build())
                .queue();
    }

    public void addVote(String userID, String reaction)
    {
        for(PollOption option : this.options)
        {
            boolean removed = option.votes.remove(userID);
            if(!removed && reaction.contains(option.reaction)) option.votes.add(userID);
        }

        Mongo.PollDB.replaceOne(Filters.eq("messageID", this.messageID), this.serialize());
        this.updateEmbed();
    }

    public void removeVote(String userID)
    {
        for(PollOption option : this.options) option.votes.remove(userID);

        Mongo.PollDB.replaceOne(Filters.eq("messageID", this.messageID), this.serialize());
        this.updateEmbed();
    }

    public boolean isAnonymous()
    {
        return this.anonymous;
    }

    private static class PollOption
    {
        public final String reaction;
        public final String text;
        public final List<String> votes;

        public PollOption(String reaction, String text)
        {
            this.reaction = reaction;
            this.text = text;
            this.votes = new ArrayList<>();
        }

        public PollOption(Document data)
        {
            this(data.getString("reaction"), data.getString("text"));
            this.votes.addAll(data.getList("votes", String.class));
        }

        public Document serialize()
        {
            return new Document()
                    .append("reaction", this.reaction)
                    .append("text", this.text)
                    .append("votes", this.votes);
        }

        public String getAsLine(boolean showVotes, String winning)
        {
            return "%s: *%s* %s".formatted(this.reaction, this.text, showVotes ? "| " + (winning.equals(this.reaction) ? "▓" : "░").repeat(this.votes.size()) + " ||" + this.votes.size() + "||" : "");
        }
    }
}
