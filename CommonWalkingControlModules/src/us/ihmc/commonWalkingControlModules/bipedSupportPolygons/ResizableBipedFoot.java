package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.ConvexHullCalculator2d;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;


public class ResizableBipedFoot implements BipedFootInterface
{
   private final YoVariableRegistry registry;

   private final RigidBody body;
   private final RobotSide robotSide;
   private final ReferenceFrame footFrame, ankleZUpFrame, soleFrame;

   private final ArrayList<FramePoint> heelPoints, toePoints;
   private final double footLength;
   private final double maxHeelPointsForward, maxToePointsBack;

   private boolean isSupportingFoot = false;

   private final double narrowWidthOnToesPercentage;

   private final EnumYoVariable<FootPolygonEnum> footPolygonInUseEnum;
   private final DoubleYoVariable shift;

   private final FramePoint insideToePoint, outsideToePoint, insideHeelPoint, outsideHeelPoint;

   // Constructor:
   public ResizableBipedFoot(RigidBody body, ReferenceFrame ankleZUpFrame, ReferenceFrame soleFrame, RobotSide robotSide,
                             ArrayList<Point3d> clockwiseToePoints, ArrayList<Point3d> clockwiseHeelPoints, double maxToePointsBack,
                             double maxHeelPointsForward, double narrowWidthOnToesPercentage, YoVariableRegistry yoVariableRegistry,
                             DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      registry = new YoVariableRegistry(robotSide + "BipedFoot");
      this.narrowWidthOnToesPercentage = narrowWidthOnToesPercentage;

      // Checks constants:
      if ((maxToePointsBack < 0.0) || (maxToePointsBack > 1.0))
         throw new RuntimeException("maxToePointsBack < 0.0 || maxToePointsBack > 1.0");
      if ((maxHeelPointsForward < 0.0) || (maxHeelPointsForward > 1.0))
         throw new RuntimeException("maxHeelPointsForward < 0.0 || maxHeelPointsForward > 1.0");

      double footLength = determineFootLength(clockwiseToePoints, clockwiseHeelPoints);
      if (footLength <= 0.0)
         throw new RuntimeException("footLength <= 0.0");

      // Check convex and clockwise:
      ArrayList<Point3d> onToesPoints = new ArrayList<Point3d>();
      for (Point3d point : clockwiseToePoints)
      {
         onToesPoints.add(new Point3d(point));
      }

      for (Point3d point : clockwiseHeelPoints)
      {
         onToesPoints.add(new Point3d(point.x + maxHeelPointsForward * footLength, point.y, point.z));
      }

      if (!ConvexHullCalculator2d.isConvexAndClockwise(projectToXYPlane(onToesPoints)))
         throw new RuntimeException("Not convex and clockwise when fully on toes!");

      ArrayList<Point3d> onHeelPoints = new ArrayList<Point3d>();
      for (Point3d point : clockwiseHeelPoints)
      {
         onHeelPoints.add(new Point3d(point));
      }

      for (Point3d point : clockwiseToePoints)
      {
         onHeelPoints.add(new Point3d(point.x - maxToePointsBack * footLength, point.y, point.z));
      }

      if (!ConvexHullCalculator2d.isConvexAndClockwise(projectToXYPlane(onHeelPoints)))
         throw new RuntimeException("Not convex and clockwise when fully on heel!");

      // Actual construction:
      this.body = body;
      this.robotSide = robotSide;

      this.footFrame = body.getParentJoint().getFrameAfterJoint();
      this.ankleZUpFrame = ankleZUpFrame;
      this.soleFrame = soleFrame;
      this.toePoints = new ArrayList<FramePoint>(clockwiseToePoints.size());
      this.heelPoints = new ArrayList<FramePoint>(clockwiseHeelPoints.size());

      for (Point3d toePoint : clockwiseToePoints)
      {
         this.toePoints.add(new FramePoint(footFrame, toePoint));
      }

      for (Point3d heelPoint : clockwiseHeelPoints)
      {
         this.heelPoints.add(new FramePoint(footFrame, heelPoint));
      }


      this.footLength = footLength;
      this.maxHeelPointsForward = maxHeelPointsForward;
      this.maxToePointsBack = maxToePointsBack;

      this.footPolygonInUseEnum = EnumYoVariable.create(robotSide + "FootPolygonInUse", FootPolygonEnum.class, registry);
      this.shift = new DoubleYoVariable(robotSide + "Shift", registry);

      if (robotSide == RobotSide.LEFT)
      {
         insideToePoint = maxXMinYPointCopy(toePoints);
         outsideToePoint = maxXMaxYPointCopy(toePoints);
         insideHeelPoint = minXMinYPointCopy(heelPoints);
         outsideHeelPoint = minXMaxYPointCopy(heelPoints);
      }
      else
      {
         insideToePoint = maxXMaxYPointCopy(toePoints);
         outsideToePoint = maxXMinYPointCopy(toePoints);
         insideHeelPoint = minXMaxYPointCopy(heelPoints);
         outsideHeelPoint = minXMinYPointCopy(heelPoints);
      }

      if (yoVariableRegistry != null)
      {
         yoVariableRegistry.addChild(registry);
      }
   }

