package ldcr.bungee.LocationBan;

import java.io.IOException;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PlayerListener implements Listener {
	LocationBan plugin;
	public PlayerListener(final LocationBan plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onLogin(final PostLoginEvent e) {
		final ProxiedPlayer player = e.getPlayer();
		if (!LocationBan.works) {
			LocationBan.consoleMessage("警告: 归属地封禁已停止运行, 无法检查玩家 "+player.getName());
			return;
		}
		final String ip = player.getAddress().getAddress().getHostAddress();
		if (LocationBan.instance.database.hasJoinedBefore(player.getName())) {
			updatePlayerIP(player, true);
			return;
		} else {
			updatePlayerIP(player, false);
		}
		if (player.hasPermission("locationban.bypass")) return;
		String location;
		try {
			location = LocationBan.instance.ipSeeker.getAddress(ip);
		} catch (final IOException e1) {
			e1.printStackTrace();
			LocationBan.consoleMessage("警告: 归属地封禁检查玩家 "+player.getName()+" 时出错");
			return;
		}
		final LocationBanEntry banEntry = LocationBan.instance.database.isBanned(location);
		if (banEntry==null) return;

		player.disconnect(new TextComponent("§4您的IP地址 §e"+ip+" §4的归属地 §b"+location+" §4已被封禁\n\n"+
				"§c§l"+banEntry.getReason()+"\n\n"+
				"§b执行者: §a"+banEntry.getExecutor()+"\n\n"+
				"§6执行此封禁的原因通常是作弊者大肆更换IP逃避封禁\n"+
				"§a如您被波及误伤, 我们表示非常抱歉, 请联系管理员"
				));
		LocationBan.consoleMessage("玩家 "+player.getName()+" ["+ip+" -> "+location+"] 被归属地封禁拦截");
		LocationBan.broadcastOP("&bLocationBan &7>> &7&o玩家 "+player.getName()+" ["+ip+" -> "+location+"] 被归属地封禁拦截");
	}

	private void updatePlayerIP(final ProxiedPlayer player, final boolean hasJoinedBefore) {
		final String playerName = player.getName();
		final String ip = player.getAddress().getAddress().getHostAddress();
		if (hasJoinedBefore) {
			LocationBan.instance.database.updateIPRecord(playerName, ip);
		} else {
			LocationBan.instance.database.newIPRecord(playerName, ip);
		}
	}
}
