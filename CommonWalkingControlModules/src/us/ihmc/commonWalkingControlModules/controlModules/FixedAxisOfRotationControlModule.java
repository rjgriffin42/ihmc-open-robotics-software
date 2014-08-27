package us.ihmc.commonWalkingControlModules.controlModules;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class FixedAxisOfRotationControlModule
{
   private final YoVariableRegistry registry;

   private final ReferenceFrame bodyFrame;
   private final ReferenceFrame baseFrame;

   private final YoFramePoint initialPosition;
   private final YoFrameOrientation initialOrientation;
   private final YoFrameVector axisOfRotation;
   private final YoFramePoint offset;

   private final FramePose desiredPose;
   private final Twist desiredTwist = new Twist();
   private final SpatialAccelerationVector feedForward = new SpatialAccelerationVector();

   private final Vector3d zero = new Vector3d();

   public FixedAxisOfRotationControlModule(String namePrefix, ReferenceFrame bodyFrame, ReferenceFrame baseFrame)
   {
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.bodyFrame = bodyFrame;
      this.baseFrame = baseFrame;
      this.desiredPose = new FramePose(baseFrame);
      this.initialPosition = new YoFramePoint(namePrefix + "InitialPosition", baseFrame, registry);
      this.initialOrientation = new YoFrameOrientation(namePrefix + "InitialOrientation", "", baseFrame, registry);
      this.axisOfRotation = new YoFrameVector(namePrefix + "Axis", baseFrame, registry);
      this.offset = new YoFramePoint(namePrefix + "Offset", baseFrame, registry);
   }

   public void initialize(FrameVector axisOfRotation, FramePoint offset)
   {
      Transform3D bodyToBase = bodyFrame.getTransformToDesiredFrame(baseFrame);
      Vector3d translation = new Vector3d();
      bodyToBase.get(translation);
      this.initialPosition.set(translation);
      this.initialOrientation.set(bodyToBase);
      
      axisOfRotation.changeFrame(baseFrame);
      offset.changeFrame(baseFrame);
      
      this.axisOfRotation.set(axisOfRotation);
      this.offset.set(offset);
   }

   public void compute(double q, double qd, double qdd)
   {
      FramePoint initial = initialPosition.getFramePointCopy();
      FrameVector initialMinusOffset = new FrameVector(initial);
      FramePoint offsetCopy = offset.getFramePointCopy();
      initialMinusOffset.sub(offsetCopy);

      Vector3d axisOfRotationVector = axisOfRotation.getFrameVectorCopy().getVector();
      AxisAngle4d axisAngle = new AxisAngle4d(axisOfRotationVector, q);
      Matrix3d rotationMatrix = new Matrix3d();
      rotationMatrix.set(axisAngle);

      rotationMatrix.transform(initialMinusOffset.getVector());
      FramePoint positionPoint = new FramePoint(initialMinusOffset);
      positionPoint.add(offsetCopy);
      desiredPose.setPosition(positionPoint);

      Matrix3d initialRotation = new Matrix3d();
      initialOrientation.getMatrix3d(initialRotation);
      rotationMatrix.mul(initialRotation, rotationMatrix);

      FrameOrientation orientation = new FrameOrientation(desiredPose.getReferenceFrame(), rotationMatrix);
      desiredPose.setOrientation(orientation);

      FrameVector axisOfRotation = this.axisOfRotation.getFrameVectorCopy();
      FramePoint offset = this.offset.getFramePointCopy();
      
      axisOfRotation.changeFrame(bodyFrame);
      offset.changeFrame(bodyFrame);
      
      desiredTwist.setScrew(bodyFrame, baseFrame, bodyFrame, qd, 0.0, axisOfRotation.getVector(), offset.getVectorCopy());
      feedForward.setScrew(bodyFrame, baseFrame, bodyFrame, qd, qdd, 0.0, 0.0, axisOfRotation.getVector(), zero, offset.getVectorCopy(), zero);
   }

   public void pack(FramePose desiredPose, Twist desiredTwist, SpatialAccelerationVector feedForward)
   {
      desiredPose.setPose(this.desiredPose);
      desiredTwist.set(this.desiredTwist);
      feedForward.set(this.feedForward);
   }
}