   private static double determineFootLength(ArrayList<Point3d> clockwiseToePoints, ArrayList<Point3d> clockwiseHeelPoints)
   {
      double maxX = Double.NEGATIVE_INFINITY;
      double minX = Double.POSITIVE_INFINITY;
      for (Point3d point : clockwiseToePoints)
      {
         if (point.x > maxX)
            maxX = point.x;
      }

      for (Point3d point : clockwiseHeelPoints)
      {
         if (point.x < minX)
            minX = point.x;
      }

      double footLength = maxX - minX;

      return footLength;
   }

   // Getters:
   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public FrameConvexPolygon2d getFootPolygonInUseInAnkleZUp()
   {
      FootPolygonEnum footPolygonEnum = footPolygonInUseEnum.getEnumValue();

      ArrayList<FramePoint> footPolygonPoints = computeFootPolygonPoints(footPolygonEnum);

      ArrayList<FramePoint2d> projectedFootPolygonPoints = changeFrameToZUpAndProjectToXYPlane(ankleZUpFrame, footPolygonPoints);
      FrameConvexPolygon2d ret = new FrameConvexPolygon2d(projectedFootPolygonPoints);

      return ret;
   }

   private ArrayList<FramePoint> computeFootPolygonPoints(FootPolygonEnum footPolygonEnum)
   {
      if ((shift.getDoubleValue() < 0.0) || (shift.getDoubleValue() > 1.0))
         throw new RuntimeException("shift < 0.0 || shift > 1.0");

      // TODO: Don't create this list every tick. Instead create it once at the beginning.
      ArrayList<FramePoint> footPolygonPoints = new ArrayList<FramePoint>(toePoints.size() + heelPoints.size());
      switch (footPolygonEnum)
      {
         case FLAT :
         case FREE :    // TODO
         {
            footPolygonPoints.addAll(toePoints);
            footPolygonPoints.addAll(heelPoints);

            break;
         }

         case ONHEEL :
         {
            if (shift.getDoubleValue() > maxToePointsBack)
               shift.set(maxToePointsBack);

            for (FramePoint point : toePoints)
            {
               footPolygonPoints.add(new FramePoint(footFrame, point.getX() - shift.getDoubleValue() * footLength, point.getY(), point.getZ()));
            }

            for (FramePoint point : heelPoints)
            {
               footPolygonPoints.add(new FramePoint(point));
            }

            break;
         }

         case ONTOES :
         {
            if (shift.getDoubleValue() > maxHeelPointsForward)
               shift.set(maxHeelPointsForward);

            for (FramePoint point : toePoints)
            {
               footPolygonPoints.add(new FramePoint(footFrame, point.getX(), point.getY() * narrowWidthOnToesPercentage, point.getZ()));
            }

            for (FramePoint point : heelPoints)
            {
               footPolygonPoints.add(new FramePoint(footFrame, point.getX() + shift.getDoubleValue() * footLength, point.getY() * narrowWidthOnToesPercentage,
                       point.getZ()));
            }

            break;
         }

         default :
         {
            throw new RuntimeException("Unrecognized foot polygon: " + footPolygonEnum);
         }
      }

      return footPolygonPoints;
   }

   public FrameConvexPolygon2d getFlatFootPolygonInAnkleZUp()
   {
      ArrayList<FramePoint> footPolygonPoints = computeFootPolygonPoints(FootPolygonEnum.FLAT);
      ArrayList<FramePoint2d> projectedFootPolygonPoints = changeFrameToZUpAndProjectToXYPlane(ankleZUpFrame, footPolygonPoints);

      return new FrameConvexPolygon2d(projectedFootPolygonPoints);
   }

   private ArrayList<FramePoint2d> changeFrameToZUpAndProjectToXYPlane(ReferenceFrame zUpFrame, ArrayList<FramePoint> points)
   {
//    if (!zUpFrame.isZupFrame())
//    {
//       throw new RuntimeException("Must be a ZUp frame!");
//    }

      ArrayList<FramePoint2d> ret = new ArrayList<FramePoint2d>(points.size());

      for (int i = 0; i < points.size(); i++)
      {
         FramePoint framePoint = points.get(i);
         framePoint = framePoint.changeFrameCopy(zUpFrame);

         ret.add(framePoint.toFramePoint2d());
      }

      return ret;
   }

