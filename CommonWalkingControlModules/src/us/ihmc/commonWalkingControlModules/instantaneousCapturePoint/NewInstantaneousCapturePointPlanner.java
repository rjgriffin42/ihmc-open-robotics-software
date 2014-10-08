package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.DoubleSupportPolynomialTrajectory;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.MinimumJerkTrajectory;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.yoUtilities.graphics.plotting.ArtifactList;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

public class NewInstantaneousCapturePointPlanner
{
	private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

	private BooleanYoVariable VISUALIZE = new BooleanYoVariable("icpPlannerVISUALIZE", registry);
	private double ICP_CORNER_POINT_SIZE = 0.004;
	private double ICP_CONSTANT_COP_POINT_SIZE = 0.005;

	private final BooleanYoVariable atAStop = new BooleanYoVariable("icpPlannerAtAStop", registry);
	private final BooleanYoVariable comeToStop = new BooleanYoVariable("icpPlannerComeToStop", registry);
	private final BooleanYoVariable isInitialTransfer = new BooleanYoVariable("icpPlannerIsInitialTransfer", registry);
	private final BooleanYoVariable isDoubleSupport = new BooleanYoVariable("icpPlannerIsDoubleSupport", registry);
	private final DoubleYoVariable timeInCurrentState = new DoubleYoVariable("icpPlannerTimeInCurrentState", registry);
	private final DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("icpPlannerDoubleSupportTime", registry);
	private final DoubleYoVariable singleSupportDuration = new DoubleYoVariable("icpPlannerSingleSupportTime", registry);
	private final DoubleYoVariable initialTime = new DoubleYoVariable("icpPlannerInitialTime", registry);
	private final IntegerYoVariable numberFootstepsToConsider = new IntegerYoVariable("", registry);
	private final YoFramePoint finalDesiredCapturePointPosition = new YoFramePoint("icpFinalDesiredCapturePointPosition",
			ReferenceFrame.getWorldFrame(), registry);
	private final YoFrameVector finalDesiredCapturePointVelocity = new YoFrameVector("icpFinalDesiredCapturePointVelocity",
			ReferenceFrame.getWorldFrame(), registry);
	private final YoFramePoint desiredCapturePointPosition = new YoFramePoint("icpPlannerDesiredCapturePointPosition",
			ReferenceFrame.getWorldFrame(), registry);
	private final YoFrameVector desiredCapturePointVelocity = new YoFrameVector("icpPlannerDesiredCapturePointVelocity",
			ReferenceFrame.getWorldFrame(), registry);
	private final YoFrameVector desiredCapturePointAcceleration = new YoFrameVector("icpPlannerDesiredCapturePointAcceleration",
			ReferenceFrame.getWorldFrame(), registry);
	private final DoubleYoVariable omega0 = new DoubleYoVariable("icpPlannerOmega0", registry);
	private final ArrayList<YoFramePoint> constantCentersOfPressure = new ArrayList<YoFramePoint>();
	private final ArrayList<YoFramePoint> capturePointCornerPoints = new ArrayList<YoFramePoint>();

	// Move the constant out of here and bring it in through the constructor somehow, maybe a class of 
	// capture point planner parameters.
	private final DoubleSupportPolynomialTrajectory doubleSupportCapturePointTrajectory = new DoubleSupportPolynomialTrajectory("icpPlannerDoubleSupportTrajectory", 5,
			ReferenceFrame.getWorldFrame(), registry);

