package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.SmoothICPComputer2D;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

public class CapturePointPlannerAdapter implements InstantaneousCapturePointPlanner
{
	ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
	boolean USE_OLD_HACKY_ICP_PLANNER = false;
	private final ArrayList<ReferenceFrame> soleFrameList = new ArrayList<ReferenceFrame>();
	private final ArrayList<FramePoint> footstepList = new ArrayList<FramePoint>();
	private final FramePoint transferToFootLocation = new FramePoint(worldFrame);
	private final EnumYoVariable<RobotSide> currentTransferToSide;
	private final CapturePointPlannerParameters capturePointPlannerParameters;
	private final FramePoint tmpFramePoint = new FramePoint(worldFrame);
	private final FramePoint tmpFramePoint2 = new FramePoint(worldFrame);
	private final FramePoint2d tmpFramePoint2d = new FramePoint2d(worldFrame);
	private final FrameVector tmpFrameVector = new FrameVector(worldFrame);
	private final CommonHumanoidReferenceFrames referenceFrames;

	InstantaneousCapturePointPlanner oldCapturePointPlanner;
	NewInstantaneousCapturePointPlannerWithTimeFreezerAndFootSlipCompensation newCapturePointPlanner;

	public CapturePointPlannerAdapter(CapturePointPlannerParameters capturePointPlannerParameters, YoVariableRegistry registry,
			YoGraphicsListRegistry yoGraphicsListRegistry, double controlDT, CommonHumanoidReferenceFrames referenceFrames)
	{
		this.capturePointPlannerParameters = capturePointPlannerParameters;
		this.referenceFrames = referenceFrames;
		this.currentTransferToSide = new EnumYoVariable<RobotSide>("icpPlannerAdapterCurrentTransferToSide", registry, RobotSide.class);

		SmoothICPComputer2D smoothICPComputer2D = new SmoothICPComputer2D(referenceFrames, controlDT,
				this.capturePointPlannerParameters.getDoubleSupportSplitFraction(),
				this.capturePointPlannerParameters.getNumberOfFootstepsToConsider(), registry, yoGraphicsListRegistry);
		smoothICPComputer2D.setICPInFromCenter(this.capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());

		this.oldCapturePointPlanner = new InstantaneousCapturePointPlannerWithTimeFreezer(smoothICPComputer2D, registry);

		newCapturePointPlanner = new NewInstantaneousCapturePointPlannerWithTimeFreezerAndFootSlipCompensation(
				this.capturePointPlannerParameters, registry, yoGraphicsListRegistry);
	}