   private ArrayList<Point2d> projectToXYPlane(ArrayList<Point3d> points)
   {
      ArrayList<Point2d> ret = new ArrayList<Point2d>(points.size());
      for (int i = 0; i < points.size(); i++)
      {
         Point3d point3d = points.get(i);
         ret.add(new Point2d(point3d.x, point3d.y));
      }

      return ret;
   }

   public void setIsSupportingFoot(boolean isSupportingFoot)
   {
      this.isSupportingFoot = isSupportingFoot;
   }

   public boolean isSupportingFoot()
   {
      return isSupportingFoot;
   }

   public FramePoint[] getToePointsCopy()
   {
      return new FramePoint[] {new FramePoint(insideToePoint), new FramePoint(outsideToePoint)};
   }

   public FramePoint[] getHeelPointsCopy()
   {
      return new FramePoint[] {new FramePoint(insideHeelPoint), new FramePoint(outsideHeelPoint)};
   }

   // Setters:
   public void setFootPolygonInUse(FootPolygonEnum footPolygonInUse)
   {
      this.footPolygonInUseEnum.set(footPolygonInUse);
      if (footPolygonInUse == FootPolygonEnum.FLAT)
         setShift(Double.NaN);
   }

   public FootPolygonEnum getFootPolygonInUse()
   {
      return footPolygonInUseEnum.getEnumValue();
   }

   public void setShift(double shift)
   {
      if ((shift < 0.0) || (shift > 1.0))
         throw new RuntimeException("shift < 0.0 || shift > 1.0");
      this.shift.set(shift);
   }

   public static ResizableBipedFoot createRectangularRightFoot(double footForward, double footBack, double footWidth, double footHeight,
           double narrowWidthOnToesPercentage, RigidBody body, ReferenceFrame ankleZUpFrame, ReferenceFrame soleFrame, DoubleYoVariable time,
           YoVariableRegistry yoVariableRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      double PREVENT_ROTATION_FACTOR = 0.75;    // 0.8;//0.8;

      return createRectangularRightFoot(PREVENT_ROTATION_FACTOR, PREVENT_ROTATION_FACTOR, footForward, footBack, footWidth, footHeight,
                                        narrowWidthOnToesPercentage, 0.8, 0.8, body, ankleZUpFrame, soleFrame, yoVariableRegistry,
                                        dynamicGraphicObjectsListRegistry);
   }

   // Foot creators:
   public static ResizableBipedFoot createRectangularRightFoot(double preventRotationFactorLength, double preventRotationFactorWidth, double footForward,
           double footBack, double footWidth, double footHeight, double narrowWidthOnToesPercentage, double maxToePointsBack, double maxHeelPointsForward,
           RigidBody body, ReferenceFrame ankleZUpFrame, ReferenceFrame soleFrame, YoVariableRegistry yoVariableRegistry,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      Point3d frontLeft = new Point3d(preventRotationFactorLength * footForward, preventRotationFactorWidth * footWidth / 2.0, -footHeight);
      Point3d frontRight = new Point3d(preventRotationFactorLength * footForward, -preventRotationFactorWidth * footWidth / 2.0, -footHeight);
      Point3d hindRight = new Point3d(-preventRotationFactorLength * footBack, -preventRotationFactorWidth * footWidth / 2.0, -footHeight);
      Point3d hindLeft = new Point3d(-preventRotationFactorLength * footBack, preventRotationFactorWidth * footWidth / 2.0, -footHeight);


      // Toe:
      ArrayList<Point3d> toePoints = new ArrayList<Point3d>();
      toePoints.add(frontLeft);
      toePoints.add(frontRight);

      // Heel:
      ArrayList<Point3d> heelPoints = new ArrayList<Point3d>();
      heelPoints.add(hindRight);
      heelPoints.add(hindLeft);

      return new ResizableBipedFoot(body, ankleZUpFrame, soleFrame, RobotSide.RIGHT, toePoints, heelPoints, maxToePointsBack, maxHeelPointsForward,
                                    narrowWidthOnToesPercentage, yoVariableRegistry, dynamicGraphicObjectsListRegistry);
   }

