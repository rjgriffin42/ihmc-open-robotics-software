package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
 * General purpose message normally used to report the solution of a whole-body trajectory planner.
 * Main usage is the IHMC WholeBodyTrajectoryToolbox.
 */
public class WholeBodyTrajectoryToolboxOutputStatus extends Packet<WholeBodyTrajectoryToolboxOutputStatus>
      implements Settable<WholeBodyTrajectoryToolboxOutputStatus>, EpsilonComparable<WholeBodyTrajectoryToolboxOutputStatus>
{
   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header header_;
   /**
    * 0: not completed. 1: fail to find initial guess. 2: fail to complete expanding tree. 3: fail
    * to optimize path. 4: solution is available.
    */
   public int planning_result_;
   public us.ihmc.idl.IDLSequence.Double trajectory_times_;
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.KinematicsToolboxOutputStatus> robot_configurations_;

   public WholeBodyTrajectoryToolboxOutputStatus()
   {
      header_ = new std_msgs.msg.dds.Header();
      trajectory_times_ = new us.ihmc.idl.IDLSequence.Double(50, "type_6");

      robot_configurations_ = new us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.KinematicsToolboxOutputStatus>(50,
                                                                                                                        controller_msgs.msg.dds.KinematicsToolboxOutputStatus.class,
                                                                                                                        new controller_msgs.msg.dds.KinematicsToolboxOutputStatusPubSubType());

   }

   public WholeBodyTrajectoryToolboxOutputStatus(WholeBodyTrajectoryToolboxOutputStatus other)
   {
      this();
      set(other);
   }

   public void set(WholeBodyTrajectoryToolboxOutputStatus other)
   {
      std_msgs.msg.dds.HeaderPubSubType.staticCopy(other.header_, header_);
      planning_result_ = other.planning_result_;

      trajectory_times_.set(other.trajectory_times_);
      robot_configurations_.set(other.robot_configurations_);
   }

   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header getHeader()
   {
      return header_;
   }

   /**
    * 0: not completed. 1: fail to find initial guess. 2: fail to complete expanding tree. 3: fail
    * to optimize path. 4: solution is available.
    */
   public void setPlanningResult(int planning_result)
   {
      planning_result_ = planning_result;
   }

   /**
    * 0: not completed. 1: fail to find initial guess. 2: fail to complete expanding tree. 3: fail
    * to optimize path. 4: solution is available.
    */
   public int getPlanningResult()
   {
      return planning_result_;
   }

   public us.ihmc.idl.IDLSequence.Double getTrajectoryTimes()
   {
      return trajectory_times_;
   }

   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.KinematicsToolboxOutputStatus> getRobotConfigurations()
   {
      return robot_configurations_;
   }

   @Override
   public boolean epsilonEquals(WholeBodyTrajectoryToolboxOutputStatus other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!this.header_.epsilonEquals(other.header_, epsilon))
         return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.planning_result_, other.planning_result_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsDoubleSequence(this.trajectory_times_, other.trajectory_times_, epsilon))
         return false;

      if (this.robot_configurations_.size() == other.robot_configurations_.size())
      {
         return false;
      }
      else
      {
         for (int i = 0; i < this.robot_configurations_.size(); i++)
         {
            if (!this.robot_configurations_.get(i).epsilonEquals(other.robot_configurations_.get(i), epsilon))
               return false;
         }
      }

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;
      if (!(other instanceof WholeBodyTrajectoryToolboxOutputStatus))
         return false;

      WholeBodyTrajectoryToolboxOutputStatus otherMyClass = (WholeBodyTrajectoryToolboxOutputStatus) other;

      if (!this.header_.equals(otherMyClass.header_))
         return false;
      if (this.planning_result_ != otherMyClass.planning_result_)
         return false;

      if (!this.trajectory_times_.equals(otherMyClass.trajectory_times_))
         return false;
      if (!this.robot_configurations_.equals(otherMyClass.robot_configurations_))
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("WholeBodyTrajectoryToolboxOutputStatus {");
      builder.append("header=");
      builder.append(this.header_);
      builder.append(", ");
      builder.append("planning_result=");
      builder.append(this.planning_result_);
      builder.append(", ");
      builder.append("trajectory_times=");
      builder.append(this.trajectory_times_);
      builder.append(", ");
      builder.append("robot_configurations=");
      builder.append(this.robot_configurations_);
      builder.append("}");
      return builder.toString();
   }
}
