package us.ihmc.darpaRoboticsChallenge.posePlayback;

import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.Random;

import org.junit.Test;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPose;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseInterpolator;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseSequence;
import us.ihmc.commonWalkingControlModules.posePlayback.PlaybackPoseSequenceReader;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public abstract class PlaybackPoseInterpolatorTest implements MultiRobotTestInterface
{
   private static final boolean SHOW_GUI = true;
   

   @Test
   public void testMoveElbowExample()
   {
      DRCRobotModel robotModel = getRobotModel();

      DRCRobotJointMap jointMap = robotModel.getJointMap();
      JaxbSDFLoader sdfLoader = robotModel.getJaxbSDFLoader();
      
      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);
      SDFRobot sdfRobot = sdfLoader.createRobot(jointMap, false);

      double delay = 0.3;
      double trajectoryTime = 1.0;
      
      PlaybackPoseSequence sequence = PosePlaybackExampleSequence.createExamplePoseSequenceMoveArm(fullRobotModel, delay, trajectoryTime);
      playASequence(sdfRobot, sequence);
   }
   
   @Test
   public void testRandomExample()
   {
      DRCRobotModel robotModel = getRobotModel();

      DRCRobotJointMap jointMap = robotModel.getJointMap();
      JaxbSDFLoader sdfLoader = robotModel.getJaxbSDFLoader();
      
      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);
      SDFRobot sdfRobot = sdfLoader.createRobot(jointMap, false);
      
      int numberOfPoses = 5;
      double delay = 0.3;
      double trajectoryTime = 1.0;
      
      Random random = new Random(1776L);
      PlaybackPoseSequence sequence = PosePlaybackExampleSequence.createRandomPlaybackPoseSequence(random, fullRobotModel, numberOfPoses, delay, trajectoryTime);

      //sequence.writeToOutputStream(fullRobotModel, System.out);

      playASequence(sdfRobot, sequence);
      
   }
   
//   @Test
//   public void testExampleOne()
//   {
//      DRCRobotJointMap jointMap = robotModel.getJointMap();
//      JaxbSDFLoader sdfLoader = DRCRobotSDFLoader.loadDRCRobot(jointMap, false);
//      
//      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);
//      SDFRobot sdfRobot = sdfLoader.createRobot(jointMap, false);
//      
//      PosePlaybackRobotPoseSequence sequence = PosePlaybackExampleSequence.createExampleSequenceFourPoses(fullRobotModel);
//      playASequence(sdfRobot, sequence);
//   }

   @Test
   public void testLoadingAndPlayingASequence()
   {
      DRCRobotModel robotModel = getRobotModel();

      DRCRobotJointMap jointMap = robotModel.getJointMap();
      JaxbSDFLoader sdfLoader = robotModel.getJaxbSDFLoader();
      
      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);
      SDFRobot sdfRobot = sdfLoader.createRobot(jointMap, false);
      
      PlaybackPoseSequence sequence = new PlaybackPoseSequence(fullRobotModel);
      PlaybackPoseSequenceReader.appendFromFile(sequence, "testSequence2.poseSequence");
      playASequence(sdfRobot, sequence);
   }

   @Test
   public void testLoadingAndPlayingAnotherSequence()
   {
      DRCRobotModel robotModel = getRobotModel();

      DRCRobotJointMap jointMap = robotModel.getJointMap();
      JaxbSDFLoader sdfLoader = robotModel.getJaxbSDFLoader();
      
      SDFFullRobotModel fullRobotModel = sdfLoader.createFullRobotModel(jointMap);
      SDFRobot sdfRobot = sdfLoader.createRobot(jointMap, false);
      
      PlaybackPoseSequence sequence = new PlaybackPoseSequence(fullRobotModel);
      PlaybackPoseSequenceReader.appendFromFile(sequence, "tenPoses.poseSequence");

      System.out.println(sequence);
      playASequence(sdfRobot, sequence);
   }


   public void playASequence(SDFRobot sdfRobot, PlaybackPoseSequence sequence)
   {
      YoVariableRegistry registry = new YoVariableRegistry("PosePlaybackSmoothPoseInterpolatorTest");
      PlaybackPoseInterpolator interpolator = new PlaybackPoseInterpolator(registry);

      double simulateDT = 0.005;
      
      SimulationConstructionSet scs = null;
      if (SHOW_GUI)
      {
         scs = new SimulationConstructionSet(sdfRobot);
         int recordFrequency = 1;
         scs.setDT(simulateDT, recordFrequency);
         scs.addYoVariableRegistry(registry);
         scs.startOnAThread();
      }

      double startTime = 1.7;
      double time = startTime;

      interpolator.startSequencePlayback(sequence, startTime);


      PlaybackPose previousPose = null;
      while (!interpolator.isDone())
      {
         time = time + simulateDT;
         scs.setTime(time);

         PlaybackPose pose = interpolator.getPose(time);

//         System.out.println(pose);

         if (SHOW_GUI)
         {
            pose.setRobotAtPose(sdfRobot);
            scs.tickAndUpdate();
         }

         assertSmallPoseDifference(pose, previousPose);
         previousPose = pose;
      }


      if (SHOW_GUI)
      {
         ThreadTools.sleepForever();
      }
   }


   private void assertSmallPoseDifference(PlaybackPose pose, PlaybackPose previousPose)
   {
      if (pose == null)
         return;
      if (previousPose == null)
         return;

      boolean smallDifference = pose.epsilonEquals(previousPose, 100.0, 100.0); //1e-2, 1.0);
      if (!smallDifference)
      {
         System.out.println("pose = " + pose);
         System.out.println("previousPose = " + previousPose);
      }
      assertTrue(smallDifference);
   }

}
