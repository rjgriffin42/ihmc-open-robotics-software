package us.ihmc.steppr.hardware.controllers;

import java.util.ArrayList;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.controllers.PDController;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class StepprPDJointController implements StepprController
{
   private final YoVariableRegistry registry = new YoVariableRegistry("StepprPDJointController");
   
   protected final ArrayList<OneDoFJoint> joints = new ArrayList<>();
   private final ArrayList<PDController> controllers = new ArrayList<>();
   private final ArrayList<DoubleYoVariable> desiredPositions = new ArrayList<>();
   private final ArrayList<DoubleYoVariable> desiredVelocities = new ArrayList<>();
   protected final ArrayList<DoubleYoVariable> tauFFs = new ArrayList<>();
   private final ArrayList<DoubleYoVariable> damping = new ArrayList<>();
   

   @Override
   public void setFullRobotModel(SDFFullRobotModel fullRobotModel)
   {
      for(OneDoFJoint joint : fullRobotModel.getOneDoFJoints())
      {
         joints.add(joint);
         controllers.add(new PDController(joint.getName(), registry));
         desiredPositions.add(new DoubleYoVariable(joint.getName() + "_q_d", registry));
         desiredVelocities.add(new DoubleYoVariable(joint.getName() + "_qd_d", registry));
         tauFFs.add(new DoubleYoVariable(joint.getName() + "_tau_ff", registry));
         damping.add(new DoubleYoVariable(joint.getName() + "_damping", registry));
      }

   }

   @Override
   public void initialize(long timestamp)
   {
      for(int i = 0; i < controllers.size(); i++)
      {
         OneDoFJoint joint = joints.get(i); 
         desiredPositions.get(i).set(joint.getQ());;
         desiredVelocities.get(i).set(0);
      }
   }

   @Override
   public void doControl(long timestamp)
   {
      for(int i = 0; i < controllers.size(); i++)
      {
         OneDoFJoint joint = joints.get(i);
         PDController controller = controllers.get(i);
         double tauFF = tauFFs.get(i).getDoubleValue(); 
         double q_d = desiredPositions.get(i).getDoubleValue();
         double qd_d = desiredVelocities.get(i).getDoubleValue();
         
         double tau = controller.compute(joint.getQ(), q_d, joint.getQd(), qd_d) + tauFF;
         
         joint.setTau(tau);
         joint.setKd(damping.get(i).getDoubleValue());
      }
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   
   public static void main(String[] args)
   {
      StepprSingleThreadedController.startController(new StepprPDJointController());
   }

   @Override
   public boolean turnOutputOn()
   {
      return false;
   }
}
