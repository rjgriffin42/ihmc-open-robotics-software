package us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity;


import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScript.HeadingAndVelocityEvaluationEvent;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class HeadingAndVelocityEvaluationScriptTest
{
   private static final boolean SHOW_GUI = false;
   
   private static final double HEADING_VIZ_Z = 0.03;
   private static final double VELOCITY_VIZ_Z = 0.06;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }
   
   @After
   public void showMemoryUsageAfterTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   @Test
   public void testHeadingAndVelocityEvaluationScript()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("HeadingAndVelocityEvaluationScriptTest");
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      
      double controlDT = 0.1;
      double desiredHeadingFinal = 0.0;
      
      SimpleDesiredHeadingControlModule desiredHeadingControlModule = new SimpleDesiredHeadingControlModule(desiredHeadingFinal , controlDT, parentRegistry);
      ManualDesiredVelocityControlModule desiredVelocityControlModule = new ManualDesiredVelocityControlModule(ReferenceFrame.getWorldFrame(), parentRegistry);
      
      boolean cycleThroughAllEvents = true;
      HeadingAndVelocityEvaluationScript script = new HeadingAndVelocityEvaluationScript(cycleThroughAllEvents, controlDT, desiredHeadingControlModule, desiredVelocityControlModule, parentRegistry); 
   
      double time = 0.0;
   
      YoFramePoint position = new YoFramePoint("position", "", ReferenceFrame.getWorldFrame(), parentRegistry);
      YoFrameVector velocity = new YoFrameVector("velocity", "", ReferenceFrame.getWorldFrame(), parentRegistry);
      YoFrameVector heading = new YoFrameVector("heading", "", ReferenceFrame.getWorldFrame(), parentRegistry);
      
      DynamicGraphicVector velocityVector = new DynamicGraphicVector("velocity", position, velocity, YoAppearance.Yellow());
      DynamicGraphicVector headingVector = new DynamicGraphicVector("heading", position, heading, YoAppearance.Blue());
      
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("velocityVector", velocityVector);
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("headingVector", headingVector);
      
      BagOfBalls bagOfBalls = new BagOfBalls(1200, 0.03, YoAppearance.Red(), parentRegistry, dynamicGraphicObjectsListRegistry);

      boolean[] seenEvents = new boolean[HeadingAndVelocityEvaluationEvent.values().length];      
      
      SimulationConstructionSet scs = null;
      if (SHOW_GUI) scs = setupAndStartSCS(parentRegistry, dynamicGraphicObjectsListRegistry, controlDT);
      
      int numberOfTicksToTest = 1200;
      
      ArrayList<FrameVector2d> desiredHeadings = new ArrayList<FrameVector2d>();
      ArrayList<FrameVector2d> desiredVelocities = new ArrayList<FrameVector2d>();
      
      for (int i=0; i<numberOfTicksToTest; i++)
      {
         script.update(time);

         desiredHeadingControlModule.updateDesiredHeadingFrame();
         
         FrameVector2d desiredHeading = desiredHeadingControlModule.getDesiredHeading();
         FrameVector2d desiredVelocity = desiredVelocityControlModule.getDesiredVelocity();
         double desiredHeadingAngle = desiredHeadingControlModule.getDesiredHeadingAngle();

         desiredHeadings.add(desiredHeading);
         desiredVelocities.add(desiredVelocity);
         
         double angleError = AngleTools.computeAngleDifferenceMinusPiToPi(desiredHeadingAngle, Math.atan2(desiredHeading.getY(), desiredHeading.getX()));
         assertTrue(Math.abs(angleError) < 1e-7);
         
         heading.set(desiredHeading.getX(), desiredHeading.getY(), HEADING_VIZ_Z);
         velocity.set(desiredVelocity.getX(), desiredVelocity.getY(), VELOCITY_VIZ_Z);
         
         position.add(desiredVelocity.getX() * controlDT, desiredVelocity.getY() * controlDT, 0.0);
        
         FramePoint location = new FramePoint(ReferenceFrame.getWorldFrame());
         location.set(position.getX(), position.getY(), 0.0);
         
         bagOfBalls.setBall(location);
         
         HeadingAndVelocityEvaluationEvent evaluationEvent = script.getEvaluationEvent();
         seenEvents[evaluationEvent.ordinal()] = true;
         
         time = time + controlDT;
         
         if (scs != null)
         {
            scs.setTime(time);
            scs.tickAndUpdate();
         }
      }
      
      for (boolean seenEvent : seenEvents)
      {
         assertTrue(seenEvent);
      }
      
      // Ensure that the maximum accelerations are within reasonable limits:
      
      double[] maxHeadingChanges = findMaxChange(desiredHeadings);
      double[] maxVelocityChanges = findMaxChange(desiredVelocities);
      
      for (int i=0; i<2; i++)
      {
         maxHeadingChanges[i] /= controlDT;
         maxVelocityChanges[i] /= controlDT;
      }
      
//      System.out.println("maxHeadingChanges = " + maxHeadingChanges[0] + ", " + maxHeadingChanges[1]);
//      System.out.println("maxVelocityChanges = " + maxVelocityChanges[0] + ", " + maxVelocityChanges[1]);
//      System.out.println("desiredHeadingControlModule.getMaxHeadingDot() = " + desiredHeadingControlModule.getMaxHeadingDot());
//      System.out.println("script.getAcceleration() = " + script.getAcceleration());
      
      assertTrue(maxHeadingChanges[0] < desiredHeadingControlModule.getMaxHeadingDot() * 1.05);
      assertTrue(maxHeadingChanges[1] < desiredHeadingControlModule.getMaxHeadingDot() * 1.05);
      
      assertTrue(maxVelocityChanges[0] < script.getAcceleration() * 2.0);
      assertTrue(maxVelocityChanges[1] < script.getAcceleration() * 2.0);
      
      if (SHOW_GUI) sleepForever();
   }
   
   private double[] findMaxChange(ArrayList<FrameVector2d> frameVectors)
   {
      double[] ret = new double[]{0.0, 0.0};
      
      FrameVector2d difference = new FrameVector2d(frameVectors.get(0).getReferenceFrame());
      
      FrameVector2d previousVector = frameVectors.get(0);
      
      for (int i=1; i<frameVectors.size(); i++)
      {
         FrameVector2d nextVector = frameVectors.get(i);
         
         difference.set(nextVector);
         difference.sub(previousVector);
         
         if (Math.abs(difference.getX()) > ret[0])
         {
            ret[0] = difference.getX();
//            System.out.println(i + ": difference = " + difference);
         }
         
         if (Math.abs(difference.getY()) > ret[1])
         {
            ret[1] = difference.getY();
//            System.out.println(i + ": difference = " + difference);
         }
         
         previousVector = nextVector;
      }
      
      return ret;
   }
   
   private void sleepForever()
   {
      while(true)
      {
         sleep(1.0);
      }
   }
   
   private void sleep(double time)
   {
      try
      {
         Thread.sleep((long) (time * 1000));
      } 
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
   }
   
   private SimulationConstructionSet setupAndStartSCS(YoVariableRegistry registryToWatch, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, double controlDT)
   {
      Robot robot = new Robot("robot");
      
      robot.getRobotsYoVariableRegistry().addChild(registryToWatch);
      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setDT(controlDT, 1);
      
      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      
      Thread thread = new Thread(scs);
      thread.start();
      
      return scs;
   }

}