	public NewInstantaneousCapturePointPlanner(int maxNumberFootstepsToConsider, YoVariableRegistry parentRegistry,
			YoGraphicsListRegistry yoGraphicsListRegistry)
	{
		if (yoGraphicsListRegistry == null)
		{
			VISUALIZE.set(false);
		}

		this.numberFootstepsToConsider.set(maxNumberFootstepsToConsider);
		this.atAStop.set(true);
		
		for (int i = 0; i < numberFootstepsToConsider.getIntegerValue(); i++)
		{
			YoFramePoint constantCopYoFramePoint = new YoFramePoint("icpConstantCoP" + i, ReferenceFrame.getWorldFrame(), registry);
			constantCentersOfPressure.add(constantCopYoFramePoint);
		}

		for (int i = 0; i < numberFootstepsToConsider.getIntegerValue() - 1; i++)
		{
			YoFramePoint icpCornerPointYoFramePoint = new YoFramePoint("icpCornerPoints" + i, ReferenceFrame.getWorldFrame(), registry);
			capturePointCornerPoints.add(icpCornerPointYoFramePoint);
		}

		parentRegistry.addChild(this.registry);

		if (VISUALIZE.getBooleanValue())
		{
			setupVisualizers(yoGraphicsListRegistry);
		}
	}
	
	private void setupVisualizers(YoGraphicsListRegistry yoGraphicsListRegistry)
	{
		YoGraphicsList yoGraphicsList = new YoGraphicsList("ICPComputer");
		ArtifactList artifactList = new ArtifactList("ICPPlanner");

		for (int i = 0; i < numberFootstepsToConsider.getIntegerValue(); i++)
		{
			if (VISUALIZE.getBooleanValue())
			{
				YoGraphicPosition constantFootCenterCentersOfPressureViz = new YoGraphicPosition("icpConstantCoP" + i,
						constantCentersOfPressure.get(i), ICP_CONSTANT_COP_POINT_SIZE, YoAppearance.Green(), GraphicType.SOLID_BALL);

				yoGraphicsList.add(constantFootCenterCentersOfPressureViz);
				artifactList.add(constantFootCenterCentersOfPressureViz.createArtifact());
			}
		}

		for (int i = 0; i < numberFootstepsToConsider.getIntegerValue() - 1; i++)
		{
			if (VISUALIZE.getBooleanValue())
			{
				YoGraphicPosition icpCornerPointsViz = new YoGraphicPosition("icpCornerPoints" + i, capturePointCornerPoints.get(i),
						ICP_CORNER_POINT_SIZE, YoAppearance.Green(), GraphicType.SOLID_BALL);

				yoGraphicsList.add(icpCornerPointsViz);
				artifactList.add(icpCornerPointsViz.createArtifact());
			}
		}

		yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
		yoGraphicsListRegistry.registerArtifactList(artifactList);
	}

	public void initializeDoubleSupport(double doubleSupportDuration, double singleSupportDuration,
			YoFramePoint currentDesiredCapturePointPosition, YoFrameVector currentDesiredCapturePointVelocity, double omega0,
			double initialTime, ArrayList<YoFramePoint> footstepList)
	{
		comeToStop.set(footstepList.size() <= 2);
		this.isDoubleSupport.set(true);
		this.omega0.set(omega0);
		this.singleSupportDuration.set(singleSupportDuration);
		this.doubleSupportDuration.set(doubleSupportDuration);
		this.initialTime.set(initialTime);

		this.desiredCapturePointPosition.set(currentDesiredCapturePointPosition);
		this.desiredCapturePointVelocity.set(currentDesiredCapturePointVelocity);

		computeConstantCentersOfPressure(footstepList);
		computeCapturePointCornerPoints();

		finalDesiredCapturePointPosition.set(capturePointCornerPoints.get(1));
		finalDesiredCapturePointVelocity.set(0.0, 0.0, 0.0);

		initializeDoubleSupportCapturePointTrajectory(currentDesiredCapturePointPosition, currentDesiredCapturePointVelocity,
				finalDesiredCapturePointPosition, finalDesiredCapturePointVelocity);

		if (atAStop.getBooleanValue())
		{
			atAStop.set(false);
		}
	}

