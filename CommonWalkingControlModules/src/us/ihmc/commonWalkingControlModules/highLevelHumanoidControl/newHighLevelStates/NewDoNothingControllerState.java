package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.newHighLevelStates;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreOutputReadOnly;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelJointControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.NewHighLevelControllerStates;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class NewDoNothingControllerState extends NewHighLevelControllerState
{
   private static final NewHighLevelControllerStates controllerState = NewHighLevelControllerStates.DO_NOTHING_STATE;

   private final HighLevelHumanoidControllerToolbox controllerToolbox;
   private final BipedSupportPolygons bipedSupportPolygons;
   private final SideDependentList<YoPlaneContactState> footContactStates = new SideDependentList<>();

   private final OneDoFJoint[] allRobotJoints;

   private final ControllerCoreCommand controllerCoreCommand = new ControllerCoreCommand(WholeBodyControllerCoreMode.OFF);
   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder;

   public NewDoNothingControllerState(HighLevelHumanoidControllerToolbox controllerToolbox)
   {
      super(controllerState);

      this.bipedSupportPolygons = controllerToolbox.getBipedSupportPolygons();
      this.controllerToolbox = controllerToolbox;
      allRobotJoints = controllerToolbox.getFullRobotModel().getOneDoFJoints();

      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody contactableFoot = controllerToolbox.getContactableFeet().get(robotSide);
         footContactStates.put(robotSide, controllerToolbox.getFootContactState(robotSide));
      }

      lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder(allRobotJoints.length);
      lowLevelOneDoFJointDesiredDataHolder.registerJointsWithEmptyData(allRobotJoints);
      lowLevelOneDoFJointDesiredDataHolder.setJointsControlMode(allRobotJoints, LowLevelJointControlMode.FORCE_CONTROL);
   }

   @Override
   public void setControllerCoreOutput(ControllerCoreOutputReadOnly controllerCoreOutput)
   {
   }

   @Override
   public void doAction()
   {
      bipedSupportPolygons.updateUsingContactStates(footContactStates);
      controllerToolbox.update();
      for (int i = 0; i < allRobotJoints.length; i++)
      {
         allRobotJoints[i].setTau(0.0);
         lowLevelOneDoFJointDesiredDataHolder.setDesiredJointTorque(allRobotJoints[i], 0.0);
      }
      controllerCoreCommand.completeLowLevelJointData(lowLevelOneDoFJointDesiredDataHolder);
   }

   @Override
   public void doTransitionIntoAction()
   {
      // Do nothing

   }

   @Override
   public void doTransitionOutOfAction()
   {
      // Do nothing

   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }

   @Override
   public ControllerCoreCommand getControllerCoreCommand()
   {
      return controllerCoreCommand;
   }
}
