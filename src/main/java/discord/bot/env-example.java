package discord.bot;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * @author Okoratu
 * a collection of environment variables
 */
public class env {
	public static final String token   = "";
	public static final String botId   = "";
	public static final String dbUser  = "";
	public static final String dbPass  = "";
	public static final String db	   = "";
	public static final String prefix  = "!";
	public static final DSLContext SQL = DSL.using(SQLDialect.MARIADB); 
}
