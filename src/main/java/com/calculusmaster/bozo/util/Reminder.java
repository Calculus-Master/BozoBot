package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.BozoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

public class Reminder
{
    private String authorID;
    private String authorName;
    private String content;
    private long timestamp;
    private boolean manualRemoval;

    private String server;
    private String channel;

    //Channel reminder
    public Reminder(User author, String content, long endTimestamp, boolean isManualRemoval, String server, String channel)
    {
        this.authorID = author.getId();
        this.authorName = author.getName();
        this.content = content;
        this.timestamp = endTimestamp;
        this.manualRemoval = false;
        this.server = server;
        this.channel = channel;
    }

    //DM reminder
    public Reminder(User author, String content, long endTimestamp, boolean isManualRemoval)
    {
        this(author, content, endTimestamp, isManualRemoval, "private", "private");
    }

    public Reminder(Document data)
    {
        this.authorID = data.getString("author");
        this.authorName = data.getString("authorName");
        this.content = data.getString("content");
        this.timestamp = Long.parseLong(data.getString("timestamp"));
        this.manualRemoval = data.getBoolean("manualRemoval");
        this.server = data.getString("server");
        this.channel = data.getString("channel");
    }

    public Document serialize()
    {
        return new Document()
                .append("author", this.authorID)
                .append("authorName", this.authorName)
                .append("content", this.content)
                .append("timestamp", String.valueOf(this.timestamp))
                .append("manualRemoval", this.manualRemoval)
                .append("server", this.server)
                .append("channel", this.channel)
                ;
    }

    public EmbedBuilder getReminderEmbed()
    {
        return new EmbedBuilder()
                .setTitle("Reminder for " + this.authorName)
                .setDescription(this.content)
                .setFooter(this.manualRemoval ? "This reminder won't be automatically removed, use /reminders to remove it!" : "This reminder will be automatically removed from your reminders list.");
    }

    public void sendReminder()
    {
        EmbedBuilder embed = this.getReminderEmbed();

        if(this.server.equals("private")) BozoBot.BOT_JDA.openPrivateChannelById(this.authorID).queue(
                c -> c.sendMessageEmbeds(embed.build()).queue(),
                t -> BozoLogger.info(Reminder.class, "Failed to DM reminder to " + this.authorName + " (" + this.authorID + ")")
                );
    }

    public boolean isManualRemoval()
    {
        return this.manualRemoval;
    }

    public long getEndTimestamp()
    {
        return this.timestamp;
    }

    public String getAuthorID()
    {
        return this.authorID;
    }
}
