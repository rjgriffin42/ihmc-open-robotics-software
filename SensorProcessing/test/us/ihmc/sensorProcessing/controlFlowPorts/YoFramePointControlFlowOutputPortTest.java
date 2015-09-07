package us.ihmc.sensorProcessing.controlFlowPorts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.tools.random.RandomTools;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class YoFramePointControlFlowOutputPortTest
{

   private static final double EPS = 1e-17;

	@EstimatedDuration(duration = 0.2)
	@Test(timeout = 30000)
   public void simpleWritingReadingTest()
   {
      ControlFlowElement controlFlowElement = new NullControlFlowElement();
      
      String namePrefix = "test";
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();
      YoVariableRegistry registry = new YoVariableRegistry("blop");
      
      YoFramePointControlFlowOutputPort controlFlowOutputPort = new YoFramePointControlFlowOutputPort(controlFlowElement, namePrefix, frame, registry);

      assertEquals(namePrefix, controlFlowOutputPort.getName());
      
      Random rand = new Random(1567);
      
      for (int i = 0; i < 1000; i++)
      {
         Vector3d vector = RandomTools.generateRandomVector(rand, RandomTools.generateRandomDouble(rand, Double.MIN_VALUE, Double.MAX_VALUE));
         FramePoint dataIn = new FramePoint(frame, vector);
         controlFlowOutputPort.setData(dataIn);
         FramePoint dataOut = controlFlowOutputPort.getData();

         assertTrue("Expected: " + dataIn + ", but was: " + dataOut, dataIn.epsilonEquals(dataOut, EPS));
      }
   }

}
