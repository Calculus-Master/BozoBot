package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.BozoBot;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Objects;

public class StarboardPost
{
    public static StarboardPost get(String messageID)
    {
        Document data = Mongo.StarboardPostDB.find(Filters.eq("sourceMessageID", messageID)).first();

        if(data == null) return null;
        else return new StarboardPost(data);
    }

    private final String sourceMessageID;
    private final String sourceChannelID;
    private String postMessageID;
    private final String authorID;
    private int reactions;

    public StarboardPost(Document data)
    {
        this.sourceMessageID = data.getString("sourceMessageID");
        this.sourceChannelID = data.getString("sourceChannelID");
        this.postMessageID = data.getString("postMessageID");
        this.authorID = data.getString("authorID");
        this.reactions = data.getInteger("reactions");
    }

    public StarboardPost(Message message, MessageReaction reaction)
    {
        this.sourceMessageID = message.getId();
        this.sourceChannelID = message.getChannel().getId();
        this.postMessageID = "";
        this.authorID = message.getAuthor().getId();
        this.reactions = reaction.getCount();
    }

    public Document serialize()
    {
        return new Document()
                .append("sourceMessageID", this.sourceMessageID)
                .append("sourceChannelID", this.sourceChannelID)
                .append("postMessageID", this.postMessageID)
                .append("authorID", this.authorID)
                .append("reactions", this.reactions)
                ;
    }

    private Bson getQuery()
    {
        return Filters.eq("messageID", this.sourceMessageID);
    }

    public void createPost()
    {
        Guild bozocord = Objects.requireNonNull(BozoBot.BOT_JDA.getGuildById("983450314885713940"));
        Objects.requireNonNull(bozocord.getChannelById(TextChannel.class, this.sourceChannelID))
                .retrieveMessageById(this.sourceMessageID)
                .queue(m ->
                {
                    EmbedBuilder embed = new EmbedBuilder()
                            .addField("Details", """
                            Author: <@%s>
                            Channel: <#%s>
                            [Link](<%s>)
                            """.formatted(m.getAuthor().getId(), m.getChannel().getId(), "https://discord.com/channels/%s/%s/%s".formatted(m.getGuild().getId(), m.getChannel().getId(), m.getId())), false)
                            .addField("Message", m.getContentRaw(), false);

                    TextChannel starboardChannel = Objects.requireNonNull(bozocord.getChannelById(TextChannel.class, "1149543873211797554"));

                    MessageCreateBuilder builder = new MessageCreateBuilder()
                            .addEmbeds(embed.build())
                            .setContent("# Score: %s".formatted(this.reactions));

                    starboardChannel.sendMessage(builder.build()).queue(message ->
                    {
                       this.postMessageID = message.getId();
                       Mongo.StarboardPostDB.insertOne(this.serialize());
                    });

                    if(!m.getAttachments().isEmpty()) m.getAttachments().forEach(a -> starboardChannel.sendMessage("[Attachment](%s)".formatted(a.getProxyUrl())).queue());
                });
    }

    public void updateReactions(int reactions)
    {
        this.reactions = reactions;
        Mongo.StarboardPostDB.updateOne(this.getQuery(), Updates.set("reactions", this.reactions));

        Guild bozocord = Objects.requireNonNull(BozoBot.BOT_JDA.getGuildById("983450314885713940"));
        Objects.requireNonNull(bozocord.getChannelById(TextChannel.class, "1149543873211797554"))
                .editMessageById(this.postMessageID, "# Score: %s".formatted(this.reactions))
                .queue();
    }
}
