package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class StateEstimatorErrorCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final StateEstimator orientationEstimator;
   private final Robot robot;
   private final Joint estimationJoint;

   private final DoubleYoVariable orientationError = new DoubleYoVariable("orientationError", registry);
   private final DoubleYoVariable angularVelocityError = new DoubleYoVariable("angularVelocityError", registry);
   private final DoubleYoVariable comXYPositionError = new DoubleYoVariable("comXYPositionError", registry);
   private final DoubleYoVariable comZPositionError = new DoubleYoVariable("comZPositionError", registry);
   private final DoubleYoVariable comVelocityError = new DoubleYoVariable("comVelocityError", registry);

   private final YoFramePoint perfectCoMPosition = new YoFramePoint("perfectCoMPosition", ReferenceFrame.getWorldFrame(), registry);
   private final YoFrameVector perfectCoMVelocity = new YoFrameVector("perfectCoMVelocity", ReferenceFrame.getWorldFrame(), registry);
   
   private final YoFrameVector perfectAngularVelocity = new YoFrameVector("perfectAngularVelocity", ReferenceFrame.getWorldFrame(), registry);

   public StateEstimatorErrorCalculator(Robot robot, Joint estimationJoint, StateEstimator orientationEstimator,
           YoVariableRegistry parentRegistry)
   {
      this.robot = robot;
      this.estimationJoint = estimationJoint;
      this.orientationEstimator = orientationEstimator;

      parentRegistry.addChild(registry);
   }

   private final FrameOrientation estimatedOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame());
   
   private void computeOrientationError()
   {
      orientationEstimator.getEstimatedOrientation(estimatedOrientation);
      Quat4d estimatedOrientationQuat4d = new Quat4d();
      estimatedOrientation.getQuaternion(estimatedOrientationQuat4d);

      Quat4d orientationErrorQuat4d = new Quat4d();
      estimationJoint.getRotationToWorld(orientationErrorQuat4d);
      orientationErrorQuat4d.mulInverse(estimatedOrientationQuat4d);

      AxisAngle4d orientationErrorAxisAngle = new AxisAngle4d();
      orientationErrorAxisAngle.set(orientationErrorQuat4d);

      double errorAngle = AngleTools.trimAngleMinusPiToPi(orientationErrorAxisAngle.getAngle());

      orientationError.set(Math.abs(errorAngle));
   }

   private final FrameVector estimatedAngularVelocityFrameVector = new FrameVector(ReferenceFrame.getWorldFrame());
   
   private void computeAngularVelocityError()
   {
      orientationEstimator.getEstimatedAngularVelocity(estimatedAngularVelocityFrameVector);
      
      Vector3d estimatedAngularVelocity = estimatedAngularVelocityFrameVector.getVectorCopy();
      Vector3d actualAngularVelocity = new Vector3d();
      estimationJoint.getAngularVelocityInBody(actualAngularVelocity);

      perfectAngularVelocity.set(actualAngularVelocity);
      
      actualAngularVelocity.sub(estimatedAngularVelocity);
      angularVelocityError.set(actualAngularVelocity.length());
   }

   private final FramePoint estimatedCoMPosition = new FramePoint();
   
   private void computeCoMPositionError()
   {
      Point3d comPoint = new Point3d();
      Vector3d linearVelocity = new Vector3d();
      Vector3d angularMomentum = new Vector3d();

      robot.computeCOMMomentum(comPoint, linearVelocity, angularMomentum);
      perfectCoMPosition.set(comPoint);
      
      Vector3d comError = new Vector3d();
      orientationEstimator.getEstimatedCoMPosition(estimatedCoMPosition);
      comError.set(estimatedCoMPosition.getPointCopy());
      comError.sub(comPoint);

      comZPositionError.set(comError.getZ());

      comError.setZ(0.0);
      comXYPositionError.set(comError.length());
   }

   private final FrameVector estimatedCoMVelocityFrameVector = new FrameVector();

   private void computeCoMVelocityError()
   {
      Point3d comPoint = new Point3d();
      Vector3d linearVelocity = new Vector3d();
      Vector3d angularMomentum = new Vector3d();

      double mass = robot.computeCOMMomentum(comPoint, linearVelocity, angularMomentum);
      linearVelocity.scale(1.0 / mass);
      perfectCoMVelocity.set(linearVelocity);
      
      orientationEstimator.getEstimatedCoMVelocity(estimatedCoMVelocityFrameVector);
      Vector3d estimatedCoMVelocity = estimatedCoMVelocityFrameVector.getVectorCopy();

      estimatedCoMVelocity.sub(linearVelocity);
      comVelocityError.set(estimatedCoMVelocity.length());
   }


   public void computeErrors()
   {
      computeOrientationError();
      computeAngularVelocityError();
      computeCoMPositionError();
      computeCoMVelocityError();
   }

}
