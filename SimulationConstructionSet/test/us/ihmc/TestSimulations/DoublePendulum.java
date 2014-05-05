package us.ihmc.TestSimulations;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.Axis;

import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.PinJoint;
import com.yobotics.simulationconstructionset.Robot;

public class DoublePendulum extends Robot
{
   private final PinJoint j1;
   private final PinJoint j2;

   public DoublePendulum()
   {
      super("DoublePendulum");

      j1 = new PinJoint("j1", new Vector3d(0, 0, 2), this, Axis.X);

      Link l1 = new Link("l1");
      l1.physics.setComOffset(0, 0, 0.5);
      l1.setMassAndRadiiOfGyration(1.0, 0.05, 0.05, 0.3);
      l1.addEllipsoidFromMassProperties(YoAppearance.Pink());
      j1.setLink(l1);

      j2 = new PinJoint("j2", new Vector3d(0.0, 0.0, 1.0), this, Axis.X);
      Link l2 = new Link("l2");
      l2.physics.setComOffset(0, 0, 0.5);
      l2.setMassAndRadiiOfGyration(1.0, 0.05, 0.05, 0.3);
      l2.addEllipsoidFromMassProperties(YoAppearance.Purple());

      j2.setLink(l2);

      j1.addJoint(j2);
      addRootJoint(j1);

   }

   public PinJoint getJ1()
   {
      return j1;
   }

   public PinJoint getJ2()
   {
      return j2;
   }
}
