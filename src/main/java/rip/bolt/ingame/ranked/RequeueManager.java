package rip.bolt.ingame.ranked;

import static net.kyori.adventure.text.Component.text;

import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltResponse;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class RequeueManager implements Listener {

  private static final ItemStack RED_DYE = createRequeueItem(1);
  private static final ItemStack GREEN_DYE = createRequeueItem(2);

  private Map<Player, Long> lastRequeues = new OnlinePlayerMapAdapter<Long>(Ingame.get());

  private static ItemStack createRequeueItem(int data) {
    ItemStack item = new ItemStack(Material.INK_SACK, 1, (short) data);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Click to requeue");
    item.setItemMeta(meta);

    return item;
  }

  public void sendRequeueMessage(MatchPlayer player) {
    player.getInventory().setItem(2, RED_DYE);
    player.sendMessage(Messages.requeue());
  }

  @EventHandler
  public void onRequestRequeue(ObserverInteractEvent event) {
    if (!(event.getClickType() == ClickType.LEFT || event.getClickType() == ClickType.RIGHT))
      return;

    if (RED_DYE.equals(event.getClickedItem())) requestRequeue(event.getPlayer());
  }

  public void requestRequeue(MatchPlayer player) {
    long time = System.currentTimeMillis();
    if (lastRequeues.getOrDefault(player.getBukkit(), 0L) + 3000L > time) return;

    lastRequeues.put(player.getBukkit(), time);
    Ingame.newChain()
        .asyncFirst(() -> Ingame.get().getApiManager().postPlayerRequeue(player.getId()))
        .syncLast(response -> sendResponse(player, response))
        .execute();
  }

  private void sendResponse(MatchPlayer player, BoltResponse response) {
    if (response.getSuccess()) {
      player.getInventory().setItem(2, GREEN_DYE);
      return;
    }

    player.sendMessage(text(response.getMessage(), NamedTextColor.RED));
  }
}
