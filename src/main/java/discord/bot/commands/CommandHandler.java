package discord.bot.commands;

import static generated_resources.Tables.GUILD_SETTINGS;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.conf.ParamType;

import discord.bot.Helpers;
import discord.bot.MessageReceivedCommand;
import discord.bot.env;
import discord.bot.listeners.DiscordGuildListenerAdapter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandHandler extends DiscordGuildListenerAdapter {

	public int rateLimitMax = 5;
	public int rateLimitCooldown = 60;
	private int commandCounter = 0;
	private long firstCommandInCounter = 0;
	private long cooldownUntil = 0;

	/**
	 * excluding determines whether the rolelisting
	 * 
	 * @param excluding
	 * @return a masskick roles command
	 */
	public MessageReceivedCommand massKick(boolean excluding) {
		return (event, args) -> {
			Guild guild = event.getGuild();
			TextChannel logChannel = this.getChannelFromDb(event, GUILD_SETTINGS, GUILD_SETTINGS.LOG_CHANNEL,
					GUILD_SETTINGS.ID);

			// Members of the guild
			List<Member> members = guild.getMembers();

			// Check all members
			members.forEach(member -> {

				// Roles of each member
				List<Role> memberRoles = member.getRoles();

				// Filter those roles against the arguments supplied to the command
				Long matches = memberRoles.stream().filter(role -> args.contains(role.getName()))
						.collect(Collectors.counting());

				// excluding is true: matches must be 0 -> user doesnt have the roles
				// excluding is false: matches must not be 0 -> user must have at least one role
				if (((excluding && matches == 0) || (!excluding && matches != 0)) && // filter must match nothing
				!member.getUser().getId().equals(env.botId) && // bot must not kick its own ass
				guild.getMemberById(env.botId).canInteract(member)) // bot must be able to interact
				{
					// Queue Kick event
					this.kickOne(guild, member, logChannel);
				}

			});
		};
	}

	public MessageReceivedCommand kickMembers() {
		return (event, args) -> {
			if (!args.isEmpty()) {
				Guild guild = event.getGuild();
				TextChannel logChannel = this.getChannelFromDb(event, GUILD_SETTINGS, GUILD_SETTINGS.LOG_CHANNEL,
						GUILD_SETTINGS.ID);
				args.forEach(arg -> {
					if (arg.length() > 3) {
						Member memberToKick = guild.getMemberById(arg.substring(2, arg.length() - 1));
						this.kickOne(guild, memberToKick, logChannel);
					}
				});
			}
		};
	}

	private void kickOne(Guild guild, Member member, TextChannel logChannel) {
		if (member != null && !member.getUser().getId().equals(env.botId)
				&& guild.getMemberById(env.botId).canInteract(member)) {

			if (logChannel != null) {
				String roleMessageAppend = "";
				List<Role> roles = member.getRoles();
				for (Role role : roles) {
					roleMessageAppend += "\n- " + role.getName();
				}
				logChannel.sendMessage("Attempted pruning of **" + member.getEffectiveName() + "**.\nTheir roles were:"
						+ roleMessageAppend).queue();
			}

			guild.getController().kick(member).queue();

		}
	}

	public MessageReceivedCommand getRoles() {
		return (event, args) -> {
			if (!args.isEmpty()) {
				if (args.get(0).length() > 3) {
					Member member = event.getGuild().getMemberById(args.get(0).substring(2, args.get(0).length() - 1));
					if (member != null) {
						List<Role> roles = member.getRoles();
						String rol = "";
						for (Role role : roles) {
							rol += "\n-" + role.getName();
						}
						event.getChannel().sendMessage("The Roles of " + member.getUser().getName() + "#"
								+ member.getUser().getDiscriminator() + ":" + rol).queue();
					} else {
						event.getChannel().sendMessage("Member not found!").queue();
					}
				}
			}
		};
	}

	public MessageReceivedCommand createChannelSetterCommand(Table<?> tab, TableField<?, String> column,
			TableField<?, String> gId) {

		return (event, args) -> {

			// Queries can fail, so this needs to be wrapped
			try {

				// Connect
				Connection conn = Helpers.connect();

				// Variable Shortcuts
				String sane_channel = "";
				String guild_id = event.getGuild().getId();

				// These commands must have arguments
				if (!args.isEmpty()) {

					// The Channel that we're looking for is returned with <#?> format, so it must
					// be at least 4 chars
					if (args.get(0).length() > 3) {
						sane_channel = args.get(0).substring(2, args.get(0).length() - 1);
					} else {
						event.getChannel().sendMessage("Try again with a proper argument").queue();
						return;
					}

					// Test if the Text Channel exists
					try {
						event.getGuild().getTextChannelById(sane_channel);
					} catch (Exception exc) {
						event.getChannel().sendMessage("Try again with a proper argument").queue();
						return; // Stop executing
					}

					// new Statement
					Statement stmnt = conn.createStatement();

					// Look up, if the entry already exists for this server
					ResultSet res = stmnt.executeQuery(
							env.SQL.select(column).from(tab).where(gId.eq(guild_id)).getSQL(ParamType.INLINED));

					// Set Pointer to the result
					if (res.first()) { // true: resultset contains a line -> update existing
						stmnt.execute(env.SQL.update(tab).set(column, sane_channel).where(gId.eq(guild_id))
								.getSQL(ParamType.INLINED));
					} else { // false: no lines -> create a new entry
						stmnt.execute(env.SQL.insertInto(tab).columns(gId, column).values(guild_id, sane_channel)
								.getSQL(ParamType.INLINED));
					}

					// Wrap up the statement and connection
					stmnt.close();
					conn.close();
				}
			} catch (SQLException e4) {
				e4.printStackTrace();
			}
		};

	}

	public MessageReceivedCommand ping() {
		return (event, args) -> {
			if (this.rateLimit())
				event.getChannel().sendMessage("I'm alive, don't worry").queue();
		};
	}

	private boolean rateLimit() {
		Long now = Instant.now().getEpochSecond();

		// Counter is hit and cooldown is active
		if (commandCounter >= rateLimitMax) {

			// the cooldown is active
			if (now < cooldownUntil) {
				System.out.println("rate limit hit");
				return false;
			} else {
				cooldownUntil = 0;
				commandCounter = 0;
			}
		}

		// commands refresh if ppl say nothing for some time
		// condition for this is cooldown must be inactive
		if ((firstCommandInCounter + rateLimitCooldown) < now && cooldownUntil == 0) {
			commandCounter = 0;
		}

		// this is the first command
		if (commandCounter == 0)
			firstCommandInCounter = now;

		// commandcounter increase
		commandCounter++;
		System.out.println("Current commandcounter: " + commandCounter);

		// command counter just hit the rate limit?
		if (commandCounter == rateLimitMax)
			cooldownUntil = now + rateLimitCooldown;

		// the command is valid in this case
		return true;

	}

	class mentionedRoles {
		public String mentioned;
		public Role role;
		public boolean include;

		public mentionedRoles(String mentioned, Role role, boolean include) {
			this.mentioned = mentioned;
			this.role = role;
			this.include = include;
		}
	}

	// gets
	public MessageReceivedCommand getUsersIfRoles(String format) {
		return (event, args) -> {
			Guild guild = event.getGuild();
			List<mentionedRoles> mentionedRoles = new ArrayList<mentionedRoles>();
			List<Member> allMembers = guild.getMembers();
			List<Member> resultingMembers = new ArrayList<Member>();
			String output = "";
			// Reconstruct the arguments
			for (String arg : args) {
				String argument = arg.replace("_", " ");
				boolean inc = (argument.startsWith("!")) ? false : true;
				if (argument.startsWith("!")) {
					argument = argument.substring(1, argument.length());
				} 
				List<Role> matches = guild.getRolesByName(argument, true);
				System.out.println(matches);
				if (matches != null)
				for (Role match : matches) {
					mentionedRoles.add(new mentionedRoles(argument, match, inc));
				}
			}
			
			for (Member member : allMembers) {
				boolean excluded = false;
				List<Role> memberRoles = member.getRoles();
				for (mentionedRoles role : mentionedRoles) {
					if (role.include == true) {
						if (!memberRoles.contains(role.role)) { excluded = true; System.out.println("Excluded " + member.getUser().getName() + " for not having " + role.role); break; }
					} else {
						if (memberRoles.contains(role.role)) { excluded = true; System.out.println("Excluded " + member.getUser().getName() + " for having " + role.role); break; }
					}
				}
				if (!excluded) resultingMembers.add(member);
			}
			if (!resultingMembers.isEmpty()) {
				output = "The following members matches the conditions:\n```";
			for (Member result : resultingMembers) {
				switch (format) {
					case "mentions": output += " " + result.getAsMention(); break;
					case "usernames": output += " " + result.getUser().getName(); break;
					case "fullyQualified": output += " " + result.getUser().getName() + "#" + result.getUser().getDiscriminator(); break;
					case "nicknames": output += " " + result.getNickname(); break;
					default: output += " " + result.getAsMention(); break;
				}
			}
				output += "```";
			} else {
				output = "no matches!";
			}
			event.getChannel().sendMessage(output).queue();
		};
	}

}
