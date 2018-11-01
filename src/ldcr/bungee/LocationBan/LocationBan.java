package ldcr.bungee.LocationBan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

import ldcr.lib.QQWry.IPSeekerUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class LocationBan extends Plugin
{
	private String mysqlServer;
	private String mysqlPort;
	private String mysqlDatabase;
	private String mysqlUser;
	private String mysqlPassword;
	public static String bannedBroadcast;

	public static LocationBan instance;
	public Configuration config;
	public ConfigurationProvider cProvider;

	public IPSeekerUtil ipSeeker;
	public static boolean works = false;
	public DatabaseManager database;
	@Override
	public void onEnable() {
		instance = this;
		logger = getLogger();
		getDataFolder().mkdirs();
		reload();
		getProxy().getPluginManager().registerListener(this, new PlayerListener(this));
		getProxy().getPluginManager().registerCommand(this, new LocationBanCommand());
	}

	public boolean reload() {
		if (!loadConfig()) {
			getLogger().severe("配置文件加载失败.");
			return false;
		}
		final File database = new File(getDataFolder(),"qqwry.dat");
		if (database.exists()) {
			try {
				ipSeeker = new IPSeekerUtil(database);
			} catch (final Exception e) {
				getLogger().severe("错误: 加载数据库时出错: "+e.getMessage());
				return false;
			}
		} else {
			getLogger().info("错误: 数据库不存在!");
			return false;
		}
		if (this.database!=null) {
			this.database.disconnect();
		}
		this.database = new DatabaseManager();
		try {
			this.database.connect(mysqlServer, mysqlPort, mysqlDatabase, mysqlUser, mysqlPassword);
		} catch (final SQLException e) {
			getLogger().severe("错误: 加载数据库时出错: "+e.getMessage());
			return false;
		}
		this.database.loadDatas();
		works = true;
		return true;
	}

	private boolean loadConfig() {
		final File config = new File(getDataFolder(), "config.yml");
		if (!config.exists()) {
			try {
				final String file =
						"mysql:\n"
								+ "  server: 'localhost'\n"
								+ "  port: '3306'\n"
								+ "  database: 'locationban'\n"
								+ "  user: 'root'\n"
								+ "  password: 'password'\n"
								+ "bannedBroadcast: '&b&lFS Clan Network &7>> &c归属地 &e%location% &c被管理员 &a%player% &c封禁: &e%reason%'";
				final FileWriter fw = new FileWriter(config);
				final BufferedWriter out = new BufferedWriter(fw);
				out.write(file);
				out.close();
				fw.close();
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
			getLogger().severe("配置文件不存在... 判断为第一次启动, 请修改配置文件数据库并使用 /locationban reload 重载");
			return false;
		}
		cProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
		try {
			this.config = cProvider.load(config);
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		mysqlServer = this.config.getString("mysql.server","localhost");
		mysqlPort = this.config.getString("mysql.port","3306");
		mysqlDatabase = this.config.getString("mysql.database","locationban");
		mysqlUser = this.config.getString("mysql.user","root");
		mysqlPassword = this.config.getString("mysql.password","password");
		bannedBroadcast = this.config.getString("bannedBroadcast","&b&lFS Clan Network &7>> &c归属地 &e%location% &c被管理员 &a%player% &c封禁: &e%reason%").replace('&', '§').replace("§§", "&");
		return true;
	}

	@Override
	public void onDisable()
	{
		config = null;
	}
	private static java.util.logging.Logger logger;
	public static void broadcastOP(String message) {
		message = message.replace('&', '§').replace("§§", "&");
		for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
			if (p.hasPermission("locationban.alert")) {
				p.sendMessage(new TextComponent(message));
			}
		}
	}
	public static void broadcast(String message) {
		message = message.replace('&', '§').replace("§§", "&");
		for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
			p.sendMessage(new TextComponent(message));
		}
	}

	public static void consoleMessage(final String string) {
		logger.info(string);
	}
}
