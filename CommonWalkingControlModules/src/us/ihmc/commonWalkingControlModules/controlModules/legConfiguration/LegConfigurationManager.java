package us.ihmc.commonWalkingControlModules.controlModules.legConfiguration;

import us.ihmc.commonWalkingControlModules.configurations.StraightLegWalkingParameters;
import us.ihmc.commonWalkingControlModules.controlModules.legConfiguration.LegConfigurationControlModule.LegConfigurationType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

public class LegConfigurationManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final BooleanYoVariable attemptToStraightenLegs = new BooleanYoVariable("attemptToStraightenLegs", registry);

   private final SideDependentList<LegConfigurationControlModule> legConfigurationControlModules = new SideDependentList<>();

   public LegConfigurationManager(HighLevelHumanoidControllerToolbox controllerToolbox, StraightLegWalkingParameters straightLegWalkingParameters,
                                  YoVariableRegistry parentRegistry)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         legConfigurationControlModules.put(robotSide, new LegConfigurationControlModule(robotSide, controllerToolbox, straightLegWalkingParameters, registry));
      }

      attemptToStraightenLegs.set(straightLegWalkingParameters.attemptToStraightenLegs());

      parentRegistry.addChild(registry);
   }

   public void initialize()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         legConfigurationControlModules.get(robotSide).initialize();
      }
   }

   public void compute()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         legConfigurationControlModules.get(robotSide).doControl();
      }
   }

   public void startSwing(RobotSide upcomingSwingSide)
   {
      if (attemptToStraightenLegs.getBooleanValue())
      {
         legConfigurationControlModules.get(upcomingSwingSide).setKneeAngleState(LegConfigurationType.BENT);
      }
   }

   public boolean isLegCollapsed(RobotSide robotSide)
   {
      LegConfigurationType legConfigurationControlState = legConfigurationControlModules.get(robotSide).getCurrentKneeControlState();
      return (legConfigurationControlState.equals(LegConfigurationType.BENT) || legConfigurationControlState.equals(LegConfigurationType.COLLAPSE));
   }

   public void collapseLegDuringTransfer(RobotSide transferSide)
   {

      if (attemptToStraightenLegs.getBooleanValue())
      {
         legConfigurationControlModules.get(transferSide.getOppositeSide()).setKneeAngleState(LegConfigurationType.BENT);
      }
   }

   public void collapseLegDuringSwing(RobotSide supportSide)
   {
      if (attemptToStraightenLegs.getBooleanValue())
      {
         legConfigurationControlModules.get(supportSide).setKneeAngleState(LegConfigurationType.COLLAPSE);
      }
   }

   public void straightenLegDuringSwing(RobotSide swingSide)
   {
      if (legConfigurationControlModules.get(swingSide).getCurrentKneeControlState() != LegConfigurationType.STRAIGHTEN_TO_STRAIGHT &&
            legConfigurationControlModules.get(swingSide).getCurrentKneeControlState() != LegConfigurationType.STRAIGHT)
      {
         //beginStraightening(swingSide);
         setStraight(swingSide);
      }
   }

   public void setStraight(RobotSide robotSide)
   {
      if (attemptToStraightenLegs.getBooleanValue())
      {
         legConfigurationControlModules.get(robotSide).setKneeAngleState(LegConfigurationType.STRAIGHT);
      }
   }

   public void beginStraightening(RobotSide robotSide)
   {
      if (attemptToStraightenLegs.getBooleanValue())
      {
         legConfigurationControlModules.get(robotSide).setKneeAngleState(LegConfigurationType.STRAIGHTEN_TO_STRAIGHT);
      }
   }

   public FeedbackControlCommand<?> getFeedbackControlCommand(RobotSide robotSide)
   {
      //return footControlModules.get(robotSide).getFeedbackControlCommand();
      return null;
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand(RobotSide robotSide)
   {
      return legConfigurationControlModules.get(robotSide).getInverseDynamicsCommand();
   }
}
