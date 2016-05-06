package us.ihmc.atlas.logDataProcessing;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controlModules.foot.PartialFootholdControlModule;
import us.ihmc.darpaRoboticsChallenge.logProcessor.LogDataProcessorFunction;
import us.ihmc.darpaRoboticsChallenge.logProcessor.LogDataProcessorHelper;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FramePoint2dReadOnly;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class FootRotationProcessor implements LogDataProcessorFunction
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
   private final SideDependentList<PartialFootholdControlModule> partialFootholdControlModules = new SideDependentList<>();

   private final LogDataProcessorHelper logDataProcessorHelper;

   public FootRotationProcessor(LogDataProcessorHelper logDataProcessorHelper)
   {
      this.logDataProcessorHelper = logDataProcessorHelper;
      SideDependentList<? extends ContactablePlaneBody> contactableFeet = logDataProcessorHelper.getContactableFeet();
      double controllerDT = logDataProcessorHelper.getControllerDT();
      TwistCalculator twistCalculator = logDataProcessorHelper.getTwistCalculator();
      WalkingControllerParameters walkingControllerParameters = logDataProcessorHelper.getWalkingControllerParameters();
      FramePoint2dReadOnly capturePoint = logDataProcessorHelper.getCapturePoint();

      for (RobotSide robotSide : RobotSide.values)
      {
         String namePrefix = contactableFeet.get(robotSide).getName();
         PartialFootholdControlModule partialFootholdControlModule = new PartialFootholdControlModule(namePrefix, controllerDT, contactableFeet.get(robotSide),
               twistCalculator, walkingControllerParameters, capturePoint, registry, yoGraphicsListRegistry);
         partialFootholdControlModules.put(robotSide, partialFootholdControlModule);
      }
   }

   private final FramePoint2d measuredCoP2d = new FramePoint2d();
   private final FramePoint2d desiredCoP2d = new FramePoint2d();

   @Override
   public void processDataAtControllerRate()
   {
      logDataProcessorHelper.update();

      for (RobotSide robotSide : RobotSide.values)
      {
         if (logDataProcessorHelper.getCurrenFootState(robotSide) == ConstraintType.FULL
               || logDataProcessorHelper.getCurrenFootState(robotSide) == ConstraintType.EXPLORE_POLYGON
               || logDataProcessorHelper.getCurrenFootState(robotSide) == ConstraintType.HOLD_POSITION)
         {
            logDataProcessorHelper.getMeasuredCoP(robotSide, measuredCoP2d);
            logDataProcessorHelper.getDesiredCoP(robotSide, desiredCoP2d);
            partialFootholdControlModules.get(robotSide).compute(desiredCoP2d, measuredCoP2d);
         }
         else
         {
            partialFootholdControlModules.get(robotSide).reset();
         }
      }
   }

   @Override
   public void processDataAtStateEstimatorRate()
   {
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public YoGraphicsListRegistry getYoGraphicsListRegistry()
   {
      return yoGraphicsListRegistry;
   }
}
