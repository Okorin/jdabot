package discord.bot;



import java.sql.Connection;
import java.sql.DriverManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;


/**
 * Hello world!
 *
 */
public class App
{
	
    public static void main( String[] args ) throws Exception
    {
    	 try (Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:3306/" + Ref.db, Ref.dbUser, Ref.dbPass)) {

    		
    	    JDA jda = new JDABuilder(AccountType.BOT)
        				.setToken(Ref.token)
        				.buildBlocking();
    	    jda.addEventListener(new MessageCommandController(conn));
        	jda.addEventListener(new GuildEventsController(conn));
    		 
         }

    }
    

}

