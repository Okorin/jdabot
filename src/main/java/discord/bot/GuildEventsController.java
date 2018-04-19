package discord.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class GuildEventsController extends ListenerAdapter {

	private Connection conn;
	private Map<String,TextChannel> landingChannels = new HashMap<>();

	public GuildEventsController(Connection conn) {
		this.conn = conn;
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent e) {
		TextChannel landingChannel = this.getLandingChannelOrReadDb(e);
		//GuildController controller = guild.getController();
		if (landingChannel != null)
		landingChannel.sendMessage(e.getUser().getAsMention() + " Hi! Please link your **osu! profile** and state which **gamemode(s)** you applied for, so that we can change your edgelord discord usernames to something less edgelord (depends on how stupid your osu!username is) and give you corresponding roles!\n\nYou'll see more channels once you're verified please stop asking why this server is so stupidly pointless, thank you!").queueAfter(4, TimeUnit.SECONDS);

	}

	private TextChannel getLandingChannelOrReadDb(GuildMemberJoinEvent e) {
		String GuildId = e.getGuild().getId();
		if (!this.landingChannels.containsKey(GuildId)) {
			try {
				this.conn = (this.conn.isClosed()) ? Ref.connect() : this.conn;
				Statement stmnt = this.conn.createStatement();
				ResultSet landingChannel = stmnt.executeQuery("SELECT landing_channel FROM guild_settings WHERE id = '" + GuildId + "'");
				if (landingChannel.first() && landingChannel.getString("landing_channel") != null) {
					this.landingChannels.put(GuildId, e.getGuild().getTextChannelById(landingChannel.getString("landing_channel")));
				} else {
					this.landingChannels.put(GuildId, null);
				}
				stmnt.close();
				this.conn.close();
			} catch (SQLException e2) {
				e2.printStackTrace();
			}
		} 
		return this.landingChannels.get(GuildId);
	}
	
}
