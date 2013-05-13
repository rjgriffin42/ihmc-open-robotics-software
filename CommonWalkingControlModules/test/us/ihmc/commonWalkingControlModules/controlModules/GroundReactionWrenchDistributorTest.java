package us.ihmc.commonWalkingControlModules.controlModules;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.WrenchDistributorTools;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.FlatGroundPlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.ContactPointGroundReactionWrenchDistributor;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributor;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributorInputData;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributorOutputData;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class GroundReactionWrenchDistributorTest
{
   private static final boolean VISUALIZE = false;
   private static boolean DEBUG = false;

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
   public void testSimpleWrenchDistributionWithGeometricFlatGroundDistributor()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      GroundReactionWrenchDistributor distributor = new GeometricFlatGroundReactionWrenchDistributor(parentRegistry, null);

      Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.0);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      testSimpleWrenchDistribution(centerOfMassFrame, distributor, parentRegistry);
   }

   @Test
   public void testSimpleWrenchDistributionWithLeeGoswamiDistributor()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.0);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      GroundReactionWrenchDistributor distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry, 1.0);
      testSimpleWrenchDistribution(centerOfMassFrame, distributor, parentRegistry);
   }

   @Test
   public void testSimpleWrenchDistributionWithContactPointDistributor()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.0);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      GroundReactionWrenchDistributor distributor = createContactPointDistributor(parentRegistry, centerOfMassFrame);
      testSimpleWrenchDistribution(centerOfMassFrame, distributor, parentRegistry);
   }

   private ContactPointGroundReactionWrenchDistributor createContactPointDistributor(YoVariableRegistry parentRegistry, PoseReferenceFrame centerOfMassFrame)
   {
      ContactPointGroundReactionWrenchDistributor distributor = new ContactPointGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry);

      double[] diagonalCWeights = new double[]
      {
         1.0, 1.0, 1.0, 1.0, 1.0, 1.0
      };
      double epsilonRho = 0.0;
      distributor.setWeights(diagonalCWeights, 0.0, epsilonRho);

      return distributor;
   }

   @Test
   public void testRandomFlatGroundExamplesWithGeometricFlatGroundDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      GroundReactionWrenchDistributor distributor = new GeometricFlatGroundReactionWrenchDistributor(parentRegistry, null);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = false;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, false, centerOfMassFrame, distributor, 1.0, parentRegistry);
   }

   @Test
   public void testRandomFlatGroundExamplesWithViableMomentSolutionsWithGeometricFlatGroundDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      GroundReactionWrenchDistributor distributor = new GeometricFlatGroundReactionWrenchDistributor(parentRegistry, null);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = true;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, false, centerOfMassFrame, distributor, 1.0, parentRegistry);
   }

   @Test
   public void testRandomFlatGroundExamplesWithLeeGoswamiDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      double rotationalCoefficientOfFrictionMultiplier = 1.0;
      LeeGoswamiGroundReactionWrenchDistributor distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry,
                                                                 rotationalCoefficientOfFrictionMultiplier);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = false;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, false, centerOfMassFrame, distributor,
                                   rotationalCoefficientOfFrictionMultiplier, parentRegistry);
   }

   @Test
   public void testRandomFlatGroundExamplesWithViableMomentSolutionsWithLeeGoswamiDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      double rotationalCoefficientOfFrictionMultiplier = 1.0;
      LeeGoswamiGroundReactionWrenchDistributor distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry,
                                                                 rotationalCoefficientOfFrictionMultiplier);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = true;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, false, centerOfMassFrame, distributor,
                                   rotationalCoefficientOfFrictionMultiplier, parentRegistry);
   }

   @Test
   public void testRandomFlatGroundExamplesWithContactPointDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      GroundReactionWrenchDistributor distributor = createContactPointDistributor(parentRegistry, centerOfMassFrame);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = false;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, true, centerOfMassFrame, distributor, 1.0, parentRegistry);
   }

   @Test
   public void testRandomFlatGroundExamplesWithViableMomentSolutionsWithContactPointDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
      GroundReactionWrenchDistributor distributor = createContactPointDistributor(parentRegistry, centerOfMassFrame);

      boolean verifyForcesAreInsideFrictionCones = false;
      boolean feasibleMomentSolutions = true;
      testRandomFlatGroundExamples(verifyForcesAreInsideFrictionCones, feasibleMomentSolutions, true, centerOfMassFrame, distributor, 1.0, parentRegistry);
   }

   @Test
   public void testSimpleNonFlatGroundExampleWithLeeGoswamiDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.0);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      GroundReactionWrenchDistributor distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry, 1.0);
      testNonFlatGroundExample(centerOfMassFrame, distributor, parentRegistry);
   }

   @Test
   public void testSimpleNonFlatGroundExampleWithContactPointDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.0);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      GroundReactionWrenchDistributor distributor = createContactPointDistributor(parentRegistry, centerOfMassFrame);
      testNonFlatGroundExample(centerOfMassFrame, distributor, parentRegistry);
   }

   @Test
   public void testTroublesomeExamplesWithLeeGoswamiDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      GroundReactionWrenchDistributor distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, parentRegistry, 1.0);
      testTroublesomeExampleOne(centerOfMassFrame, distributor, parentRegistry);
      testTroublesomeExampleTwo(centerOfMassFrame, distributor, parentRegistry);
   }

   @Test
   public void testTroublesomeExamplesWithGeometricFlatGroundDistributor()
   {
      Point3d centerOfMassPoint3d = new Point3d(0.2, 0.1, 1.07);
      PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);

      YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");

      GroundReactionWrenchDistributor distributor = new GeometricFlatGroundReactionWrenchDistributor(parentRegistry, null);
      testTroublesomeExampleOne(centerOfMassFrame, distributor, parentRegistry);
      testTroublesomeExampleTwo(centerOfMassFrame, distributor, parentRegistry);
   }

