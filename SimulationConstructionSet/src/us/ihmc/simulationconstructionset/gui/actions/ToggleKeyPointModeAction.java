package us.ihmc.simulationconstructionset.gui.actions;

import us.ihmc.simulationconstructionset.commands.ToggleKeyPointModeCommandExecutor;
import us.ihmc.simulationconstructionset.commands.ToggleKeyPointModeCommandListener;
import us.ihmc.simulationconstructionset.gui.SCSAction;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.net.URL;

@SuppressWarnings("serial")
public class ToggleKeyPointModeAction extends SCSAction implements ToggleKeyPointModeCommandListener
{
   private URL iconURL = ToggleKeyPointModeAction.class.getClassLoader().getResource("icons/toggleKey.gif");
   private ImageIcon icon = new ImageIcon(iconURL);
   private ToggleKeyPointModeCommandExecutor executor;

   public ToggleKeyPointModeAction(ToggleKeyPointModeCommandExecutor executor)
   {
      super("Toggle Key Mode",
              "icons/toggleKey.gif",
              KeyEvent.VK_F,
              "Short Description",
              "Long Description"
      );

      this.executor = executor;

      this.putValue(Action.SMALL_ICON, icon);

      executor.registerToggleKeyPointModeCommandListener(this);
   }

   @Override
   public void doAction()
   {
      executor.toggleKeyPointMode();
   }

   @Override
   public void updateKeyPointModeStatus()
   {
      if (executor.isKeyPointModeToggled())
      {
         iconURL = ToggleKeyPointModeAction.class.getClassLoader().getResource("icons/toggleKeyPressed.gif");
         ImageIcon tmp = new ImageIcon(iconURL);
         icon.setImage(tmp.getImage());
      }
      else
      {
         iconURL = ToggleKeyPointModeAction.class.getClassLoader().getResource("icons/toggleKey.gif");
         ImageIcon tmp = new ImageIcon(iconURL);
         icon.setImage(tmp.getImage());
      }
   }

   @Override
   public void closeAndDispose()
   {
      iconURL = null;
      icon = null;
      if (executor != null) executor.closeAndDispose();
      executor = null;
   }
}
