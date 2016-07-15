package us.ihmc.robotics.math.frames;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class YoFramePointInMultipleFramesTest
{
   private static final Random random = new Random(1516351L);

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final ReferenceFrame frameA = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("frameA", worldFrame,
         RigidBodyTransform.generateRandomTransform(random));
   private static final ReferenceFrame frameB = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("frameB", worldFrame,
         RigidBodyTransform.generateRandomTransform(random));
   private static final ReferenceFrame frameC = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("frameC", worldFrame,
         RigidBodyTransform.generateRandomTransform(random));

   private static final ReferenceFrame[] allFrames = new ReferenceFrame[] { worldFrame, frameA, frameB, frameC };

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testConstructor()
   {
      YoVariableRegistry registry = new YoVariableRegistry("youhou");
      try
      {
         new YoFramePointInMultipleFrames("blop1", registry, allFrames);
      }
      catch (Exception e)
      {
         fail("Could not create " + YoFramePointInMultipleFrames.class.getSimpleName());
      }

      try
      {
         new YoFramePointInMultipleFrames("blop2", registry);
         fail("Should have thrown an exception");
      }
      catch (Exception e)
      {
         // Good
      }

      try
      {
         new YoFramePointInMultipleFrames("blop3", registry, new ReferenceFrame[]{});
         fail("Should have thrown an exception");
      }
      catch (Exception e)
      {
         // Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testRegisterFrame()
   {
      ArrayList<ReferenceFrame> referenceFrames = new ArrayList<ReferenceFrame>();
      
      YoVariableRegistry registry = new YoVariableRegistry("youhou");
      YoFramePointInMultipleFrames yoFramePointInMultipleFrames = new YoFramePointInMultipleFrames("blop", registry, worldFrame);
      
      assertEquals(1, yoFramePointInMultipleFrames.getNumberOfReferenceFramesRegistered());
      
      yoFramePointInMultipleFrames.getRegisteredReferenceFrames(referenceFrames);
      
      assertEquals(1, referenceFrames.size());
      assertEquals(worldFrame, referenceFrames.get(0));
      
//      try
//      {
         yoFramePointInMultipleFrames.registerReferenceFrame(worldFrame);
//         fail("Should have thrown an exception");
//      }
//      catch (Exception e)
//      {
//         // Good
//      }
      
      yoFramePointInMultipleFrames.registerReferenceFrame(frameA);

      assertEquals(2, yoFramePointInMultipleFrames.getNumberOfReferenceFramesRegistered());
      
      referenceFrames.clear();
      yoFramePointInMultipleFrames.getRegisteredReferenceFrames(referenceFrames);
      
      assertEquals(2, referenceFrames.size());
      assertEquals(worldFrame, referenceFrames.get(0));
      assertEquals(frameA, referenceFrames.get(1));
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testSetToZero()
   {
      YoVariableRegistry registry = new YoVariableRegistry("youhou");
      YoFramePointInMultipleFrames yoFramePointInMultipleFrames = new YoFramePointInMultipleFrames("blop", registry, allFrames);
      yoFramePointInMultipleFrames.switchCurrentReferenceFrame(worldFrame);
      
      FramePoint framePoint = new FramePoint(worldFrame);
      framePoint.setToZero(worldFrame);

      assertTrue(framePoint.epsilonEquals(yoFramePointInMultipleFrames.getFrameTuple(), 1e-10));
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testChangeToRegisteredFrame()
   {
      YoVariableRegistry registry = new YoVariableRegistry("youhou");
      YoFramePointInMultipleFrames yoFramePointInMultipleFrames = new YoFramePointInMultipleFrames("blop", registry, new ReferenceFrame[]{worldFrame, frameA});
      yoFramePointInMultipleFrames.switchCurrentReferenceFrame(worldFrame);
      
      FramePoint framePoint = new FramePoint(worldFrame);

      Point3d point = RandomTools.generateRandomPoint(random, 100.0, 100.0, 100.0);
      
      yoFramePointInMultipleFrames.set(point);
      framePoint.set(point);
      
      yoFramePointInMultipleFrames.changeFrame(frameA);
      framePoint.changeFrame(frameA);
      
      assertTrue(framePoint.epsilonEquals(yoFramePointInMultipleFrames.getFrameTuple(), 1e-10));
      
      try
      {
         yoFramePointInMultipleFrames.changeFrame(frameB);
         fail("Should have thrown an exception");
      }
      catch (Exception e)
      {
         // Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testSetIncludingFrame()
   {
      YoVariableRegistry registry = new YoVariableRegistry("youhou");
      YoFramePointInMultipleFrames yoFramePointInMultipleFrames = new YoFramePointInMultipleFrames("blop", registry, new ReferenceFrame[]{worldFrame, frameA});
      yoFramePointInMultipleFrames.switchCurrentReferenceFrame(worldFrame);
      
      FramePoint framePoint = FramePoint.generateRandomFramePoint(random, frameA, -100.0, 100.0, -100.0, 100.0, -100.0, 100.0);
      
      yoFramePointInMultipleFrames.setIncludingFrame(framePoint);
      assertTrue(framePoint.epsilonEquals(yoFramePointInMultipleFrames.getFrameTuple(), 1e-10));
      
      try
      {
         yoFramePointInMultipleFrames.setIncludingFrame(new FramePoint(frameC));
         fail("Should have thrown an exception");
      }
      catch (Exception e)
      {
         // Good
      }
   }
}
