package us.ihmc.acsell.parameters;

import static us.ihmc.acsell.parameters.BonoPhysicalProperties.footLength;
import static us.ihmc.acsell.parameters.BonoPhysicalProperties.footWidth;
import static us.ihmc.acsell.parameters.BonoPhysicalProperties.toeWidth;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.simulationconstructionset.util.LinearGroundContactModel;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.wholeBodyController.DRCRobotContactPointParameters;
import us.ihmc.wholeBodyController.DRCRobotJointMap;

public class BonoContactPointParameters extends DRCRobotContactPointParameters
{
   private final ContactableBodiesFactory contactableBodiesFactory = new ContactableBodiesFactory();

   private final Vector3d pelvisBoxOffset = new Vector3d(-0.100000, 0.000000, -0.050000);
   private final double pelvisBoxSizeX = 0.100000;
   private final double pelvisBoxSizeY = 0.150000;
   private final double pelvisBoxSizeZ = 0.200000;
   private final RigidBodyTransform pelvisContactPointTransform = new RigidBodyTransform();
   private final List<Point2d> pelvisContactPoints = new ArrayList<Point2d>();
   private final RigidBodyTransform pelvisBackContactPointTransform = new RigidBodyTransform();
   private final List<Point2d> pelvisBackContactPoints = new ArrayList<Point2d>();
   private final Vector3d chestBoxOffset = new Vector3d(0.044600, 0.000000, 0.186900);
   private final double chestBoxSizeX = 0.318800;
   private final double chestBoxSizeY = 0.240000;
   private final double chestBoxSizeZ = 0.316200;
   private final RigidBodyTransform chestBackContactPointTransform = new RigidBodyTransform();
   private final List<Point2d> chestBackContactPoints = new ArrayList<Point2d>();
   private final SideDependentList<RigidBodyTransform> thighContactPointTransforms = new SideDependentList<RigidBodyTransform>();
   private final SideDependentList<List<Point2d>> thighContactPoints = new SideDependentList<List<Point2d>>();
   private final List<Pair<String, Vector3d>> jointNameGroundContactPointMap = new ArrayList<Pair<String, Vector3d>>();
   private final SideDependentList<ArrayList<Point2d>> footGroundContactPoints = new SideDependentList<ArrayList<Point2d>>();

   public BonoContactPointParameters(DRCRobotJointMap jointMap)
   {
      Vector3d t0 = new Vector3d(0.0, 0.0, -pelvisBoxSizeZ / 2.0);
      t0.add(pelvisBoxOffset);
      pelvisContactPointTransform.setTranslation(t0);

      pelvisContactPoints.add(new Point2d(pelvisBoxSizeX / 2.0, pelvisBoxSizeY / 2.0));
      pelvisContactPoints.add(new Point2d(pelvisBoxSizeX / 2.0, -pelvisBoxSizeY / 2.0));
      pelvisContactPoints.add(new Point2d(-pelvisBoxSizeX / 2.0, pelvisBoxSizeY / 2.0));
      pelvisContactPoints.add(new Point2d(-pelvisBoxSizeX / 2.0, -pelvisBoxSizeY / 2.0));

      Matrix3d r0 = new Matrix3d();
      RotationFunctions.setYawPitchRoll(r0, 0.0, Math.PI / 2.0, 0.0);
      pelvisBackContactPointTransform.setRotationAndZeroTranslation(r0);

      Vector3d t1 = new Vector3d(-pelvisBoxSizeX / 2.0, 0.0, 0.0);
      t1.add(pelvisBoxOffset);
      pelvisBackContactPointTransform.setTranslation(t1);
      pelvisBackContactPoints.add(new Point2d(-pelvisBoxSizeZ / 2.0, pelvisBoxSizeY / 2.0));
      pelvisBackContactPoints.add(new Point2d(-pelvisBoxSizeZ / 2.0, -pelvisBoxSizeY / 2.0));
      pelvisBackContactPoints.add(new Point2d(pelvisBoxSizeZ / 2.0, pelvisBoxSizeY / 2.0));
      pelvisBackContactPoints.add(new Point2d(pelvisBoxSizeZ / 2.0, -pelvisBoxSizeY / 2.0));

      Matrix3d r1 = new Matrix3d();
      RotationFunctions.setYawPitchRoll(r1, 0.0, Math.PI / 2.0, 0.0);
      chestBackContactPointTransform.setRotationAndZeroTranslation(r1);

      Vector3d t2 = new Vector3d(-chestBoxSizeX / 2.0, 0.0, 0.0);
      t2.add(chestBoxOffset);
      chestBackContactPointTransform.setTranslation(t2);

      chestBackContactPoints.add(new Point2d(0.0, chestBoxSizeY / 2.0));
      chestBackContactPoints.add(new Point2d(0.0, -chestBoxSizeY / 2.0));
      chestBackContactPoints.add(new Point2d(chestBoxSizeZ / 2.0, chestBoxSizeY / 2.0));
      chestBackContactPoints.add(new Point2d(chestBoxSizeZ / 2.0, -chestBoxSizeY / 2.0));

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBodyTransform thighContactPointTransform = new RigidBodyTransform();
         double pitch = Math.PI / 2.0;
         thighContactPointTransform.setEuler(0.0, pitch, 0.0);
         thighContactPointTransform.setTranslation(-0.1179, robotSide.negateIfRightSide(0.02085), -0.08);
         thighContactPointTransforms.put(robotSide, thighContactPointTransform);
      }

