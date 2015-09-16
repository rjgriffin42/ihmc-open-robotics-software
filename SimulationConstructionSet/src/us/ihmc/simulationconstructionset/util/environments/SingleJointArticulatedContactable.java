package us.ihmc.simulationconstructionset.util.environments;

import java.util.ArrayList;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.ground.Contactable;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public abstract class SingleJointArticulatedContactable implements Contactable
{
   private final String name;
   private final Robot robot;
   
   private final ArrayList<GroundContactPoint> allGroundContactPoints = new ArrayList<GroundContactPoint>();
   private final ArrayList<BooleanYoVariable> contactsAvailable = new ArrayList<BooleanYoVariable>();

   public SingleJointArticulatedContactable(String name, Robot robot)
   {
      this.name = name;
      this.robot = robot;
   }

   public abstract Joint getJoint();

   public void createAvailableContactPoints(int groupIdentifier, int totalContactPointsAvailable, double forceVectorScale, boolean addDynamicGraphicForceVectorsForceVectors)
   {
      YoGraphicsListRegistry yoGraphicsListRegistry = null;
      if (addDynamicGraphicForceVectorsForceVectors) yoGraphicsListRegistry = new YoGraphicsListRegistry();

      for (int i = 0; i < totalContactPointsAvailable; i++)
      {
         GroundContactPoint contactPoint = new GroundContactPoint("contact_" + name + "_" + i, robot.getRobotsYoVariableRegistry());
         getJoint().addGroundContactPoint(groupIdentifier, contactPoint);
         allGroundContactPoints.add(contactPoint);

         BooleanYoVariable contactAvailable = new BooleanYoVariable("contact_" + name + "_" + i + "_avail", robot.getRobotsYoVariableRegistry());
         contactAvailable.set(true);
         contactsAvailable.add(contactAvailable);

         if (addDynamicGraphicForceVectorsForceVectors)
         {
            YoGraphicPosition dynamicGraphicPosition = new YoGraphicPosition(name + "Point" + i, contactPoint.getYoPosition(), 0.02, YoAppearance.Green());
            YoGraphicVector dynamicGraphicVector = new YoGraphicVector(name + "Force" + i, contactPoint.getYoPosition(), contactPoint.getYoForce(), forceVectorScale, YoAppearance.Green());
            yoGraphicsListRegistry.registerYoGraphic(name, dynamicGraphicPosition);
            yoGraphicsListRegistry.registerYoGraphic(name, dynamicGraphicVector);
         }
      }

      if (addDynamicGraphicForceVectorsForceVectors)
      {
         robot.addDynamicGraphicObjectsListRegistry(yoGraphicsListRegistry);
      }
   }

   public int getAndLockAvailableContactPoint()
   {
      for (int i = 0; i < allGroundContactPoints.size(); i++)
      {
         BooleanYoVariable contactAvailable = contactsAvailable.get(i);

         if (contactAvailable.getBooleanValue())
         {
            contactAvailable.set(false);

            return i;
         }
      }

      throw new RuntimeException("No contact points are available");
   }

   public void unlockContactPoint(GroundContactPoint groundContactPoint)
   {
      for (int i = 0; i < allGroundContactPoints.size(); i++)
      {
         if (groundContactPoint == allGroundContactPoints.get(i))
         {
            BooleanYoVariable contactAvailable = contactsAvailable.get(i);
            if (!contactAvailable.getBooleanValue())
            {
               contactAvailable.set(true);

               return;
            }
            else
            {
               throw new RuntimeException("Returning a contact point that is already available!");
            }
         }
      }
   }

   public GroundContactPoint getLockedContactPoint(int contactPointIndex)
   {
      if (contactsAvailable.get(contactPointIndex).getBooleanValue())
      {
         throw new RuntimeException("Trying to get a contact point that isn't checked out!");
      }

      return allGroundContactPoints.get(contactPointIndex);
   }

   public void updateContactPoints()
   {
      robot.update();
      robot.updateVelocities();
   }

}
