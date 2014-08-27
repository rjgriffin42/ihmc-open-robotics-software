package us.ihmc.sensorProcessing.controlFlowPorts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class YoFrameVectorControlFlowOutputPortTest
{

   private static final double EPS = 1e-17;

   @Test
   public void simpleWritingReadingTest()
   {
      ControlFlowElement controlFlowElement = new NullControlFlowElement();
      
      String namePrefix = "test";
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();
      YoVariableRegistry registry = new YoVariableRegistry("blop");
      
      YoFrameVectorControlFlowOutputPort controlFlowOutputPort = new YoFrameVectorControlFlowOutputPort(controlFlowElement, namePrefix, frame, registry);

      assertEquals(namePrefix, controlFlowOutputPort.getName());
      
      Random rand = new Random(1567);
      
      for (int i = 0; i < 1000; i++)
      {
         Vector3d vector = RandomTools.generateRandomVector(rand, RandomTools.generateRandomDouble(rand, Double.MIN_VALUE, Double.MAX_VALUE));
         FrameVector dataIn = new FrameVector(frame, vector);
         controlFlowOutputPort.setData(dataIn);
         FrameVector dataOut = controlFlowOutputPort.getData();

         assertTrue("Expected: " + dataIn + ", but was: " + dataOut, dataIn.epsilonEquals(dataOut, EPS));
      }
   }

}
