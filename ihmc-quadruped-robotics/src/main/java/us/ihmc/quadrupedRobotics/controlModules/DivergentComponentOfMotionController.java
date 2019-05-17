package us.ihmc.quadrupedRobotics.controlModules;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.QuadrupedSupportPolygons;
import us.ihmc.commonWalkingControlModules.capturePoint.ICPControlGains;
import us.ihmc.commonWalkingControlModules.capturePoint.ICPControlGainsProvider;
import us.ihmc.commonWalkingControlModules.capturePoint.ParameterizedICPControlGains;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.*;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.quadrupedRobotics.controller.toolbox.LinearInvertedPendulumModel;
import us.ihmc.robotics.geometry.ConvexPolygonScaler;
import us.ihmc.robotics.math.filters.RateLimitedYoFramePoint;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoFramePoint2D;
import us.ihmc.yoVariables.variable.YoFramePoint3D;
import us.ihmc.yoVariables.variable.YoFrameVector2D;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

import static us.ihmc.humanoidRobotics.footstep.FootstepUtils.worldFrame;

public class DivergentComponentOfMotionController
{
   private final ReferenceFrame comZUpFrame;
   private final LinearInvertedPendulumModel lipModel;
   private final double controlDT;
   private final FixedFramePoint3DBasics vrpPositionSetpoint;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final Vector2dZUpFrame dcmVelocityDirectionFrame = new Vector2dZUpFrame("dcmVelocityDirectionFrame", worldFrame);
   private final DoubleProvider maxVRPDistanceFromSupport = new DoubleParameter("maxVRPDistanceFromSupport", registry, Double.POSITIVE_INFINITY);

   private final ConvexPolygonScaler polygonScaler = new ConvexPolygonScaler();

   private final ICPControlGainsProvider capturePositionGains;

   private final YoFrameVector3D dcmError;

   private final YoFrameVector3D vrpProportionalAction;
   private final YoFrameVector3D vrpIntegralAction;
   private final YoFrameVector3D vrpFeedbackAction;
   private final YoFrameVector2D cumulativeDcmError;

   private final RateLimitedYoFramePoint yoLimitedVrpPositionSetpoint;
   private final YoFramePoint3D yoProjectedVrpPositionSetpoint;

   private final FramePoint3D dcmPositionEstimate = new FramePoint3D();
   private final FramePoint3D dcmPositionSetpoint = new FramePoint3D();
   private final FrameVector3D dcmVelocitySetpoint = new FrameVector3D();

   private final FrameConvexPolygon2DReadOnly supportPolygonInMidZUpFrame;
   private final FrameConvexPolygon2D scaledPolygon;

   public DivergentComponentOfMotionController(QuadrupedSupportPolygons supportPolygons, ReferenceFrame comZUpFrame, double controlDT, LinearInvertedPendulumModel lipModel, YoVariableRegistry parentRegistry)
   {
      this.comZUpFrame = comZUpFrame;
      this.controlDT = controlDT;
      this.lipModel = lipModel;

      supportPolygonInMidZUpFrame = supportPolygons.getSupportPolygonInMidFeetZUp();
      scaledPolygon = new FrameConvexPolygon2D(supportPolygonInMidZUpFrame.getReferenceFrame());

      dcmError = new YoFrameVector3D("dcmError", comZUpFrame, registry);

      cumulativeDcmError = new YoFrameVector2D("cumulativeDcmError", comZUpFrame, registry);

      vrpProportionalAction = new YoFrameVector3D("vrpProportionalAction", comZUpFrame, registry);
      vrpIntegralAction = new YoFrameVector3D("vrpIntegralAction", comZUpFrame, registry);
      vrpFeedbackAction = new YoFrameVector3D("vrpFeedbackAction", comZUpFrame, registry);

      vrpPositionSetpoint = new FramePoint3D(comZUpFrame);

      ICPControlGains defaultIcpControlGains = new ICPControlGains();
      defaultIcpControlGains.setKpParallelToMotion(1.0);
      defaultIcpControlGains.setKpOrthogonalToMotion(1.0);

      capturePositionGains = new ParameterizedICPControlGains("", defaultIcpControlGains, registry);

      yoLimitedVrpPositionSetpoint = new RateLimitedYoFramePoint("vrpPositionSetpointInCoMZUpFrame", "", registry, capturePositionGains.getYoFeedbackPartMaxRate(), controlDT, comZUpFrame);
      yoProjectedVrpPositionSetpoint = new YoFramePoint3D("projectedVrpPositionSetpointInCoMZUpFrame", comZUpFrame, registry);

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      cumulativeDcmError.setToZero();
      yoLimitedVrpPositionSetpoint.reset();
   }


   private final FramePoint2D unprojectedPoint = new FramePoint2D();
   private final FramePoint2D projectedPoint = new FramePoint2D();