// @Test
// public void testSimpleFourFeetFlatGroundExampleWithLeeGoswamiDistributor()
// {
//    Point3d centerOfMassPoint3d = new Point3d(0.0, 0.0, 1.00);
//    PoseReferenceFrame centerOfMassFrame = createCenterOfMassFrame(centerOfMassPoint3d);
//
//    int nSupportVectors = 4;
//    YoVariableRegistry parentRegistry = new YoVariableRegistry("registry");
//
//    GroundReactionWrenchDistributorInterface distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, nSupportVectors, parentRegistry);
//    testFourFeetExample(centerOfMassFrame, distributor, parentRegistry);
// }

   private void testSimpleWrenchDistribution(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry)
   {
      double coefficientOfFriction = 1.0;
      double footLength = 0.3;
      double footWidth = 0.15;
      Point3d leftMidfootLocation = new Point3d(0.0, 0.5, 0.0);
      FlatGroundPlaneContactState leftFootContactState = new FlatGroundPlaneContactState(footLength, footWidth, leftMidfootLocation, coefficientOfFriction);

      Point3d rightMidfootLocation = new Point3d(0.0, -0.5, 0.0);
      FlatGroundPlaneContactState rightFootContactState = new FlatGroundPlaneContactState(footLength, footWidth, rightMidfootLocation, coefficientOfFriction);

      simpleTwoFootTest(centerOfMassFrame, distributor, parentRegistry, leftFootContactState, rightFootContactState, coefficientOfFriction);
   }

   private void testTroublesomeExampleOne(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry)
   {
      double[][] contactPointLocationsOne = new double[][]
      {
         {-0.18748618308569526, 0.4993664950063565}, {-0.18748618308569526, 0.3107721369458269}, {-0.4272566950489601, 0.3107721369458269},
         {-0.4272566950489601, 0.4993664950063565}
      };

      double[][] contactPointLocationsTwo = new double[][]
      {
         {-0.1133318132874206, -0.49087142187157046}, {-0.1133318132874206, -0.6010308443768181}, {-0.33136117053395964, -0.6010308443768181},
         {-0.33136117053395964, -0.49087142187157046}
      };

      testTroublesomeExample(centerOfMassFrame, distributor, contactPointLocationsOne, contactPointLocationsTwo,
                             new Vector3d(-2.810834363235027, -9.249454803442402, 76.9108583580996),
                             new Vector3d(-36.06373668027517, 39.43047643829655, 62.59792486812425));
   }

   private void testTroublesomeExampleTwo(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry)
   {
      double[][] contactPointLocationsOne = new double[][]
      {
         {0.9795417664487718, 0.5885587484763559}, {0.9795417664487718, 0.4381570540985258}, {0.8166018745568961, 0.4381570540985258},
         {0.8166018745568961, 0.5885587484763559}
      };

      double[][] contactPointLocationsTwo = new double[][]
      {
         {-0.8707570392292157, -0.6236048380817167}, {-0.8707570392292157, -0.8067104155445308}, {-1.0414910774920054, -0.8067104155445308},
         {-1.0414910774920054, -0.6236048380817167}
      };

      testTroublesomeExample(centerOfMassFrame, distributor, contactPointLocationsOne, contactPointLocationsTwo,
                             new Vector3d(-36.955722108464826, 36.82778916715679, 127.13798022142323),
                             new Vector3d(-5.664078207221138, 95.25549492340134, -20.334006511537165));
   }

   private void testTroublesomeExample(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, double[][] contactPointLocationsOne,
           double[][] contactPointLocationsTwo, Vector3d linearPart, Vector3d angularPart)
   {
      SpatialForceVector desiredNetSpatialForceVector = new SpatialForceVector(centerOfMassFrame, linearPart, angularPart);

      double coefficientOfFriction = 0.87;
      FlatGroundPlaneContactState contactStateOne = new FlatGroundPlaneContactState(contactPointLocationsOne, coefficientOfFriction);
      FlatGroundPlaneContactState contactStateTwo = new FlatGroundPlaneContactState(contactPointLocationsTwo, coefficientOfFriction);

      ArrayList<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();

      contactStates.add(contactStateOne);
      contactStates.add(contactStateTwo);


      GroundReactionWrenchDistributorInputData inputData = new GroundReactionWrenchDistributorInputData();

      inputData.addPlaneContact(contactStateOne);
      inputData.addPlaneContact(contactStateTwo);


      GroundReactionWrenchDistributorVisualizer visualizer = null;
      SimulationConstructionSet scs = null;

      if (VISUALIZE)
      {
         Robot robot = new Robot("null");

         DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
         scs = new SimulationConstructionSet(robot);

         int maxNumberOfFeet = 2;    // 6;
         int maxNumberOfVertices = 10;
         visualizer = new GroundReactionWrenchDistributorVisualizer(maxNumberOfFeet, maxNumberOfVertices, scs.getRootRegistry(),
                 dynamicGraphicObjectsListRegistry);

         dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);

         addCoordinateSystem(scs);
         scs.startOnAThread();
      }

