package us.ihmc.valkyrie.paramaters;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotContactPointParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class ValkyrieContactPointParameters extends DRCRobotContactPointParameters
{
   
   private final Vector3d pelvisBoxOffset = new Vector3d(-0.100000, 0.000000, -0.050000);
   private final double pelvisBoxSizeX = 0.100000;
   private final double pelvisBoxSizeY = 0.150000;
   private final double pelvisBoxSizeZ = 0.200000;
   private final Transform3D pelvisContactPointTransform = new Transform3D();
   private final List<Point2d> pelvisContacts = new ArrayList<Point2d>();
   private final Transform3D pelvisBackContactPointTransform = new Transform3D();
   private final List<Point2d> pelvisBackContacts = new ArrayList<Point2d>();
   
   private final Vector3d chestBoxOffset = new Vector3d(0.044600, 0.000000, 0.186900);
   private final double chestBoxSizeX = 0.318800;
   private final double chestBoxSizeY = 0.240000;
   private final double chestBoxSizeZ = 0.316200;
   private final Transform3D chestBackContactPointTransform = new Transform3D();
   private final List<Point2d> chestBackContacts = new ArrayList<Point2d>();
   private final SideDependentList<Transform3D> thighContactPointTransforms = new SideDependentList<Transform3D>();
   private final SideDependentList<List<Point2d>> thighContactPoints = new SideDependentList<List<Point2d>>();
   
   private final List<Pair<String, Vector3d>> jointNameGroundContactPointMap = new ArrayList<Pair<String, Vector3d>>();
   private final SideDependentList<ArrayList<Point2d>> controllerContactPointsInSoleFrame = new SideDependentList<>();

   public ValkyrieContactPointParameters(DRCRobotJointMap jointMap)
   {
      Vector3d t0 = new Vector3d(0.0, 0.0, -pelvisBoxSizeZ / 2.0);
      t0.add(pelvisBoxOffset);
      pelvisContactPointTransform.setTranslation(t0);
      
      pelvisContacts.add(new Point2d(pelvisBoxSizeX / 2.0, pelvisBoxSizeY / 2.0));
      pelvisContacts.add(new Point2d(pelvisBoxSizeX / 2.0, -pelvisBoxSizeY / 2.0));
      pelvisContacts.add(new Point2d(-pelvisBoxSizeX / 2.0, pelvisBoxSizeY / 2.0));
      pelvisContacts.add(new Point2d(-pelvisBoxSizeX / 2.0, -pelvisBoxSizeY / 2.0));
      
      Matrix3d r0 = new Matrix3d();
      RotationFunctions.setYawPitchRoll(r0, 0.0, Math.PI / 2.0, 0.0);
      pelvisBackContactPointTransform.set(r0);
      
      Vector3d t1 = new Vector3d(-pelvisBoxSizeX / 2.0, 0.0, 0.0);
      t1.add(pelvisBoxOffset);
      pelvisBackContactPointTransform.setTranslation(t1);
      pelvisBackContacts.add(new Point2d(-pelvisBoxSizeZ / 2.0, pelvisBoxSizeY / 2.0));
      pelvisBackContacts.add(new Point2d(-pelvisBoxSizeZ / 2.0, -pelvisBoxSizeY / 2.0));
      pelvisBackContacts.add(new Point2d(pelvisBoxSizeZ / 2.0, pelvisBoxSizeY / 2.0));
      pelvisBackContacts.add(new Point2d(pelvisBoxSizeZ / 2.0, -pelvisBoxSizeY / 2.0));
      
      Matrix3d r1 = new Matrix3d();
      RotationFunctions.setYawPitchRoll(r1, 0.0, Math.PI / 2.0, 0.0);
      chestBackContactPointTransform.set(r1);
      
      Vector3d t2 = new Vector3d(-chestBoxSizeX / 2.0, 0.0, 0.0);
      t2.add(chestBoxOffset);
      chestBackContactPointTransform.setTranslation(t2);
      
      chestBackContacts.add(new Point2d(0.0, chestBoxSizeY / 2.0));
      chestBackContacts.add(new Point2d(0.0, -chestBoxSizeY / 2.0));
      chestBackContacts.add(new Point2d(chestBoxSizeZ / 2.0, chestBoxSizeY / 2.0));
      chestBackContacts.add(new Point2d(chestBoxSizeZ / 2.0, -chestBoxSizeY / 2.0));
      
      for (RobotSide robotSide : RobotSide.values)
      {
         Transform3D thighContactPointTransform = new Transform3D();
         double pitch = Math.PI / 2.0;
         thighContactPointTransform.setEuler(new Vector3d(0.0, pitch, 0.0));
         thighContactPointTransform.setTranslation(new Vector3d(-0.1179, robotSide.negateIfRightSide(0.02085), -0.08));
         thighContactPointTransforms.put(robotSide, thighContactPointTransform);
      }
      
      double[] xOffsets = new double[] {0.0, 0.1};// {0.0, 0.2};
      double[] yOffsets = new double[] {0.0, 0.0};
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
    	  controllerContactPointsInSoleFrame.put(robotSide, new ArrayList<Point2d>());
    	  Transform3D ankleToSoleFrame = ValkyriePhysicalProperties.getAnkleToSoleFrameTransform(robotSide);
    	  
	      ArrayList<Pair<String, Point2d>> footGroundContactPoints = new ArrayList<>();
	      footGroundContactPoints.add(new Pair<String, Point2d>(jointMap.getJointBeforeFootName(robotSide), new Point2d(ValkyriePhysicalProperties.footForward, -ValkyriePhysicalProperties.footWidth / 2.0)));
	      footGroundContactPoints.add(new Pair<String, Point2d>(jointMap.getJointBeforeFootName(robotSide), new Point2d(ValkyriePhysicalProperties.footForward, ValkyriePhysicalProperties.footWidth / 2.0)));
	      footGroundContactPoints.add(new Pair<String, Point2d>(jointMap.getJointBeforeFootName(robotSide), new Point2d(-ValkyriePhysicalProperties.footBack, -ValkyriePhysicalProperties.footWidth / 2.0)));
	      footGroundContactPoints.add(new Pair<String, Point2d>(jointMap.getJointBeforeFootName(robotSide), new Point2d(-ValkyriePhysicalProperties.footBack, ValkyriePhysicalProperties.footWidth / 2.0)));
	
	      
	      for(Pair<String, Point2d> gc : footGroundContactPoints)
	      {
	         controllerContactPointsInSoleFrame.get(robotSide).add(gc.second());
	         
	         Point3d gcOffset = new Point3d(gc.second().getX(), gc.second().getY(), 0.0);
	         ankleToSoleFrame.transform(gcOffset);
	         jointNameGroundContactPointMap.add(new Pair<String, Vector3d>(gc.first(), new Vector3d(gcOffset)));
	      }
      }
   }

   @Override
   public Transform3D getPelvisContactPointTransform()
   {
      return pelvisContactPointTransform;
   }

   @Override
   public List<Point2d> getPelvisContactPoints()
   {
      return pelvisContacts;
   }

   @Override
   public Transform3D getPelvisBackContactPointTransform()
   {
      return pelvisBackContactPointTransform;
   }

   @Override
   public List<Point2d> getPelvisBackContactPoints()
   {
      return pelvisBackContacts;
   }

   @Override
   public Transform3D getChestBackContactPointTransform()
   {
      return chestBackContactPointTransform;
   }

   @Override
   public List<Point2d> getChestBackContactPoints()
   {
      return chestBackContacts;
   }

   @Override
   public SideDependentList<Transform3D> getThighContactPointTransforms()
   {
      return thighContactPointTransforms;
   }

   @Override
   public SideDependentList<List<Point2d>>  getThighContactPoints()
   {
      return thighContactPoints;
   }
   
   public SideDependentList<ArrayList<Point2d>> getControllerContactPointsInSoleFrame() {
	   return controllerContactPointsInSoleFrame;
   }
   
   @Override
   public List<Pair<String, Vector3d>> getJointNameGroundContactPointMap()
   {
	   return jointNameGroundContactPointMap;
   }
   
   @Override
   public SideDependentList<ArrayList<Point2d>> getFootGroundContactPointsInSoleFrameForController()
   {
	   return controllerContactPointsInSoleFrame;
   }

   @Override
   public List<Pair<String, Vector3d>> getFootContactPoints(RobotSide robotSide)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override 
   public List<Pair<String, Vector3d>> getThighContactPoints(RobotSide robotSide)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<Pair<String, Vector3d>> getHandContactPoints(RobotSide robotSide)
   {
      // TODO Auto-generated method stub
      return null;
   }
}
