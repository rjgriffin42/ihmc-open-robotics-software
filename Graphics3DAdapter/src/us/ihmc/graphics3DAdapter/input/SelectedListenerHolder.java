package us.ihmc.graphics3DAdapter.input;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.utilities.inputDevices.keyboard.ModifierKeyInterface;

public class SelectedListenerHolder
{
   private final ArrayList<SelectedListener> selectedListeners = new ArrayList<SelectedListener>();
   
   public void addSelectedListener(SelectedListener listener)
   {
      selectedListeners.add(listener);
   }

   public void selected(Graphics3DNode graphics3dNode, ModifierKeyInterface modifierKeyInterface, Point3d location, Point3d cameraLocation, Quat4d cameraRotation)
   {
      for(SelectedListener selectedListener : selectedListeners)
      {
         selectedListener.selected(graphics3dNode, modifierKeyInterface, location, cameraLocation, cameraRotation);
      }
   }
}
