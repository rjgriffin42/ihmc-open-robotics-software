package us.ihmc.commonWalkingControlModules.trajectories;

import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.sensorProcessing.pointClouds.combinationQuadTreeOctTree.QuadTreeHeightMapGeneratorTools;
import us.ihmc.sensorProcessing.pointClouds.combinationQuadTreeOctTree.QuadTreeHeightMapInterface;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.dataStructures.DoubleHashHeightMap;
import us.ihmc.utilities.math.dataStructures.HeightMapWithPoints;
import us.ihmc.utilities.math.geometry.BoundingBox2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.BagOfBalls;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

/**
 * Created by agrabertilton on 2/11/15.
 */
public class ConvexHullTrajectoryGeneratorTest
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables(); 
   
   @AverageDuration(duration = 0.1)
   @Test(timeout = 300000)
   public void testWithHeightMap()
   {
      boolean VISUALIZE = simulationTestingParameters.getKeepSCSUp(); //don't check in true

      CombinedTerrainObject3D groundProfile = createWalledTerrainProfile();
      double centerX = 0;
      double centerY = 0;
      double halfWidth = 1.0;
      double resolution = 0.02;
      BoundingBox2d rangeOfPointsToTest = new BoundingBox2d(centerX - halfWidth, centerY - halfWidth, centerX + halfWidth, centerY + halfWidth);
      QuadTreeHeightMapInterface groundMap = QuadTreeHeightMapGeneratorTools.createHeightMap(groundProfile, rangeOfPointsToTest, resolution);


      double horizontalBuffer = .1; //10cm
      double verticalBuffer = 0.05; //5cm
      double pathWidth = 0.12; //12cm

      ConvexHullTrajectoryGenerator generator = new ConvexHullTrajectoryGenerator(horizontalBuffer, verticalBuffer, pathWidth);
      FramePose startPose = new FramePose(ReferenceFrame.getWorldFrame());
      FramePose endPose = new FramePose(ReferenceFrame.getWorldFrame());

      Point3d startPosition = new Point3d(0.0, 0.0, 0.0);
      Quat4d startOrientation = new Quat4d(0.0, 0.0, 0.0, 1.0);
      startPose.setPose(startPosition, startOrientation);

      Point3d endPosition = new Point3d(0.5, 0.0, 0.0);
      Quat4d endOrientation = new Quat4d(0.0, 0.0, 0.0, 1.0);
      endPose.setPose(endPosition, endOrientation);

      List<FramePoint> trajectoryPoints = generator.computeSwingTrajectoryPoints(startPose, endPose, groundMap);

      if (VISUALIZE)
      {
         SimulationConstructionSet scs = createSCSNullRobotInstance(groundProfile.getLinkGraphics());
         visualizeTrajectoryPoints(scs, trajectoryPoints);
         ThreadTools.sleepForever();
      }
   }

   @AverageDuration(duration = 0.1)
   @Test(timeout = 300000)
   public void testSmallXAxisDistanceWithoutHeightMap()
   {
      boolean VISUALIZE = false;
      double horizontalBuffer = .1; //10cm
      double verticalBuffer = 0.05; //5cm
      double pathWidth = 0.12; //12cm

      ConvexHullTrajectoryGenerator generator = new ConvexHullTrajectoryGenerator(horizontalBuffer, verticalBuffer, pathWidth);
      FramePose startPose = new FramePose(ReferenceFrame.getWorldFrame());
      FramePose endPose = new FramePose(ReferenceFrame.getWorldFrame());
      HeightMapWithPoints groundMap = new DoubleHashHeightMap(0.01);

      Point3d startPosition = new Point3d(0.0, 0.0, 0.0);
      Quat4d startOrientation = new Quat4d(0.0, 0.0, 0.0, 1.0);
      startPose.setPose(startPosition, startOrientation);

      Point3d endPosition = new Point3d(startPosition.x + horizontalBuffer, startPosition.y, startPosition.z);
      Quat4d endOrientation = startOrientation;
      endPose.setPose(endPosition, endOrientation);

      List<FramePoint> trajectoryPoints = generator.computeSwingTrajectoryPoints(startPose, endPose, groundMap);
      assertTrue(trajectoryPoints.size() == 3);
      assertTrue(trajectoryPoints.get(0).epsilonEquals(startPosition, 1e-13));

      Point3d pointMiddle = new Point3d(startPosition);
      pointMiddle.add(endPosition);
      pointMiddle.scale(0.5);
      pointMiddle.add(new Point3d(0.0, 0.0, verticalBuffer));
      assertTrue(trajectoryPoints.get(1).epsilonEquals(pointMiddle, 1e-13));

      assertTrue(trajectoryPoints.get(2).epsilonEquals(endPosition, 1e-13));


      if (VISUALIZE)
      {
         visualizeTrajectoryPoints(createSCSNullRobotInstance(), trajectoryPoints);
         ThreadTools.sleepForever();
      }
   }

   @AverageDuration(duration = 0.1)
   @Test(timeout = 300000)
   public void testBigXAxisDistanceWithoutHeightMap()
   {
      boolean VISUALIZE = false;
      double horizontalBuffer = .1; //10cm
      double verticalBuffer = 0.05; //5cm
      double pathWidth = 0.12; //12cm

      ConvexHullTrajectoryGenerator generator = new ConvexHullTrajectoryGenerator(horizontalBuffer, verticalBuffer, pathWidth);
      FramePose startPose = new FramePose(ReferenceFrame.getWorldFrame());
      FramePose endPose = new FramePose(ReferenceFrame.getWorldFrame());
      HeightMapWithPoints groundMap = new DoubleHashHeightMap(0.01);

      Point3d startPosition = new Point3d(0.0, 0.0, 0.0);
      Quat4d startOrientation = new Quat4d(0.0, 0.0, 0.0, 1.0);
      startPose.setPose(startPosition, startOrientation);

      Point3d endPosition = new Point3d(startPosition.x + 3* horizontalBuffer, startPosition.y, startPosition.z);
      Quat4d endOrientation = startOrientation;
      endPose.setPose(endPosition, endOrientation);

      List<FramePoint> trajectoryPoints = generator.computeSwingTrajectoryPoints(startPose, endPose, groundMap);
      assertTrue(trajectoryPoints.size() == 4);
      assertTrue(trajectoryPoints.get(0).epsilonEquals(startPosition, 1e-13));

      Point3d pointA = new Point3d(startPosition.x + horizontalBuffer, startPosition.y, startPosition.z + verticalBuffer);
      assertTrue(trajectoryPoints.get(1).epsilonEquals(pointA, 1e-13));

      Point3d pointB = new Point3d(endPosition.x - horizontalBuffer, endPosition.y, endPosition.z + verticalBuffer);
      assertTrue(trajectoryPoints.get(2).epsilonEquals(pointB, 1e-13));

      assertTrue(trajectoryPoints.get(3).epsilonEquals(endPosition, 1e-13));


      if (VISUALIZE)
      {
         visualizeTrajectoryPoints(createSCSNullRobotInstance(), trajectoryPoints);
         ThreadTools.sleepForever();
      }
   }

   private SimulationConstructionSet createSCSNullRobotInstance(){
      return createSCSNullRobotInstance(null);
   }

   private SimulationConstructionSet createSCSNullRobotInstance(Graphics3DObject linkGraphics){
      Robot nullRobot = new Robot("FootstepVisualizerRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(nullRobot);

      if (linkGraphics != null)
      {
         scs.setGroundVisible(false);
         scs.addStaticLinkGraphics(linkGraphics);
      }

      scs.setDT(1, 1);
      return scs;
   }

   private void visualizeTrajectoryPoints(SimulationConstructionSet scs, List<FramePoint> trajectoryPoints){
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
      YoVariableRegistry registry = new YoVariableRegistry("SwingRegistry");
      BagOfBalls bagOfBalls = new BagOfBalls(registry, yoGraphicsListRegistry);

      scs.addYoGraphicsListRegistry(yoGraphicsListRegistry);
      scs.addYoVariableRegistry(registry);
      scs.tickAndUpdate();

      for (FramePoint point : trajectoryPoints)
      {
         bagOfBalls.setBallLoop(point);
         scs.tickAndUpdate();
      }
      scs.startOnAThread();
   }

   private CombinedTerrainObject3D createWalledTerrainProfile()
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("stairs");

      AppearanceDefinition color = YoAppearance.DarkGray();
      combinedTerrainObject.addBox(-100.0, -100.0, 100.0, 100.0, 0.001, color);

      combinedTerrainObject.addBox(0.10, -0.5, 0.2, 0.5, 0.1);

      combinedTerrainObject.addBox(0.35, -0.5, 0.4, 0.5, 0.05);
      return combinedTerrainObject;
   }
}
