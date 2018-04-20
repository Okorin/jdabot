package discord.bot.listeners;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static generated_resources.Tables.*;

import discord.bot.MessageReceivedCommand;
import discord.bot.env;
import discord.bot.factories.CommandFactory;
import discord.bot.Helpers;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

/**
 * @author Okoratu
 * Fires whenever a Message is sent to the bot in a channel it can see
 */
public class MessageReceivedEventListener extends ListenerAdapter {

	// Command Prefix, take config, must not be re-registered
	protected final String prefix = env.prefix;

	// Commands the Bot registers
	private Map<String, MessageReceivedCommand> commands = new HashMap<>();

	// Access Control List
	private Map<String, Integer> ACL = new HashMap<>();

	private Map<String, TextChannel> botLogChannels = new HashMap<>();

	// DB connection
	private Connection conn;

	// For now this just registers stuff from the config
	// if i end up having a db of sorts, these methods can fetch those from db
	/**
	 * @param conn an open DB connection
	 */
	public MessageReceivedEventListener(Connection conn) {

		this.conn = conn;

		// Populate commands
		registerCommands();

		// Populate ACL
		registerUsers();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		this.handle(event);
	}
	
	/**
	 * @param e the Message Event
	 * @return get Botchannel
	 */
	private TextChannel getBotChannelOrReadFromDb(MessageReceivedEvent e) {
		String GuildId = e.getGuild().getId();
		try {
			this.conn = (conn.isClosed()) ? Helpers.connect() : this.conn;

			// On first call it won't contain shit for this so if you cant find it, read it
			// from DB
			if (!this.botLogChannels.containsKey(GuildId)) {
				Statement stmnt = this.conn.createStatement();
				ResultSet botLog = stmnt.executeQuery("SELECT log_channel FROM guild_settings WHERE id = \"" + GuildId + "\"");
					if (botLog.first() && botLog.getString("log_channel") != null) {
						System.out.println("condition tru");
						System.out.println(botLog.getString("log_channel"));
						System.out.println(GuildId);
						this.botLogChannels.put(GuildId, e.getGuild().getTextChannelById(botLog.getString("log_channel")));
						System.out.println((e.getGuild().getTextChannelById(botLog.getString("log_channel"))));
					} else {
						System.out.println("condition false");
						this.botLogChannels.put(GuildId, null);
					}
				stmnt.close();
			}

			conn.close();
			return this.botLogChannels.get(GuildId);
			
		} catch (SQLException excp) {
			return null;
		}
	}

	// be the message Handler
	/**
	 * @param e the Messageevent to handle
	 */
	private void handle(MessageReceivedEvent e) {
		
		// only process further Checks if the message Starts with the bot's prefix
		String Message = e.getMessage().getContentRaw();
		TextChannel logChannel = this.getBotChannelOrReadFromDb(e);
		if (Message.startsWith(this.prefix)) {
			// UserId
			String uId = e.getAuthor().getId();

			// AuthorityCheck the user
			if (this.ACL.containsKey(uId)) {

				// Message is only processed further if it contains something potentially
				// relevant to execute.
				String[] enteredArguments = Message.split(" ");

				// needing the command later
				String commandStr = enteredArguments[0].substring(this.prefix.length());

				// The first enteredArgument minus the prefix is the command we check against
				if (this.commands.containsKey(commandStr)) {

					// Translate commands into list
					List<String> args = new ArrayList<>(Arrays.asList(enteredArguments));

					// remove the first argument in this list because it is the command key
					args.remove(0);

					// run the command
					this.commands.get(commandStr).run(e, args);
				}

			} else {
				if (logChannel != null)
					logChannel.sendMessage(
							"unauthorized user tried to issue " + Message.split(" ")[0].substring(this.prefix.length()))
							.queue();
			}

		}
	}

	// OnLoad -> Register Base commands
	/**
	 * tells the bot what commands it accepts in this context
	 */
	private void registerCommands() {

		// Masskick Excluding Roles, args is a list of Rolenames to exclude
		commands.put("massKickExcluding", (event, args) -> {
			// the guild the message is in
			System.out.print("masskickran");
			Guild guild = event.getGuild();
			GuildController controller = guild.getController();
			TextChannel logChannel = this.getBotChannelOrReadFromDb(event);

			// Members of the guild
			List<Member> members = guild.getMembers();

			// Check all members
			members.forEach(member -> {

				// Roles of each member
				List<Role> memberRoles = member.getRoles();

				// Filter those roles against the arguments supplied to the command
				Long matches = memberRoles.stream().filter(role -> args.contains(role.getName()))
						.collect(Collectors.counting());

				// if the amount of matches for the member were empty, kick them
				if (matches == 0 && // filter must match nothing
				!member.getUser().getId().equals(env.botId) && // bot must not kick its own ass
				guild.getMemberById(env.botId).canInteract(member)) // bot must be able to interact
				{

					// Queue Kick event
					controller.kick(member).queue();
					// Log this action
					if (logChannel != null)
						logChannel.sendMessage("Attempted pruning of " + member.getEffectiveName()).queue();
				}

			});

		});
		
		// Factory two setter commands
		commands.put("setLanding", CommandFactory.createChannelSetterCommand(GUILD_SETTINGS,GUILD_SETTINGS.LANDING_CHANNEL, GUILD_SETTINGS.ID));
		commands.put("setLog", CommandFactory.createChannelSetterCommand(GUILD_SETTINGS, GUILD_SETTINGS.LOG_CHANNEL, GUILD_SETTINGS.ID));

		// Debug to console
		commands.put("getParms", (event, args) -> {
		  System.out.println(this.getBotChannelOrReadFromDb(event));
		});
	}

	// OnLoad -> Register Users Authorized to use commands
	private void registerUsers() {
		int i = 1;
		try (Statement stmnt = this.conn.createStatement()) {
			ResultSet admins = stmnt.executeQuery("select uId from admin_users where 1");
			while (admins.next()) {

				this.ACL.put(admins.getString("uId"), i++);
			}
			stmnt.close();
		} catch (SQLException ex) {

		}
	}

}
