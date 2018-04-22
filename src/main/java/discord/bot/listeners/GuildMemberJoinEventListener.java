package discord.bot.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static generated_resources.Tables.*;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;

public class GuildMemberJoinEventListener extends DiscordGuildListenerAdapter {

	//private Connection conn;
	private Map<String,TextChannel> landingChannels = new HashMap<>();


	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent e) {
		TextChannel landingChannel = this.getChannelOrReadDb(e.getGuild(), this.landingChannels, GUILD_SETTINGS, GUILD_SETTINGS.LANDING_CHANNEL, GUILD_SETTINGS.ID);
		//GuildController controller = guild.getController();
		System.out.println(landingChannel);
		if (landingChannel != null)
		landingChannel.sendMessage(e.getUser().getAsMention() + " Hi! Please link your **osu! profile** and state which **gamemode(s)** you applied for, so that we can change your edgelord discord usernames to something less edgelord (depends on how stupid your osu!username is) and give you corresponding roles!\n\nYou'll see more channels once you're verified please stop asking why this server is so stupidly pointless, thank you!").queueAfter(4, TimeUnit.SECONDS);

	}
	
}
