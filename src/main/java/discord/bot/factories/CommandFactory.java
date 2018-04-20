package discord.bot.factories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.conf.ParamType;

import discord.bot.Helpers;
import discord.bot.MessageReceivedCommand;
import discord.bot.env;


/**
 * @author Okoratu
 *
 */
public interface CommandFactory {
	


	// Create a generic command to interact with the db using a Table
	
	/**
	 * @param tab 		The Table to base all of this on
	 * @param column	The Column that is set or Read
	 * @param gId		The Column to save Guild information into
	 * @return
	 */
	public static MessageReceivedCommand createChannelSetterCommand(Table<?>  tab, TableField<?,String> column,
			TableField<?,String> gId) {

		return (event, args) -> {
			
			// Queries can fail, so this needs to be wrapped
			try {
				
				// Connect
				Connection conn = Helpers.connect();
				
				// Variable Shortcuts
				String sane_channel = "";
				String guild_id     = event.getGuild().getId();
				
				// These commands must have arguments
				if (!args.isEmpty()) {
					
					// The Channel that we're looking for is returned with <#?> format, so it must be at least 4 chars
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
					ResultSet res = stmnt.executeQuery(env.SQL.select(column)
															  .from(tab)
															  .where(gId.eq(guild_id))
															  .getSQL(ParamType.INLINED));
					
					// Set Pointer to the result
					if (res.first()) { // true: resultset contains a line -> update existing
						stmnt.execute(env.SQL.update(tab)
										     .set(column, sane_channel)
										     .where(gId.eq(guild_id))
										     .getSQL(ParamType.INLINED));
					} else {		   // false: no lines -> create a new entry
						stmnt.execute(env.SQL.insertInto(tab) 
											 .columns(gId, column)
											 .values(guild_id, sane_channel)
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
}
