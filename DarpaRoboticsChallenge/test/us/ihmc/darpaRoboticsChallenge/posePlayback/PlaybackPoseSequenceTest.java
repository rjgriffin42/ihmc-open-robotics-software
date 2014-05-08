package us.ihmc.darpaRoboticsChallenge.posePlayback;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Random;

import org.junit.Test;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseSequence;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseSequenceReader;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseSequenceWriter;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;

public abstract class PlaybackPoseSequenceTest implements MultiRobotTestInterface
{
   @Test
   public void testReadAndWriteWithRandomSequence()
   {

      DRCRobotJointMap jointMap = getRobotModel().getJointMap();
      JaxbSDFLoader sdfLoader = getRobotModel().getJaxbSDFLoader();

      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);

      int numberOfPoses = 5;
      double delay = 0.3;
      double trajectoryTime = 1.0;

      Random random = new Random(1776L);
      PlaybackPoseSequence sequence = PosePlaybackExampleSequence.createRandomPlaybackPoseSequence(random, fullRobotModel, numberOfPoses, delay,
                                                  trajectoryTime);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PlaybackPoseSequenceWriter.writeToOutputStream(sequence, outputStream);

      String outputAsString = outputStream.toString();

//      System.out.println(outputAsString);

      PlaybackPoseSequence sequenceTwo = new PlaybackPoseSequence(fullRobotModel);

      StringReader reader = new StringReader(outputAsString);
      PlaybackPoseSequenceReader.appendFromFile(sequenceTwo, reader);

      double jointEpsilon = 1e-7;
      double timeEpsilon = 1e-7;
      assertTrue(sequence.epsilonEquals(sequenceTwo, jointEpsilon, timeEpsilon));

   }

}
