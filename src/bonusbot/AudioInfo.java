package bonusbot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import bonusbot.guild.GuildExtends;
import bonusbot.lavaplayer.Track;
import bonusbot.lavaplayer.TrackScheduler;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.EmbedObject.EmbedFieldObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmbed;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

/**
 * Manage the audio-info embed for !playing and the audio-info channel.
 * 
 * @author emre1702
 *
 */
public class AudioInfo {
	/** map for last created embedobject in guild */
	private static Map<Long, EmbedObject> guildlastembed = new HashMap<Long, EmbedObject>();

	/**
	 * Get queue infos to display.
	 * 
	 * @param queue
	 *            The queue where we want to get the infos from.
	 * @return List of String for queue-display.
	 */
	private static List<String> getQueueInfos(List<Track> queue) {
		List<String> queuestrlist = new ArrayList<String>();
		String queuestr = "";
		int queuei = 0;
		for (Track track : queue) {
			++queuei;
			AudioTrackInfo queuetrackinfo = track.audio.getInfo();
			if ((queuestr + queuei + ". " + queuetrackinfo.title + "\n").length() > EmbedBuilder.FIELD_CONTENT_LIMIT) {
				queuestrlist.add(queuestr);
				queuestr = queuei + ". " + queuetrackinfo.title + "\n";
			} else {
				queuestr += queuei + ". " + queuetrackinfo.title + "\n";
			}
		}
		if (queuei == 0)
			queuestr = "-";
		queuestrlist.add(queuestr);
		return queuestrlist;
	}