//    System.out.println("desiredNetSpatialForceVector = " + desiredNetSpatialForceVector);
      inputData.setSpatialForceVectorAndUpcomingSupportSide(desiredNetSpatialForceVector, null);

      GroundReactionWrenchDistributorOutputData distributedWrench = new GroundReactionWrenchDistributorOutputData();
      distributor.solve(distributedWrench, inputData);

//    System.out.println("desiredNetSpatialForceVector = " + desiredNetSpatialForceVector);
      verifyForcesAreInsideFrictionCones(distributedWrench, contactStates, coefficientOfFriction);
      verifyWrenchesSumToExpectedTotal(centerOfMassFrame, desiredNetSpatialForceVector, contactStates, distributedWrench, 1e-7, true);

      if (VISUALIZE)
      {
         visualizer.update(scs, distributedWrench, centerOfMassFrame, contactStates, desiredNetSpatialForceVector);

//       deleteFirstDataPointAndCropData(scs);
         ThreadTools.sleepForever();
      }
   }

   private void testRandomFlatGroundExamples(boolean verifyForcesAreInsideFrictionCones, boolean feasibleMomentSolutions, boolean contactPointDistributor,
           ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, double rotationalCoefficientOfFrictionMultiplier,
           YoVariableRegistry parentRegistry)
   {
      Random random = new Random(1776L);
      ArrayList<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();

      double coefficientOfFriction = 0.87;

      GroundReactionWrenchDistributorVisualizer visualizer = null;
      SimulationConstructionSet scs = null;

      if (VISUALIZE)
      {
         Robot robot = new Robot("null");

         DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
         scs = new SimulationConstructionSet(robot);

         int maxNumberOfFeet = 2;    // 6;
         int maxNumberOfVertices = 10;
         visualizer = new GroundReactionWrenchDistributorVisualizer(maxNumberOfFeet, maxNumberOfVertices, scs.getRootRegistry(),
                 dynamicGraphicObjectsListRegistry);

         dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);

         addCoordinateSystem(scs);
         scs.startOnAThread();
      }

      int numberOfTests = 25;

      for (int i = 0; i < numberOfTests; i++)
      {
         contactStates.clear();

         FlatGroundPlaneContactState leftFootContactState = FlatGroundPlaneContactState.createRandomFlatGroundContactState(random, true, coefficientOfFriction);
         FlatGroundPlaneContactState rightFootContactState = FlatGroundPlaneContactState.createRandomFlatGroundContactState(random, false, coefficientOfFriction);

         contactStates.add(leftFootContactState);
         contactStates.add(rightFootContactState);

         GroundReactionWrenchDistributorInputData inputData = new GroundReactionWrenchDistributorInputData();

         inputData.addPlaneContact(leftFootContactState);
         inputData.addPlaneContact(rightFootContactState);

         SpatialForceVector desiredNetSpatialForceVector;

         if (contactPointDistributor)
         {
            desiredNetSpatialForceVector = generateRandomAchievableSpatialForceVectorUsingContactPoints(random, centerOfMassFrame, contactStates,
                    coefficientOfFriction, feasibleMomentSolutions);
         }

         else
         {
            desiredNetSpatialForceVector = generateRandomAchievableSpatialForceVector(random, centerOfMassFrame, contactStates, coefficientOfFriction,
                    coefficientOfFriction * rotationalCoefficientOfFrictionMultiplier, feasibleMomentSolutions);
         }

         inputData.setSpatialForceVectorAndUpcomingSupportSide(desiredNetSpatialForceVector, null);

         GroundReactionWrenchDistributorOutputData distributedWrench = new GroundReactionWrenchDistributorOutputData();
         distributor.solve(distributedWrench, inputData);

         if (verifyForcesAreInsideFrictionCones)
         {
            verifyForcesAreInsideFrictionCones(distributedWrench, contactStates, coefficientOfFriction);
         }

//       if (centersOfPressureAreInsideContactPolygons(distributor, contactStates))
//       {
         verifyCentersOfPressureAreInsideContactPolygons(distributedWrench, contactStates);
         verifyWrenchesSumToExpectedTotal(centerOfMassFrame, desiredNetSpatialForceVector, contactStates, distributedWrench, 1e-4, !feasibleMomentSolutions);

//       }

         if (VISUALIZE)
         {
            visualizer.update(scs, distributedWrench, centerOfMassFrame, contactStates, desiredNetSpatialForceVector);
         }
      }

      if (VISUALIZE)
      {
         deleteFirstDataPointAndCropData(scs);
         ThreadTools.sleepForever();
      }
   }

   private void testNonFlatGroundExample(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry)
   {
      double coefficientOfFriction = 1.0;
      double footLength = 0.3;
      double footWidth = 0.15;

      Point3d leftMidfootLocation = new Point3d(0.0, 0.5, 0.0);
      Vector3d leftNormalToContactPlane = new Vector3d(0.1, 0.0, 1.0);
      leftNormalToContactPlane.normalize();
      NonFlatGroundPlaneContactState leftFootContactState = new NonFlatGroundPlaneContactState(footLength, footWidth, leftMidfootLocation,
                                                               leftNormalToContactPlane, coefficientOfFriction);

      Point3d rightMidfootLocation = new Point3d(0.0, -0.5, 0.0);
      Vector3d rightNormalToContactPlane = new Vector3d(-0.1, 0.0, 1.0);
      rightNormalToContactPlane.normalize();
      NonFlatGroundPlaneContactState rightFootContactState = new NonFlatGroundPlaneContactState(footLength, footWidth, rightMidfootLocation,
                                                                rightNormalToContactPlane, coefficientOfFriction);

      simpleTwoFootTest(centerOfMassFrame, distributor, parentRegistry, leftFootContactState, rightFootContactState, coefficientOfFriction);
   }

