package us.ihmc.darpaRoboticsChallenge;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.UnreasonableAccelerationException;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.simulationRunner.SimulationRewindabilityVerifier;
import com.yobotics.simulationconstructionset.util.simulationRunner.VariableDifference;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.utilities.MemoryTools;

public class DRCFlatGroundRewindabilityTest {
	
	private static final boolean SHOW_GUI = true;
	private static final double totalTimeToTest = 3.0; //0.6; //3.0;
	private static final double timeToTickAhead = 1.5;
	private static final double timePerTick = 0.1;
	   
	@Before
	public void setUp() throws Exception
	{
	      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");		
	}
	
	@After
	public void showMemoryUsageAfterTest()
	{
	   MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
	}
	
	@Ignore
	@Test
	public void testCanRewindAndGoForward() throws UnreasonableAccelerationException
	{
		BambooTools.reportTestStartedMessage();
		
		int numberOfSteps = 100;
		
		SimulationConstructionSet scs = setupScs();
		
		for (int i = 0; i < numberOfSteps; i++) 
		{
			scs.simulateOneRecordStepNow();
			scs.simulateOneRecordStepNow();
			scs.stepBackwardNow();
		}

		scs.closeAndDispose();

		BambooTools.reportTestFinishedMessage();
	}
	
	@Test
	public void testRewindability() throws UnreasonableAccelerationException, SimulationExceededMaximumTimeException
	{
		BambooTools.reportTestStartedMessage();
		
        int numTicksToTest = (int) Math.round(totalTimeToTest / timePerTick);
        if (numTicksToTest < 1)
           numTicksToTest = 1;
        
        int numTicksToSimulateAhead = (int) Math.round(timeToTickAhead / timePerTick);
        if (numTicksToSimulateAhead < 1)
           numTicksToSimulateAhead = 1;
        
        SimulationConstructionSet scs1 = setupScs(); // createTheSimulation(ticksForDataBuffer);
        SimulationConstructionSet scs2 = setupScs(); // createTheSimulation(ticksForDataBuffer);
        
        try
        {
           Thread.sleep(1000); // Weird random failures sometimes if we don't sleep a little...
        } 
        catch (InterruptedException e)
        {
        }

        ArrayList<String> exceptions = new ArrayList<String>();
        exceptions.add("gc_");
        exceptions.add("ef_");
        exceptions.add("kp_");
        exceptions.add("TimeNano");
        exceptions.add("DurationMilli");
        SimulationRewindabilityVerifier checker = new SimulationRewindabilityVerifier(scs1, scs2, exceptions);
        
        double maxDifferenceAllowed = 1e-7;
        ArrayList<VariableDifference> variableDifferences;

        variableDifferences = checker.checkRewindabilityWithSimpleMethod(numTicksToTest, maxDifferenceAllowed);

		if (!variableDifferences.isEmpty()) {
			System.err.println("variableDifferences: " + VariableDifference.allVariableDifferencesToString(variableDifferences));
			if (SHOW_GUI)
				sleepForever();
			fail("Found Variable Differences!");
		}

		// sleepForever();

		scs1.closeAndDispose();
		scs2.closeAndDispose();

		BambooTools.reportTestFinishedMessage();
	}
	
	private SimulationConstructionSet setupScs() 
	{
		boolean useVelocityAndHeadingScript = true;
	    boolean cheatWithGroundHeightAtForFootstep = true;

	    GroundProfile groundProfile = new FlatGroundProfile();

	    DRCRobotWalkingControllerParameters drcControlParameters = new DRCRobotWalkingControllerParameters();
			    
		AutomaticSimulationRunner automaticSimulationRunner = null;
		DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();

		DRCRobotModel robotModel = DRCRobotModel.getDefaultRobotModel();
		double timePerRecordTick = 0.005;
		int simulationDataBufferSize = 16000;
		boolean doChestOrientationControl = true;

		RobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(0.0);
		DRCRobotInterface robotInterface = new PlainDRCRobot(robotModel);
		DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT());

		DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(drcControlParameters, robotInterface, robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript,
				automaticSimulationRunner, timePerRecordTick,simulationDataBufferSize, doChestOrientationControl, cheatWithGroundHeightAtForFootstep);

		SimulationConstructionSet scs = drcFlatGroundWalkingTrack.getSimulationConstructionSet();

		setupCameraForUnitTest(scs);

		return scs;
	}
	
	private void setupCameraForUnitTest(SimulationConstructionSet scs) 
	{
		CameraConfiguration cameraConfiguration = new CameraConfiguration("testCamera");
		cameraConfiguration.setCameraFix(0.6, 0.4, 1.1);
		cameraConfiguration.setCameraPosition(-0.15, 10.0, 3.0);
		cameraConfiguration.setCameraTracking(true, true, true, false);
		cameraConfiguration.setCameraDolly(true, true, true, false);
		scs.setupCamera(cameraConfiguration);
		scs.selectCamera("testCamera");
	}

	private DRCGuiInitialSetup createGUIInitialSetup() 
	{
		DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);
		guiInitialSetup.setIsGuiShown(SHOW_GUI);

		return guiInitialSetup;
	}
	
	private void sleepForever() 
	{
		while (true) 
		{
			try 
			{
				Thread.sleep(1000);
			} 
			catch (InterruptedException e) 
			{
			}

		}
	}
}