	/**
	 * Creates the EmbedObject with informations the audiotrack for audio-info
	 * channel.
	 * 
	 * @param audiotrack
	 *            The audiotrack we want to get the infos of.
	 * @param user
	 *            User who added the audiotrack.
	 * @param guild
	 *            Guild where all happenes.
	 * @param dateadded
	 *            Date when the audio got added.
	 * @param scheduler
	 *            The TrackScheduler to get the queue from.
	 * 
	 * @return EmbedObject with infos for the audio-info channel.
	 */
	public static EmbedObject createAudioInfo(AudioTrack audiotrack, IUser user, IGuild guild, LocalDateTime dateadded,
			TrackScheduler scheduler) {
		try {
			EmbedBuilder builder = new EmbedBuilder();
			AudioTrackInfo info = audiotrack.getInfo();

			builder.withAuthorName(user.getDisplayName(guild));
			builder.withAuthorIcon(user.getAvatarURL());
			builder.withColor(0, 0, 150);
			if (info.isStream) {
				builder.withDescription(info.uri);
				builder.withUrl(info.uri);
			} else
				builder.withDescription(info.uri);

			builder.withFooterText("Audio-info");
			// builder.withImage( "https://www.youtube.com/yts/img/yt_1200-vfl4C3T0K.png" );
			builder.withThumbnail(
					"https://lh3.googleusercontent.com/Ned_Tu_ge6GgJZ_lIO_5mieIEmjDpq9kfgD05wapmvzcInvT4qQMxhxq_hEazf8ZsqA=w300");
			builder.withTitle(info.title);

			int minutes = (int) (Math.floor(info.length / 60000));
			int seconds = (int) (Math.floor(info.length / 1000) % 60);
			builder.appendField("Status:", "playing", true);
			builder.appendField("Volume:",
					String.valueOf(GuildExtends.get(guild).getAudioManager().getPlayer().getVolume()), true);
			builder.appendField("Length:", minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds), true);
			builder.appendField("Added:", Util.getTimestamp(dateadded), true);

			List<String> queueinfos = getQueueInfos(scheduler.getQueue());
			for (int i = 0; i < queueinfos.size(); ++i) {
				builder.appendField("Queue:", queueinfos.get(i), false);
			}

			EmbedObject obj = builder.build();

			refreshLastChangedTimestamp(obj);

			guildlastembed.put(guild.getLongID(), obj);

			return obj;
		} catch (Exception e) {
			LogManager.getLogger().error(e);
			return null;
		}
	}

	/**
	 * Refresh last-changed info in embed
	 * 
	 * @param obj
	 *            EmbedObject
	 */
	private static void refreshLastChangedTimestamp(EmbedObject obj) {
		obj.timestamp = Util.getTimestampForDiscord();
	}

	/**
	 * Replace the audio-info of a guild with a new EmbedObject (or old edited).
	 * 
	 * @param guild At which guild.
	 * @param obj The new EmbedObject to add.
	 */
	private static void replaceAudioInfo(IGuild guild, EmbedObject obj) {
		GuildExtends guildext = GuildExtends.get(guild);
		IChannel audioinfochannel = guildext.getChannel("audioInfoChannel");
		if (audioinfochannel != null) {
			IMessage msg = audioinfochannel.getFullMessageHistory().getEarliestMessage();
			if (msg != null) {
				Util.editMessage(msg, obj);
			}
		}
	}

	/**
	 * Change the status in the EmbedObject. If the audio-info is available, refresh
	 * it there.
	 * 
	 * @param guild
	 *            Guild where you want to change the EmbedObject.
	 * @param status
	 *            The new status.
	 */
	public static void changeAudioInfoStatus(IGuild guild, String status) {
		EmbedObject obj = getLastAudioInfo(guild);
		if (obj != null && obj.fields.length > 0) {
			obj.fields[0].value = status;
			refreshLastChangedTimestamp(obj);
			replaceAudioInfo(guild, obj);
		}
	}

	/**
	 * Change the volume-info in the EmbedObject. If the audio-info is available,
	 * refresh it there.
	 * 
	 * @param guild
	 *            Guild where you want to change the EmbedObject.
	 * @param volume
	 *            The new volume-info.
	 */
	public static void changeAudioInfoVolume(IGuild guild, int volume) {
		EmbedObject obj = getLastAudioInfo(guild);
		if (obj != null && obj.fields.length > 1) {
			obj.fields[1].value = String.valueOf(volume);
			refreshLastChangedTimestamp(obj);
			replaceAudioInfo(guild, obj);
		}
	}

	/**
	 * Change the queue-info in the EmbedObject. If the audio-info is available,
	 * refresh it there.
	 * 
	 * @param guild
	 *            Guild where you want to change the EmbedObject.
	 * @param scheduler
	 *            The TrackScheduler to get the queue.
	 */
	public static void refreshAudioInfoQueue(IGuild guild, TrackScheduler scheduler) {
		EmbedObject obj = getLastAudioInfo(guild);
		if (obj != null && obj.fields.length > 4) {
			List<String> queueinfos = getQueueInfos(scheduler.getQueue());
			int queueinfossize = queueinfos.size();
			if (obj.fields.length != 4 + queueinfossize) {
				EmbedFieldObject[] newarr = new EmbedFieldObject[4 + queueinfossize];
				System.arraycopy(obj.fields, 0, newarr, 0, 4);
				obj.fields = newarr;
				for (int i = 4; i < 4 + queueinfossize; ++i) {
					obj.fields[i] = new EmbedFieldObject();
					obj.fields[i].inline = false;
					obj.fields[i].name = "Queue:";
					obj.fields[i].value = "test";
				}
			}

			for (int i = 0; i < queueinfossize; ++i) {
				obj.fields[4 + i].value = queueinfos.get(i);
			}
			replaceAudioInfo(guild, obj);
		}
	}

	/**
	 * Getter for the last created audio-info-embed for the guild. If there is none
	 * in the map, check the audio-info channel.
	 * 
	 * @param guild
	 *            The guild whose audio-info we want to retrieve.
	 * @return The EmbedObject.
	 */
	public static EmbedObject getLastAudioInfo(IGuild guild) {
		EmbedObject obj = guildlastembed.get(guild.getLongID());
		if (obj == null) {
			GuildExtends guildext = GuildExtends.get(guild);
			IChannel audioinfochannel = guildext.getChannel("audioInfoChannel");
			if (audioinfochannel != null) {
				IMessage msg = audioinfochannel.getFullMessageHistory().getEarliestMessage();
				if (msg != null) {
					List<IEmbed> embeds = msg.getEmbeds();
					if (!embeds.isEmpty()) {
						obj = new EmbedObject(embeds.get(0));
					}
				}
			}
		}
		return obj;
	}

	/**
	 * Get the YouTube search result at !ytsearch for !ytplay/!ytqueue
	 * 
	 * @param guild
	 *            The guild where !ytsearch got written.
	 * @param user
	 *            The user who used !ytsearch.
	 * @param title
	 *            Search-string at !ytsearch.
	 * @param audiolist
	 *            List of audios retrieved by YouTube after searching.
	 * @param showamount
	 *            Show only amount of tracks.
	 * @return EmbedObject we want to send to the user.
	 */
	public static EmbedObject getAudioListInfo(IGuild guild, IUser user, String title, List<AudioTrack> audiolist,
			int showamount) {
		try {
			EmbedBuilder builder = new EmbedBuilder();

			builder.withAuthorName(title);
			builder.withTitle("-");

			showamount = audiolist.size() < showamount ? audiolist.size() : showamount;

			for (int i = 0; i < showamount; i++) {
				AudioTrack track = audiolist.get(i);
				AudioTrackInfo info = track.getInfo();
				int minutes = (int) (Math.floor(info.length / 60000));
				int seconds = (int) (Math.floor(info.length / 1000) % 60);
				String infostr = " ";
				/**
				 * 0 - 5: length, url and channel-name 6 - 10: length and channel-name 11+:
				 * length
				 */
				if (showamount <= 5) {
					infostr = Lang.getLang("length", user, guild) + ": "
							+ (minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds)) + "\n"
							+ Lang.getLang("url", user, guild) + ": " + info.uri + "\n"
							+ Lang.getLang("channel", user, guild) + ": " + info.author;
				} else if (showamount <= 10) {
					infostr = Lang.getLang("length", user, guild) + ": "
							+ (minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds)) + "\n"
							+ Lang.getLang("channel", user, guild) + ": " + info.author;
				} else
					infostr = Lang.getLang("length", user, guild) + ": "
							+ (minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds));
				builder.appendField((i + 1) + ". " + info.title, infostr, true);

			}

			builder.withFooterText("!ytqueue/!ytplay [" + Lang.getLang("number", user, guild) + "]");

			EmbedObject obj = builder.build();

			refreshLastChangedTimestamp(obj);

			return obj;
		} catch (Exception e) {
			LogManager.getLogger().error(e);
			return null;
		}
	}

}
