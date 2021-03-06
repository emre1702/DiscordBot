package bonusbot;

import org.apache.logging.log4j.LogManager;

import bonusbot.webhook.BonusHttpServer;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

/**
 * Main-method
 * 
 * @author emre1702
 *
 */
public class MainRunner {

	/**
	 * static void main
	 * 
	 * @param args
	 *            Console-args, not used
	 */
	public static void main(String[] args) {
		try {
			Settings.loadSettings();
			Database.check();
			if (Settings.get("webhookName") == null && Settings.get("httpServerPort") != null) {
				new BonusHttpServer(Settings.<Integer>get("httpServerPort"));
			}
			IDiscordClient client = Client.createClient(Settings.get("token"), true);

			EventDispatcher dispatcher = client.getDispatcher();
			dispatcher.registerListener(new bonusbot.commands.Handler());
			dispatcher.registerListener(new EventsListener());
		} catch (Exception e) {
			LogManager.getLogger().error(e);
		}
	}
}

// Invitation-Link://
// https://discordapp.com/api/oauth2/authorize?client_id=356578515472089089&scope=bot&permissions=70618192
// Infos: https://discordapp.com/developers/docs/topics/oauth2
// Permissions:
// https://discordapp.com/developers/docs/topics/permissions#permissions-bitwise-permission-flags