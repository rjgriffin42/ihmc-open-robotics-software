package us.ihmc.wholeBodyController;
 
import us.ihmc.SdfLoader.FullHumanoidRobotModelFactory;
import us.ihmc.SdfLoader.HumanoidFloatingRootJointRobot;
import us.ihmc.SdfLoader.models.FullRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.simulationconstructionset.robotController.OutputProcessor;
import us.ihmc.wholeBodyController.parameters.DefaultArmConfigurations;

public interface WholeBodyControllerParameters extends FullHumanoidRobotModelFactory
{
	public CapturePointPlannerParameters getCapturePointPlannerParameters();

	public ICPOptimizationParameters getICPOptimizationParameters();

	public ArmControllerParameters getArmControllerParameters();

	public WalkingControllerParameters getWalkingControllerParameters();
	
	public WalkingControllerParameters getMultiContactControllerParameters();
	
	public RobotContactPointParameters getContactPointParameters();
	
	public double getControllerDT();

	public HumanoidFloatingRootJointRobot createSdfRobot(boolean createCollisionMeshes);
	
	
	public OutputProcessor getOutputProcessor(FullRobotModel controllerFullRobotModel);
	
	public DefaultArmConfigurations getDefaultArmConfigurations();
}
