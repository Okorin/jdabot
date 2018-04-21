package discord.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Okoratu
 *
 */
public class Helpers {
	/**
	 * @return shorthand opened DB Connection
	 */
	public static Connection connect() {
		try {
			return DriverManager.getConnection("jdbc:mysql://localhost:3307/" + env.db + "?useSSL=false", env.dbUser, env.dbPass);
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
