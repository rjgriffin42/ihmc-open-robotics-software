package us.ihmc.humanoidRobotics.footstep.footstepSnapper;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepData;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.HeightMapWithPoints;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FramePose2d;
import us.ihmc.robotics.geometry.InsufficientDataException;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;

/**
 * Created by agrabertilton on 1/28/15.
 */
public class AdjustingFootstepSnapper implements FootstepSnapper
{
   private FootstepSnappingParameters footstepSnappingParameters;
   private double distanceAdjustment;
   private double angleAdjustment;
   private ConvexHullFootstepSnapper convexHullFootstepSnapper;

   public AdjustingFootstepSnapper(FootstepValueFunction valueFunction, FootstepSnappingParameters parameters)
   {
      convexHullFootstepSnapper = new ConvexHullFootstepSnapper(valueFunction, parameters);
      this.footstepSnappingParameters = parameters;
      this.distanceAdjustment = parameters.getDistanceAdjustment();
      this.angleAdjustment = parameters.getAngleAdjustment();
   }

   @Override
   public void setMask(List<Point2d> footShape)
   {
      convexHullFootstepSnapper.setMask(footShape);
   }

   @Override
   public void setUseMask(boolean useMask, double kernelMaskSafetyBuffer, double boundingBoxDimension)
   {
      convexHullFootstepSnapper.setUseMask(useMask, kernelMaskSafetyBuffer, boundingBoxDimension);
   }

   @Override
   public List<Point3d> getPointList()
   {
      return convexHullFootstepSnapper.getPointList();
   }

   public void updateParameters(FootstepSnappingParameters newParameters)
   {
      this.footstepSnappingParameters = newParameters;
      convexHullFootstepSnapper.updateParameters(newParameters);
   }

   public FootstepSnappingParameters getParameters()
   {
      return footstepSnappingParameters;
   }

   @Override
   public void adjustFootstepWithoutHeightmap(FootstepData footstep, double height, Vector3d planeNormal)
   {
      convexHullFootstepSnapper.adjustFootstepWithoutHeightmap(footstep, height, planeNormal);
   }

   @Override
   public void adjustFootstepWithoutHeightmap(Footstep footstep, double height, Vector3d planeNormal)
   {
      convexHullFootstepSnapper.adjustFootstepWithoutHeightmap(footstep, height, planeNormal);
   }


   @Override
   public Footstep generateFootstepWithoutHeightMap(FramePose2d desiredSolePosition, RigidBody foot, ReferenceFrame soleFrame, RobotSide robotSide,
           double height, Vector3d planeNormal)
   {
      return convexHullFootstepSnapper.generateFootstepWithoutHeightMap(desiredSolePosition, foot, soleFrame, robotSide, height, planeNormal);
   }

   @Override
   public Footstep generateSnappedFootstep(double soleX, double soleY, double yaw, RigidBody foot, ReferenceFrame soleFrame, RobotSide robotSide,
           HeightMapWithPoints heightMap)
           throws InsufficientDataException
   {
      FramePose2d footPose2d = new FramePose2d(ReferenceFrame.getWorldFrame(), new Point2d(soleX, soleY), yaw);

      return generateFootstepUsingHeightMap(footPose2d, foot, soleFrame, robotSide, heightMap);
   }

   @Override
   public Footstep generateFootstepUsingHeightMap(FramePose2d desiredSolePosition, RigidBody foot, ReferenceFrame soleFrame, RobotSide robotSide,
           HeightMapWithPoints heightMap)
           throws InsufficientDataException
   {
      Footstep toReturn = convexHullFootstepSnapper.generateFootstepUsingHeightMap(desiredSolePosition, foot, soleFrame, robotSide, heightMap);
      snapFootstep(toReturn, heightMap);

      return toReturn;
   }


