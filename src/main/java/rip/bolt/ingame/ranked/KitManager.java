package rip.bolt.ingame.ranked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.kits.ApplyItemKitEvent;
import tc.oc.pgm.kits.KitMatchModule;
import tc.oc.pgm.kits.Slot;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class KitManager implements Listener {

  private ItemStack tool;
  private OnlinePlayerMapAdapter<CustomKit> kits;

  public KitManager() {
    this.tool = new ItemStack(Material.CHEST);
    ItemMeta meta = tool.getItemMeta();
    meta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "Loadout Editor");
    tool.setItemMeta(meta);

    this.kits = new OnlinePlayerMapAdapter<CustomKit>(Ingame.get());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onObserverKit(ObserverKitApplyEvent event) {
    Inventory inventory = event.getPlayer().getInventory();
    ItemStack freezer = inventory.getItem(6);
    if (freezer != null) inventory.setItem(7, freezer);
    inventory.setItem(6, tool.clone());

    inventory.setItem(0, new ItemStack(Material.COMPASS));
  }

  @EventHandler
  public void onStartEditing(ObserverInteractEvent event) {
    if (event.getClickType() != ClickType.RIGHT) return;

    if (!tool.equals(event.getClickedItem())) return;

    MatchPlayer player = event.getPlayer();

    player.getInventory().clear();
    player.getBukkit().setGameMode(GameMode.ADVENTURE);
    player.getBukkit().setAllowFlight(true);
    player.getBukkit().setFlying(true);

    event
        .getMatch()
        .needModule(KitMatchModule.class)
        .getModule()
        .getKits()
        .forEach(kit -> kit.apply(player, true, new ArrayList<>()));

    player.getInventory().setArmorContents(new ItemStack[] {null, null, null, null});

    this.kits.computeIfAbsent(player.getBukkit(), p -> new CustomKit()).startEditing();
    player.getBukkit().openInventory(player.getInventory());

    for (Component message : Messages.loadoutEditorTutorial()) player.sendMessage(message);
  }

  @EventHandler
  public void onKitEdit(InventoryClickEvent event) {
    Player player = (Player) event.getWhoClicked();
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (!matchPlayer.isObserving()) return;

    CustomKit kit = this.kits.get(player);
    if (kit == null || !kit.editing) return;

    event.setCancelled(true);
    if (event.getClick() != ClickType.LEFT
        || event.getSlotType() == SlotType.OUTSIDE
        || event.getSlotType() == SlotType.ARMOR) return;

    if (kit.lastSlot == -1) {
      kit.lastSlot = event.getSlot();
      return;
    }

    int tmp = kit.slots[kit.lastSlot];
    kit.slots[kit.lastSlot] = event.getSlot();
    kit.slots[event.getSlot()] = tmp;

    ItemStack last = player.getInventory().getItem(kit.lastSlot);
    player.getInventory().setItem(kit.lastSlot, event.getCurrentItem());
    player.getInventory().setItem(event.getSlot(), last);

    kit.lastSlot = -1;
  }

  @EventHandler
  public void onFinishEditing(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (!matchPlayer.isObserving()) return;

    CustomKit kit = this.kits.get(player);
    if (kit == null || !kit.editing) return;
    kit.editing = false;

    player.getInventory().clear();
    player.setGameMode(GameMode.CREATIVE);
    Bukkit.getPluginManager().callEvent(new ObserverKitApplyEvent(matchPlayer));
  }

  @EventHandler
  public void onKitApply(ApplyItemKitEvent event) {
    CustomKit kit = kits.get(event.getPlayer().getBukkit());
    if (kit == null) return;

    Map<Slot, ItemStack> items = new HashMap<Slot, ItemStack>();
    for (Entry<Slot, ItemStack> entry : event.getSlotItems().entrySet()) {
      int targetSlot = entry.getKey().getIndex();
      inner:
      for (int i = 0; i < 36; i++) {
        if (kit.slots[i] == entry.getKey().getIndex()) {
          targetSlot = i;

          break inner;
        }
      }

      items.put(Slot.Player.forIndex(targetSlot), entry.getValue());
    }

    event.getSlotItems().clear();
    items.forEach(event.getSlotItems()::put);
  }

  @EventHandler
  public void onPlayerJoin(MatchPlayerAddEvent event) {
    kits.remove(event.getPlayer().getBukkit());
  }

  private static class CustomKit {

    private int[] slots = new int[36];

    private int lastSlot;
    private boolean editing;

    private CustomKit() {
      for (int i = 0; i < slots.length; i++) slots[i] = i;
    }

    public void startEditing() {
      this.lastSlot = -1;
      this.editing = true;
    }
  }
}
