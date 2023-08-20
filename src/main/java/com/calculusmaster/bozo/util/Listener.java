package com.calculusmaster.bozo.util;

import com.calculusmaster.bozo.commands.*;
import com.calculusmaster.bozo.commands.core.CommandData;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.calculusmaster.bozo.BozoBot.COMMANDS;

public class Listener extends ListenerAdapter
{
    public static void init()
    {
        CommandBozo.init();
        CommandStaffQuote.init();
        CommandRandomWackyLoadout.init();
        CommandRandomWackyName.init();
        CommandQuestions.init();
        CommandDev.init();
        CommandOhHeavens.init();
        CommandBaltiMoments.init();
        CommandJoyBoyMoments.init();
        CommandHandsomeMoments.init();
        CommandStolasMoments.init();
        CommandToxMoments.init();
        CommandAegisMoments.init();
        CommandHorsMoments.init();
        CommandSuggestBozo.init();
        CommandLFG.init();
        CommandRemoveNameChanger.init();
        CommandLeaderboard.init();

        Listener.registerResponses();
    }

    private CommandData findCommandData(Predicate<CommandData> predicate)
    {
        return COMMANDS.stream().filter(predicate).findFirst().orElse(null);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Slash Command not found: " + event.getName());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else if(!(event.getChannel() instanceof TextChannel))
        {
            BozoLogger.warn(Listener.class, "Attempted use of Slash Command (%s) used in non-TextChannel (%s, Type: %s)".formatted(event.getName(), event.getChannel().getName(), event.getChannel().getType()));
            event.reply("Slash Command usage outside of standard Text Channels is not currently supported.").setEphemeral(true).queue();
        }
        else
        {
            BozoLogger.info(Listener.class, "Parsing Slash Command: /" + event.getFullCommandName() + " " + event.getOptions().stream().map(o -> o.getName() + ":" + o.getAsString()).collect(Collectors.joining(" ")));
            data.getInstance().parseSlashCommand(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.getCommandName().equals(event.getName()));

        if(data == null) BozoLogger.error(Listener.class, "Autocomplete Slash Command not found: " + event.getName());
        else data.getInstance().parseAutocomplete(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        CommandData data = this.findCommandData(c -> c.hasButton(event.getComponentId()));

        if(data == null)
        {
            BozoLogger.error(Listener.class, "Button ID not found: " + event.getComponentId());
            event.reply("An error has occurred.").setEphemeral(true).queue();
        }
        else data.getInstance().parseButtonInteraction(event);
    }

    private static final Map<String, CounterData> COUNTER_DATA_MAP = new HashMap<>();
    private static final Map<String, String> UNIQUE_RESPONSES = new HashMap<>();

    private static final Map<String, Pair<Integer, Integer>> INTERVALS = new HashMap<>();
    static
    {
        INTERVALS.put("983450314885713940", new Pair<>(5, 5)); //Bozocord
        INTERVALS.put("878207461117550642", new Pair<>(15, 12)); //Onyxcord
        INTERVALS.put("1000959891604779068", new Pair<>(7, 7)); //Avluscord
        INTERVALS.put("943354742384521267", new Pair<>(6, 6)); //Wiacord
    }

    private static void registerResponses()
    {
        UNIQUE_RESPONSES.put("490401640843706368", "grape");
        UNIQUE_RESPONSES.put("776195690149576704", "ok pvp bot");
        UNIQUE_RESPONSES.put("429601532363931659", "it really is that shrimple");
        UNIQUE_RESPONSES.put("160843328898727936", "ikr, hunters are so mid");
        UNIQUE_RESPONSES.put("752237938779226173", "chainsword>chainsawman");
        UNIQUE_RESPONSES.put("274068634798915584", "misinfo!!!!!");
        UNIQUE_RESPONSES.put("445222471332003840", "demo best perk");
        UNIQUE_RESPONSES.put("557696215903633439", "## no anime");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Random r = new Random();
        String guildID = event.getGuild().getId();
        String authorID = event.getAuthor().getId();
        String content = event.getMessage().getContentRaw().toLowerCase();

        boolean isBozocord = guildID.equals("983450314885713940");

        if(!COUNTER_DATA_MAP.containsKey(guildID))
        {
            Pair<Integer, Integer> values = INTERVALS.getOrDefault(guildID, new Pair<>(5, 5));
            COUNTER_DATA_MAP.put(guildID, new CounterData(values.getFirst(), values.getSecond()));
        }

        CounterData data = COUNTER_DATA_MAP.get(guildID);
        data.update();

        if(MessageLeaderboardHandler.hasServer(guildID))
            MessageLeaderboardHandler.addUserMessage(guildID, authorID, event.getAuthor().getName());

        //Pin
        if(r.nextInt(8192) == 0)
        {
            event.getMessage().pin().queue();
            return;
        }

        //Content-Based Responses
        if(content.length() >= 500)
        {
            event.getChannel().sendMessage("i ain't reading all that").queue();
            event.getChannel().sendMessage("i'm happy for you tho").queue();
            event.getChannel().sendMessage("or sorry that happened").queue();
        }
        else if(content.length() <= 100 && r.nextFloat() < 0.01F && event.getGuild().retrieveMember(event.getAuthor()).complete().getRoles().stream().anyMatch(role -> role.getId().equals("1070462116655534101")))
        {
            StringBuilder modified = new StringBuilder();
            for(char c : event.getMessage().getContentRaw().toCharArray()) modified.append(r.nextBoolean() ? String.valueOf(c).toUpperCase() : String.valueOf(c).toLowerCase());
            event.getChannel().sendMessage(modified.toString()).queue();
        }

        if(r.nextFloat() < 0.2F && content.contains("rose"))
            event.getChannel().sendMessage("\"Strong hands he held a " + (r.nextFloat() < 0.2F ? "cock" : "rose") +  ",aura burn bright.\" - " + event.getMember().getEffectiveName()).queue();

        if(!event.getAuthor().isBot() && r.nextFloat() < 0.75F && content.contains("upended"))
        {
            if(r.nextFloat() < 0.8F)
            {
                event.getChannel().sendMessage("***THE UPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH TURNS WORLDS UPSIDE DOWN***").queue();
            }
            else
            {
                event.getChannel().sendMessage("***THE APPENDED***").queue();
                event.getChannel().sendMessage("***A TITLE SUITABLE FOR THAT WHICH EXTENDS***").queue();
            }
        }

        if(isBozocord && r.nextFloat() < 0.1F && content.contains("mods"))
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " they're watching you...").queue();

        //General Responses
        if(data.messageCounterResponses >= data.responseInterval && !event.getAuthor().isBot() && r.nextFloat() < 0.05F)
        {
            List<String> oneWordResponses = List.of("yeah", "no", "L", "lol", "true", "cringe", "based", "smh", "wow", "ok", "bruh", "bozo", ":)", "wrong", "whar", "real", "simp", "mid", "hi", "perfect", "interesting", "lmao", "heh", "ikr", "bye", "always", "definitely", "totally", "sure", "NOPE", "...", "never", "oh", "how", "when", "why", "?", "!", "!!!", "???", ".", ":(", "wtf", "tf", "wdym", "poggies", "weird", "leave", "W", "OMG", "really", "fr", "ofc", "duh", "bro", "whatever", event.getAuthor().getAsMention());

            if(UNIQUE_RESPONSES.containsKey(authorID) && r.nextFloat() < 0.15F)
                event.getChannel().sendMessage(UNIQUE_RESPONSES.get(authorID)).queue();
            else if(isBozocord && Objects.requireNonNull(event.getMember()).getRoles().stream().noneMatch(role -> role.getId().equals("1015047797420085329")) && r.nextFloat() < 0.05F)
                event.getChannel().sendMessage("join clan bozo").queue();
            else event.getChannel().sendMessage(oneWordResponses.get(r.nextInt(oneWordResponses.size()))).queue();

            data.messageCounterResponses = 0;
        }
        else if(event.getAuthor().isBot() && r.nextFloat() < (content.equalsIgnoreCase("best bot") ? 0.025F : 0.05F))
            event.getChannel().sendMessage("best bot").queue();

        //General Reactions
        if(data.messageCounterReactions >= data.reactionInterval && r.nextFloat() < 0.05F)
        {
            String emoji = BotConfig.REACTIONS_POOL.get(r.nextInt(BotConfig.REACTIONS_POOL.size()));

            event.getMessage().addReaction(Emoji.fromFormatted(emoji)).queue();

            data.messageCounterReactions = 0;
        }

        //Updating Questions Voting
        if(isBozocord && event.getChannel().getId().equals("998041223489138738") && !event.getMessage().getAttachments().isEmpty())
            event.getMessage().getAttachments().forEach(a ->
            {
                Document attachmentData = new Document(new LinkedHashMap<>());

                attachmentData.append("attachmentID", a.getId());
                attachmentData.append("link", a.getUrl());
                attachmentData.append("voters", new ArrayList<>());
                attachmentData.append("votes_keep", 0);
                attachmentData.append("votes_shard", 0);
                attachmentData.append("flag", "none");

                Mongo.QuestionsVotingDB.insertOne(attachmentData);
                BozoLogger.info(Listener.class, "Inserted new attachment data into QuestionsVotingDB (ID: " + a.getId() + ").");
            });
    }

    private static class CounterData
    {
        private int messageCounterReactions = 0;
        private final int reactionInterval;
        private int messageCounterResponses = 0;
        private final int responseInterval;

        CounterData(int reactionInterval, int responseInterval)
        {
            this.reactionInterval = reactionInterval;
            this.responseInterval = responseInterval;
        }

        public void update()
        {
            this.messageCounterReactions++;
            this.messageCounterResponses++;
        }
    }
}