// private void testFourFeetExample(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributorInterface distributor, YoVariableRegistry parentRegistry)
// {
//    double footLength = 0.3;
//    double footWidth = 0.15;
//
//    Point3d footOneLocation = new Point3d(0.0, 0.5, 0.0);
//    FlatGroundPlaneContactState foot1ContactState = new FlatGroundPlaneContactState(footLength, footWidth, footOneLocation);
//
//    Point3d footTwoLocation = new Point3d(0.0, -0.5, 0.0);
//    FlatGroundPlaneContactState FootTwoContactState = new FlatGroundPlaneContactState(footLength, footWidth, footTwoLocation);
//
//    Point3d footThreeLocation = new Point3d(1.0, 0.0, 0.0);
//    FlatGroundPlaneContactState FootThreeContactState = new FlatGroundPlaneContactState(footLength, footWidth, footThreeLocation);
//
//    Point3d footFourLocation = new Point3d(-1.0, 0.0, 0.0);
//    FlatGroundPlaneContactState FootFourContactState = new FlatGroundPlaneContactState(footLength, footWidth, footFourLocation);
//
//    simpleNFootTest(centerOfMassFrame, distributor, parentRegistry, new PlaneContactState[] {foot1ContactState, FootTwoContactState, FootThreeContactState,
//            FootFourContactState});
// }

   private void simpleTwoFootTest(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry,
                                  PlaneContactState leftFootContactState, PlaneContactState rightFootContactState, double coefficientOfFriction)
   {
      simpleNFootTest(centerOfMassFrame, distributor, parentRegistry, new PlaneContactState[] {leftFootContactState, rightFootContactState}, coefficientOfFriction);
   }

   private void simpleNFootTest(ReferenceFrame centerOfMassFrame, GroundReactionWrenchDistributor distributor, YoVariableRegistry parentRegistry,
                                PlaneContactState[] feetContactStates, double coefficientOfFriction)
   {
      ArrayList<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();


      GroundReactionWrenchDistributorInputData inputData = new GroundReactionWrenchDistributorInputData();

      for (int i = 0; i < feetContactStates.length; i++)
      {
         inputData.addPlaneContact(feetContactStates[i]);
         contactStates.add(feetContactStates[i]);
      }

      Vector3d linearPart = new Vector3d(0.0, 0.0, 1000.0);    // 50.0, 60.0, 1000.0);
      Vector3d angularPart = new Vector3d();    // 10.0, 12.0, 13.0);

      SpatialForceVector desiredNetSpatialForceVector = new SpatialForceVector(centerOfMassFrame, linearPart, angularPart);

      inputData.setSpatialForceVectorAndUpcomingSupportSide(desiredNetSpatialForceVector, null);

      GroundReactionWrenchDistributorOutputData distributedWrench = new GroundReactionWrenchDistributorOutputData();
      distributor.solve(distributedWrench, inputData);


      for (int i = 0; i < feetContactStates.length; i++)
      {
         printIfDebug("force" + i + " = " + distributedWrench.getForce(feetContactStates[i]));
         printIfDebug("leftNormalTorque" + i + " = " + distributedWrench.getNormalTorque(feetContactStates[i]));
         printIfDebug("leftCenterOfPressure" + i + " = " + distributedWrench.getCenterOfPressure(feetContactStates[i]));
      }

      verifyForcesAreInsideFrictionCones(distributedWrench, contactStates, coefficientOfFriction);
      verifyCentersOfPressureAreInsideContactPolygons(distributedWrench, contactStates);
      verifyWrenchesSumToExpectedTotal(centerOfMassFrame, desiredNetSpatialForceVector, contactStates, distributedWrench, 1e-7, false);

      if (VISUALIZE)
      {
         Robot robot = new Robot("null");

         DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
         SimulationConstructionSet scs = new SimulationConstructionSet(robot);

         int maxNumberOfFeet = 2;
         int maxNumberOfVertices = 10;
         GroundReactionWrenchDistributorVisualizer visualizer = new GroundReactionWrenchDistributorVisualizer(maxNumberOfFeet, maxNumberOfVertices,
                                                                   scs.getRootRegistry(), dynamicGraphicObjectsListRegistry);

         dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
         addCoordinateSystem(scs);

         scs.startOnAThread();

         visualizer.update(scs, distributedWrench, centerOfMassFrame, contactStates, desiredNetSpatialForceVector);

         ThreadTools.sleepForever();
      }
   }

   private void deleteFirstDataPointAndCropData(SimulationConstructionSet scs)
   {
      scs.gotoInPointNow();
      scs.tick(1);
      scs.setInPoint();
      scs.cropBuffer();
   }

   private void addCoordinateSystem(SimulationConstructionSet scs)
   {
      Graphics3DObject coordinateSystem = new Graphics3DObject();
      coordinateSystem.addCoordinateSystem(0.2);
      scs.addStaticLinkGraphics(coordinateSystem, Graphics3DNodeType.VISUALIZATION);
   }

   private PoseReferenceFrame createCenterOfMassFrame(Point3d centerOfMassPosition)
   {
      PoseReferenceFrame centerOfMassFrame = new PoseReferenceFrame("com", ReferenceFrame.getWorldFrame());
      FramePose centerOfMassPose = new FramePose(ReferenceFrame.getWorldFrame(), centerOfMassPosition, new Quat4d());
      centerOfMassFrame.updatePose(centerOfMassPose);
      centerOfMassFrame.update();

      return centerOfMassFrame;
   }

   private void printIfDebug(String string)
   {
      if (DEBUG)
         System.out.println(string);
   }

   private void verifyWrenchesSumToExpectedTotal(ReferenceFrame centerOfMassFrame, SpatialForceVector totalBodyWrench,
           ArrayList<PlaneContactState> contactStates, GroundReactionWrenchDistributorOutputData distributedWrench, double epsilon, boolean onlyForces)
   {
      ReferenceFrame expressedInFrame = totalBodyWrench.getExpressedInFrame();

      SpatialForceVector achievedWrench = GroundReactionWrenchDistributorAchievedWrenchCalculator.computeAchievedWrench(distributedWrench, expressedInFrame,
                                             contactStates);

      FrameVector totalBodyForce = totalBodyWrench.getLinearPartAsFrameVectorCopy();
      assertTrue("achievedWrench = " + achievedWrench + "\ntotalBodyForce = " + totalBodyForce,
                 achievedWrench.getLinearPartAsFrameVectorCopy().epsilonEquals(totalBodyForce, epsilon));

      FrameVector totalBodyMoment = totalBodyWrench.getAngularPartAsFrameVectorCopy();

//    if (!achievedWrench.getAngularPartAsFrameVectorCopy().epsilonEquals(totalBodyMoment, epsilon))
//    {
//       for (PlaneContactState cs : contactStates)
//       {
//          System.err.println(cs);
//       }
//
//       System.err.println("achievedWrench = " + achievedWrench + ", \ntotalBodyWrench = " + totalBodyWrench);
//       System.err.println("CoMFrame = " + centerOfMassFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame()));
//    }

      if (!onlyForces)
      {
         assertTrue("achievedWrench = " + achievedWrench + ", \ntotalBodyWrench = " + totalBodyWrench,
                    achievedWrench.getAngularPartAsFrameVectorCopy().epsilonEquals(totalBodyMoment, epsilon));
      }
   }

   private void verifyForcesAreInsideFrictionCones(GroundReactionWrenchDistributorOutputData distributedWrench, ArrayList<PlaneContactState> contactStates,
           double coefficientOfFriction)
   {
      for (PlaneContactState contactState : contactStates)
      {
         FrameVector force = distributedWrench.getForce(contactState);

         verifyForceIsInsideFrictionCone(force, contactState, coefficientOfFriction);
      }
   }