   public ResizableBipedFoot createLeftFootAsMirrorImage(RigidBody body, ReferenceFrame ankleZUpFrame, ReferenceFrame soleFrame,
           YoVariableRegistry yoVariableRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      if (this.getRobotSide() != RobotSide.RIGHT)
         throw new RuntimeException("Implicit parameter is not a right foot!");

      ArrayList<Point3d> mirrorToePoints = new ArrayList<Point3d>(toePoints.size());

      // Add the mirrored points in reverse order:
      for (int i = toePoints.size() - 1; i >= 0; i--)
      {
         mirrorToePoints.add(new Point3d(toePoints.get(i).getX(), -toePoints.get(i).getY(), toePoints.get(i).getZ()));
      }

      ArrayList<Point3d> mirrorHeelPoints = new ArrayList<Point3d>(heelPoints.size());
      for (int i = heelPoints.size() - 1; i >= 0; i--)
      {
         mirrorHeelPoints.add(new Point3d(heelPoints.get(i).getX(), -heelPoints.get(i).getY(), heelPoints.get(i).getZ()));
      }

      return new ResizableBipedFoot(body, ankleZUpFrame, soleFrame, RobotSide.LEFT, mirrorToePoints, mirrorHeelPoints, this.maxToePointsBack,
                                    this.maxHeelPointsForward, this.narrowWidthOnToesPercentage, yoVariableRegistry, dynamicGraphicObjectsListRegistry);
   }

   private static FramePoint minXMaxYPointCopy(ArrayList<FramePoint> pointList)
   {
      FramePoint ret = null;
      double retX = Double.POSITIVE_INFINITY;
      double retY = Double.NEGATIVE_INFINITY;
      for (FramePoint point : pointList)
      {
         if (point.getX() < retX)
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
         else if ((point.getX() == retX) && (point.getY() > retY))
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
      }

      if (ret == null)
         throw new RuntimeException("ret is still null");

      return new FramePoint(ret);
   }

   private static FramePoint minXMinYPointCopy(ArrayList<FramePoint> pointList)
   {
      FramePoint ret = null;
      double retX = Double.POSITIVE_INFINITY;
      double retY = Double.POSITIVE_INFINITY;
      for (FramePoint point : pointList)
      {
         if (point.getX() < retX)
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
         else if ((point.getX() == retX) && (point.getY() < retY))
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
      }

      if (ret == null)
         throw new RuntimeException("ret is still null");

      return new FramePoint(ret);
   }

   private static FramePoint maxXMaxYPointCopy(ArrayList<FramePoint> pointList)
   {
      FramePoint ret = null;
      double retX = Double.NEGATIVE_INFINITY;
      double retY = Double.NEGATIVE_INFINITY;
      for (FramePoint point : pointList)
      {
         if (point.getX() > retX)
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
         else if ((point.getX() == retX) && (point.getY() > retY))
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
      }

      if (ret == null)
         throw new RuntimeException("ret is still null");

      return new FramePoint(ret);
   }

   private static FramePoint maxXMinYPointCopy(ArrayList<FramePoint> pointList)
   {
      FramePoint ret = null;
      double retX = Double.NEGATIVE_INFINITY;
      double retY = Double.POSITIVE_INFINITY;
      for (FramePoint point : pointList)
      {
         if (point.getX() > retX)
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
         else if ((point.getX() == retX) && (point.getY() < retY))
         {
            ret = point;
            retX = point.getX();
            retY = point.getY();
         }
      }

      if (ret == null)
         throw new RuntimeException("ret is still null");

      return new FramePoint(ret);
   }

   public String toString()
   {
      return "footLength = " + footLength + ", isSupportingFoot = " + isSupportingFoot;
   }

   public FrameConvexPolygon2d getFootPolygonInSoleFrame()
   {
      FootPolygonEnum footPolygonEnum = footPolygonInUseEnum.getEnumValue();

      ArrayList<FramePoint> footPolygonPoints = computeFootPolygonPoints(footPolygonEnum);

      ArrayList<FramePoint2d> projectedFootPolygonPoints = changeFrameToZUpAndProjectToXYPlane(soleFrame, footPolygonPoints);
      FrameConvexPolygon2d ret = new FrameConvexPolygon2d(projectedFootPolygonPoints);

      return ret;
   }

   public void setFootPolygon(FrameConvexPolygon2d footPolygon)
   {
      // empty
   }

   public ReferenceFrame getFootFrame()
   {
      return getBodyFrame();
   }

   public FrameConvexPolygon2d getContactPolygon()
   {
      return getFootPolygonInSoleFrame();
   }

   public RigidBody getRigidBody()
   {
      return body;
   }

   public List<FramePoint> getContactPoints()
   {
      List<FramePoint> ret = new ArrayList<FramePoint>(heelPoints.size() + toePoints.size());

      for (FramePoint point : heelPoints)
      {
         ret.add(new FramePoint(point));
      }

      for (FramePoint point : toePoints)
      {
         ret.add(new FramePoint(point));
      }

      return ret;
   }

   public ReferenceFrame getBodyFrame()
   {
      return footFrame;
   }

   public List<FramePoint> computeFootPoints()
   {
      return computeFootPolygonPoints(getFootPolygonInUse());
   }
}
