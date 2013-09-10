package us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects;

import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Wrench;

public class MomentumModuleDataObject
{   
   // Desired commands:
   private final ArrayList<DesiredRateOfChangeOfMomentumCommand> desiredRateOfChangeOfMomentumCommands = new ArrayList<DesiredRateOfChangeOfMomentumCommand>();

   private final ArrayList<DesiredJointAccelerationCommand> desiredJointAccelerationCommands = new ArrayList<DesiredJointAccelerationCommand>();
   private final ArrayList<DesiredSpatialAccelerationCommand> desiredSpatialAccelerationCommands = new ArrayList<DesiredSpatialAccelerationCommand>();
   private final ArrayList<DesiredPointAccelerationCommand> desiredPointAccelerationCommands = new ArrayList<DesiredPointAccelerationCommand>();
   
   // External Wrenches to compensate for:
   private final ArrayList<ExternalWrenchCommand> externalWrenchCommands = new ArrayList<ExternalWrenchCommand>();

   // Contact states:
//   private final ArrayList<PlaneContactStateCommand> planeContactStateCommands = new ArrayList<PlaneContactStateCommand>();
//   private final ArrayList<RollingContactStateCommand> rollingContactStateCommands = new ArrayList<RollingContactStateCommand>();
//   private final ArrayList<CylindricalContactInContactCommand> cylindricalContactInContactCommands = new ArrayList<CylindricalContactInContactCommand>();
   

   
   public MomentumModuleDataObject()
   {
      
   }
   
   public void reset()
   {
      desiredRateOfChangeOfMomentumCommands.clear();
      desiredJointAccelerationCommands.clear();
      desiredSpatialAccelerationCommands.clear();
      desiredPointAccelerationCommands.clear();
      
      externalWrenchCommands.clear();
   }


   public void setDesiredRateOfChangeOfMomentum(DesiredRateOfChangeOfMomentumCommand desiredRateOfChangeOfMomentumCommand)
   {
      DesiredRateOfChangeOfMomentumCommand command = new DesiredRateOfChangeOfMomentumCommand(desiredRateOfChangeOfMomentumCommand);
      desiredRateOfChangeOfMomentumCommands.add(command);
   }

   public void setDesiredJointAcceleration(DesiredJointAccelerationCommand desiredJointAccelerationCommand)
   {
      DesiredJointAccelerationCommand command = new DesiredJointAccelerationCommand(desiredJointAccelerationCommand);
      desiredJointAccelerationCommands.add(command);
   }

   public void setDesiredSpatialAcceleration(DesiredSpatialAccelerationCommand desiredSpatialAccelerationCommand)
   {
      DesiredSpatialAccelerationCommand command = new DesiredSpatialAccelerationCommand(desiredSpatialAccelerationCommand);
      desiredSpatialAccelerationCommands.add(command);
   }
   

   public void setDesiredPointAcceleration(DesiredPointAccelerationCommand desiredPointAccelerationCommand)
   {
      DesiredPointAccelerationCommand command = new DesiredPointAccelerationCommand(desiredPointAccelerationCommand);
      desiredPointAccelerationCommands.add(command);
   }
   
   public void setExternalWrenchToCompensateFor(RigidBody rigidBody, Wrench wrench)
   {
      ExternalWrenchCommand command = new ExternalWrenchCommand(rigidBody, wrench);
      externalWrenchCommands.add(command);
   }

   public ArrayList<DesiredRateOfChangeOfMomentumCommand> getDesiredRateOfChangeOfMomentumCommands()
   {
      return desiredRateOfChangeOfMomentumCommands;
   }

   public ArrayList<DesiredJointAccelerationCommand> getDesiredJointAccelerationCommands()
   {
      return desiredJointAccelerationCommands;
   }

   public ArrayList<DesiredSpatialAccelerationCommand> getDesiredSpatialAccelerationCommands()
   {
      return desiredSpatialAccelerationCommands;
   }

   public ArrayList<DesiredPointAccelerationCommand> getDesiredPointAccelerationCommands()
   {
      return desiredPointAccelerationCommands;
   }

   public ArrayList<ExternalWrenchCommand> getExternalWrenchCommands()
   {
      return externalWrenchCommands;
   }

  
   
}
