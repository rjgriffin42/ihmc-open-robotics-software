package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.communication.controllerAPI.command.QueueableCommand;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidRobotics.communication.controllerAPI.converter.FrameBasedCommand;
import us.ihmc.humanoidRobotics.communication.packets.AbstractSE3TrajectoryMessage;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSE3TrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSE3TrajectoryPointList;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;

public abstract class SE3TrajectoryControllerCommand<T extends SE3TrajectoryControllerCommand<T, M>, M extends AbstractSE3TrajectoryMessage<M>> extends QueueableCommand<T, M> implements FrameBasedCommand<M>
{
   private final FrameSE3TrajectoryPointList trajectoryPointList = new FrameSE3TrajectoryPointList();
   private final SelectionMatrix6D selectionMatrix = new SelectionMatrix6D();
   private ReferenceFrame trajectoryFrame;

   private boolean useCustomControlFrame = false;
   private final RigidBodyTransform controlFramePoseInBodyFrame = new RigidBodyTransform();

   public SE3TrajectoryControllerCommand()
   {
   }

   public SE3TrajectoryControllerCommand(ReferenceFrame dataFrame, ReferenceFrame trajectoryFrame)
   {
      clear(dataFrame);
      this.trajectoryFrame = trajectoryFrame;
   }

   @Override
   public void clear()
   {
      clearQueuableCommandVariables();
      selectionMatrix.resetSelection();
   }

   public void clear(ReferenceFrame referenceFrame)
   {
      trajectoryPointList.clear(referenceFrame);
      clearQueuableCommandVariables();
      selectionMatrix.resetSelection();
   }

   @Override
   public void set(T other)
   {
      trajectoryPointList.setIncludingFrame(other.getTrajectoryPointList());
      setPropertiesOnly(other);
      trajectoryFrame = other.getTrajectoryFrame();
      useCustomControlFrame = other.useCustomControlFrame();
      other.packControlFramePose(controlFramePoseInBodyFrame);
   }

   @Override
   public void set(ReferenceFrame dataFrame, ReferenceFrame trajectoryFrame, M message)
   {
      this.trajectoryFrame = trajectoryFrame;
      clear(dataFrame);
      set(message);
   }

   /**
    * Same as {@link #set(T)} but does not change the trajectory points.
    * @param other
    */
   public void setPropertiesOnly(T other)
   {
      setQueueqableCommandVariables(other);
      selectionMatrix.set(other.getSelectionMatrix());
      trajectoryFrame = other.getTrajectoryFrame();
   }

   @Override
   public void set(M message)
   {
      message.getTrajectoryPoints(trajectoryPointList);
      setQueueqableCommandVariables(message);
      message.getSelectionMatrix(selectionMatrix);
      useCustomControlFrame = message.useCustomControlFrame();
      message.getTransformFromBodyToControlFrame(controlFramePoseInBodyFrame);
   }

   public void setTrajectoryPointList(FrameSE3TrajectoryPointList trajectoryPointList)
   {
      this.trajectoryPointList.setIncludingFrame(trajectoryPointList);
   }

   public SelectionMatrix6D getSelectionMatrix()
   {
      return selectionMatrix;
   }

   public FrameSE3TrajectoryPointList getTrajectoryPointList()
   {
      return trajectoryPointList;
   }

   @Override
   public boolean isCommandValid()
   {
      return executionModeValid() && trajectoryPointList.getNumberOfTrajectoryPoints() > 0;
   }

   /** {@inheritDoc}} */
   @Override
   public void addTimeOffset(double timeOffsetToAdd)
   {
      trajectoryPointList.addTimeOffset(timeOffsetToAdd);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public int getNumberOfTrajectoryPoints()
   {
      return trajectoryPointList.getNumberOfTrajectoryPoints();
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public void subtractTimeOffset(double timeOffsetToSubtract)
   {
      trajectoryPointList.subtractTimeOffset(timeOffsetToSubtract);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public void addTrajectoryPoint(double time, Point3D position, Quaternion orientation, Vector3D linearVelocity, Vector3D angularVelocity)
   {
      trajectoryPointList.addTrajectoryPoint(time, position, orientation, linearVelocity, angularVelocity);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public void addTrajectoryPoint(FrameSE3TrajectoryPoint trajectoryPoint)
   {
      trajectoryPointList.addTrajectoryPoint(trajectoryPoint);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public FrameSE3TrajectoryPoint getTrajectoryPoint(int trajectoryPointIndex)
   {
      return trajectoryPointList.getTrajectoryPoint(trajectoryPointIndex);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public FrameSE3TrajectoryPoint getLastTrajectoryPoint()
   {
      return trajectoryPointList.getLastTrajectoryPoint();
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public void changeFrame(ReferenceFrame referenceFrame)
   {
      trajectoryPointList.changeFrame(referenceFrame);
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public ReferenceFrame getDataFrame()
   {
      return trajectoryPointList.getReferenceFrame();
   }

   /**
    * Convenience method for accessing {@link #trajectoryPointList}. To get the list use {@link #getTrajectoryPointList()}.
    */
   public void checkReferenceFrameMatch(ReferenceFrame frame)
   {
      trajectoryPointList.checkReferenceFrameMatch(frame);
   }

   public ReferenceFrame getTrajectoryFrame()
   {
      return trajectoryFrame;
   }

   public void setTrajectoryFrame(ReferenceFrame trajectoryFrame)
   {
      this.trajectoryFrame = trajectoryFrame;
   }

   @Override
   public void packControlFramePose(RigidBodyTransform transformToPack)
   {
      transformToPack.set(controlFramePoseInBodyFrame);
   }

   @Override
   public boolean useCustomControlFrame()
   {
      return useCustomControlFrame;
   }
}
