package discord.bot;

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

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

public class MessageCommandController extends ListenerAdapter {

	// Command Prefix, take config, must not be re-registered
	protected final String prefix = Ref.prefix;

	// Commands the Bot registers
	private Map<String, Command> commands = new HashMap<>();

	// Access Control List
	private Map<String, Integer> ACL = new HashMap<>();

	private Map<String, TextChannel> botLogChannels = new HashMap<>();

	// DB connection
	private Connection conn;

	// For now this just registers stuff from the config
	// if i end up having a db of sorts, these methods can fetch those from db
	public MessageCommandController(Connection conn) {

		this.conn = conn;

		registerCommands();

		registerUsers();
	}

	private TextChannel getBotChannelOrReadFromDb(MessageReceivedEvent e) {
		String GuildId = e.getGuild().getId();
		try {
			this.conn = (conn.isClosed()) ? Ref.connect() : this.conn;

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
	private void handle(MessageReceivedEvent e) {
		// only process further Checks if the message Starts with the bot's prefix
		String Message = e.getMessage().getContentRaw();
		String GuildId = e.getGuild().getId();
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
	// i'm not using the keys for anything yet, but could be
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
				!member.getUser().getId().equals(Ref.botId) && // bot must not kick its own ass
				guild.getMemberById(Ref.botId).canInteract(member)) // bot must be able to interact
				{

					// Queue Kick event
					controller.kick(member).queue();
					// Log this action
					if (logChannel != null)
						logChannel.sendMessage("Attempted pruning of " + member.getEffectiveName()).queue();
				}

			});

		});

		commands.put("setLanding", (event, args) -> {
			try {
				this.conn = (this.conn.isClosed()) ? Ref.connect() : this.conn;

				System.out.println(args.get(0));
				String sane_channel = "";
				if (args.get(0).length() > 3) {
					sane_channel = args.get(0).substring(2, args.get(0).length()-1);
				} else {
					event.getChannel().sendMessage("Try again with a proper argument").queue();
					return;
				}
				try {
					event.getGuild().getTextChannelById(sane_channel);
				} catch (Exception exc) {
					event.getChannel().sendMessage("Try again with a proper argument").queue();
					return;
				}

				Statement stmnt = this.conn.createStatement();

				ResultSet res = stmnt.executeQuery(
						"SELECT landing_channel FROM guild_settings WHERE id = '" + event.getGuild().getId() + "'");
				if (res.first()) {
					System.out.println("update");
					stmnt.execute("UPDATE guild_settings set landing_channel = \"" + sane_channel + "\""
							+ " WHERE id = \"" + event.getGuild().getId() + "\"");
				} else {
					System.out.println("insert");
					stmnt.execute("insert into guild_settings (id, landing_channel) values (\"" + event.getGuild().getId()
							+ "\", \"" + sane_channel + "\")");
				}
				stmnt.close();

				this.conn.close();
			} catch (SQLException e4) {
				e4.printStackTrace();
			}
		});
		commands.put("setLog", (event, args) -> {
			try {
				this.conn = (this.conn.isClosed()) ? Ref.connect() : this.conn;

				System.out.println(args.get(0));
				String sane_channel = "";
				if (args.get(0).length() > 3) {
					sane_channel = args.get(0).substring(2, args.get(0).length()-1);
				} else {
					event.getChannel().sendMessage("Try again with a proper argument").queue();
					return;
				}
				try {
					event.getGuild().getTextChannelById(sane_channel);
				} catch (Exception exc) {
					event.getChannel().sendMessage("Try again with a proper argument").queue();
					return;
				}

				Statement stmnt = this.conn.createStatement();

				ResultSet res = stmnt.executeQuery(
						"SELECT log_channel FROM guild_settings WHERE id = '" + event.getGuild().getId() + "'");
				if (res.first()) {
					System.out.println("update");
					stmnt.execute("UPDATE guild_settings set log_channel = \"" + sane_channel + "\""
							+ " WHERE id = \"" + event.getGuild().getId() + "\"");
				} else {
					System.out.println("insert");
					stmnt.execute("insert into guild_settings (id, log_channel) values (\"" + event.getGuild().getId()
							+ "\", \"" + sane_channel + "\")");
				}
				stmnt.close();

				this.conn.close();
			} catch (SQLException e4) {
				e4.printStackTrace();
			}
		});

		commands.put("getParms", (event, args) -> {
		  System.out.println(this.getBotChannelOrReadFromDb(event));
		});
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		this.handle(event);
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
