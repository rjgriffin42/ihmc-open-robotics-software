package us.ihmc.robotics.screwTheory;

import java.util.Random;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.geometry.ReferenceFrameMismatchException;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class Twist extends SpatialMotionVector
{
   private Vector3D freeVector = new Vector3D();    // to store intermediate results
   private RigidBodyTransform freeTransform = new RigidBodyTransform();

   public Twist()
   {
      super();
   }

   /**
    * Initiates the angular velocity and linear velocity to zero
    * @param bodyFrame what we're specifying the twist of
    * @param baseFrame with respect to what we're specifying the twist
    * @param expressedInFrame in which reference frame the twist is expressed
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame)
   {
      super(bodyFrame, baseFrame, expressedInFrame);
   }

   /**
    * @param bodyFrame what we're specifying the twist of
    * @param baseFrame with respect to what we're specifying the twist
    * @param expressedInFrame in which reference frame the twist is expressed
    * @param linearVelocity linear velocity part of the twist
    * @param angularVelocity angular velocity part of the twist
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, Vector3DReadOnly linearVelocity, Vector3DReadOnly angularVelocity)
   {
      super(bodyFrame, baseFrame, expressedInFrame, linearVelocity, angularVelocity);
   }

   /**
    * @param bodyFrame what we're specifying the motion of
    * @param baseFrame with respect to what we're specifying the motion
    * @param linearPart linear part of the spatial motion vector expressed in the {@code expressedInFrame} to use.
    * @param angularPart angular part of the spatial motion vector expressed in the {@code expressedInFrame} to use.
    * @throws ReferenceFrameMismatchException if the linear and angular parts are not expressed in the same reference frame.
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, FrameVector3D linearVelocity, FrameVector3D angularVelocity)
   {
      super(bodyFrame, baseFrame, linearVelocity, angularVelocity);
   }

   /**
    * Construct using a Matrix ([omega; v])
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, DenseMatrix64F twistMatrix)
   {
      super(bodyFrame, baseFrame, expressedInFrame, twistMatrix);
   }

   /**
    * Construct using a double array ([omega; v])
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double[] twist)
   {
      super(bodyFrame, baseFrame, expressedInFrame, twist);
   }

   /**
    * Copy constructor
    */
   public Twist(Twist other)
   {
      super(other);
   }

   /**
    * Construct based on a screw representation of the twist
    *
    * @param bodyFrame what we're specifying the twist of
    * @param baseFrame with respect to what we're specifying the twist
    * @param expressedInFrame in which reference frame the twist is expressed
    * @param angularVelocityMagnitude magnitude of angular velocity about axisOfRotation
    * @param linearVelocityMagnitude magnitude of linear velocity in the direction of axisOfRotation
    * @param axisOfRotation axis of rotation
    * @param offset any vector from the origin of expressedInFrame to axisOfRotation
    */
   public Twist(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double angularVelocityMagnitude,
                double linearVelocityMagnitude, Vector3DReadOnly axisOfRotation, Vector3DReadOnly offset)
   {
      setScrew(bodyFrame, baseFrame, expressedInFrame, angularVelocityMagnitude, linearVelocityMagnitude, axisOfRotation, offset);
   }

   /**
    * Sets this twist so that it is the same as another twist
    */
   public void checkAndSet(Twist other)
   {
      this.bodyFrame.checkReferenceFrameMatch(other.bodyFrame);
      this.baseFrame.checkReferenceFrameMatch(other.baseFrame);
      this.expressedInFrame.checkReferenceFrameMatch(other.expressedInFrame);
      set(other);
   }

   /*
    * TODO: check if the *InBaseFrame methods generalize to SpatialMotionVector
    */

   /**
    * Packs the angular velocity of the body frame with respect to the base frame, expressed in the base frame.
    * The vector is computed by simply rotating the angular velocity part of this twist to base frame.
    */
   public void getAngularVelocityInBaseFrame(Vector3D vectorToPack)
   {
      vectorToPack.set(angularPart);

      if (expressedInFrame == baseFrame)
      {
         return;    // shortcut for special case
      }
      else
      {
         expressedInFrame.getTransformToDesiredFrame(freeTransform, baseFrame);
         freeTransform.transform(vectorToPack);    // only does rotation
      }
   }

   /**
    * Packs the angular velocity of the body frame with respect to the base frame, expressed in the base frame.
    * The vector is computed by simply rotating the angular velocity part of this twist to base frame.
    */
   public void getAngularVelocityInBaseFrame(FrameVector3D vectorToPack)
   {
      vectorToPack.setToZero(baseFrame);
      getAngularVelocityInBaseFrame(vectorToPack.getVector());
   }

   /**
    * Packs a version of the linear velocity, rotated to the base frame.
    */
   public void getBodyOriginLinearPartInBaseFrame(Vector3D linearVelocityAtBodyOriginToPack)
   {
      if (expressedInFrame == bodyFrame)
      {
         linearVelocityAtBodyOriginToPack.set(linearPart);    // shortcut for special case
      }
      else
      {
         bodyFrame.getTransformToDesiredFrame(freeTransform, expressedInFrame);
         freeTransform.getTranslation(freeVector);

         linearVelocityAtBodyOriginToPack.cross(angularPart, freeVector);    // omega x p
         linearVelocityAtBodyOriginToPack.add(linearPart);    // omega x p + v
      }

      if (expressedInFrame == baseFrame)
      {
         return;    // shortcut for special case
      }
      else
      {
         expressedInFrame.getTransformToDesiredFrame(freeTransform, baseFrame);
         freeTransform.transform(linearVelocityAtBodyOriginToPack);    // only does rotation
      }
   }

   /**
    * Packs a version of the linear velocity, rotated to the base frame.
    */
   public void getBodyOriginLinearPartInBaseFrame(FrameVector3D linearVelocityAtBodyOriginToPack)
   {
      linearVelocityAtBodyOriginToPack.setToZero(baseFrame);
      getBodyOriginLinearPartInBaseFrame(linearVelocityAtBodyOriginToPack.getVector());
   }

   /**
    * Packs the linear velocity of a point that is fixed in bodyFrame,
    * with respect to baseFrame. The resulting vector is expressed in {@code this.getExpressedInFrame()}.
    */
   public void getLinearVelocityOfPointFixedInBodyFrame(FrameVector3D linearVelocityToPack, FramePoint3D pointFixedInBodyFrame)
   {
      pointFixedInBodyFrame.checkReferenceFrameMatch(expressedInFrame);
      pointFixedInBodyFrame.get(freeVector);

      linearVelocityToPack.setToZero(expressedInFrame);
      linearVelocityToPack.cross(angularPart, freeVector);
      linearVelocityToPack.add(linearPart);
   }

   /**
    * Packs the linear velocity of a 2D point that is fixed in bodyFrame,
    * with respect to baseFrame. The resulting vector is expressed in {@code this.getExpressedInFrame()}.
    */
   public void getLineaVelocityOfPoint2dFixedInBodyFrame(FrameVector3D linearVelocityToPack, FramePoint2D point2dFixedInBodyFrame)
   {
      baseFrame.checkReferenceFrameMatch(expressedInFrame);
      point2dFixedInBodyFrame.checkReferenceFrameMatch(baseFrame);

      point2dFixedInBodyFrame.get(freeVector);

      linearVelocityToPack.setToZero(expressedInFrame);
      linearVelocityToPack.cross(angularPart, freeVector);
      linearVelocityToPack.add(linearPart);
   }

   /**
    * Changes the body frame, assuming there is no relative twist between the old body frame and the new body frame
    * A consequence of Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking Robots, page 25, lemma 2.8 (a)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void changeBodyFrameNoRelativeTwist(ReferenceFrame newBodyFrame)
   {
      this.bodyFrame = newBodyFrame;
   }

   /**
    * Changes the base frame, assuming there is no relative twist between the old base frame and the new base frame
    * A consequence of Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking Robots, page 25, lemma 2.8 (a)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void changeBaseFrameNoRelativeTwist(ReferenceFrame newBaseFrame)
   {
      this.baseFrame = newBaseFrame;
   }

   /**
    * Changes the reference frame in which this spatial motion vector is expressed
    * See Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking Robots, page 25, lemma 2.8 (c)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void changeFrame(ReferenceFrame newReferenceFrame)
   {
      // trivial case:
      if (this.expressedInFrame == newReferenceFrame)
      {
         return;
      }

      // non-trivial case
      // essentially using the Adjoint operator, Ad_H = [R, 0; tilde(p) * R, R] (Matlab notation), but without creating a 6x6 matrix
      // compute the relevant rotations and translations
      expressedInFrame.getTransformToDesiredFrame(freeTransform, newReferenceFrame);
      freeTransform.getTranslation(freeVector);    // p

      // transform the velocities so that they are expressed in newReferenceFrame
      freeTransform.transform(angularPart);    // only rotates, since we're passing in a vector
      freeTransform.transform(linearPart);
      freeVector.cross(freeVector, angularPart);    // p x omega
      linearPart.add(freeVector);

      // change this spatial motion vector's expressedInFrame to newReferenceFrame
      this.expressedInFrame = newReferenceFrame;
   }

   /**
    * Adds another twist to this twist, after doing some reference frame checks.
    * See Duindam, Port-Based Modeling and Control for Efficient Bipedal Walking Robots, page 25, lemma 2.8 (e)
    * http://sites.google.com/site/vincentduindam/publications
    */
   public void add(Twist other)
   {
      // make sure they're expressed in the same reference frame
      expressedInFrame.checkReferenceFrameMatch(other.expressedInFrame);

      // make sure that the bodyFrame of this frame equals the baseFrame of the values that's being added
      bodyFrame.checkReferenceFrameMatch(other.baseFrame);

      // now it should be safe to add, and change this Twist's bodyFrame
      angularPart.add(other.angularPart);
      linearPart.add(other.linearPart);
      bodyFrame = other.bodyFrame;
   }

   public void sub(Twist other)
   {
      // make sure they're expressed in the same reference frame
      expressedInFrame.checkReferenceFrameMatch(other.expressedInFrame);

      // make sure that either the bodyFrames or baseFrames are the same
      if (baseFrame == other.baseFrame)
      {
         angularPart.sub(other.angularPart);
         linearPart.sub(other.linearPart);
         baseFrame = other.bodyFrame;
      }
      else if (bodyFrame == other.bodyFrame)
      {
         angularPart.sub(other.angularPart);
         linearPart.sub(other.linearPart);
         bodyFrame = other.baseFrame;
      }
      else
      {
         throw new RuntimeException("frames don't match");
      }
   }

   /**
    * Takes the dot product of this twist and a wrench, resulting in the (reference frame independent) instantaneous power.
    * @param wrench a wrench that
    *       1) has an 'onWhat' reference frame that is the same as this twist's 'bodyFrame' reference frame.
    *       2) is expressed in the same reference frame as this twist
    * @return the instantaneous power associated with this twist and the wrench
    */
   public double dot(Wrench wrench)
   {
      this.bodyFrame.checkReferenceFrameMatch(wrench.getBodyFrame());
      this.expressedInFrame.checkReferenceFrameMatch(wrench.getExpressedInFrame());

      double power = this.angularPart.dot(wrench.getAngularPart()) + this.linearPart.dot(wrench.getLinearPart());

      return power;
   }

   public void set(Twist other)
   {
      super.set(other);
   }

   public void setScrew(ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame, double angularVelocityMagnitude,
         double linearVelocityMagnitude, Vector3DReadOnly axisOfRotation, Vector3DReadOnly offset)
   {
      double epsilon = 1e-12;
      if (!MathTools.epsilonEquals(1.0, axisOfRotation.lengthSquared(), epsilon))
         throw new RuntimeException("axis of rotation must be of unit magnitude. axisOfRotation: " + axisOfRotation);

      this.bodyFrame = bodyFrame;
      this.baseFrame = baseFrame;
      this.expressedInFrame = expressedInFrame;

      this.freeVector.cross(offset, axisOfRotation);
      this.freeVector.scale(angularVelocityMagnitude);

      this.linearPart.set(axisOfRotation);
      this.linearPart.scale(linearVelocityMagnitude);
      this.linearPart.add(freeVector);

      this.angularPart.set(axisOfRotation);
      this.angularPart.scale(angularVelocityMagnitude);
   }

   ///CLOVER:OFF
   @Override
   public String toString()
   {
      String ret = new String("Twist of " + bodyFrame + ", with respect to " + baseFrame + ", expressed in " + expressedInFrame + "\n" + "Linear part: "
                              + linearPart + "\n" + "Angular part: " + angularPart + "\n");

      return ret;
   }

   public static Twist generateRandomTwist(Random random, ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame)
   {
      double linearVelocityMagnitude = random.nextDouble();
      double angularVelocityMagnitude = random.nextDouble();
      return generateRandomTwist(random, bodyFrame, baseFrame, expressedInFrame, angularVelocityMagnitude, linearVelocityMagnitude);
   }

   public static Twist generateRandomTwist(Random random, ReferenceFrame bodyFrame, ReferenceFrame baseFrame, ReferenceFrame expressedInFrame,
                                           double angularVelocityMagnitude, double linearVelocityMagnitude)
   {
      Twist randomTwist = new Twist(bodyFrame, baseFrame, expressedInFrame);
      randomTwist.setLinearPart(RandomGeometry.nextVector3D(random, linearVelocityMagnitude));
      randomTwist.setAngularPart(RandomGeometry.nextVector3D(random, angularVelocityMagnitude));
      return randomTwist;
   }

   ///CLOVER:ON
}