	protected void initializeDoubleSupportCapturePointTrajectory(YoFramePoint initialCapturePointPosition,
			YoFrameVector initialCapturePointVelocity, YoFramePoint finalDesiredCapturePointPosition,
			YoFrameVector finalDesiredCapturePointVelocity)
	{
		doubleSupportCapturePointTrajectory.initialize(doubleSupportDuration.getDoubleValue(), initialCapturePointPosition,
				initialCapturePointVelocity, finalDesiredCapturePointPosition, finalDesiredCapturePointVelocity);
	}

	public void initializeSingleSupport(double doubleSupportDuration, double singleSupportDuration, double omega0, double initialTime,
			ArrayList<YoFramePoint> footstepList)
	{
		this.isDoubleSupport.set(false);
		this.omega0.set(omega0);
		this.initialTime.set(initialTime);
		this.isInitialTransfer.set(false);
		this.singleSupportDuration.set(singleSupportDuration);
		this.doubleSupportDuration.set(doubleSupportDuration);
		comeToStop.set(footstepList.size() <= 2);
		atAStop.set(false);

		computeConstantCentersOfPressure(footstepList);
		computeCapturePointCornerPoints();
	}

	protected void computeConstantCentersOfPressure(ArrayList<YoFramePoint> footstepList)
	{
		comeToStop.set(footstepList.size() <= 2);

		int numberOfCentersOfPressurToPlan = (this.numberFootstepsToConsider.getIntegerValue() > footstepList.size()) ? footstepList.size() : this.numberFootstepsToConsider.getIntegerValue();
		
		if (atAStop.getBooleanValue())
		{
			if(!comeToStop.getBooleanValue())//footstepList.size() > this.numberFootstepsToConsider.getIntegerValue())
			{
				CapturePointTools.computeConstantCentersOfPressureWithStartBetweenFeetAndRestOnFeet(constantCentersOfPressure, footstepList,
						numberOfCentersOfPressurToPlan);
			}
			else
			{
				CapturePointTools.computeConstantCentersOfPressuresWithBeginningAndEndBetweenFeetRestOnFeet(constantCentersOfPressure,
						footstepList, numberOfCentersOfPressurToPlan);
			}
		}
		else if (!atAStop.getBooleanValue())
		{
			if(comeToStop.getBooleanValue())
			{
				CapturePointTools.computeConstantCentersOfPressuresOnFeetWithEndBetweenFeet(constantCentersOfPressure, footstepList,
						numberOfCentersOfPressurToPlan);
			}
			else
			{
				CapturePointTools.computeConstantCentersOfPressuresOnFeet(constantCentersOfPressure, footstepList,
						numberOfCentersOfPressurToPlan);
			}
		}
	}

	protected void computeCapturePointCornerPoints()
	{
		double steppingTime = doubleSupportDuration.getDoubleValue() + singleSupportDuration.getDoubleValue();

		CapturePointTools.computeDesiredEndOfStepCapturePointLocations(constantCentersOfPressure, capturePointCornerPoints, steppingTime,
				omega0.getDoubleValue());
	}

	protected void computeDesiredCapturePointPosition(double time)
	{
		if (!isDoubleSupport.getBooleanValue())
		{
			YoFramePoint initialCapturePoint = capturePointCornerPoints.get(0);
			YoFramePoint initialCenterOfPressure = constantCentersOfPressure.get(0);

			CapturePointTools.computeDesiredCapturePointPosition(omega0.getDoubleValue(), time, initialCapturePoint,
					initialCenterOfPressure, desiredCapturePointPosition);
		}
		else
		{
			doubleSupportCapturePointTrajectory.compute(time);
			doubleSupportCapturePointTrajectory.getPosition(desiredCapturePointPosition);
		}
	}

