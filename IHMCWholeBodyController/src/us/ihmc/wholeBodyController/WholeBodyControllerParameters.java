package us.ihmc.wholeBodyController;
 
import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.SdfLoader.SDFFullHumanoidRobotModelFactory;
import us.ihmc.SdfLoader.SDFHumanoidRobot;
import us.ihmc.SdfLoader.models.FullRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.simulationconstructionset.robotController.OutputProcessor;
import us.ihmc.wholeBodyController.parameters.DefaultArmConfigurations;

public interface WholeBodyControllerParameters extends SDFFullHumanoidRobotModelFactory
{
	public CapturePointPlannerParameters getCapturePointPlannerParameters();

	public ArmControllerParameters getArmControllerParameters();

	public WalkingControllerParameters getWalkingControllerParameters();
	
	public WalkingControllerParameters getMultiContactControllerParameters();
	
	public RobotContactPointParameters getContactPointParameters();
	
	public double getControllerDT();

	public SDFHumanoidRobot createSdfRobot(boolean createCollisionMeshes);
	
	
	public OutputProcessor getOutputProcessor(FullRobotModel controllerFullRobotModel);
	
	public DefaultArmConfigurations getDefaultArmConfigurations();
}
