package us.ihmc.robotics.math.frames;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.referenceFrames.TranslationReferenceFrame;

public class YoMultipleFramesHelperTest
{

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout=300000)
   public void testCommonUsageOfYoMultipleFramesHelper()
   {
      String namePrefix = "framesHelper";
      YoVariableRegistry registry = new YoVariableRegistry("framesHelper");

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      ReferenceFrame frameA = new TranslationReferenceFrame("frameA", worldFrame);
      ReferenceFrame frameB = new TranslationReferenceFrame("frameB", frameA);

      YoMultipleFramesHelper helper = new YoMultipleFramesHelper(namePrefix, registry, worldFrame, frameA);
      assertEquals(2, helper.getNumberOfReferenceFramesRegistered());

      assertTrue(worldFrame == helper.getCurrentReferenceFrame());
      assertTrue(worldFrame == helper.getReferenceFrame());
      helper.checkReferenceFrameMatch(worldFrame);

      assertTrue(helper.isReferenceFrameRegistered(worldFrame));
      assertTrue(helper.isReferenceFrameRegistered(frameA));
      assertFalse(helper.isReferenceFrameRegistered(frameB));

      helper.registerReferenceFrame(frameB);
      assertEquals(3, helper.getNumberOfReferenceFramesRegistered());

      List<ReferenceFrame> referenceFrames = new ArrayList<ReferenceFrame>();
      helper.getRegisteredReferenceFrames(referenceFrames);

      assertEquals(3, referenceFrames.size());
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout=300000)
   public void testRepeatFrames()
   {
      YoVariableRegistry registry = new YoVariableRegistry("framesHelper");

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      YoMultipleFramesHelper helper = new YoMultipleFramesHelper("framesHelperThree", registry, worldFrame, worldFrame);

      assertEquals("Should ignore repeat frames!", 1, helper.getNumberOfReferenceFramesRegistered());

      helper.registerReferenceFrame(worldFrame);
      assertEquals("Should ignore repeat frames!", 1, helper.getNumberOfReferenceFramesRegistered());
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout=300000)
   public void testExceptions()
   {
      YoVariableRegistry registry = new YoVariableRegistry("framesHelper");

      try
      {
         new YoMultipleFramesHelper("framesHelperOne", registry);
         fail("Need to have at least one reference frame when you create a YoMultipleFramesHelper");
      }
      catch (RuntimeException e)
      {
      }

      try
      {
         new YoMultipleFramesHelper("framesHelperTwo", registry, (ReferenceFrame) null);
         fail("The Reference Frames cannot be null");
      }
      catch (RuntimeException e)
      {
      }

      try
      {
         new YoMultipleFramesHelper("framesHelperTwoB", registry);
         fail("The Reference Frames cannot be null");
      }
      catch (RuntimeException e)
      {
      }

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      YoMultipleFramesHelper helper = new YoMultipleFramesHelper("framesHelperFour", registry, worldFrame);
      assertEquals(1, helper.getNumberOfReferenceFramesRegistered());

      ReferenceFrame unregisteredFrame = new TranslationReferenceFrame("unregistered", ReferenceFrame.getWorldFrame());
      try
      {
         helper.switchCurrentReferenceFrame(unregisteredFrame);
         fail("Cannot switch to an unregistered frame");
      }
      catch (RuntimeException e)
      {
      }
      assertEquals(worldFrame, helper.getCurrentReferenceFrame());
   }

}
