package us.ihmc.sensorProcessing.controlFlowPorts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class YoFrameQuaternionControlFlowOutputPortTest
{

   private static final double EPS = 1e-15;

	@EstimatedDuration
	@Test(timeout=300000)
   public void simpleWritingReadingTest()
   {
      ControlFlowElement controlFlowElement = new NullControlFlowElement();
      
      String namePrefix = "test";
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();
      YoVariableRegistry registry = new YoVariableRegistry("blop");
      
      YoFrameQuaternionControlFlowOutputPort controlFlowOutputPort = new YoFrameQuaternionControlFlowOutputPort(controlFlowElement, namePrefix, frame, registry);

      assertEquals(namePrefix, controlFlowOutputPort.getName());
      
      Random rand = new Random(1567);
      
      for (int i = 0; i < 1000; i++)
      {
         FrameOrientation dataIn = new FrameOrientation(frame, RandomTools.generateRandomQuaternion(rand));
         controlFlowOutputPort.setData(dataIn);
         FrameOrientation dataOut = controlFlowOutputPort.getData();

         assertTrue("Expected: " + dataIn + ", but was: " + dataOut, dataIn.getQuaternionCopy().epsilonEquals(dataOut.getQuaternionCopy(), EPS));
      }
   }

}