// private boolean centersOfPressureAreInsideContactPolygons(GroundReactionWrenchDistributorInterface distributor, ArrayList<PlaneContactState> contactStates)
// {
//    for (PlaneContactState contactState : contactStates)
//    {
//       FramePoint2d centerOfPressure = distributor.getCenterOfPressure(contactState);
//       if (!centerOfPressureIsInsideFoot(centerOfPressure, contactState))
//          return true;
//    }
//
//    return false;
// }

// private boolean centerOfPressureIsInsideFoot(FramePoint2d centerOfPressure, PlaneContactState planeContactState)
// {
//    centerOfPressure.checkReferenceFrameMatch(planeContactState.getPlaneFrame());
//    List<FramePoint2d> contactPoints = planeContactState.getContactPoints2d();
//    FrameConvexPolygon2d footPolygon = new FrameConvexPolygon2d(contactPoints);
//
//    return footPolygon.isPointInside(centerOfPressure);
// }

   private void verifyCentersOfPressureAreInsideContactPolygons(GroundReactionWrenchDistributorOutputData distributedWrench,
           ArrayList<PlaneContactState> contactStates)
   {
      for (PlaneContactState contactState : contactStates)
      {
         FramePoint2d centerOfPressure = distributedWrench.getCenterOfPressure(contactState);
         verifyCenterOfPressureIsInsideFoot(centerOfPressure, contactState);
      }
   }

   private void verifyCenterOfPressureIsInsideFoot(FramePoint2d centerOfPressure, PlaneContactState planeContactState)
   {
      centerOfPressure.checkReferenceFrameMatch(planeContactState.getPlaneFrame());
      List<FramePoint2d> contactPoints = planeContactState.getContactPoints2d();
      FrameConvexPolygon2d footPolygon = new FrameConvexPolygon2d(contactPoints);

      assertTrue(footPolygon.distance(centerOfPressure) < 1e-7);
   }

   private void verifyForceIsInsideFrictionCone(FrameVector forceVector, PlaneContactState planeContactState, double coefficientOfFriction)
   {
      forceVector = forceVector.changeFrameCopy(planeContactState.getPlaneFrame());

      double normalForce = forceVector.getZ();
      double parallelForce = Math.sqrt(forceVector.getX() * forceVector.getX() + forceVector.getY() * forceVector.getY());

      if (parallelForce > coefficientOfFriction * normalForce)
         fail("Outside of Friction Cone! forceVector = " + forceVector + ", planeContactState = " + planeContactState);
   }

   private void verifyForceInsideNormalTorqueCone(FrameVector forceVector, double normalTorque, double rotationalCoefficientOfFriction)
   {
      double normalForce = forceVector.getZ();

      if (Math.abs(normalTorque) > rotationalCoefficientOfFriction * normalForce)
         fail("Too much normal torque! normalTorque = " + normalTorque + ", normalForce = " + normalForce + ", normalTorqueCoefficientOfFriction = "
              + rotationalCoefficientOfFriction);
   }


   private static SpatialForceVector generateRandomAchievableSpatialForceVector(Random random, ReferenceFrame centerOfMassFrame,
           ArrayList<PlaneContactState> contactStates, double coefficientOfFriction, double normalTorqueCoefficientOfFriction, boolean feasibleMomentSolution)
   {
      SpatialForceVector spatialForceVector = new SpatialForceVector(centerOfMassFrame);

      for (PlaneContactState contactState : contactStates)
      {
         ReferenceFrame contactPlaneFrame = contactState.getPlaneFrame();

         double normalForce = RandomTools.generateRandomDouble(random, 10.0, feasibleMomentSolution ? 12.0 : 100.0);
         double parallelForceMagnitude = random.nextDouble() * coefficientOfFriction * normalForce;

         Vector2d parallelForce2d = RandomTools.generateRandomVector2d(random, parallelForceMagnitude);
         Vector3d totalForce = new Vector3d(parallelForce2d.getX(), parallelForce2d.getY(), normalForce);

         double normalTorque = random.nextDouble() * normalTorqueCoefficientOfFriction * normalForce;
         Vector3d totalTorque = new Vector3d(0.0, 0.0, normalTorque);

         ReferenceFrame centerOfPressureFrame = generateRandomCenterOfPressureFrame(random, contactState, contactPlaneFrame);
         SpatialForceVector spatialForceVectorContributedByThisContact = new SpatialForceVector(centerOfPressureFrame, totalForce, totalTorque);

         spatialForceVectorContributedByThisContact.changeFrame(centerOfMassFrame);
         spatialForceVector.add(spatialForceVectorContributedByThisContact);
      }

      return spatialForceVector;
   }

   private static SpatialForceVector generateRandomAchievableSpatialForceVectorUsingContactPoints(Random random, ReferenceFrame centerOfMassFrame,
           ArrayList<PlaneContactState> contactStates, double coefficientOfFriction, boolean feasibleSolution)
   {
      SpatialForceVector spatialForceVector = new SpatialForceVector(centerOfMassFrame);

      FrameVector totalTorque = new FrameVector(centerOfMassFrame, 0.0, 0.0, 0.0);
      FrameVector totalForce = new FrameVector(centerOfMassFrame, 0.0, 0.0, 0.0);

      for (PlaneContactState contactState : contactStates)
      {
         ReferenceFrame contactPlaneFrame = contactState.getPlaneFrame();

         List<FrameVector> normalizedSupportVectors = new ArrayList<FrameVector>();

         for (int i = 0; i < 4; i++)
         {
            normalizedSupportVectors.add(new FrameVector(contactPlaneFrame));
         }

         WrenchDistributorTools.getSupportVectors(normalizedSupportVectors, coefficientOfFriction, contactPlaneFrame);

         FrameVector tempSupportVector = new FrameVector(centerOfMassFrame);
         FrameVector tempCrossVector = new FrameVector(centerOfMassFrame);
         FramePoint tempContactPoint = new FramePoint(centerOfMassFrame);

         for (FramePoint2d contactPoint : contactState.getContactPoints2d())
         {
            tempContactPoint.set(contactPoint.getReferenceFrame(), contactPoint.getX(), contactPoint.getY(), 0.0);
            tempContactPoint.changeFrame(centerOfMassFrame);

            for (FrameVector supportVector : normalizedSupportVectors)
            {
               double scale = RandomTools.generateRandomDouble(random, 10.0, 50.0);
               tempSupportVector.set(centerOfMassFrame, supportVector.getX(), supportVector.getY(), supportVector.getZ());
               tempSupportVector.scale(scale);

               tempCrossVector.cross(tempContactPoint, tempSupportVector);

               totalForce.add(tempSupportVector);
               totalTorque.add(tempCrossVector);
            }
         }
      }

      spatialForceVector = new SpatialForceVector(centerOfMassFrame, totalForce.getVector(), totalTorque.getVector());

      return spatialForceVector;
   }

   private static ReferenceFrame generateRandomCenterOfPressureFrame(Random random, PlaneContactState contactState, ReferenceFrame contactPlaneFrame)
   {
      PoseReferenceFrame centerOfPressureFrame = new PoseReferenceFrame("centerOfPressure", contactPlaneFrame);
      Point2d pointInsideContact = generateRandomPointInsideContact(random, contactState);
      Point3d centerOfPressurePosition = new Point3d(pointInsideContact.getX(), pointInsideContact.getY(), 0.0);
      FramePose framePose = new FramePose(contactPlaneFrame, centerOfPressurePosition, new Quat4d());
      centerOfPressureFrame.updatePose(framePose);
      centerOfPressureFrame.update();

      return centerOfPressureFrame;
   }

   private static Point2d generateRandomPointInsideContact(Random random, PlaneContactState contactState)
   {
      Point2d ret = new Point2d();
      double totalWeight = 0.0;

      List<FramePoint2d> contactPoints = contactState.getContactPoints2d();
      for (FramePoint2d contactPoint : contactPoints)
      {
         Point2d point2d = contactPoint.getPointCopy();
         double weight = random.nextDouble();
         point2d.scale(weight);
         ret.add(point2d);
         totalWeight += weight;
      }

      ret.scale(1.0 / totalWeight);

      return ret;
   }


}
