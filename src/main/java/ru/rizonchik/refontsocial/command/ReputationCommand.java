package ru.rizonchik.refontsocial.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.NumberUtil;

import java.util.*;
import java.util.stream.Collectors;

public final class ReputationCommand implements CommandExecutor, TabCompleter {

    private final RefontSocial plugin;

    public ReputationCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("repreload")) {
            return handleReload(sender);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(plugin, "playerOnly"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(plugin, "noPermission"));
            return true;
        }

        if (args.length == 0) {
            plugin.getReputationService().sendShow(player, player.getUniqueId(), player.getName());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("help")) {
            Colors.sendList(player, plugin, "help");
            return true;
        }

        if (sub.equals("profile")) {
            if (args.length < 2) {
                Colors.sendList(player, plugin, "help");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                player.sendMessage(Colors.msg(plugin, "playerNotFound"));
                return true;
            }
            plugin.getGuiService().openProfile(player, target.getUniqueId(), target.getName());
            return true;
        }

        if (sub.equals("top")) {
            TopCategory category = TopCategory.SCORE;
            int page = 1;

            if (args.length >= 2) {
                String c = args[1].toUpperCase(Locale.ROOT);
                if (c.equals("SCORE")) category = TopCategory.SCORE;
                else if (c.equals("LIKES")) category = TopCategory.LIKES;
                else if (c.equals("DISLIKES")) category = TopCategory.DISLIKES;
                else if (c.equals("VOTES")) category = TopCategory.VOTES;
                else {
                    page = NumberUtil.parseInt(args[1], 1);
                }
            }

            if (args.length >= 3) {
                page = NumberUtil.parseInt(args[2], 1);
            }

            if (page < 1) page = 1;
            plugin.getGuiService().openCategoryTop(player, category, page);
            return true;
        }

        if (sub.equals("like") || sub.equals("dislike")) {
            if (args.length < 2) {
                Colors.sendList(player, plugin, "help");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                player.sendMessage(Colors.msg(plugin, "playerNotFound"));
                return true;
            }

            boolean like = sub.equals("like");
            boolean reasonsEnabled = plugin.getConfig().getBoolean("reasons.enabled", true);

            if (reasonsEnabled) {
                plugin.getGuiService().openReasons(player, target.getUniqueId(), target.getName(), like);
            } else {
                plugin.getReputationService().vote(player, target.getUniqueId(), target.getName(), like);
            }
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(Colors.msg(plugin, "playerNotFound"));
            return true;
        }

        plugin.getGuiService().openRate(player, target.getUniqueId(), target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean canUse = sender.hasPermission("refontsocial.use");
        boolean canAdmin = sender.hasPermission("refontsocial.admin");

        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            if (canUse) {
                base.addAll(Arrays.asList("help", "top", "like", "dislike", "profile"));
            }
            if (canAdmin) {
                base.add("reload");
            }
            if (base.isEmpty()) {
                return Collections.emptyList();
            }

            String p = args[0].toLowerCase(Locale.ROOT);
            return base.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("like")
                || args[0].equalsIgnoreCase("dislike")
                || args[0].equalsIgnoreCase("profile"))) {
            if (!canUse) {
                return Collections.emptyList();
            }

            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(30)
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            if (!canUse) {
                return Collections.emptyList();
            }

            String p = args[1].toLowerCase(Locale.ROOT);
            return Arrays.asList("score", "likes", "dislikes", "votes").stream()
                    .filter(s -> s.startsWith(p))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            if (!canUse) {
                return Collections.emptyList();
            }
            return Collections.singletonList("page");
        }

        return Collections.emptyList();
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("refontsocial.admin")) {
            sender.sendMessage(Colors.msg(plugin, "noPermission"));
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(Colors.msg(plugin, "reloaded"));
        return true;
    }
}
