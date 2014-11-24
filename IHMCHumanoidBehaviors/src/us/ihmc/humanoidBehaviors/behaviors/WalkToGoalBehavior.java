package us.ihmc.humanoidBehaviors.behaviors;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.behaviors.WalkToGoalBehaviorPacket;
import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.communication.packets.walking.FootstepPathPlanPacket;
import us.ihmc.communication.packets.walking.FootstepPlanRequestPacket;
import us.ihmc.communication.packets.walking.FootstepStatus;
import us.ihmc.communication.packets.walking.FootstepStatus.Status;
import us.ihmc.communication.packets.walking.SnapFootstepPacket;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class WalkToGoalBehavior extends BehaviorInterface {

	private final BooleanYoVariable DEBUG = new BooleanYoVariable("DEBUG", registry);
	private final DoubleYoVariable yoTime;
	private double searchStartTime = 0;

	private final ConcurrentListeningQueue<WalkToGoalBehaviorPacket> inputListeningQueue = new ConcurrentListeningQueue<WalkToGoalBehaviorPacket>();
	private final ConcurrentListeningQueue<FootstepPathPlanPacket> plannedPathListeningQueue = new ConcurrentListeningQueue<FootstepPathPlanPacket>();
	private final ConcurrentListeningQueue<FootstepStatus> footstepStatusQueue  = new ConcurrentListeningQueue<FootstepStatus>();;
	private final BooleanYoVariable isDone;
	private final BooleanYoVariable hasInputBeenSet;
	private final FullRobotModel fullRobotModel;

	private FootstepData startFootstep;
	private ArrayList<FootstepData> goalFootsteps = new ArrayList<FootstepData>();
	private double startYaw;

	private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

	private final BooleanYoVariable hasNewPlan = new BooleanYoVariable("hasNewPlan", registry);
	private final BooleanYoVariable stepCompleted = new BooleanYoVariable("stepCompleted", registry);
	private final BooleanYoVariable allStepsCompleted = new BooleanYoVariable("allStepsCompleted", registry);
	private final BooleanYoVariable waitingForValidPlan = new BooleanYoVariable("waitingForValidPlan", registry);
	private final BooleanYoVariable executePlan = new BooleanYoVariable("executePlan", registry);
	private final BooleanYoVariable executeUnknown = new BooleanYoVariable("executeUnknown", registry);

//	private ArrayList<FootstepData> footsteps = new ArrayList<FootstepData>();
	
	private FootstepData currentLocation;
	private FootstepData predictedLocation;
	private FootstepPathPlanPacket currentPlan;
	private List<FootstepData> stepsRequested;
	private double ankleHeight = 0;


	public WalkToGoalBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, FullRobotModel fullRobotModel, DoubleYoVariable yoTime, double ankleHeight)
	{
		super(outgoingCommunicationBridge);
		DEBUG.set(true);
		this.yoTime = yoTime;
		this.ankleHeight = ankleHeight;

		isDone = new BooleanYoVariable("isDone", registry);
		hasInputBeenSet = new BooleanYoVariable("hasInputsBeenSet", registry);

		this.fullRobotModel = fullRobotModel;

		this.attachNetworkProcessorListeningQueue(inputListeningQueue, WalkToGoalBehaviorPacket.class);
		this.attachNetworkProcessorListeningQueue(plannedPathListeningQueue, FootstepPathPlanPacket.class);
		this.attachControllerListeningQueue(footstepStatusQueue, FootstepStatus.class);
	}

	@Override
	public void doControl() {
		if (!isDone.getBooleanValue()){
			checkForNewInputs();
		}
		if (!hasInputBeenSet()){
			return;
		}
		if (atGoal()){
			hasInputBeenSet.set(false);
			return;
		}
		if (checkForNewPlan()){
			return;
		}		
		if (checkForStepCompleted()){
			return;
		}

		if (executePlan.getBooleanValue() && hasNewPlan.getBooleanValue() && (!stepCompleted.getBooleanValue() || allStepsCompleted.getBooleanValue())){
			if (planValid(currentPlan) && (!currentPlan.footstepUnknown.get(1) || executeUnknown.getBooleanValue())){
				processNextStep();
			}
		}
	}
	
	private boolean checkForNewPlan(){
		//return true if new packet, else return false.
		FootstepPathPlanPacket newestPacket = plannedPathListeningQueue.getNewestPacket();
		if (newestPacket != null){
			System.out.println("New plan received. Checking validity...");
			if (planValid(newestPacket)){
				currentPlan = newestPacket;
				hasNewPlan.set(true);
				//stop current steps
				visualizePlan(currentPlan);
			}else{
//				visualizePlan(newestPacket);
				System.out.println("Plan is not Valid!");
			}
			return true;
		}		
		return false;
	}
	
	private void visualizePlan(FootstepPathPlanPacket plan){
		if (plan.pathPlan == null || plan.pathPlan.size() == 0) return;
		int size = plan.pathPlan.size();
		SnapFootstepPacket planVisualizationPacket = new SnapFootstepPacket();
		planVisualizationPacket.footstepData = new ArrayList<FootstepData>();
		planVisualizationPacket.footstepOrder = new int[size];
		planVisualizationPacket.flag = new byte[size];
		
		for (int i = 0; i < size; i++){
			planVisualizationPacket.footstepData.add(adjustFootstepForAnkleHeight(plan.pathPlan.get(i)));
			planVisualizationPacket.footstepOrder[i] = i;
			planVisualizationPacket.flag[i] = (byte) (plan.footstepUnknown.get(i) ? 0 : 2);
		}
		planVisualizationPacket.setDestination(PacketDestination.NETWORK_PROCESSOR);
		sendPacketToNetworkProcessor(planVisualizationPacket);
	}
	
	private boolean planValid(FootstepPathPlanPacket plan){
		if (plan == null) return false;
		if (!plan.goalsValid) return false;
		for (int i=0; i < plan.originalGoals.size(); i++){
			if (!approximatelyEqual(plan.originalGoals.get(i),goalFootsteps.get(i))) return false;
		}
		while (plan.pathPlan.size() > 0 && !approximatelyEqual(plan.pathPlan.get(0), predictedLocation)){
			plan.pathPlan.remove(0);
			plan.footstepUnknown.remove(0);
		}
		if (plan.pathPlan.size() < 2) return false;
		waitingForValidPlan.set(false);
		return true;
	}
	
	private boolean checkForStepCompleted(){
		//return true if there was a new packet, otherwise return false.
		FootstepStatus newestPacket = footstepStatusQueue.getNewestPacket();
		if (newestPacket != null){
			//TODO: update current location and predicted location from the feedback
			if (newestPacket.status == Status.STARTED){
				predictedLocation = stepsRequested.get(newestPacket.footstepIndex);
				sendUpdateStart(predictedLocation);
				stepCompleted.set(false);
			}else if (newestPacket.status == Status.COMPLETED){
				currentLocation = stepsRequested.get(newestPacket.footstepIndex);
				stepCompleted.set(true);
				if (newestPacket.footstepIndex == stepsRequested.size()-1){
					allStepsCompleted.set(true);
					executePlan.set(false);
				}
			}
			return true;
		}
		return false;
	}
	
	private void processNextStep(){
		if (!planValid(currentPlan)){
			if (!waitingForValidPlan.getBooleanValue()){
				sendUpdateStart(predictedLocation);
				waitingForValidPlan.set(true);
			}
		}else{
			takeStep();
			hasNewPlan.set(false);
		}
	}

	private void takeStep(){
		System.out.println("Taking step.");
		//remove current location from plan, element 1 is next step
		currentPlan.pathPlan.remove(0);
		currentPlan.footstepUnknown.remove(0);
		//element 1 is now element 0
		if (currentPlan.footstepUnknown.get(0)) return;
		
		sendStepsToController();
		stepCompleted.set(false);
		
	}
	
	private boolean atGoal(){
		if (currentLocation == null) return false;
		for (FootstepData goal : goalFootsteps){
			if (approximatelyEqual(currentLocation, goal)){
				hasInputBeenSet.set(false);
				return true;
			}
		}
		return false;
	}

	private boolean approximatelyEqual(FootstepData currentLocation, FootstepData checkAgainst){
		if (currentLocation == null) return false;
		double xDiff = currentLocation.location.x - checkAgainst.location.x;
		double yDiff = currentLocation.location.y - checkAgainst.location.y;
		if (currentLocation.robotSide != checkAgainst.robotSide) return false;
		if (Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff,2)) > 0.05) return false;
		if (Math.abs(RotationFunctions.getYawFromQuaternion(currentLocation.orientation) - RotationFunctions.getYawFromQuaternion(checkAgainst.orientation)) > Math.PI/16) return false;
		return true;
	}

	private void requestFootstepPlan()
	{
		FootstepPlanRequestPacket footstepPlanRequestPacket = new FootstepPlanRequestPacket(FootstepPlanRequestPacket.RequestType.START_SEARCH, startFootstep,startYaw,goalFootsteps);
		outgoingCommunicationBridge.sendPacketToNetworkProcessor(footstepPlanRequestPacket);
		waitingForValidPlan.set(true);
	}

	private void requestSearchStop(){
		FootstepPlanRequestPacket stopSearchRequestPacket = new FootstepPlanRequestPacket(FootstepPlanRequestPacket.RequestType.STOP_SEARCH,new FootstepData(), 0.0, null);
		outgoingCommunicationBridge.sendPacketToNetworkProcessor(stopSearchRequestPacket);
		waitingForValidPlan.set(false);
	}
	
	private void sendUpdateStart(FootstepData updatedLocation){
		if (updatedLocation.orientation.epsilonEquals(new Quat4d(), .003)) return;
		FootstepPlanRequestPacket updateStartPacket = new FootstepPlanRequestPacket(FootstepPlanRequestPacket.RequestType.UPDATE_START, updatedLocation, RotationFunctions.getYawFromQuaternion(updatedLocation.orientation), null);
		outgoingCommunicationBridge.sendPacketToNetworkProcessor(updateStartPacket);
	}
	
	private void sendStepsToController(){
		int size = currentPlan.footstepUnknown.size();
		FootstepDataList outgoingFootsteps = new FootstepDataList();
		for (int i = 0; i < size; i ++){
			if (!currentPlan.footstepUnknown.get(i) || executeUnknown.getBooleanValue()){
				outgoingFootsteps.footstepDataList.add(adjustFootstepForAnkleHeight(currentPlan.pathPlan.get(i)));
			}else{
				break;
			}
		}
		stepsRequested = outgoingFootsteps.footstepDataList;
		System.out.println(stepsRequested.size() + " steps sent to controller");
		outgoingFootsteps.setDestination(PacketDestination.CONTROLLER);
		allStepsCompleted.set(false);
        sendPacketToController(outgoingFootsteps);
    }
	
	private FootstepData adjustFootstepForAnkleHeight(FootstepData footstep){
		FootstepData copy = new FootstepData(footstep);
		Point3d ankleOffset = new Point3d(0, 0, ankleHeight);
		RigidBodyTransform footTransform = new RigidBodyTransform();
		footTransform.setRotationAndZeroTranslation(copy.getOrientation());
		footTransform.transform(ankleOffset);
		copy.getLocation().add(ankleOffset);
		return copy;
	}
	
	private void checkForNewInputs()
	{
		WalkToGoalBehaviorPacket newestPacket = inputListeningQueue.getNewestPacket();
		if (newestPacket != null)
		{
			if (newestPacket.execute){
				executePlan.set(true);
			}else{
				set(newestPacket.getGoalPosition()[0], newestPacket.getGoalPosition()[1], newestPacket.getGoalPosition()[2], newestPacket.getGoalSide());
				requestFootstepPlan();
				hasInputBeenSet.set(true);
			}
		}
	}

	public void set(double xGoal, double yGoal, double thetaGoal, RobotSide goalSide)
	{
		RobotSide startSide = RobotSide.LEFT;
		Vector3d startTranslation = new Vector3d();
		Matrix3d startRotation = new Matrix3d();
		fullRobotModel.getFoot(startSide).getBodyFixedFrame().getTransformToDesiredFrame(worldFrame).getTranslation(startTranslation);
		fullRobotModel.getFoot(startSide).getBodyFixedFrame().getTransformToDesiredFrame(worldFrame).getRotation(startRotation);
		startYaw = RotationFunctions.getYaw(startRotation);
		Quat4d startOrientation = new Quat4d();
		RotationFunctions.setQuaternionBasedOnMatrix3d(startOrientation, startRotation);
		startFootstep = new FootstepData(startSide, new Point3d(startTranslation), startOrientation);
		currentLocation = new FootstepData(startFootstep);
		predictedLocation = currentLocation;
		stepCompleted.set(true);

		double robotYOffset = 0.1;
		double xOffset = -1 * robotYOffset * Math.sin(thetaGoal);
		double yOffset = robotYOffset * Math.cos(thetaGoal);

		goalFootsteps.clear();
		//set left foot goal 
		Quat4d leftGoalOrientation = new Quat4d();
		RotationFunctions.setQuaternionBasedOnYawPitchRoll(leftGoalOrientation, thetaGoal, 0,0);
		goalFootsteps.add(new FootstepData(RobotSide.LEFT, new Point3d(xGoal + xOffset, yGoal + yOffset, 0), leftGoalOrientation));

		Quat4d rightGoalOrientation = new Quat4d();
		RotationFunctions.setQuaternionBasedOnYawPitchRoll(rightGoalOrientation, thetaGoal, 0,0);
		goalFootsteps.add(new FootstepData(RobotSide.RIGHT, new Point3d(xGoal - xOffset, yGoal - yOffset, 0), rightGoalOrientation));

		hasInputBeenSet.set(true);
	}

	@Override
	public void initialize()
	{
		stepCompleted.set(true);
		hasNewPlan.set(false);
		waitingForValidPlan.set(false);
		hasInputBeenSet.set(false);
		executePlan.set(false);
		executeUnknown.set(false);
		allStepsCompleted.set(true);
	}

	@Override
	protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
	{
	}

	@Override
	protected void passReceivedControllerObjectToChildBehaviors(Object object)
	{
	}

	@Override
	public void stop()
	{
		requestSearchStop();
		waitingForValidPlan.set(false);
		isStopped.set(true);
	}

	@Override
	public void enableActions()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void pause()
	{
		isPaused.set(true);
	}

	@Override
	public void resume()
	{
		isPaused.set(false);

	}

	@Override
	public boolean isDone()
	{
		return atGoal();
	}

	@Override
	public void finalize()
	{
		isPaused.set(false);
		isStopped.set(false);
		stepCompleted.set(true);
		hasNewPlan.set(false);
		waitingForValidPlan.set(false);
		executePlan.set(false);
		executeUnknown.set(false);
		requestSearchStop();
		hasInputBeenSet.set(false);
		allStepsCompleted.set(true);
	}

	public boolean hasInputBeenSet()
	{
		return hasInputBeenSet.getBooleanValue();
	}
}
