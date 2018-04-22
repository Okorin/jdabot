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


import org.jooq.conf.ParamType;

import static generated_resources.Tables.*;

import discord.bot.MessageReceivedCommand;
import discord.bot.env;
import discord.bot.commands.CommandHandler;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * @author Okoratu Fires whenever a Message is sent to the bot in a channel it
 *         can see
 */
public class MessageReceivedEventListener extends DiscordGuildListenerAdapter {

	// Command Prefix, take config, must not be re-registered
	protected final String prefix = env.prefix;

	// Commands the Bot registers
	private Map<String, MessageReceivedCommand> commands = new HashMap<>();
	private Map<String, MessageReceivedCommand> publicCommands = new HashMap<>();

	// Access Control List
	private Map<String, Integer> ACL = new HashMap<>();

	private Map<String, TextChannel> botLogChannels = new HashMap<>();

	// DB connection
	private Connection conn;

	// For now this just registers stuff from the config
	// if i end up having a db of sorts, these methods can fetch those from db
	/**
	 * @param conn
	 *            an open DB connection
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

	// be the message Handler
	/**
	 * @param e
	 *            the Messageevent to handle
	 */
	private void handle(MessageReceivedEvent e) {

		// only process further Checks if the message Starts with the bot's prefix
		String Message = e.getMessage().getContentRaw();
		TextChannel logChannel = this.getChannelOrReadDb(e, this.botLogChannels, GUILD_SETTINGS, GUILD_SETTINGS.LOG_CHANNEL, GUILD_SETTINGS.ID); 
		if (Message.startsWith(this.prefix)) {
			// UserId
			String uId = e.getAuthor().getId();
			// relevant to execute.
			String[] enteredArguments = Message.split(" ");

			// needing the command later
			String commandStr = enteredArguments[0].substring(this.prefix.length());
			
			// the command is public
			if (this.publicCommands.containsKey(commandStr)) { 
				List<String> pubArgs = new ArrayList<>(Arrays.asList(enteredArguments));
				
				pubArgs.remove(0);
				
				this.publicCommands.get(commandStr).run(e, pubArgs);
			} else if (this.ACL.containsKey(uId)) { // the command isnt public so maybe the user is authorized

				// The first enteredArgument minus the prefix is the command we check against
				if (this.commands.containsKey(commandStr)) {

					// Translate commands into list
					List<String> args = new ArrayList<>(Arrays.asList(enteredArguments));

					// remove the first argument in this list because it is the command key
					args.remove(0);

					// run the command
					this.commands.get(commandStr).run(e, args);
				}

			} else { // the command isn't public and the user isn't authorized
				System.out.println(Message.substring(env.prefix.length(), Message.length()));
				
				if (logChannel != null && commands.containsKey(commandStr))
					logChannel.sendMessage(
							"Unauthorized user " + e.getAuthor().getAsMention() + " tried to issue " + Message.split(" ")[0].substring(this.prefix.length()))
							.queue();
			}

		}
	}

	// OnLoad -> Register Base commands
	/**
	 * tells the bot what commands it accepts in this context
	 */
	private void registerCommands() {
		
		// The command handler also rate public commands
		CommandHandler handler = new CommandHandler();

		// Masskick Excluding Roles, args is a list of Rolenames to exclude
		commands.put("massKickExcluding", handler.massKick(true));
		
		// Masskick including is the inverse
		commands.put("massKickRoles", handler.massKick(false));
		
		// Kickmembers expects a list of members as mentions
		commands.put("kickMembers", handler.kickMembers());

		// Factory two setter commands
		commands.put("setLanding", handler.createChannelSetterCommand(GUILD_SETTINGS,GUILD_SETTINGS.LANDING_CHANNEL, GUILD_SETTINGS.ID));
		commands.put("setLog", handler.createChannelSetterCommand(GUILD_SETTINGS, GUILD_SETTINGS.LOG_CHANNEL,GUILD_SETTINGS.ID));
		
		commands.put("getRoles", handler.getRoles());
		commands.put("getAsMentionIfRoles", handler.getUsersIfRoles("mentions"));
		commands.put("getAsUsernameIfRoles", handler.getUsersIfRoles("usernames"));
		commands.put("getAsFullTagIfRoles", handler.getUsersIfRoles("fullyQualified"));
		commands.put("getAsNicknameIfRoles", handler.getUsersIfRoles("nicknames"));
		publicCommands.put("ping", handler.ping());
	}

	// OnLoad -> Register Users Authorized to use commands
	private void registerUsers() {
		int i = 1;
		try (Statement stmnt = this.conn.createStatement()) {
			ResultSet admins = stmnt.executeQuery(env.SQL.select(ADMIN_USERS.UID)
														 .from(ADMIN_USERS)
														 .getSQL(ParamType.INLINED));
			while (admins.next()) {

				this.ACL.put(admins.getString(ADMIN_USERS.UID.getName()), i++);
			}
			stmnt.close();
		} catch (SQLException ex) {

		}
	}

}
