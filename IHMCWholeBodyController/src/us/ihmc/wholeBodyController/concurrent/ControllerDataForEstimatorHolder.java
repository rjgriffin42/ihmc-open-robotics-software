package us.ihmc.wholeBodyController.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import javax.vecmath.Point2d;

import us.ihmc.robotics.humanoidRobot.RobotMotionStatus;
import us.ihmc.robotics.humanoidRobot.model.CenterOfPressureDataHolder;
import us.ihmc.robotics.humanoidRobot.model.DesiredJointDataHolder;
import us.ihmc.robotics.humanoidRobot.model.IntermediateDesiredJointDataHolder;
import us.ihmc.robotics.humanoidRobot.model.RobotMotionStatusHolder;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;

public class ControllerDataForEstimatorHolder
{
   // Do not use FramePoint here, as ReferenceFrames are not shared between controller/estimator
   private final SideDependentList<Point2d> centerOfPressure = new SideDependentList<>();
   private AtomicReference<RobotMotionStatus> robotMotionStatus = new AtomicReference<RobotMotionStatus>(null);

   private final CenterOfPressureDataHolder controllerCenterOfPressureDataHolder;
   private final CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder;

   private final RobotMotionStatusHolder controllerRobotMotionStatusHolder;
   private final RobotMotionStatusHolder estimatorRobotMotionStatusHolder;

   private final IntermediateDesiredJointDataHolder intermediateDesiredJointDataHolder;

   public ControllerDataForEstimatorHolder(CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder,
         CenterOfPressureDataHolder controllerCenterOfPressureDataHolder, RobotMotionStatusHolder estimatorRobotMotionStatusHolder,
         RobotMotionStatusHolder controllerRobotMotionStatusHolder, DesiredJointDataHolder estimatorJointDataHolder,
         DesiredJointDataHolder controllerJointDataHolder)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         centerOfPressure.put(robotSide, new Point2d());
      }

      this.estimatorCenterOfPressureDataHolder = estimatorCenterOfPressureDataHolder;
      this.controllerCenterOfPressureDataHolder = controllerCenterOfPressureDataHolder;

      this.estimatorRobotMotionStatusHolder = estimatorRobotMotionStatusHolder;
      this.controllerRobotMotionStatusHolder = controllerRobotMotionStatusHolder;

      this.intermediateDesiredJointDataHolder = new IntermediateDesiredJointDataHolder(estimatorJointDataHolder, controllerJointDataHolder);
   }

   public void readControllerDataIntoEstimator()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         estimatorCenterOfPressureDataHolder.setCenterOfPressure(centerOfPressure.get(robotSide), robotSide);
      }

      if (robotMotionStatus.get() != null)
         estimatorRobotMotionStatusHolder.setCurrentRobotMotionStatus(robotMotionStatus.getAndSet(null));
      
      intermediateDesiredJointDataHolder.readIntoEstimator();
   }

   public void writeControllerDataFromController()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         controllerCenterOfPressureDataHolder.getCenterOfPressure(centerOfPressure.get(robotSide), robotSide);
      }

      robotMotionStatus.set(controllerRobotMotionStatusHolder.getCurrentRobotMotionStatus());

      intermediateDesiredJointDataHolder.copyFromController();
   }

   public static class Builder implements us.ihmc.concurrent.Builder<ControllerDataForEstimatorHolder>
   {
      private final CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder;
      private final CenterOfPressureDataHolder controllerCenterOfPressureDataHolder;

      private final RobotMotionStatusHolder estimatorRobotMotionStatusHolder;
      private final RobotMotionStatusHolder controllerRobotMotionStatusHolder;

      private final DesiredJointDataHolder estimatorDesiredJointDataHolder;
      private final DesiredJointDataHolder controllerDesiredJointDataHolder;

      public Builder(CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder, CenterOfPressureDataHolder controllerCenterOfPressureDataHolder,
            RobotMotionStatusHolder estimatorRobotMotionStatusHolder, RobotMotionStatusHolder controllerRobotMotionStatusHolder,
            DesiredJointDataHolder estimatorDesiredJointDataHolder, DesiredJointDataHolder controllerDesiredJointDataHolder)
      {
         this.estimatorCenterOfPressureDataHolder = estimatorCenterOfPressureDataHolder;
         this.controllerCenterOfPressureDataHolder = controllerCenterOfPressureDataHolder;

         this.estimatorRobotMotionStatusHolder = estimatorRobotMotionStatusHolder;
         this.controllerRobotMotionStatusHolder = controllerRobotMotionStatusHolder;

         this.estimatorDesiredJointDataHolder = estimatorDesiredJointDataHolder;
         this.controllerDesiredJointDataHolder = controllerDesiredJointDataHolder;
      }

      @Override
      public ControllerDataForEstimatorHolder newInstance()
      {
         return new ControllerDataForEstimatorHolder(estimatorCenterOfPressureDataHolder, controllerCenterOfPressureDataHolder,
               estimatorRobotMotionStatusHolder, controllerRobotMotionStatusHolder, estimatorDesiredJointDataHolder, controllerDesiredJointDataHolder);
      }

   }
}
