package us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommandType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;

public class PrivilegedConfigurationCommand implements InverseKinematicsCommand<PrivilegedConfigurationCommand>, InverseDynamicsCommand<PrivilegedConfigurationCommand>
{
   private final int initialCapacity = 40;
   private final List<String> jointNames = new ArrayList<>(initialCapacity);
   private final List<OneDoFJoint> joints = new ArrayList<>(initialCapacity);
   private final TDoubleArrayList privilegedOneDoFJointConfigurations = new TDoubleArrayList(initialCapacity);
   private final Map<OneDoFJoint, PrivilegedConfigurationOption> privilegedOneDoFJointConfigurationOptions = new HashMap<>(initialCapacity);

   private final List<RigidBody> bases = new ArrayList<>();
   private final List<RigidBody> endEffectors = new ArrayList<>();

   private boolean enable = false;

   public enum PrivilegedConfigurationOption
   {
      AT_CURRENT, AT_MID_RANGE, AT_ZERO
   };

   private PrivilegedConfigurationOption defaultOption;
   private double weight = Double.NaN;

   public PrivilegedConfigurationCommand()
   {
      clear();
   }

   public void clear()
   {
      enable = false;
      defaultOption = null;
      weight = Double.NaN;
      jointNames.clear();
      joints.clear();
      privilegedOneDoFJointConfigurations.reset();
      privilegedOneDoFJointConfigurationOptions.clear();

      bases.clear();
      endEffectors.clear();
   }

   public void disable()
   {
      enable = false;
   }

   public void enable()
   {
      enable = true;
   }

   public void setDefaultWeight(double defaultWeight)
   {
      this.weight = defaultWeight;
   }

   public void setPrivilegedConfigurationOption(PrivilegedConfigurationOption option)
   {
      enable();
      this.defaultOption = option;
   }

   public void addJoint(OneDoFJoint joint, double privilegedConfiguration)
   {
      enable();
      joints.add(joint);
      jointNames.add(joint.getName());
      privilegedOneDoFJointConfigurations.add(privilegedConfiguration);
      privilegedOneDoFJointConfigurationOptions.put(joint, null);
   }

   public void addJoint(OneDoFJoint joint, PrivilegedConfigurationOption privilegedConfiguration)
   {
      enable();
      joints.add(joint);
      jointNames.add(joint.getName());
      privilegedOneDoFJointConfigurations.add(Double.NaN);
      privilegedOneDoFJointConfigurationOptions.put(joint, privilegedConfiguration);
   }

   public void applyPrivilegedConfigurationToSubChain(RigidBody base, RigidBody endEffector)
   {
      bases.add(base);
      endEffectors.add(endEffector);
   }

   @Override
   public void set(PrivilegedConfigurationCommand other)
   {
      clear();
      enable = other.enable;
      defaultOption = other.defaultOption;
      weight = other.weight;

      for (int i = 0; i < other.getNumberOfJoints(); i++)
      {
         OneDoFJoint joint = other.joints.get(i);
         joints.add(joint);
         jointNames.add(other.jointNames.get(i));
         privilegedOneDoFJointConfigurations.add(other.privilegedOneDoFJointConfigurations.get(i));
         privilegedOneDoFJointConfigurationOptions.put(joint, other.privilegedOneDoFJointConfigurationOptions.get(joint));
      }
   }

   public boolean isEnabled()
   {
      return enable;
   }

   public boolean hasNewWeight()
   {
      return !Double.isNaN(weight);
   }

   public double getWeight()
   {
      return weight;
   }

   public boolean hasNewPrivilegedConfigurationDefaultOption()
   {
      return defaultOption != null;
   }

   public PrivilegedConfigurationOption getPrivilegedConfigurationDefaultOption()
   {
      return defaultOption;
   }

   public boolean hasNewPrivilegedConfiguration(int jointIndex)
   {
      return !Double.isNaN(privilegedOneDoFJointConfigurations.get(jointIndex));
   }

   public double getPrivilegedConfiguration(int jointIndex)
   {
      return privilegedOneDoFJointConfigurations.get(jointIndex);
   }

   public boolean hasNewPrivilegedConfigurationOption(int jointIndex)
   {
      return getPrivilegedConfigurationOption(jointIndex) != null;
   }

   public PrivilegedConfigurationOption getPrivilegedConfigurationOption(int jointIndex)
   {
      return privilegedOneDoFJointConfigurationOptions.get(joints.get(jointIndex));
   }

   public int getNumberOfJoints()
   {
      return joints.size();
   }

   public OneDoFJoint getJoint(int jointIndex)
   {
      return joints.get(jointIndex);
   }

   public int getNumberOfChains()
   {
      return bases.size();
   }

   public RigidBody getChainBase(int chainIndex)
   {
      return bases.get(chainIndex);
   }

   public RigidBody getChainEndEffector(int chainIndex)
   {
      return endEffectors.get(chainIndex);
   }

   @Override
   public ControllerCoreCommandType getCommandType()
   {
      return ControllerCoreCommandType.PRIVILEGED_CONFIGURATION;
   }
}
