package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Objects;

public class CommandFood extends Command
{
    public static void init()
    {
        CommandData
                .create("foodpost")
                .withConstructor(CommandFood::new)
                .withCommand(Commands
                        .slash("foodpost", "Post a Food picture. Needs to be done in the food channel.")
                        .addOption(OptionType.ATTACHMENT, "image", "Food image you're posting.", true)
                        .addOption(OptionType.STRING, "name", "Name of the food or a description of the image.", true)
                )
                .register();
    }

    @Override
    protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
    {
        if(!event.getChannel().getId().equals("1161443955032997888")) return this.error("This command can only be used in <#1161443955032997888>.");

        String label = Objects.requireNonNull(event.getOption("name")).getAsString();
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("image")).getAsAttachment();

        attachment.getProxy().download().thenAcceptAsync(i ->
                event.getChannel()
                        .sendMessage(label + " (by " + event.getUser().getAsMention() + ")")
                        .addFiles(FileUpload.fromData(i, label.substring(0, Math.min(10, label.length())) + ".png"))
                        .queue(m -> m.pin().queue())
        );

        this.response = "Submitted your food.";
        this.ephemeral = true;

        return true;
    }
}