	protected void computeDesiredCapturePointVelocity(double time)
	{
		if (!isDoubleSupport.getBooleanValue())
		{
			YoFramePoint initialCapturePoint = capturePointCornerPoints.get(0);
			YoFramePoint initialCenterOfPressure = constantCentersOfPressure.get(0);

			CapturePointTools.computeDesiredCapturePointVelocity(omega0.getDoubleValue(), time, initialCapturePoint,
					initialCenterOfPressure, desiredCapturePointVelocity);
		}
		else
		{
			doubleSupportCapturePointTrajectory.compute(time);
			doubleSupportCapturePointTrajectory.getVelocity(desiredCapturePointVelocity);
		}
	}

	protected void computeDesiredCapturePointAcceleration(double time)
	{
		if (!isDoubleSupport.getBooleanValue())
		{
			YoFramePoint initialCapturePoint = capturePointCornerPoints.get(0);
			YoFramePoint initialCenterOfPressure = constantCentersOfPressure.get(0);

			CapturePointTools.computeDesiredCapturePointAcceleration(omega0.getDoubleValue(), time, initialCapturePoint,
					initialCenterOfPressure, desiredCapturePointAcceleration);
		}
		else
		{
			doubleSupportCapturePointTrajectory.compute(time);
			doubleSupportCapturePointTrajectory.getAcceleration(desiredCapturePointAcceleration);
		}
	}

	protected void computeDesiredCornerPoints(ArrayList<YoFramePoint> constantCentersOfPressure, double stepTime, double omega0)
	{
		CapturePointTools.computeDesiredEndOfStepCapturePointLocations(constantCentersOfPressure, capturePointCornerPoints, stepTime,
				omega0);
	}

	public void packDesiredCapturePointPositionVelocityAndAcceleration(YoFramePoint desiredCapturePointPositionToPack,
			YoFrameVector desiredCapturePointVelocityToPack, YoFrameVector desiredCapturePointAccelerationToPack, double time)
	{
		computeTimeInCurrentState(time);

		computeDesiredCapturePointPosition(timeInCurrentState.getDoubleValue());
		computeDesiredCapturePointVelocity(timeInCurrentState.getDoubleValue());
		computeDesiredCapturePointAcceleration(timeInCurrentState.getDoubleValue());

		desiredCapturePointPositionToPack.set(this.desiredCapturePointPosition);
		desiredCapturePointVelocityToPack.set(this.desiredCapturePointVelocity);
		desiredCapturePointAccelerationToPack.set(this.desiredCapturePointAcceleration);
	}

	protected void computeTimeInCurrentState(double time)
	{
		timeInCurrentState.set(time - initialTime.getDoubleValue());
	}

	public YoFramePoint getDesiredCapturePointPosition()
	{
		return desiredCapturePointPosition;
	}

	public ArrayList<YoFramePoint> getConstantCentersOfPressure()
	{
		return constantCentersOfPressure;
	}

	public ArrayList<YoFramePoint> getCapturePointCornerPoints()
	{
		return capturePointCornerPoints;
	}

	public void packDesiredCapturePointIntoPoint3d(Point3d pointToPack)
	{
		desiredCapturePointPosition.get(pointToPack);
	}

	public YoFrameVector getDesiredCapturePointVelocity()
	{
		return desiredCapturePointVelocity;
	}

	public void packDesiredCapturePointVelocityIntoVector3d(Vector3d vectorToPack)
	{
		desiredCapturePointVelocity.get(vectorToPack);
	}

	public YoFrameVector getDesiredCapturePointAcceleration()
	{
		return desiredCapturePointAcceleration;
	}

	public void packDesiredCapturePointAccelerationIntoVector3d(Vector3d vectorToPack)
	{
		desiredCapturePointAcceleration.get(vectorToPack);
	}

	public void setDoubleSupportTime(double time)
	{
		this.doubleSupportDuration.set(time);
	}

	public void setSingleSupportTime(double time)
	{
		this.singleSupportDuration.set(time);
	}

	public void setOmega0(double omega0)
	{
		this.omega0.set(omega0);
	}

	public boolean isDone(double time)
	{
		return false;
	}
}
