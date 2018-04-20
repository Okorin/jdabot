package discord.bot;



import java.sql.Connection;
import java.sql.DriverManager;

import discord.bot.listeners.GuildMemberJoinEventListener;
import discord.bot.listeners.MessageReceivedEventListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;


/**
 * @author Okoratu
 *
 */
public class App
{
	
    public static void main( String[] args ) throws Exception
    {
    	 try (Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:3306/" + env.db, env.dbUser, env.dbPass)) {

    		// Initialize
    	    JDA jda = new JDABuilder(AccountType.BOT)
        				.setToken(env.token)
        				.buildBlocking();
    	    
    	    // Add Event Listeners
    	    jda.addEventListener(new MessageReceivedEventListener(conn)); // Handle Messages in channels the bot can read
        	jda.addEventListener(new GuildMemberJoinEventListener(conn)); // Handle New Members joined
    		 
         }

    }
    

}