   @Override
   public Footstep.FootstepType snapFootstep(Footstep footstep, HeightMapWithPoints heightMap){
      FootstepData originalFootstep = new FootstepData(footstep);

      //set to the sole pose
      Vector3d position = new Vector3d();
      Quat4d orientation = new Quat4d();
      RigidBodyTransform solePose = new RigidBodyTransform();
      footstep.getSolePose(solePose);
      solePose.get(orientation, position);
      originalFootstep.setLocation(new Point3d(position));
      originalFootstep.setOrientation(orientation);

      //get the footstep
      Footstep.FootstepType type = snapFootstep(originalFootstep, heightMap);
      if (type == Footstep.FootstepType.FULL_FOOTSTEP && originalFootstep.getPredictedContactPoints() != null){
         throw new RuntimeException(this.getClass().getSimpleName() + "Full Footstep should have null contact points");
      }
      footstep.setPredictedContactPointsFromPoint2ds(originalFootstep.getPredictedContactPoints());
      footstep.setFootstepType(type);
      FramePose solePoseInWorld = new FramePose(ReferenceFrame.getWorldFrame(), originalFootstep.getLocation(), originalFootstep.getOrientation());
      footstep.setSolePose(solePoseInWorld);

      footstep.setSwingHeight(originalFootstep.getSwingHeight());
      footstep.setTrajectoryType(originalFootstep.getTrajectoryType());

      return type;
   }

   @Override
   public Footstep.FootstepType snapFootstep(FootstepData footstep, HeightMapWithPoints heightMap)
   {
      Footstep.FootstepType footstepFound = convexHullFootstepSnapper.snapFootstep(footstep, heightMap);

      if (footstepFound != Footstep.FootstepType.BAD_FOOTSTEP)
      {
         if (footstepFound == Footstep.FootstepType.FULL_FOOTSTEP && footstep.getPredictedContactPoints() != null){
            throw new RuntimeException(this.getClass().getSimpleName() + "Full Footstep should have null contact points");
         }
         return footstepFound;
      }

      FootstepData originalFootstepFound = new FootstepData(footstep);

      Vector3d position = new Vector3d();
      Matrix3d orientation = new Matrix3d();
      Vector3d zOrientation = new Vector3d();

      position.set(originalFootstepFound.getLocation());
      orientation.set(originalFootstepFound.getOrientation());
      orientation.getColumn(2, zOrientation);
      double originalYaw = RotationTools.getYawFromQuaternion(originalFootstepFound.getOrientation());

      double[] angleOffsets;
      if (angleAdjustment > 0)
      {
         angleOffsets = new double[] {0.0, -angleAdjustment, angleAdjustment};
      }
      else
      {
         angleOffsets = new double[] {0.0};
      }

      ArrayList<Point2d> possiblePositions = new ArrayList<Point2d>();
      Point2d originalPosition = new Point2d(position.getX(), position.getY());
      possiblePositions.add(originalPosition);

      if (distanceAdjustment > 0)
      {
         for (int i = 0; i < 8; i++)
         {
            double angle = Math.PI / 4 * i + originalYaw;
            possiblePositions.add(new Point2d(originalPosition.x + Math.cos(angle) * distanceAdjustment,
                                              originalPosition.y + Math.sin(angle) * distanceAdjustment));
         }
      }

      boolean isOriginalPosition = true;
      FramePose2d desiredSolePosition = new FramePose2d(ReferenceFrame.getWorldFrame(), originalPosition, originalYaw);
      FramePose2d newDesiredSolePosition = new FramePose2d(desiredSolePosition);
      for (int i = 0; i < angleOffsets.length; i++)
      {
         for (Point2d point2d : possiblePositions)
         {
            if (isOriginalPosition)
            {
               isOriginalPosition = false;

               continue;
            }

            newDesiredSolePosition.setPoseIncludingFrame(desiredSolePosition.getReferenceFrame(), point2d.x, point2d.y, originalYaw + angleOffsets[i]);
            footstepFound = convexHullFootstepSnapper.snapFootstep(footstep, heightMap);

            if (footstepFound!= Footstep.FootstepType.BAD_FOOTSTEP)
            {
               if (footstepFound == Footstep.FootstepType.FULL_FOOTSTEP && footstep.getPredictedContactPoints() != null){
                  throw new RuntimeException(this.getClass().getSimpleName() + "Full Footstep should have null contact points");
               }
               return footstepFound;
            }
         }
      }


      footstep.location = originalFootstepFound.location;
      footstep.orientation = originalFootstepFound.orientation;
      return Footstep.FootstepType.BAD_FOOTSTEP;
   }
}
