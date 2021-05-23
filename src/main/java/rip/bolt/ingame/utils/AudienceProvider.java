package rip.bolt.ingame.utils;

import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.lib.app.ashcon.intake.argument.CommandArgs;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import tc.oc.pgm.util.Audience;

public final class AudienceProvider implements BukkitProvider<Audience> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public Audience get(CommandSender sender, CommandArgs args, List<? extends Annotation> list) {
    return Audience.get(sender);
  }
}
