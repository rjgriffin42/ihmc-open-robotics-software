package us.ihmc.aware.controller.force.toolbox;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.aware.mechanics.contactForces.QuadrupedContactForceLimits;
import us.ihmc.aware.mechanics.contactForces.QuadrupedContactForceOptimization;
import us.ihmc.aware.mechanics.contactForces.QuadrupedContactForceOptimizationSettings;
import us.ihmc.aware.mechanics.virtualModelControl.*;
import us.ihmc.aware.planning.ContactState;
import us.ihmc.aware.model.QuadrupedJointNameMap;
import us.ihmc.aware.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class QuadrupedTaskSpaceController
{
   public static class Settings
   {
      private final QuadrantDependentList<ContactState> contactState;
      private final QuadrupedContactForceLimits contactForceLimits;
      private final QuadrupedContactForceOptimizationSettings contactForceOptimizationSettings;
      private final QuadrupedVirtualModelControllerSettings virtualModelControllerSettings;

      public Settings()
      {
         contactState = new QuadrantDependentList<>();
         contactForceLimits = new QuadrupedContactForceLimits();
         contactForceOptimizationSettings = new QuadrupedContactForceOptimizationSettings();
         virtualModelControllerSettings = new QuadrupedVirtualModelControllerSettings();
         initialize();
      }

      public void initialize()
      {
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            contactState.set(robotQuadrant, ContactState.NO_CONTACT);
         }
         contactForceLimits.setDefaults();
         contactForceOptimizationSettings.setDefaults();
         virtualModelControllerSettings.setDefaults();
      }

      public void setContactState(RobotQuadrant robotQuadrant, ContactState contactState)
      {
         this.contactState.set(robotQuadrant, contactState);
      }

      public void setContactState(QuadrantDependentList<ContactState> contactState)
      {
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            this.contactState.set(robotQuadrant, contactState.get(robotQuadrant));
         }
      }

      public ContactState getContactState(RobotQuadrant robotQuadrant)
      {
         return contactState.get(robotQuadrant);
      }

      public QuadrantDependentList<ContactState> getContactState()
      {
         return contactState;
      }

      public QuadrupedContactForceLimits getContactForceLimits()
      {
         return contactForceLimits;
      }

      public QuadrupedContactForceOptimizationSettings getContactForceOptimizationSettings()
      {
         return contactForceOptimizationSettings;
      }

      public QuadrupedVirtualModelControllerSettings getVirtualModelControllerSettings()
      {
         return virtualModelControllerSettings;
      }
   }

   public static class Commands
   {
      private final FrameVector comForce = new FrameVector();
      private final FrameVector comTorque = new FrameVector();
      private final QuadrantDependentList<FrameVector> soleForce = new QuadrantDependentList<>();

      public Commands()
      {
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            soleForce.set(robotQuadrant, new FrameVector());
         }
         initialize();
      }

      public void initialize()
      {
         comForce.setToZero();
         comTorque.setToZero();
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            soleForce.get(robotQuadrant).setToZero();
         }
      }

      public FrameVector getComForce()
      {
         return comForce;
      }

      public FrameVector getComTorque()
      {
         return comTorque;
      }

      public FrameVector getSoleForce(RobotQuadrant robotQuadrant)
      {
         return soleForce.get(robotQuadrant);
      }

      public QuadrantDependentList<FrameVector> getSoleForce()
      {
         return soleForce;
      }
   }

   private final QuadrupedJointLimits jointLimits;
   private final QuadrupedVirtualModelController virtualModelController;
   private final QuadrupedContactForceOptimization contactForceOptimization;
   private final FrameVector contactForceStorage;
   private final YoVariableRegistry registry = new YoVariableRegistry("taskSpaceController");

   public QuadrupedTaskSpaceController(SDFFullRobotModel fullRobotModel, QuadrupedReferenceFrames referenceFrames, QuadrupedJointNameMap jointNameMap, QuadrupedJointLimits jointLimits, double controlDT, YoVariableRegistry parentRegistry)
   {
      this.jointLimits = jointLimits;

      // virtual model controller
      virtualModelController = new QuadrupedVirtualModelController(fullRobotModel, referenceFrames, jointNameMap, controlDT, registry);
      contactForceOptimization = new QuadrupedContactForceOptimization(referenceFrames, registry);
      contactForceStorage = new FrameVector();

      parentRegistry.addChild(registry);
      reset();
   }

   public void reset()
   {
      virtualModelController.reset();
      contactForceOptimization.reset();
   }

   public void registerGraphics(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      virtualModelController.registerGraphics(yoGraphicsListRegistry);
   }

   public void compute(Settings settings, Commands commands)
   {
      // compute optimal contact force distribution for quadrants that are in contact
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         // note: sole forces are inverted to obtain commanded reaction forces
         commands.getSoleForce().get(robotQuadrant).scale(-1.0);
         contactForceOptimization.setContactForceCommand(robotQuadrant, commands.getSoleForce().get(robotQuadrant));
         commands.getSoleForce().get(robotQuadrant).scale(-1.0);
         contactForceOptimization.setContactState(robotQuadrant, settings.getContactState(robotQuadrant));
      }
      contactForceOptimization.setComForceCommand(commands.getComForce());
      contactForceOptimization.setComTorqueCommand(commands.getComTorque());
      contactForceOptimization.solve(settings.getContactForceLimits(), settings.getContactForceOptimizationSettings());

      // compute leg joint torques using jacobian transpose
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (settings.getContactState(robotQuadrant) == ContactState.IN_CONTACT)
         {
            contactForceOptimization.getContactForceSolution(robotQuadrant, contactForceStorage);
            virtualModelController.setSoleContactForce(robotQuadrant, contactForceStorage);
            virtualModelController.setSoleContactForceVisible(robotQuadrant, true);
            virtualModelController.setSoleVirtualForceVisible(robotQuadrant, false);
         }
         else
         {
            virtualModelController.setSoleVirtualForce(robotQuadrant, commands.getSoleForce(robotQuadrant));
            virtualModelController.setSoleContactForceVisible(robotQuadrant, false);
            virtualModelController.setSoleVirtualForceVisible(robotQuadrant, true);
         }
      }
      virtualModelController.compute(jointLimits, settings.getVirtualModelControllerSettings());
   }

   public ContactState getContactState(RobotQuadrant robotQuadrant)
   {
      return contactForceOptimization.getContactState(robotQuadrant);
   }
}
