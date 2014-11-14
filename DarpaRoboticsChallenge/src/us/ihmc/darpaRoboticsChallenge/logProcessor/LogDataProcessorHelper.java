package us.ihmc.darpaRoboticsChallenge.logProcessor;

import us.ihmc.SdfLoader.SDFPerfectSimulatedSensorReader;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.momentumBasedController.PlaneContactWrenchProcessor;
import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.math.frames.YoFramePoint2d;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class LogDataProcessorHelper
{
   private final FullRobotModel fullRobotModel;
   private final ReferenceFrames referenceFrames;
   private final SDFPerfectSimulatedSensorReader sensorReader;
   private final TwistCalculator twistCalculator;
   public WalkingControllerParameters getWalkingControllerParameters()
   {
      return walkingControllerParameters;
   }

   private final SideDependentList<ContactablePlaneBody> contactableFeet;

   private final SideDependentList<YoFramePoint2d> cops = new SideDependentList<>();
   private final SideDependentList<YoFramePoint2d> desiredCoPs = new SideDependentList<>();
   private final SideDependentList<EnumYoVariable<?>> footStates = new SideDependentList<>();

   private final double controllerDT;
   private final WalkingControllerParameters walkingControllerParameters;

   public LogDataProcessorHelper(DRCRobotModel model, SimulationConstructionSet scs, SDFRobot sdfRobot)
   {
      fullRobotModel = model.createFullRobotModel();
      referenceFrames = new ReferenceFrames(fullRobotModel);
      sensorReader = new SDFPerfectSimulatedSensorReader(sdfRobot, fullRobotModel, referenceFrames);
      twistCalculator = new TwistCalculator(fullRobotModel.getElevatorFrame(), fullRobotModel.getElevator());

      controllerDT = model.getControllerDT();
      this.walkingControllerParameters = model.getWalkingControllerParameters();

      ContactableBodiesFactory contactableBodiesFactory = model.getContactPointParameters().getContactableBodiesFactory();
      contactableFeet = contactableBodiesFactory.createFootContactableBodies(fullRobotModel, referenceFrames);

      for (RobotSide robotSide : RobotSide.values)
      {
         ReferenceFrame soleFrame = referenceFrames.getSoleFrame(robotSide);

         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         String bodyName = contactableFeet.get(robotSide).getName();
         String copNamePrefix = bodyName + "StateEstimator";
         String copNameSpace = copNamePrefix + WrenchBasedFootSwitch.class.getSimpleName();
         String copName = copNamePrefix + "ResolvedCoP";
         DoubleYoVariable copx = (DoubleYoVariable) scs.getVariable(copNameSpace, copName + "X");
         DoubleYoVariable copy = (DoubleYoVariable) scs.getVariable(copNameSpace, copName + "Y");
         YoFramePoint2d cop = new YoFramePoint2d(copx, copy, soleFrame);
         cops.put(robotSide, cop);

         String desiredCoPNameSpace = PlaneContactWrenchProcessor.class.getSimpleName();
         String desiredCoPName = side + "SoleCoP2d";
         DoubleYoVariable desiredCoPx = (DoubleYoVariable) scs.getVariable(desiredCoPNameSpace, desiredCoPName + "X");
         DoubleYoVariable desiredCoPy = (DoubleYoVariable) scs.getVariable(desiredCoPNameSpace, desiredCoPName + "Y");
         YoFramePoint2d desiredCoP = new YoFramePoint2d(desiredCoPx, desiredCoPy, soleFrame);
         desiredCoPs.put(robotSide, desiredCoP);

         String namePrefix = robotSide.getShortLowerCaseName() + "_foot";
         String footStateNameSpace = "DRCControllerThread.DRCMomentumBasedController.HighLevelHumanoidControllerManager.MomentumBasedControllerFactory.FeetManager."
               + namePrefix + "FootControlModule";
         String footStateName = namePrefix + "State";
         @SuppressWarnings("unchecked")
         EnumYoVariable<?> footState = (EnumYoVariable<ConstraintType>) scs.getVariable(footStateNameSpace, footStateName);
         footStates.put(robotSide, footState);
      }
   }

   public void update()
   {
      sensorReader.read();
      twistCalculator.compute();
   }

   public FullRobotModel getFullRobotModel()
   {
      return fullRobotModel;
   }

   public ReferenceFrames getReferenceFrames()
   {
      return referenceFrames;
   }

   public TwistCalculator getTwistCalculator()
   {
      return twistCalculator;
   }

   public SideDependentList<ContactablePlaneBody> getContactableFeet()
   {
      return contactableFeet;
   }

   public ConstraintType getCurrenFootState(RobotSide robotSide)
   {
      return ConstraintType.valueOf(footStates.get(robotSide).getEnumValue().toString());
   }

   public void getMeasuredCoP(RobotSide robotSide, FramePoint2d copToPack)
   {
      cops.get(robotSide).getFrameTuple2dIncludingFrame(copToPack);
   }

   public void getDesiredCoP(RobotSide robotSide, FramePoint2d desiredCoPToPack)
   {
      desiredCoPs.get(robotSide).getFrameTuple2dIncludingFrame(desiredCoPToPack);
   }

   public double getControllerDT()
   {
      return controllerDT;
   }
}
