package us.ihmc.simulationconstructionset.util.perturbance;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

//Used to make ground contact points slip a delta.
public class GroundContactPointsSlipper implements RobotController
{
   private final YoVariableRegistry registry;

   private final ArrayList<GroundContactPoint> groundContactPointsToSlip;
   private final YoFrameVector slipAmount;
   private final YoFrameOrientation slipRotation;

   private final DoubleYoVariable percentToSlipPerTick;
   private final BooleanYoVariable doSlip;

   
   public GroundContactPointsSlipper(String registryPrefix)
   {
      registry = new YoVariableRegistry(registryPrefix + getClass().getSimpleName());

      groundContactPointsToSlip = new ArrayList<GroundContactPoint>();
      slipAmount = new YoFrameVector("slipAmount", ReferenceFrame.getWorldFrame(), registry);
      slipRotation = new YoFrameOrientation("slipRotation", ReferenceFrame.getWorldFrame(), registry);

      percentToSlipPerTick = new DoubleYoVariable("percentToSlipPerTick", registry);
      doSlip = new BooleanYoVariable("doSlip", registry);
   }

   public void addGroundContactPoints(List<GroundContactPoint> footGroundContactPoints)
   {
      for (GroundContactPoint groundContactPoint : footGroundContactPoints)
      {
         addGroundContactPoint(groundContactPoint);
      }
   }

   public void setGroundContactPoints(List<GroundContactPoint> footGroundContactPoints)
   {
      this.groundContactPointsToSlip.clear();

      for (GroundContactPoint groundContactPoint : footGroundContactPoints)
      {
         addGroundContactPoint(groundContactPoint);
      }
   }

   public void addGroundContactPoint(GroundContactPoint groundContactPoint)
   {
      this.groundContactPointsToSlip.add(groundContactPoint);
   }

   public void addGroundContactPoint(Robot robot, String groundContactPointName)
   {
      ArrayList<GroundContactPoint> allGroundContactPoints = robot.getAllGroundContactPoints();

      for (GroundContactPoint groundContactPoint : allGroundContactPoints)
      {
         if (groundContactPoint.getName().equals(groundContactPointName))
         {
            this.addGroundContactPoint(groundContactPoint);

            return;
         }
      }
   }

   public void setDoSlip(boolean doSlip)
   {
      this.doSlip.set(doSlip);
   }

   public void setPercentToSlipPerTick(double percentToSlipPerTick)
   {
      this.percentToSlipPerTick.set(percentToSlipPerTick);
   }

   public void setSlipTranslation(Vector3d slipAmount)
   {
      this.slipAmount.set(slipAmount);
   }
   
   public void setSlipRotationYawPitchRoll(double[] yawPitchRoll) 
   {
      this.slipRotation.setYawPitchRoll(yawPitchRoll[0], yawPitchRoll[1], yawPitchRoll[2]);
   }
   
   public void setSlipRotationYawPitchRoll(double yaw, double pitch, double roll)
   {
      this.slipRotation.setYawPitchRoll(yaw, pitch, roll);
   }
   
   public void setSlipRotationEulerAngles(Vector3d eulerAngles)
   {
      this.slipRotation.setEulerAngles(eulerAngles);
   }
   
   public boolean isDoneSlipping()
   {
      boolean translationalSlipDone = slipAmount.lengthSquared() < 0.0001 * 0.0001;
      
      Vector3d eulerAngles = new Vector3d();
      slipRotation.getEulerAngles(eulerAngles);
      boolean rotationalSlipDone = eulerAngles.lengthSquared() < 0.001 * 0.001;
      
      return translationalSlipDone & rotationalSlipDone;
   }

   public void slipALittle(double percentOfDelta)
   {
      if (percentOfDelta < 0.0)
         return;
      if (percentOfDelta > 1.0)
         return;

      applyTranslationalSlip(percentOfDelta);
      applyRotationalSlip(percentOfDelta);
   }
   
   private void applyTranslationalSlip(double percentOfDelta) 
   {
      FrameVector slipDelta = slipAmount.getFrameVectorCopy();
      slipDelta.scale(percentOfDelta);
      slipAmount.sub(slipDelta);

      Point3d touchdownLocation = new Point3d();

      for (int i = 0; i < groundContactPointsToSlip.size(); i++)
      {
         GroundContactPoint groundContactPointToSlip = groundContactPointsToSlip.get(i);

         boolean touchedDown = (groundContactPointToSlip.isInContact());

         if (touchedDown)
         {
            groundContactPointToSlip.getTouchdownLocation(touchdownLocation);
            touchdownLocation.add(slipDelta.getVectorCopy());
            groundContactPointToSlip.setTouchdownLocation(touchdownLocation);
         }
      }
   }
   
   private void applyRotationalSlip(double percentOfDelta)
   {
      FrameOrientation identity = new FrameOrientation(ReferenceFrame.getWorldFrame());
      FrameOrientation desired = slipRotation.getFrameOrientationCopy();
      FrameOrientation delta = new FrameOrientation();

      delta.interpolate(identity, desired, percentOfDelta);
      
      desired.interpolate(identity, desired, 1.0-percentOfDelta);
      slipRotation.set(desired);

      Point3d touchdownCoM = computeTouchdownCoM();
      Matrix3d deltaRotation = delta.getMatrix3dCopy();

      Point3d touchdownLocation = new Point3d();

      for (int i = 0; i < groundContactPointsToSlip.size(); i++)
      {
         GroundContactPoint groundContactPointToSlip = groundContactPointsToSlip.get(i);

         boolean touchedDown = (groundContactPointToSlip.isInContact());

         if (touchedDown)
         {
            groundContactPointToSlip.getTouchdownLocation(touchdownLocation);
            touchdownLocation.sub(touchdownCoM);
            deltaRotation.transform(touchdownLocation);
            touchdownLocation.add(touchdownCoM);
            groundContactPointToSlip.setTouchdownLocation(touchdownLocation);
         }
      }
   }

   private Point3d computeTouchdownCoM()
   {
      int touchdownCount = 0;
      Point3d touchdownCoM = new Point3d();
      Point3d touchdownLocation = new Point3d();

      for (int i = 0; i < groundContactPointsToSlip.size(); i++)
      {
         GroundContactPoint groundContactPointToSlip = groundContactPointsToSlip.get(i);

         boolean touchedDown = (groundContactPointToSlip.isInContact());
         if (touchedDown)
         {
            groundContactPointToSlip.getTouchdownLocation(touchdownLocation);
            touchdownCoM.add(touchdownLocation);
            touchdownCount++;
         }
      }

      touchdownCoM.scale(1.0/touchdownCount);
      return touchdownCoM;
   }

   public void initialize()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return registry.getName();
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      if (doSlip.getBooleanValue())
      {
         slipALittle(percentToSlipPerTick.getDoubleValue());
      }
   }


}
