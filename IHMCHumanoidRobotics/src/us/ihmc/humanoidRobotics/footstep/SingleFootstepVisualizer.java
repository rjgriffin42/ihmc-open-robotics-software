package us.ihmc.humanoidRobotics.footstep;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFrameConvexPolygon2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePose;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPolygon;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;

public class SingleFootstepVisualizer
{
   private static final SideDependentList<AppearanceDefinition> footPolygonAppearances = new SideDependentList<AppearanceDefinition>(YoAppearance.Purple(),
         YoAppearance.Green());
   
   private static SideDependentList<Integer> indices = new SideDependentList<Integer>(0, 0);

   private final YoFramePose soleFramePose;
   private final YoFramePoint[] yoContactPoints;
   private final YoFrameConvexPolygon2d footPolygon;
   private final YoGraphicPolygon footPolygonViz;
   private final RobotSide robotSide;

   public SingleFootstepVisualizer(RobotSide robotSide, int maxContactPoints, YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      Integer index = indices.get(robotSide);
      String namePrefix = robotSide.getLowerCaseName() + "Foot" + index;

      YoGraphicsList yoGraphicsList = new YoGraphicsList(namePrefix);
      this.robotSide = robotSide;

      ArrayList<Point2d> polyPoints = new ArrayList<Point2d>();
      yoContactPoints = new YoFramePoint[maxContactPoints];

      for (int i = 0; i < maxContactPoints; i++)
      {
         yoContactPoints[i] = new YoFramePoint(namePrefix + "ContactPoint" + i, ReferenceFrame.getWorldFrame(), registry);
         yoContactPoints[i].set(0.0, 0.0, -1.0);

         YoGraphicPosition baseControlPointViz = new YoGraphicPosition(namePrefix + "Point" + i, yoContactPoints[i], 0.01, YoAppearance.Blue());
         yoGraphicsList.add(baseControlPointViz);

         polyPoints.add(new Point2d());
      }

      footPolygon = new YoFrameConvexPolygon2d(namePrefix + "yoPolygon", "", ReferenceFrame.getWorldFrame(), maxContactPoints, registry);
      footPolygon.setConvexPolygon2d(new ConvexPolygon2d(polyPoints));

      soleFramePose = new YoFramePose(namePrefix + "polygonPose", "", ReferenceFrame.getWorldFrame(), registry);
      soleFramePose.setXYZ(0.0, 0.0, -1.0);

      footPolygonViz = new YoGraphicPolygon(namePrefix + "graphicPolygon", footPolygon, soleFramePose, 1.0, footPolygonAppearances.get(robotSide));

      yoGraphicsList.add(footPolygonViz);

      if (yoGraphicsListRegistry != null)
      {
         yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
         yoGraphicsListRegistry.registerGraphicsUpdatableToUpdateInAPlaybackListener(footPolygonViz);
      }

      index++;
      indices.set(robotSide, index);
   }

   public void visualizeFootstep(Footstep footstep, ContactablePlaneBody bipedFoot)
   {
      List<Point2d> predictedContactPoints = footstep.getPredictedContactPoints();
      
      if (robotSide != footstep.getRobotSide())
         throw new RuntimeException("Wrong Robot Side!");

      if ((predictedContactPoints == null) || (predictedContactPoints.isEmpty()))
      {
         predictedContactPoints = new ArrayList<Point2d>();

         List<FramePoint2d> contactPointsFromContactablePlaneBody = bipedFoot.getContactPoints2d();
         for (int i=0; i<contactPointsFromContactablePlaneBody.size(); i++)
         {
            FramePoint2d point = contactPointsFromContactablePlaneBody.get(i);
            predictedContactPoints.add(point.getPointCopy());
         }
      }
      
      ReferenceFrame soleReferenceFrame = footstep.getSoleReferenceFrame();
      double increaseZSlightlyToSeeBetter = 0.001;
      FramePose soleFramePose = new FramePose(soleReferenceFrame, new Point3d(0.0, 0.0, increaseZSlightlyToSeeBetter), new AxisAngle4d());
      soleFramePose.changeFrame(ReferenceFrame.getWorldFrame());
      
      this.soleFramePose.set(soleFramePose);
      
      for (int i=0; i<predictedContactPoints.size(); i++)
      {
         Point2d contactPoint = predictedContactPoints.get(i);
         
         FramePoint pointInWorld = new FramePoint(soleReferenceFrame, contactPoint.getX(), contactPoint.getY(), 0.0);
         pointInWorld.changeFrame(ReferenceFrame.getWorldFrame());
         
         yoContactPoints[i].set(pointInWorld.getPoint());
      }

      footPolygon.setConvexPolygon2d(new ConvexPolygon2d(predictedContactPoints));
      footPolygonViz.update();
   }
}
