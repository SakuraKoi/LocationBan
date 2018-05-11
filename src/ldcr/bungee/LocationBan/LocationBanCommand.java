package ldcr.bungee.LocationBan;

import java.io.IOException;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class LocationBanCommand extends Command {
	public LocationBanCommand() {
		super("locationban");
	}

	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if (!sender.hasPermission("LocationBan.admin")) {
			sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c你没有权限执行此命令!"));
			return;
		}
		if (args.length==0) {
			sender.sendMessage(new TextComponent("§e/locationban info <玩家>                查询玩家上次登录信息及归属地"));
			sender.sendMessage(new TextComponent("§e/locationban ban <原因> <归属地关键字>  封禁IP归属地"));
			sender.sendMessage(new TextComponent("§e/locationban unban <归属地关键字>       解封IP归属地"));
			sender.sendMessage(new TextComponent("§e/locationban list                      列出所有被封禁的IP归属地"));
			return;
		}
		switch (args[0].toLowerCase()) {
		case "reload": {
			if (!LocationBan.instance.reload()) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §b重载失败"));
				return;
			}
			sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §b归属地封禁已重载!"));
			return;
		}
		case "info": {
			if (args.length<2) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §e/locationban info <玩家>                查询玩家上次登录信息及归属地"));
				return;
			}
			final String ip = LocationBan.instance.database.getLastLoginIP(args[1]);
			if (ip.isEmpty()) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c玩家 "+args[1]+" 没有登录过服务器"));
				return;
			}
			String location;
			try {
				location = LocationBan.instance.ipSeeker.getAddress(ip);
			} catch (final IOException e1) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c错误: 在数据库中查询IP "+ip+" 时出错."));
				return;
			}
			sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §a玩家 §b"+args[1]+" §a上次登录于 §e"+ip+" ["+location+"]"));
			return;
		}
		case "ip": {
			final String ip = args[1];
			String location;
			try {
				location = LocationBan.instance.ipSeeker.getAddress(ip);
			} catch (final IOException e1) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c错误: 在数据库中查询IP "+ip+" 时出错."));
				return;
			}
			sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §aIP §b"+ip+" §a归属于 §e"+location));
			return;
		}
		case "ban": {
			if (args.length<3) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §e/locationban ban <原因> <归属地关键字>  封禁IP归属地"));
				return;
			}
			final String reason = args[1].replace('&', '§').replace("§§", "&");;
			final StringBuilder builder = new StringBuilder();
			for (int i = 2;i<args.length;i++) {
				builder.append(' ');
				builder.append(args[i]);
			}
			builder.deleteCharAt(0);
			final String keyword = builder.toString();
			final LocationBanEntry entry = new LocationBanEntry(keyword,reason,sender.getName());
			final int result = LocationBan.instance.database.ban(entry);
			if (result==0) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §a成功封禁归属地 §c"+keyword+"§a: §e"+reason));
				LocationBan.broadcast(LocationBan.bannedBroadcast.replace("%location%", keyword).replace("%player%", sender.getName()).replace("%reason%", reason));
			} else if (result==1) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c您要封禁的归属地 §e"+keyword+"§c已被另一归属地封禁覆盖"));
			} else if (result==2) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c归属地 §e"+keyword+"§c已被封禁"));
			} else {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c封禁归属地时出现未知错误"));
			}
			return;
		}
		case "unban": {
			if (args.length<2) {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §e/locationban unban <归属地关键字>       解封IP归属地"));
				return;
			}
			final StringBuilder builder = new StringBuilder();
			for (int i = 1;i<args.length;i++) {
				builder.append(' ');
				builder.append(args[i]);
			}
			builder.deleteCharAt(0);
			final String keyword = builder.toString();
			final LocationBanEntry entry = LocationBan.instance.database.isBanned(keyword);
			if (entry!=null) {
				if (LocationBan.instance.database.unban(entry)) {
					sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §a已成功解封归属地 "+keyword));
					return;
				} else {
					sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c归属地 "+keyword+" 未被封禁"));
					return;
				}
			} else {
				sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §c归属地 "+keyword+" 未被封禁"));
				return;
			}
		}
		case "list": {
			sender.sendMessage(new TextComponent("§a§lLocationBan §7>> §a所有被封禁的归属地: "));
			for (final LocationBanEntry entry : LocationBan.instance.database.getAllBanned()) {
				sender.sendMessage(new TextComponent("§e[ §c"+entry.getKeyword()+" §e] §a"+entry.getReason()+" §eBy §b"+entry.getExecutor()));
			}
			return;
		}
		}

	}

}
