package ldcr.bungee.LocationBan;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import ldcr.bungee.LocationBan.utils.MysqlConnection;

public class DatabaseManager {
	private MysqlConnection conn = null;
	private final HashSet<LocationBanEntry> banList = new HashSet<LocationBanEntry>();
	private final String BANNED_TABLE_NAME = "LocationBan_banned";
	private final String IP_TABLE_NAME = "LocationBan_ip";

	public void connect(final String mysqlServer, final String mysqlPort, final String mysqlDatabase, final String mysqlUser,
			final String mysqlPassword) throws SQLException {
		if (conn!=null) {
			disconnect();
		}
		LocationBan.consoleMessage("正在连接Mysql数据库 "+mysqlServer+":"+mysqlPort+" ...");
		conn = new MysqlConnection(mysqlServer, mysqlUser, mysqlPort, mysqlPassword, mysqlDatabase, 10, LocationBan.instance);
		if (conn.isConnection()) {
			conn.createTable(BANNED_TABLE_NAME, "keyword", "reason", "executor");
			conn.createTable(IP_TABLE_NAME, "player","address");
		} else throw new SQLException("Failed connect Database");
	}
	public void disconnect() {
		if (conn!=null) {
			if (conn.isConnection()) {
				LocationBan.consoleMessage("正在关闭数据库连接...");
				conn.closeConnection();
				LocationBan.consoleMessage("已与数据库断线.");
			}
		}
	}
	public void loadDatas() {
		banList.clear();
		final LinkedList<HashMap<String, Object>> datas = conn.getValues(BANNED_TABLE_NAME, -1, "keyword","reason","executor");
		for (final HashMap<String, Object> data : datas) {
			banList.add(new LocationBanEntry(data.get("keyword").toString(), data.get("reason").toString(), data.get("executor").toString()));
		}
	}
	public String getLastLoginIP(final String player) {
		final Object result = conn.getValue(IP_TABLE_NAME, "player", player.toLowerCase(), "address");
		return result==null ? "" : result.toString();
	}
	public void newIPRecord(final String player, final String ip) {
		conn.intoValue(IP_TABLE_NAME, player.toLowerCase(), ip);
	}
	public void updateIPRecord(final String player, final String ip) {
		conn.setValue(IP_TABLE_NAME, "player", player.toLowerCase(), "address", ip);
	}
	private final HashSet<String> joinedBeforeCache = new HashSet<String>();
	public boolean hasJoinedBefore(final String player) {
		if (joinedBeforeCache.contains(player.toLowerCase())) return true;
		final boolean has = conn.isExists(IP_TABLE_NAME, "player", player.toLowerCase());
		if (has) {
			joinedBeforeCache.add(player.toLowerCase());
		}
		return has;
	}
	public LocationBanEntry isBanned(final String location) {
		for (final LocationBanEntry entry : banList) {
			if (location.contains(entry.getKeyword())) return entry;
		}
		return null;
	}
	public HashSet<LocationBanEntry> getAllBanned() {
		return banList;
	}
	public int ban(final LocationBanEntry entry) { // 0=success 1=override 2=has
		if (banList.contains(entry)) return 2;
		if (isBanned(entry.getKeyword())!=null) return 1;
		banList.add(entry);
		conn.intoValue(BANNED_TABLE_NAME, entry.getKeyword(),entry.getReason(),entry.getExecutor());
		return 0;
	}
	public boolean unban(final LocationBanEntry entry) {
		if (banList.contains(entry)) {
			banList.remove(entry);
			conn.deleteValue(BANNED_TABLE_NAME, "keyword", entry.getKeyword());
			return true;
		}
		return false;
	}
}
