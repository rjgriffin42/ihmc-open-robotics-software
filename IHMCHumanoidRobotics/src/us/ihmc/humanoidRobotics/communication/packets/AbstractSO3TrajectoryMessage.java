package us.ihmc.humanoidRobotics.communication.packets;

import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.communication.packets.QueueableMessage;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.ros.generators.RosIgnoredField;
import us.ihmc.euclid.interfaces.Transformable;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSO3TrajectoryPointList;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public abstract class AbstractSO3TrajectoryMessage<T extends AbstractSO3TrajectoryMessage<T>> extends QueueableMessage<T> implements Transformable
{
   @RosExportedField(documentation = "List of trajectory points (in taskpsace) to go through while executing the trajectory. All the information contained in these trajectory points needs to be expressed in world frame.")
   public SO3TrajectoryPointMessage[] taskspaceTrajectoryPoints;

   @RosIgnoredField
   public float[] selectionMatrixDiagonal;

   /**
    * Empty constructor for serialization.
    */
   public AbstractSO3TrajectoryMessage()
   {
      super();
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public AbstractSO3TrajectoryMessage(Random random)
   {
      super(random);
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      int randomNumberOfPoints = random.nextInt(16) + 1;
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[randomNumberOfPoints];
      for(int i = 0; i < randomNumberOfPoints; i++)
      {
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(random);
      }
   }

   public AbstractSO3TrajectoryMessage(AbstractSO3TrajectoryMessage<?> so3TrajectoryMessage)
   {
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[so3TrajectoryMessage.getNumberOfTrajectoryPoints()];
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(so3TrajectoryMessage.taskspaceTrajectoryPoints[i]);
      setExecutionMode(so3TrajectoryMessage.getExecutionMode(), so3TrajectoryMessage.getPreviousMessageId());

      setUniqueId(so3TrajectoryMessage.getUniqueId());
      setDestination(so3TrajectoryMessage.getDestination());
   }

   public AbstractSO3TrajectoryMessage(double trajectoryTime, Quaternion desiredOrientation)
   {
      Vector3D zeroAngularVelocity = new Vector3D();
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[] {new SO3TrajectoryPointMessage(trajectoryTime, desiredOrientation, zeroAngularVelocity)};
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public AbstractSO3TrajectoryMessage(int numberOfTrajectoryPoints)
   {
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[numberOfTrajectoryPoints];
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public void getTrajectoryPoints(FrameSO3TrajectoryPointList trajectoryPointListToPack)
   {
      trajectoryPointListToPack.clear(ReferenceFrame.getWorldFrame());

      SO3TrajectoryPointMessage[] trajectoryPointMessages = getTrajectoryPoints();
      int numberOfPoints = trajectoryPointMessages.length;

      for (int i = 0; i < numberOfPoints; i++)
      {
         SO3TrajectoryPointMessage so3TrajectoryPointMessage = trajectoryPointMessages[i];
         trajectoryPointListToPack.addTrajectoryPoint(so3TrajectoryPointMessage.time, so3TrajectoryPointMessage.orientation,
               so3TrajectoryPointMessage.angularVelocity);
      }
   }

   public void set(T other)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         throw new RuntimeException("Must the same number of waypoints.");
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(other.taskspaceTrajectoryPoints[i]);
      setExecutionMode(other.getExecutionMode(), other.getPreviousMessageId());
   }

   /**
    * Create a trajectory point.
    * @param trajectoryPointIndex index of the trajectory point to create.
    * @param time time at which the trajectory point has to be reached. The time is relative to when the trajectory starts.
    * @param orientation define the desired 3D orientation to be reached at this trajectory point. It is expressed in world frame.
    * @param angularVelocity define the desired 3D angular velocity to be reached at this trajectory point. It is expressed in world frame.
    */
   public final void setTrajectoryPoint(int trajectoryPointIndex, double time, Quaternion orientation, Vector3D angularVelocity)
   {
      rangeCheck(trajectoryPointIndex);
      taskspaceTrajectoryPoints[trajectoryPointIndex] = new SO3TrajectoryPointMessage(time, orientation, angularVelocity);
   }

   @Override
   public void applyTransform(Transform transform)
   {
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i].applyTransform(transform);
   }

   /**
    * The selectionMatrix needs to be nx6 or nx3.
    * @param selectionMatrix
    */
   public void setSelectionMatrix(DenseMatrix64F selectionMatrix)
   {
      if (selectionMatrixDiagonal == null)
         selectionMatrixDiagonal = new float[3];

      DenseMatrix64F inner = new DenseMatrix64F(selectionMatrix.getNumCols(), selectionMatrix.getNumCols());
      CommonOps.multInner(selectionMatrix, inner);

      for (int i = 0; i < 3; i++)
         selectionMatrixDiagonal[i] = (float) inner.get(i, i);
   }

   public final int getNumberOfTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints.length;
   }

   public final SO3TrajectoryPointMessage[] getTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints;
   }

   public final SO3TrajectoryPointMessage getTrajectoryPoint(int trajectoryPointIndex)
   {
      rangeCheck(trajectoryPointIndex);
      return taskspaceTrajectoryPoints[trajectoryPointIndex];
   }

   public final SO3TrajectoryPointMessage getLastTrajectoryPoint()
   {
      return taskspaceTrajectoryPoints[taskspaceTrajectoryPoints.length - 1];
   }

   public final double getTrajectoryTime()
   {
      return getLastTrajectoryPoint().time;
   }

   public boolean hasSelectionMatrix()
   {
      return selectionMatrixDiagonal != null;
   }

   public void getSelectionMatrix(DenseMatrix64F selectionMatrixToPack)
   {
      selectionMatrixToPack.reshape(3, 6);
      selectionMatrixToPack.zero();

      if (selectionMatrixDiagonal != null)
      {
         for (int i = 0; i < 3; i++)
            selectionMatrixToPack.set(i, i, selectionMatrixDiagonal[i]);
         MatrixTools.removeZeroRows(selectionMatrixToPack, 1.0e-5);
      }
      else
      {
         for (int i = 0; i < 3; i++)
            selectionMatrixToPack.set(i, i, 1.0);
      }
   }

   private void rangeCheck(int trajectoryPointIndex)
   {
      if (trajectoryPointIndex >= getNumberOfTrajectoryPoints() || trajectoryPointIndex < 0)
         throw new IndexOutOfBoundsException(
               "Trajectory point index: " + trajectoryPointIndex + ", number of trajectory points: " + getNumberOfTrajectoryPoints());
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateSO3TrajectoryMessage(this);
   }

   @Override
   public String toString()
   {
      if (taskspaceTrajectoryPoints != null)
         return getClass().getSimpleName() + ": number of SO3 trajectory points = " + getNumberOfTrajectoryPoints();
      else
         return getClass().getSimpleName() + ": no SO3 trajectory points";
   }

   @Override
   public boolean epsilonEquals(T other, double epsilon)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         return false;

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         if (!taskspaceTrajectoryPoints[i].epsilonEquals(other.taskspaceTrajectoryPoints[i], epsilon))
            return false;
      }

      return super.epsilonEquals(other, epsilon);
   }
}
