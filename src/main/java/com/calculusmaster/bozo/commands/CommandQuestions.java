package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import com.calculusmaster.bozo.util.Mongo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class CommandQuestions extends Command
{
    public static final String VOTE_KEEP = "QUESTIONS_VOTE_KEEP";
    public static final String VOTE_SHARD = "QUESTIONS_VOTE_SHARD";
    public static final String JONGOS_MID = "JONGOS_MID";

    public static final List<String> ATTACHMENTS = new ArrayList<>();

    public static void readAttachmentsCached()
    {
        Mongo.QuestionsVotingDB.find().forEach(d -> CommandQuestions.ATTACHMENTS.add(d.getString("link")));
    }

    public static void init()
    {
        CommandData
                .create("questions")
                .withConstructor(CommandQuestions::new)
                .withButtons(VOTE_KEEP, VOTE_SHARD, JONGOS_MID)
                .withCommand(Commands
                        .slash("questions", "Gets a random questions moment.")
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        //TODO: Use actual staff quotes channel
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, "998041223489138738");
        if(channel == null) return this.error("Can't access questions channel or it doesn't exist.");

        Random r = new Random();

        String url = ATTACHMENTS.get(r.nextInt(ATTACHMENTS.size()));
        String id = url.split("/")[5];
        this.embed.setImage(url);

        Button keep = Button.of(ButtonStyle.SUCCESS, VOTE_KEEP + "-" + id, "Keep");
        Button shard = Button.of(ButtonStyle.DANGER, VOTE_SHARD + "-" + id, "Shard");
        Button mid = Button.of(ButtonStyle.SECONDARY, JONGOS_MID + "-" + id, "Jongo's Mid");

        event.replyEmbeds(this.embed.build())
                .addActionRow(keep, shard, mid).queue();

        this.embed = null;
        this.response = "";

        return true;
    }

    @Override
    protected boolean buttonLogic(ButtonInteractionEvent event)
    {
        String attachmentID = event.getComponentId().split("-")[1];
        boolean keep = event.getComponentId().startsWith(VOTE_KEEP);
        Document data = Mongo.QuestionsVotingDB.find(Filters.eq("attachmentID", attachmentID)).first();

        if(data == null) return this.error("Attachment not found!");
        else if(event.getComponentId().startsWith(JONGOS_MID))
        {
            Member jongo = event.getGuild().retrieveMemberById("274068634798915584").complete();
            this.response = (new Random().nextFloat() < 0.1F ? jongo.getAsMention() + "\n" : "") + data;

            return true;
        }
        else if(data.getList("voters", String.class).contains(event.getUser().getId())) return this.error(event.getUser().getAsMention() + " You've already voted on this attachment!");
        else
        {
            Bson query = Filters.eq("attachmentID", attachmentID);

            if(keep) Mongo.QuestionsVotingDB.updateOne(query, Updates.inc("votes_keep", 1));
            else Mongo.QuestionsVotingDB.updateOne(query, Updates.inc("votes_shard", 1));

            Mongo.QuestionsVotingDB.updateOne(query, Updates.push("voters", event.getUser().getId()));

            this.response = event.getUser().getAsMention() + " You successfully voted to " + (keep ? "keep" : "shard") + " this attachment!";

            TextChannel general = event.getGuild().getChannelById(TextChannel.class, "1069872555541938297");
            Member me = event.getGuild().retrieveMemberById("309135641453527040").complete();

            if(me != null && general != null && !data.getString("flag").equals("none"))
            {
                int thresh = 4;
                if(data.getInteger("votes_keep") + 1 >= thresh)
                    general.sendMessage(me.getAsMention() + " – Attachment ID {%s} has been flagged **to be kept**!".formatted(data.getString("attachmentID"))).queue();
                else if(data.getInteger("votes_shard") + 1 >= thresh)
                    general.sendMessage(me.getAsMention() + " – Attachment ID {%s} has been flagged **for removal**!".formatted(data.getString("attachmentID"))).queue();
            }

            return true;
        }
    }
}
