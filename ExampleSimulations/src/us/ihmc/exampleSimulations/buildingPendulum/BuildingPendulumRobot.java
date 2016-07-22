package us.ihmc.exampleSimulations.buildingPendulum;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.Axis;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.NullJoint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.Robot;

public class BuildingPendulumRobot extends Robot
{

   public static final double mass = 181.0;

   public static final double length = 7.6;
   public static final double distance = 1.0;

   private static final double midAngle = Math.atan2(distance/2.0, length);

   private final SideDependentList<PinJoint> joints = new SideDependentList<>();

   public BuildingPendulumRobot()
   {
      super("BuildingPendulumRobot");

      NullJoint rootJoint = new NullJoint("CeilingJoint", new Vector3d(), this);



      Link ceiling = new Link("link1");
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCube(5, 5, 0.1);

      ceiling.setLinkGraphics(linkGraphics);
      rootJoint.setLink(ceiling);

      PinJoint pendulumJoint1 = new PinJoint("jointLeft", new Vector3d(-distance/2.0, 0.0, 0.0), this, Axis.Y);
      PinJoint pendulumJoint2 = new PinJoint("jointRight", new Vector3d(distance/2.0, 0.0, 0.0), this, Axis.Y);

      pendulumJoint1.setLink(createLink("pendulum1"));
      rootJoint.addJoint(pendulumJoint1);
      pendulumJoint2.setLink(createLink("pendulum2"));
      rootJoint.addJoint(pendulumJoint2);


      joints.put(RobotSide.LEFT, pendulumJoint1);
      joints.put(RobotSide.RIGHT, pendulumJoint2);

      double startingAngle = ((distance/2)+1)/length;
      double startl =getSwitchAngle(RobotSide.LEFT) - startingAngle;


      pendulumJoint1.setInitialState(startl, 0.0);

      pendulumJoint2.setInitialState(0.0, 0.0);
      this.addRootJoint(rootJoint);
   }

   private Link createLink(String name)
   {
      Link ret = new Link(name);
      ret.setMass(mass);
      ret.setComOffset(0.0, 0.0, length);
      double ixx = mass/3.0 * (length*length);
      double iyy = ixx;
      double izz = 0.0;
      ret.setMomentOfInertia(ixx, iyy, izz);
      // create a LinkGraphics object to manipulate the visual representation of the link
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(length, 0.05, YoAppearance.Red());

      // associate the linkGraphics object with the link object
      ret.setLinkGraphics(linkGraphics);
      return ret;
   }
   public double getPendulumAngle(RobotSide activeSide)
   {
      PinJoint joint = joints.get(activeSide);
      return joint.getQ().getDoubleValue();
   }

   public void setPendulumAngle(RobotSide activeSide, double q)
   {
      PinJoint joint = joints.get(activeSide);
      joint.setQ(q);
   }

   public void setPendulumVelocity(RobotSide activeSide, double qd)
   {
      PinJoint joint = joints.get(activeSide);
      joint.setQd(qd);
   }

   public double getPendulumVelocity(RobotSide activeSide)
   {
      PinJoint joint = joints.get(activeSide);
       return joint.getQD().getDoubleValue();
   }
   public double getSwitchAngle(RobotSide activeSide)
   {
      if (activeSide == RobotSide.LEFT)
         return - midAngle + Math.PI;
      else
         return midAngle + Math.PI;
   }
}
