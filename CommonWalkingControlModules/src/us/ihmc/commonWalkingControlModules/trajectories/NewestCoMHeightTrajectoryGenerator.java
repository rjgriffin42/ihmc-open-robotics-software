package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.List;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.LineSegment2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class NewestCoMHeightTrajectoryGenerator implements CoMHeightTrajectoryGenerator
{
   private static final boolean DEBUG = false; 
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final TwoPointSpline1D spline = new TwoPointSpline1D(registry);
   private final DoubleYoVariable nominalHeightAboveGround = new DoubleYoVariable("nominalHeightAboveGround", registry);
   private LineSegment2d projectionSegment;

   public NewestCoMHeightTrajectoryGenerator(double nominalHeightAboveGround, YoVariableRegistry parentRegistry)
   {
      this.nominalHeightAboveGround.set(nominalHeightAboveGround);
      parentRegistry.addChild(registry);
   }
   
   public void setNominalHeightAboveGround(double nominalHeightAboveGround)
   {
      this.nominalHeightAboveGround.set(nominalHeightAboveGround);
   }

   public void initialize(RobotSide supportLeg, Footstep nextFootstep, List<PlaneContactState> contactStates)
   {
      FramePoint[] contactFramePositions = getContactStateCenters(contactStates, nextFootstep);
      projectionSegment = new LineSegment2d(getPoint2d(contactFramePositions[0]), getPoint2d(contactFramePositions[1]));
      double s0 = 0.0;
      double sF = projectionSegment.length();
      double z0 = contactFramePositions[0].getZ() + nominalHeightAboveGround.getDoubleValue();
      double zF = contactFramePositions[1].getZ() + nominalHeightAboveGround.getDoubleValue();
      Point2d point0 = new Point2d(s0, z0);
      Point2d pointF = new Point2d(sF, zF);
      Point2d[] points = new Point2d[] {point0, pointF};
      double[] slopes = new double[] {0.0, 0.0};
      double[] secondDerivatives = new double[] {0.0, 0.0};
      spline.setPoints(points, slopes, secondDerivatives);
   }

   private Point2d getPoint2d(FramePoint point)
   {
      return new Point2d(point.getX(), point.getY());
   }

   public void solve(CoMHeightPartialDerivativesData coMHeightPartialDerivativesDataToPack, ContactStatesAndUpcomingFootstepData centerOfMassHeightInputData)
   {
      Point2d queryPoint = getCenterOfMass2d(centerOfMassHeightInputData.getCenterOfMassFrame());

      projectionSegment.orthogonalProjection(queryPoint);
      double splineQuery = projectionSegment.percentageAlongLineSegment(queryPoint) * projectionSegment.length();

      double[] splineOutput = spline.getZSlopeAndSecondDerivative(splineQuery);
      double z = splineOutput[0];
      double dzds = splineOutput[1];
      double ddzdds = splineOutput[2];

      double[] partialDerivativesWithRespectToS = getPartialDerivativesWithRespectToS(projectionSegment);
      double dsdx = partialDerivativesWithRespectToS[0];
      double dsdy = partialDerivativesWithRespectToS[1];
      double ddsddx = 0;
      double ddsddy = 0;
      double ddsdxdy = 0;
      
      double dzdx = dsdx * dzds;
      double dzdy = dsdy * dzds;
      double ddzddx = dzds * ddsddx + ddzdds * dsdx * dsdx;
      double ddzddy = dzds * ddsddy + ddzdds * dsdy * dsdy;
      double ddzdxdy = ddzdds * dsdx * dsdy + dzds * ddsdxdy;
      
      coMHeightPartialDerivativesDataToPack.setCoMHeight(z);
      coMHeightPartialDerivativesDataToPack.setPartialDzDx(dzdx);
      coMHeightPartialDerivativesDataToPack.setPartialDzDy(dzdy);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDxDy(ddzdxdy);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDx2(ddzddx);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDy2(ddzddy);
   }

   private double[] getPartialDerivativesWithRespectToS(LineSegment2d segment)
   {
      double dsdx = (segment.getSecondEndPointCopy().getX() - segment.getFirstEndPointCopy().getX()) / segment.length();
      double dsdy = (segment.getSecondEndPointCopy().getY() - segment.getFirstEndPointCopy().getY()) / segment.length();

      return new double[] {dsdx, dsdy};
   }

   private FramePoint[] getContactStateCenters(List<PlaneContactState> contactStates, Footstep nextFootstep)
   {
      FramePoint contactFramePosition0 = new FramePoint(contactStates.get(0).getBodyFrame());
      contactFramePosition0.changeFrame(worldFrame);
      FramePoint contactFramePosition1;
      if (nextFootstep == null)
      {
         contactFramePosition1 = new FramePoint(contactStates.get(1).getBodyFrame());
         contactFramePosition1.changeFrame(worldFrame);
      }
      else
      {
         contactFramePosition1 = nextFootstep.getPositionInFrame(worldFrame);
      }
      if (DEBUG)
      {
         System.out.println("nextFootstep: " + nextFootstep);
         System.out.println("contactFramePosition0: " + contactFramePosition0);
         System.out.println("contactFramePosition1: " + contactFramePosition1 + "\n");
      }
      return new FramePoint[]{contactFramePosition0, contactFramePosition1};
   }

   private Point2d getCenterOfMass2d(ReferenceFrame centerOfMassFrame)
   {
      FramePoint coM = new FramePoint(centerOfMassFrame);
      coM.changeFrame(worldFrame);

      return getPoint2d(coM);
   }
}
