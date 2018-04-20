package discord.bot;

import java.util.List;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface MessageReceivedCommand {
	void run(MessageReceivedEvent event, List<String> args);
}
