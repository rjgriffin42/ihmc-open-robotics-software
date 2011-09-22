package us.ihmc.commonWalkingControlModules.kinematics;

import java.util.ArrayList;
import java.util.EnumMap;

import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointVelocities;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegTorques;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.Wrench;

import com.mathworks.jama.Matrix;

public class SwingFullLegJacobian
{
   private final RobotSide robotSide;
   private final GeometricJacobian openChainJacobian;
   private final LegJointName[] legJointNames;
   
   /**
    * Constructs a new SwingFullLegJacobian, for the given side of the robot
    * @param legFrames TODO
    * @param pelvisFrame TODO
    * @param endEffectorName TODO
    */
   public SwingFullLegJacobian(LegJointName[] legJointNames, RobotSide robotSide, EnumMap<LegJointName, ReferenceFrame> legFrames, ReferenceFrame pelvisFrame, LegJointName endEffectorName)
   {
      this.legJointNames = legJointNames;
      this.robotSide = robotSide;

      // frames
      // unit twists in body frames
      Vector3d zero = new Vector3d();
      
      ArrayList<Twist> legTwists = new ArrayList<Twist>();
      ReferenceFrame twistBaseFrame = pelvisFrame;
      for (LegJointName legJointName : legJointNames)
      {
         ReferenceFrame twistBodyFrame = legFrames.get(legJointName);
         ReferenceFrame expressedInFrame = twistBodyFrame;
         Twist twist = new Twist(twistBodyFrame, twistBaseFrame, expressedInFrame, zero, legJointName.getJointAxis());
         
         legTwists.add(twist);
         
         twistBaseFrame = twistBodyFrame; // for next iteration
      }

      // frames
      ReferenceFrame endEffectorFrame = legFrames.get(endEffectorName);
      ReferenceFrame baseFrame = pelvisFrame;
      ReferenceFrame jacobianFrame = endEffectorFrame;

      // create Jacobian
      openChainJacobian = new GeometricJacobian(legTwists, endEffectorFrame, baseFrame, jacobianFrame);

   }

   /**
    * Computes the underlying openChainJacobian and the vtpJacobian
    */
   public void computeJacobian()
   {
      openChainJacobian.compute();
   }

   /**
    * @return the determinant of the Jacobian matrix
    */
   public double det()
   {
      return openChainJacobian.det();
   }

   /**
    * Returns the twist of the ankle pitch frame with respect to the pelvis frame, expressed in the ankle pitch frame,
    * corresponding to the given joint velocities.
    */
   public Twist getTwistOfFootWithRespectToPelvisInFootFrame(LegJointVelocities jointVelocities)
   {
      Matrix jointVelocitiesVector = new Matrix(legJointNames.length, 1);
      for (int i = 0; i < legJointNames.length; i++)
      {
         LegJointName legJointName = legJointNames[i];
         jointVelocitiesVector.set(i, 0, jointVelocities.getJointVelocity(legJointName));
      }

      return openChainJacobian.getTwist(jointVelocitiesVector);
   }

   /**
    * Returns the joint velocities corresponding to the twist of the foot, with respect to the pelvis, expressed in ankle pitch frame
    * @param anklePitchTwistInAnklePitchFrame
    * @return corresponding joint velocities
    */
   public LegJointVelocities getJointVelocitiesGivenTwist(Twist anklePitchTwistInAnklePitchFrame, double alpha)
   {
      LegJointVelocities ret = new LegJointVelocities(legJointNames, robotSide);
      packJointVelocitiesGivenTwist(ret, anklePitchTwistInAnklePitchFrame, alpha);
      return ret;
   }
   
   /**
    * Packs the joint velocities corresponding to the twist of the foot, with respect to the pelvis, expressed in ankle pitch frame
    * @param anklePitchTwistInAnklePitchFrame
    * @return corresponding joint velocities
    */
   public void packJointVelocitiesGivenTwist(LegJointVelocities legJointVelocitiesToPack, Twist anklePitchTwistInAnklePitchFrame, double alpha)
   {
      Matrix jointVelocities = openChainJacobian.computeJointVelocities(anklePitchTwistInAnklePitchFrame, alpha);
      for (int i = 0; i < legJointNames.length; i++)
      {
         LegJointName legJointName = legJointNames[i];
         legJointVelocitiesToPack.setJointVelocity(legJointName, jointVelocities.get(i, 0));
      }
   }

   /**
    * Packs a LegTorques object with the torques corresponding to the given wrench on the foot.
    */
   public void packLegTorques(LegTorques legTorquesToPack, Wrench wrenchOnFootInFootFrame)
   {
      // check that the LegTorques object we're packing has the correct RobotSide.
      if (this.robotSide != legTorquesToPack.getRobotSide())
      {
         throw new RuntimeException("legTorques object has the wrong RobotSide");
      }

      // the actual computation
      Matrix jointTorques = openChainJacobian.computeJointTorques(wrenchOnFootInFootFrame);

      for (int i = 0; i < legJointNames.length; i++)
      {
         LegJointName legJointName = legJointNames[i];
         legTorquesToPack.setTorque(legJointName, jointTorques.get(i, 0));
      }
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public Matrix computeJointAccelerations(SpatialAccelerationVector accelerationOfFootWithRespectToBody, SpatialAccelerationVector jacobianDerivativeTerm, double alpha)
   {
      Matrix biasedAccelerations = accelerationOfFootWithRespectToBody.toMatrix();    // unbiased at this point
      Matrix bias = jacobianDerivativeTerm.toMatrix();
      biasedAccelerations.minusEquals(bias);
      Matrix ret = openChainJacobian.solveUsingDampedLeastSquares(biasedAccelerations, alpha);

      return ret;
   }

   /**
    * For testing purposes only.
    */
   public Matrix getJacobian()
   {
      return openChainJacobian.getJacobianMatrix().copy();
   }
   
   public String toString()
   {
      return openChainJacobian.toString();
   }
}
