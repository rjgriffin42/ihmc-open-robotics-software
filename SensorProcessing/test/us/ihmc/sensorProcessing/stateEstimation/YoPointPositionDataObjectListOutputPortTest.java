package us.ihmc.sensorProcessing.stateEstimation;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointPositionDataObject;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

/**
 * @author twan
 *         Date: 4/27/13
 */
public class YoPointPositionDataObjectListOutputPortTest
{

	@AverageDuration
	@Test(timeout=300000)
   public void testRandom()
   {
      Random random  = new Random(1235561L);
      ControlFlowElement controlFlowElement = new NullControlFlowElement();
      YoVariableRegistry registry = new YoVariableRegistry("test");

      List<ReferenceFrame> frames = new ArrayList<ReferenceFrame>();
      int nFrames = 10;
      for (int i = 0; i < nFrames; i++)
      {
         RigidBodyTransform transformToParent = RandomTools.generateRandomTransform(random);
         ReferenceFrame frame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("frame" + i, ReferenceFrame.getWorldFrame(), transformToParent);
         frame.update();
         frames.add(frame);
      }

      AfterJointReferenceFrameNameMap referenceFrameMap = new AfterJointReferenceFrameNameMap(frames);
      YoPointPositionDataObjectListOutputPort outputPort = new YoPointPositionDataObjectListOutputPort(controlFlowElement, "test", referenceFrameMap, registry);
      int nTests = 100000;
      int nDataMax = 10;

      for (int i = 0; i < nTests; i++)
      {
         List<PointPositionDataObject> dataIn = createData(random, frames, nDataMax);
         outputPort.setData(dataIn);
         List<PointPositionDataObject> dataOut = outputPort.getData();
         verify(dataIn, dataOut);
      }

      int numberOfYoPointPositionDataObjects = outputPort.getNumberOfYoPointPositionDataObjects();
      assertTrue(numberOfYoPointPositionDataObjects < nFrames * nDataMax);
   }

   private List<PointPositionDataObject> createData(Random random, List<ReferenceFrame> frames, int nDataMax)
   {
      List<PointPositionDataObject> dataIn = new ArrayList<PointPositionDataObject>();

      int nData = random.nextInt(nDataMax);
      for (int j = 0; j < nData; j++)
      {
         PointPositionDataObject pointPositionDataObject = new PointPositionDataObject();
         int referenceFrameIndex = random.nextInt(frames.size());
         ReferenceFrame frame = frames.get(referenceFrameIndex);
         FramePoint measurementPointInBodyFrame = new FramePoint(frame, RandomTools.generateRandomVector(random));
         FramePoint measurementPointInWorldFrame = new FramePoint(ReferenceFrame.getWorldFrame(), RandomTools.generateRandomVector(random));
         boolean isPointPositionValid = true;
         pointPositionDataObject.set(measurementPointInBodyFrame, measurementPointInWorldFrame, isPointPositionValid);

         dataIn.add(pointPositionDataObject);
      }
      return dataIn;
   }

   private void verify(List<PointPositionDataObject> dataIn, List<PointPositionDataObject> dataOut)
   {
      if (dataIn.size() != dataOut.size())
         fail();

      for (PointPositionDataObject positionDataObjectIn : dataIn)
      {
         PointPositionDataObject matchInOut = null;
         for (PointPositionDataObject positionDataObjectOut : dataOut)
         {
            if (positionDataObjectIn.epsilonEquals(positionDataObjectOut, 0.0));
            {
               matchInOut = positionDataObjectIn;
               break;
            }
         }
         if (matchInOut == null)
            fail();
         else
            dataOut.remove(matchInOut);
      }
   }
}
