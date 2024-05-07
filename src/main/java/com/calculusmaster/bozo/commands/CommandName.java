package com.calculusmaster.bozo.commands;

import com.calculusmaster.bozo.commands.core.Command;
import com.calculusmaster.bozo.commands.core.CommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandName extends Command
{
	public static void init()
	{
		CommandData
				.create("name")
				.withConstructor(CommandName::new)
				.withCommand(Commands
						.slash("name", "Change user names more efficiently as the Name Changer.")
				)
				.register();
	}

	@Override
	protected boolean slashCommandLogic(SlashCommandInteractionEvent event)
	{
		if(event.getMember().getRoles().stream().noneMatch(r -> r.getId().equals("1075631470913278013")))
			return this.error("You're not an Adept Name Changer.", true);

		return true;
	}
}
