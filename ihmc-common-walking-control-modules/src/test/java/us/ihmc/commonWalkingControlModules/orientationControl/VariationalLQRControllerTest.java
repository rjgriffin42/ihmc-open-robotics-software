package us.ihmc.commonWalkingControlModules.orientationControl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionBasics;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;

import java.util.Random;

import static us.ihmc.robotics.Assert.assertEquals;

public class VariationalLQRControllerTest
{
   @Test
   public void testWithNoFeedback()
   {
      VariationalLQRController controller = new VariationalLQRController();

      int iters = 1000;
      Random random = new Random(1738L);

      for (int i = 0; i < iters; i++)
      {
         QuaternionReadOnly desiredOrientation = EuclidCoreRandomTools.nextQuaternion(random);
         Vector3DReadOnly desiredAngularVelocity = EuclidCoreRandomTools.nextVector3D(random, new Vector3D(10.0, 10.0, 10.0));
         Vector3DReadOnly desiredTorque = EuclidCoreRandomTools.nextVector3D(random, new Vector3D(10.0, 10.0, 10.0));

         controller.setDesired(desiredOrientation, desiredAngularVelocity, desiredTorque);
         controller.compute(desiredOrientation, desiredAngularVelocity);

         Vector3DBasics feedbackTorque = new Vector3D();
         controller.getDesiredTorque(feedbackTorque);

         EuclidCoreTestTools.assertVector3DGeometricallyEquals(desiredTorque, feedbackTorque, 1e-5);
      }
   }

   @Disabled
   @Test
   public void testWithEasyOrientationError()
   {
      VariationalLQRController controller = new VariationalLQRController();

      int iters = 1000;
      Random random = new Random(1738L);

      for (int i = 0; i < iters; i++)
      {
         QuaternionReadOnly desiredOrientation = EuclidCoreRandomTools.nextQuaternion(random);
         Vector3DReadOnly desiredAngularVelocity = EuclidCoreRandomTools.nextVector3D(random, new Vector3D(10.0, 10.0, 10.0));
//         Vector3DReadOnly desiredTorque = EuclidCoreRandomTools.nextVector3D(random, new Vector3D(10.0, 10.0, 10.0));
         Vector3D desiredTorque = new Vector3D();

         QuaternionBasics pitchedOrientation = new Quaternion(desiredOrientation);
         QuaternionBasics yawedOrientation = new Quaternion(desiredOrientation);
         QuaternionBasics rolledOrientation = new Quaternion(desiredOrientation);

         double pitchRotation = RandomNumbers.nextDouble(random, Math.toRadians(10.0));
         double yawRotation = RandomNumbers.nextDouble(random, Math.toRadians(10.0));
         double rollRotation = RandomNumbers.nextDouble(random, Math.toRadians(10.0));

         pitchedOrientation.appendPitchRotation(pitchRotation);
         yawedOrientation.appendYawRotation(yawRotation);
         rolledOrientation.appendRollRotation(rollRotation);

         controller.setDesired(desiredOrientation, desiredAngularVelocity, desiredTorque);
         Vector3DBasics feedbackTorque = new Vector3D();

         // check yaw only
         controller.compute(yawedOrientation, desiredAngularVelocity);

         controller.getDesiredTorque(feedbackTorque);
         assertEquals(0.0, feedbackTorque.getX() - desiredTorque.getX(), 1e-5);
         assertEquals(0.0, feedbackTorque.getY() - desiredTorque.getY(), 1e-5);
         assertEquals(-Math.signum(yawRotation), Math.signum(feedbackTorque.getZ() - desiredTorque.getZ()), 1e-5);

         // check pitch only
         controller.compute(pitchedOrientation, desiredAngularVelocity);

         controller.getDesiredTorque(feedbackTorque);
         assertEquals(0.0, feedbackTorque.getX(), 1e-5);
         assertEquals(-Math.signum(pitchRotation), Math.signum(feedbackTorque.getY()), 1e-5);
         assertEquals(0.0, feedbackTorque.getZ(), 1e-5);
      }
   }
}
