/**
 * 
 */
package discord.bot.listeners;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.conf.ParamType;

import discord.bot.Helpers;
import discord.bot.env;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * @author Okoratu
 * 		   This Class unifies generic DB interactions for Discord Guilds 
 */
public abstract class DiscordGuildListenerAdapter extends ListenerAdapter {

	// Command Prefix, take config, must not be re-registered
	/**
	 * 
	 */
	protected final String prefix = env.prefix;


	

	/**
	 * Gets a channel for a Guild from the DB.
	 * @param guild
	 * @param table
	 * @param field
	 * @param guildId
	 * @return the TextChannel, or NULL
	 */
	protected TextChannel getChannelFromDb(Guild guild, Table<?> table, TableField<?, String> field, TableField<?, String> guildId) {
		try {
			String guild_id = guild.getId();
			Connection conn = Helpers.connect();
			Statement stmnt = conn.createStatement();
			TextChannel ret = null;
			ResultSet chann = stmnt.executeQuery(
					env.SQL.select(field).from(table).where(guildId.eq(guild_id)).getSQL(ParamType.INLINED));
			if (chann.first() && !chann.wasNull()) {
				ret = guild.getTextChannelById(chann.getString(field.getName()));
			}
			System.out.println(ret);
			conn.close();
			stmnt.close();
			return ret;
		} catch (SQLException excp) {
			return null;
		}	
	}

	
	/**
	 * @param guild
	 * @param channelCache
	 * @param table
	 * @param field
	 * @param guildId
	 * @return Decided based on the ChannelCache, whether it needs to query
	 */
	protected TextChannel getChannelOrReadDb(Guild guild, Map<String, TextChannel> channelCache, Table<?> table, TableField<?, String> field, TableField<?,String> guildId)
	{
		String guild_id = guild.getId();
		
		if (!channelCache.containsKey(guild_id))
		{
			channelCache.put(guild_id, this.getChannelFromDb(guild,table,field,guildId));
		} 
		
		return channelCache.get(guild_id);
	}
	
	/**
	 * Overload for generic Guild Events
	 * @param e
	 * @param table
	 * @param field
	 * @param guildId
	 * @return the TextChannel or null
	 */
	protected TextChannel getChannelFromDb(GenericGuildEvent e, Table<?> table, TableField<?, String> field, TableField<?, String> guildId) {
		return this.getChannelFromDb(e.getGuild(), table, field, guildId);
	}
	/**
	 * Overload for generic Message Events
	 * @param e
	 * @param table
	 * @param field
	 * @param guildId
	 * @return the TextChannel or null
	 */
	protected TextChannel getChannelFromDb(GenericMessageEvent e, Table<?> table, TableField<?, String> field, TableField<?, String> guildId) {
		return this.getChannelFromDb(e.getGuild(), table, field, guildId);
	}
	
	/**
	 * Overload for generic Message Events
	 * @param e
	 * @param channelCache
	 * @param table
	 * @param field
	 * @param guildId
	 * @return the TextChannel or null
	 */
	protected TextChannel getChannelOrReadDb(GenericMessageEvent e, Map<String, TextChannel> channelCache, Table<?> table, TableField<?, String> field, TableField<?, String> guildId) {
		return this.getChannelOrReadDb(e.getGuild(), channelCache, table, field, guildId);
	}
	/**
	 * Overload for generic Message Events
	 * @param e
	 * @param channelCache
	 * @param table
	 * @param field
	 * @param guildId
	 * @return the TextChannel or null
	 */
	protected TextChannel getChannelOrReadDb(GenericGuildEvent e, Map<String, TextChannel> channelCache, Table<?> table, TableField<?, String> field, TableField<?, String> guildId) {
		return this.getChannelOrReadDb(e.getGuild(), channelCache, table, field, guildId);
	}
	


}