      double[] xOffsets = new double[] { 0.0, 0.1 };// {0.0, 0.2};
      double[] yOffsets = new double[] { 0.0, 0.0 };
      for (RobotSide robotSide : RobotSide.values)
      {
         ArrayList<Point2d> offsetsForSide = new ArrayList<Point2d>();

         for (int i = 0; i < 2; i++)
         {
            double xOffset = xOffsets[i];
            double yOffset = robotSide.negateIfRightSide(yOffsets[i]);

            offsetsForSide.add(new Point2d(xOffset, yOffset));
         }

         thighContactPoints.put(robotSide, offsetsForSide);
      }
      
      
      for (RobotSide robotSide : RobotSide.values)
      {
         footGroundContactPoints.put(robotSide, new ArrayList<Point2d>());
         RigidBodyTransform ankleToSoleFrame = BonoPhysicalProperties.getAnkleToSoleFrameTransform(robotSide);

         ArrayList<Pair<String, Point2d>> footGCs = new ArrayList<Pair<String, Point2d>>();
         String jointBeforeFootName = jointMap.getJointBeforeFootName(robotSide);
         double shrinkFootLength = footLength / 2.0 - 0.005;
         double shrinkToeWidth = toeWidth / 2.0 - 0.01;
         double shrinkHeelWdith = footWidth / 2.0 - 0.01;
         footGCs.add(new Pair<String, Point2d>(jointBeforeFootName, new Point2d(shrinkFootLength, -shrinkToeWidth)));
         footGCs.add(new Pair<String, Point2d>(jointBeforeFootName, new Point2d(shrinkFootLength, shrinkToeWidth)));
         footGCs.add(new Pair<String, Point2d>(jointBeforeFootName, new Point2d(-shrinkFootLength, -shrinkHeelWdith)));
         footGCs.add(new Pair<String, Point2d>(jointBeforeFootName, new Point2d(-shrinkFootLength, shrinkHeelWdith)));

         //SCS contact points
         for (Pair<String, Point2d> footGC : footGCs)
         {
            footGroundContactPoints.get(robotSide).add(footGC.second());

            Point3d gcOffset = new Point3d(footGC.second().getX(), footGC.second().getY(), 0.0);
            ankleToSoleFrame.transform(gcOffset);
            jointNameGroundContactPointMap.add(new Pair<String, Vector3d>(footGC.first(), new Vector3d(gcOffset)));
         }
      }

      setupContactableBodiesFactory(jointMap);
   }

   private void setupContactableBodiesFactory(DRCRobotJointMap jointMap)
   {
      contactableBodiesFactory.addFootContactParameters(getFootContactPoints());
   }

   @Override
   public RigidBodyTransform getPelvisContactPointTransform()
   {
      return pelvisContactPointTransform;
   }

   @Override
   public List<Point2d> getPelvisContactPoints()
   {
      return pelvisContactPoints;
   }

   @Override
   public RigidBodyTransform getPelvisBackContactPointTransform()
   {
      return pelvisBackContactPointTransform;
   }

   @Override
   public List<Point2d> getPelvisBackContactPoints()
   {
      return pelvisBackContactPoints;
   }

   @Override
   public RigidBodyTransform getChestBackContactPointTransform()
   {
      return chestBackContactPointTransform;
   }

   @Override
   public List<Point2d> getChestBackContactPoints()
   {
      return chestBackContactPoints;
   }

   @Override
   public SideDependentList<RigidBodyTransform> getThighContactPointTransforms()
   {
      return thighContactPointTransforms;
   }

   @Override
   public SideDependentList<List<Point2d>> getThighContactPoints()
   {
      return thighContactPoints;
   }

   @Override
   public List<Pair<String, Vector3d>> getJointNameGroundContactPointMap()
   {
      return jointNameGroundContactPointMap;
   }

   @Override
   public SideDependentList<ArrayList<Point2d>> getFootContactPoints()
   {
      return footGroundContactPoints;
   }

   @Override
   public ContactableBodiesFactory getContactableBodiesFactory()
   {
      return contactableBodiesFactory;
   }

   @Override
   public SideDependentList<RigidBodyTransform> getHandContactPointTransforms()
   {
      return null;
   }

   @Override
   public SideDependentList<List<Point2d>> getHandContactPoints()
   {
      return null;
   }

   @Override
   public void setupGroundContactModelParameters(LinearGroundContactModel linearGroundContactModel)
   {
      linearGroundContactModel.setZStiffness(2000.0);      
      linearGroundContactModel.setZDamping(1500.0);      
      linearGroundContactModel.setXYStiffness(50000.0);      
      linearGroundContactModel.setXYDamping(2000.0);      
   }
}