	@Override
	public void initializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, initialTime);
		}
		else
		{
		   footstepList.clear();
         soleFrameList.clear();
         
			transferToAndNextFootstepsData.getFootLocationList(footstepList, soleFrameList,
					capturePointPlannerParameters.getCapturePointForwardFromFootCenterDistance(),
					capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());

			newCapturePointPlanner.setOmega0(transferToAndNextFootstepsData.getW0());
			newCapturePointPlanner.initializeSingleSupport(initialTime, footstepList);
		}
	}

	@Override
	public void reInitializeSingleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double currentTime)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.reInitializeSingleSupport(transferToAndNextFootstepsData, currentTime);
		}
		else
		{
		}

	}

	@Override
	public void initializeDoubleSupportInitialTransfer(TransferToAndNextFootstepsData transferToAndNextFootstepsData,
			Point2d initialICPPosition, double initialTime)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.initializeDoubleSupportInitialTransfer(transferToAndNextFootstepsData, initialICPPosition, initialTime);
		}
		else
		{
		   footstepList.clear();
		   soleFrameList.clear();
		   
			transferToAndNextFootstepsData.getFootLocationList(footstepList, soleFrameList,
					capturePointPlannerParameters.getCapturePointForwardFromFootCenterDistance(),
					capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());

			currentTransferToSide.set(transferToAndNextFootstepsData.getTransferToSide());

			tmpFramePoint.set(initialICPPosition.getX(), initialICPPosition.getY(), 0.0);
			tmpFrameVector.set(0.0, 0.0, 0.0);
			transferToFootLocation.setToZero(referenceFrames.getSoleFrame(transferToAndNextFootstepsData.getTransferToSide()));
			transferToFootLocation.changeFrame(worldFrame);

			newCapturePointPlanner.setOmega0(transferToAndNextFootstepsData.getW0());
			newCapturePointPlanner.initializeDoubleSupport(tmpFramePoint, tmpFrameVector, initialTime, footstepList, transferToAndNextFootstepsData.getTransferToSide(), transferToFootLocation);
		}
	}

	@Override
	public void initializeDoubleSupport(TransferToAndNextFootstepsData transferToAndNextFootstepsData, double initialTime)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.initializeDoubleSupport(transferToAndNextFootstepsData, initialTime);
		}
		else
		{
		   footstepList.clear();
		   soleFrameList.clear();
		   
			transferToAndNextFootstepsData.getFootLocationList(footstepList, soleFrameList,
					capturePointPlannerParameters.getCapturePointForwardFromFootCenterDistance(),
					capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());
			
			currentTransferToSide.set(transferToAndNextFootstepsData.getTransferToSide());

			tmpFramePoint.set(transferToAndNextFootstepsData.getCurrentDesiredICP().getX(), transferToAndNextFootstepsData
					.getCurrentDesiredICP().getY(), 0.0);
			tmpFrameVector.set(transferToAndNextFootstepsData.getCurrentDesiredICPVelocity().getX(), transferToAndNextFootstepsData
					.getCurrentDesiredICPVelocity().getY(), 0.0);
			
			transferToFootLocation.setToZero(referenceFrames.getSoleFrame(transferToAndNextFootstepsData.getTransferToSide()));
         transferToFootLocation.changeFrame(worldFrame);

			newCapturePointPlanner.setOmega0(transferToAndNextFootstepsData.getW0());
			newCapturePointPlanner.initializeDoubleSupport(tmpFramePoint, tmpFrameVector, initialTime, footstepList,transferToAndNextFootstepsData.getTransferToSide(), transferToFootLocation);
		}
	}

	@Override
	public void getICPPositionAndVelocity(FramePoint2d icpPositionToPack, FrameVector2d icpVelocityToPack, FramePoint2d ecmpToPack,
			FramePoint2d actualICP, double time)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.getICPPositionAndVelocity(icpPositionToPack, icpVelocityToPack, ecmpToPack, actualICP, time);
		}
		else
		{
		   transferToFootLocation.setToZero(referenceFrames.getSoleFrame(currentTransferToSide.getEnumValue()));
		   transferToFootLocation.changeFrame(worldFrame);
		   
			tmpFramePoint2.set(actualICP.getX(), actualICP.getY(), 0.0);
			newCapturePointPlanner.packDesiredCapturePointPositionAndVelocity(tmpFramePoint, tmpFrameVector, time, tmpFramePoint2,
					transferToFootLocation);

			icpPositionToPack.set(tmpFramePoint.getX(), tmpFramePoint.getY());
			
			icpVelocityToPack.set(tmpFrameVector.getX(), tmpFrameVector.getY());
			
			newCapturePointPlanner.packDesiredCentroidalMomentumPivotPosition(tmpFramePoint);
			ecmpToPack.set(tmpFramePoint.getX(), tmpFramePoint.getY());
		}
	}

	@Override
	public void reset(double time)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.reset(time);
		}
		else
		{
			newCapturePointPlanner.reset(time);
		}
	}

	@Override
	public boolean isDone(double time)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.isDone(time);
		}
		else
		{
		   // This is a hack, and is needed for now because with the current icp planner, the single and double support 
		   // durations are 0 on construction and then isDone() is called which returns true even though the 
		   // planner has done nothing.
		   if(newCapturePointPlanner.getHasBeenWokenUp())
		   {
		      return newCapturePointPlanner.isDone(time);
		   }
		   else
		   {
		      newCapturePointPlanner.wakeUp();
		      return true;
		   }
		}
	}

	@Override
	public double getEstimatedTimeRemainingForState(double time)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.getEstimatedTimeRemainingForState(time);
		}
		else
		{
			return newCapturePointPlanner.computeAndReturnTimeRemaining(time);
		}
	}

	@Override
	public boolean isPerformingICPDoubleSupport()
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.isPerformingICPDoubleSupport();
		}
		else
		{
			return newCapturePointPlanner.isDoubleSupport.getBooleanValue();
		}
	}

	@Override
	public FramePoint2d getFinalDesiredICP()
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.getFinalDesiredICP();
		}
		else
		{
			newCapturePointPlanner.getFinalDesiredCapturePointPosition(tmpFramePoint);
			tmpFramePoint2d.set(tmpFramePoint.getX(), tmpFramePoint.getY());
			return tmpFramePoint2d;
		}
	}

	@Override
	public FramePoint2d getConstantCenterOfPressure()
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.getConstantCenterOfPressure();
		}
		else
		{
			newCapturePointPlanner.getConstantCentroidalMomentumPivotPosition(tmpFramePoint);
			tmpFramePoint2d.set(tmpFramePoint.getX(), tmpFramePoint.getY());
			return tmpFramePoint2d;
		}
	}

	@Override
	public FramePoint2d getSingleSupportStartICP()
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.getSingleSupportStartICP();
		}
		else
		{
			newCapturePointPlanner.getSingleSupportInitialCapturePointPosition(tmpFramePoint);
			tmpFramePoint2d.set(tmpFramePoint.getX(), tmpFramePoint.getY());
			return tmpFramePoint2d;
		}
	}

	@Override
	public double getTimeInState(double time)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			return oldCapturePointPlanner.getTimeInState(time);
		}
		else
		{
			return newCapturePointPlanner.computeAndReturnTimeInCurrentState(time);
		}
	}

	@Override
	public void setDoHeelToToeTransfer(boolean doHeelToToeTransfer)
	{
		if (USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.setDoHeelToToeTransfer(doHeelToToeTransfer);
		}
		else
		{
			throw new RuntimeException("Not implemented");
		}
	}

	@Override
	public void updatePlanForSingleSupportPush(TransferToAndNextFootstepsData transferToAndNextFootstepsData, FramePoint actualCapturePointPosition, double time) 
	{
		if(USE_OLD_HACKY_ICP_PLANNER)
		{
			oldCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, time);
		}
		else
		{
		   footstepList.clear();
         soleFrameList.clear();
         
         transferToAndNextFootstepsData.getFootLocationList(footstepList, soleFrameList,
               capturePointPlannerParameters.getCapturePointForwardFromFootCenterDistance(),
               capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());

         newCapturePointPlanner.setOmega0(transferToAndNextFootstepsData.getW0());
         newCapturePointPlanner.initializeSingleSupport(time, footstepList);
         
			newCapturePointPlanner.updatePlanForSingleSupportPush(footstepList, actualCapturePointPosition, time);
		}
	}
	
	@Override
   public void updatePlanForDoubleSupportPush(TransferToAndNextFootstepsData transferToAndNextFootstepsData, FramePoint actualCapturePointPosition,
         double time)
   {
      if(USE_OLD_HACKY_ICP_PLANNER)
      {
         oldCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, time);
      }
      else
      {
         footstepList.clear();
         soleFrameList.clear();
         
         transferToAndNextFootstepsData.getFootLocationList(footstepList, soleFrameList,
               capturePointPlannerParameters.getCapturePointForwardFromFootCenterDistance(),
               capturePointPlannerParameters.getCapturePointInFromFootCenterDistance());

         newCapturePointPlanner.setOmega0(transferToAndNextFootstepsData.getW0());
         newCapturePointPlanner.initializeSingleSupport(time, footstepList);
         
         newCapturePointPlanner.updatePlanForDoubleSupportPush(footstepList, actualCapturePointPosition, time);
      }
   }
}
