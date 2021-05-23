package rip.bolt.ingame.utils;

import org.bukkit.command.CommandSender;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.pugs.PugManager;
import tc.oc.pgm.lib.app.ashcon.intake.util.auth.AuthorizationException;

public class CommandsUtil {
  public static void checkPermissionsRanked(
      CommandSender sender, String node, GameManager gameManager) throws AuthorizationException {
    if (!(gameManager instanceof PugManager) && !sender.hasPermission(node))
      throw new AuthorizationException();
  }
}