   public void compute(FixedFramePoint3DBasics vrpPositionSetpointToPack, FramePoint3DReadOnly dcmPositionEstimate, FramePoint3DReadOnly dcmPositionSetpoint,
                       FrameVector3DReadOnly dcmVelocitySetpoint)
   {
      this.dcmPositionEstimate.setIncludingFrame(dcmPositionEstimate);
      this.dcmPositionSetpoint.setIncludingFrame(dcmPositionSetpoint);
      this.dcmVelocitySetpoint.setIncludingFrame(dcmVelocitySetpoint);

      this.dcmPositionSetpoint.changeFrame(comZUpFrame);
      this.dcmVelocitySetpoint.changeFrame(comZUpFrame);
      this.dcmPositionEstimate.changeFrame(comZUpFrame);

      dcmError.sub(this.dcmPositionSetpoint, this.dcmPositionEstimate);

      vrpProportionalAction.set(computeProportionalAction());
      vrpIntegralAction.set(computeIntegralAction());

      vrpFeedbackAction.add(vrpProportionalAction, vrpIntegralAction);

      double omega = lipModel.getNaturalFrequency();

      vrpPositionSetpoint.set(this.dcmVelocitySetpoint);
      vrpPositionSetpoint.scale(-1.0 / omega);
      vrpPositionSetpoint.sub(vrpFeedbackAction);
      vrpPositionSetpoint.add(this.dcmPositionEstimate);

      yoLimitedVrpPositionSetpoint.update(vrpPositionSetpoint);

      if (Double.isFinite(maxVRPDistanceFromSupport.getValue()))
      {
         scaledPolygon.clear();
         scaledPolygon.setReferenceFrame(supportPolygonInMidZUpFrame.getReferenceFrame());
         polygonScaler.scaleConvexPolygon(supportPolygonInMidZUpFrame, -maxVRPDistanceFromSupport.getValue(), scaledPolygon);

         unprojectedPoint.setToZero(comZUpFrame);
         projectedPoint.setToZero(comZUpFrame);

         unprojectedPoint.set(yoLimitedVrpPositionSetpoint);

         scaledPolygon.changeFrame(comZUpFrame);
         if (scaledPolygon.isPointInside(unprojectedPoint))
         {
            projectedPoint.set(unprojectedPoint);
         }
         else
         {
            scaledPolygon.orthogonalProjection(unprojectedPoint, projectedPoint);
         }

         yoProjectedVrpPositionSetpoint.set(yoLimitedVrpPositionSetpoint);
         yoProjectedVrpPositionSetpoint.set(projectedPoint);
      }
      else
      {
         yoProjectedVrpPositionSetpoint.set(yoLimitedVrpPositionSetpoint);
      }

      vrpPositionSetpointToPack.setMatchingFrame(yoProjectedVrpPositionSetpoint);
   }

   public FrameVector3DReadOnly getDcmError()
   {
      return dcmError;
   }

   private final FrameVector2D tmpProportionalAction = new FrameVector2D();

   private FrameVector2DReadOnly computeProportionalAction()
   {
      double epsilonZeroICPVelocity = 1e-5;

      if (dcmVelocitySetpoint.lengthSquared() > MathTools.square(epsilonZeroICPVelocity))
      {
         dcmVelocityDirectionFrame.setXAxis(dcmVelocitySetpoint);
         tmpProportionalAction.setIncludingFrame(dcmError);
         tmpProportionalAction.changeFrame(dcmVelocityDirectionFrame);
         tmpProportionalAction.scale(capturePositionGains.getKpParallelToMotion(), capturePositionGains.getKpOrthogonalToMotion());
         tmpProportionalAction.setX(MathTools.clamp(tmpProportionalAction.getX(), capturePositionGains.getFeedbackPartMaxValueParallelToMotion()));
         tmpProportionalAction.setY(MathTools.clamp(tmpProportionalAction.getY(), capturePositionGains.getFeedbackPartMaxValueOrthogonalToMotion()));
         tmpProportionalAction.changeFrame(comZUpFrame);
      }
      else
      {
         tmpProportionalAction.setIncludingFrame(dcmError);
         tmpProportionalAction.scale(capturePositionGains.getKpOrthogonalToMotion(), capturePositionGains.getKpOrthogonalToMotion());
         tmpProportionalAction.setX(MathTools.clamp(tmpProportionalAction.getX(), capturePositionGains.getFeedbackPartMaxValueOrthogonalToMotion()));
         tmpProportionalAction.setY(MathTools.clamp(tmpProportionalAction.getY(), capturePositionGains.getFeedbackPartMaxValueOrthogonalToMotion()));
      }

      return tmpProportionalAction;
   }

   private final FrameVector2D tmpIntegralAction = new FrameVector2D();

   private FrameVector2DReadOnly computeIntegralAction()
   {
      tmpIntegralAction.setToZero(comZUpFrame);
      if (capturePositionGains.getKi() < 1.0e-5)
      {
         cumulativeDcmError.setToZero();
      }
      else
      {
         double maxError = capturePositionGains.getMaxIntegralError();
         cumulativeDcmError.scale(capturePositionGains.getIntegralLeakRatio());
         cumulativeDcmError.checkReferenceFrameMatch(dcmError);
         cumulativeDcmError.add(controlDT * dcmError.getX(), controlDT * dcmError.getY());

         cumulativeDcmError.scale(Math.max(cumulativeDcmError.length(), maxError) / cumulativeDcmError.length());

         tmpIntegralAction.set(cumulativeDcmError);
         tmpIntegralAction.scale(capturePositionGains.getKi());
      }

      return tmpIntegralAction;
   }


   private class Vector2dZUpFrame extends ReferenceFrame
   {
      private final FrameVector2D xAxis;
      private final Vector3D x = new Vector3D();
      private final Vector3D y = new Vector3D();
      private final Vector3D z = new Vector3D();
      private final RotationMatrix rotation = new RotationMatrix();

      Vector2dZUpFrame(String string, ReferenceFrame parentFrame)
      {
         super(string, parentFrame);
         xAxis = new FrameVector2D(parentFrame);
      }

      void setXAxis(FrameTuple3DReadOnly xAxis)
      {
         this.xAxis.setIncludingFrame(xAxis);
         this.xAxis.changeFrameAndProjectToXYPlane(getParent());
         this.xAxis.normalize();
         update();
      }

      @Override
      protected void updateTransformToParent(RigidBodyTransform transformToParent)
      {
         x.set(xAxis.getX(), xAxis.getY(), 0.0);
         z.set(0.0, 0.0, 1.0);
         y.cross(z, x);

         rotation.setColumns(x, y, z);

         transformToParent.setRotationAndZeroTranslation(rotation);
      }
   }
}
