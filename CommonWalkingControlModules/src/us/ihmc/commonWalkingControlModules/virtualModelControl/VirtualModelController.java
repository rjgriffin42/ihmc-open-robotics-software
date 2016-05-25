package us.ihmc.commonWalkingControlModules.virtualModelControl;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualWrenchCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.GeometricJacobianHolder;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.frames.YoMatrix;
import us.ihmc.robotics.math.frames.YoWrench;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.*;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.tools.io.printing.PrintTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualModelController
{
   private final static boolean DEBUG = true;
   private final static boolean VISUALIZE_DESIRED_WRENCHES = false;
   private final static boolean USE_SUPER_JACOBIAN = false;

   private final YoGraphicsListRegistry yoGraphicsListRegistry;
   private final YoVariableRegistry registry;
   private final Map<RigidBody, YoFrameVector> yoForceVectors = new HashMap<>();
   private final Map<RigidBody, YoFramePoint> yoForcePoints = new HashMap<>();
   private final Map<RigidBody, FramePoint> controlledBodyPoints = new HashMap<>();

   private final Map<RigidBody, YoWrench> yoWrenches = new HashMap<>();

   private final Map<RigidBody, YoMatrix> yoJacobians = new HashMap<>();
   private final Map<OneDoFJoint, DoubleYoVariable> vmcTorques = new HashMap<>();

   private final GeometricJacobianHolder geometricJacobianHolder;
   private final RigidBody defaultRootBody;
   private final ReferenceFrame centerOfMassFrame;

   private final Map<InverseDynamicsJoint, Double> jointTorques = new HashMap<>();

   private final VirtualModelControlDataHandler vmcDataHandler = new VirtualModelControlDataHandler();

   private final DenseMatrix64F fullJTMatrix = new DenseMatrix64F(0, 0);
   private final DenseMatrix64F fullObjectiveWrench = new DenseMatrix64F(0, 0, true); // make it row major
   private final DenseMatrix64F fullEffortMatrix = new DenseMatrix64F(1, 1);

   public VirtualModelController(GeometricJacobianHolder geometricJacobianHolder, RigidBody defaultRootBody, OneDoFJoint[] controlledJoints,
         ReferenceFrame centerOfMassFrame, YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.geometricJacobianHolder = geometricJacobianHolder;
      this.defaultRootBody = defaultRootBody;
      this.centerOfMassFrame = centerOfMassFrame;
      this.registry = registry;
      this.yoGraphicsListRegistry = yoGraphicsListRegistry;

      fullJTMatrix.reshape(0, 0);
      fullObjectiveWrench.reshape(0, 0);

      if (DEBUG)
      {
         for (OneDoFJoint joint : controlledJoints)
            vmcTorques.put(joint, new DoubleYoVariable("tau_vmc_" + joint.getName(), registry));
      }
   }

   public void registerControlledBody(RigidBody controlledBody)
   {
      registerControlledBody(controlledBody, defaultRootBody);
   }

   public void registerControlledBody(RigidBody controlledBody, RigidBody baseOfControl)
   {
      OneDoFJoint[] joints = ScrewTools.createOneDoFJointPath(baseOfControl, controlledBody);
      registerControlledBody(controlledBody, joints);
   }

   public void registerControlledBody(RigidBody controlledBody, OneDoFJoint[] jointsToUse)
   {
      vmcDataHandler.addBodyForControl(controlledBody);
      vmcDataHandler.addJointsForControl(controlledBody, jointsToUse);
   }

   public void createYoVariable(RigidBody controlledBody)
   {
      YoWrench yoWrench = new YoWrench(controlledBody.getName() + "_desiredWrench", centerOfMassFrame, centerOfMassFrame,
            registry);
      yoWrenches.put(controlledBody, yoWrench);

      if (DEBUG)
      {
         YoMatrix yoMatrix = new YoMatrix(controlledBody.getName() + "JacobianMatrix", 6, 3, registry);
         yoJacobians.put(controlledBody, yoMatrix);
      }
   }

   public void submitControlledBodyVirtualWrench(RigidBody controlledBody, Wrench wrench)
   {
      submitControlledBodyVirtualWrench(controlledBody, wrench, new CommonOps().identity(Wrench.SIZE, Wrench.SIZE));
   }

   public void submitControlledBodyVirtualWrench(VirtualWrenchCommand virtualWrenchCommand)
   {
      submitControlledBodyVirtualWrench(virtualWrenchCommand.getControlledBody(), virtualWrenchCommand.getVirtualWrench(), virtualWrenchCommand.getSelectionMatrix());
   }

   public void submitControlledBodyVirtualWrench(RigidBody controlledBody, Wrench wrench, DenseMatrix64F selectionMatrix)
   {
      //wrench.changeBodyFrameAttachedToSameBody(controlledBody.getBodyFixedFrame());

      vmcDataHandler.addDesiredWrench(controlledBody, wrench);
      vmcDataHandler.addDesiredSelectionMatrix(controlledBody, selectionMatrix);
   }

   public List<RigidBody> getControlledBodies()
   {
      return vmcDataHandler.getControlledBodies();
   }

   public void reset()
   {
      vmcDataHandler.reset();
      jointTorques.clear();
   }

   public void clear()
   {
      vmcDataHandler.clear();
      jointTorques.clear();
   }

   private final DenseMatrix64F wrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
   private final DenseMatrix64F tmpWrench = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tmpJMatrix = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F tmpJTMatrix = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F matrixToCopy = new DenseMatrix64F(1, 1);

   public void compute(VirtualModelControlSolution virtualModelControlSolutionToPack)
   {
      matrixToCopy.reshape(0, 0);
      fullJTMatrix.reshape(0, 0);
      fullObjectiveWrench.reshape(0, 0);
      fullEffortMatrix.reshape(vmcDataHandler.numberOfControlledJoints, 1);
      matrixToCopy.zero();
      fullJTMatrix.zero();
      fullObjectiveWrench.zero();
      fullEffortMatrix.zero();

      if (USE_SUPER_JACOBIAN)
      {
         for (RigidBody controlledBody : vmcDataHandler.getControlledBodies())
         {
            if (vmcDataHandler.hasWrench(controlledBody) && vmcDataHandler.hasSelectionMatrix(controlledBody))
            {
               //vmcDataHandler.loadBody(controlledBody);

               int numberOfControlChains = vmcDataHandler.numberOfChains(controlledBody);

               Wrench wrench = vmcDataHandler.getDesiredWrench(controlledBody);
               DenseMatrix64F selectionMatrix = vmcDataHandler.getDesiredSelectionMatrix(controlledBody);
               int taskSize = selectionMatrix.getNumRows();
               // check and set frames
               wrench.changeFrame(centerOfMassFrame);
               if (yoWrenches.get(controlledBody) != null)
                  yoWrenches.get(controlledBody).set(wrench);

               if (VISUALIZE_DESIRED_WRENCHES && (registry != null) && (yoGraphicsListRegistry != null))
               {
                  controlledBodyPoints.get(controlledBody).setToZero(controlledBody.getBodyFixedFrame());
                  controlledBodyPoints.get(controlledBody).changeFrame(ReferenceFrame.getWorldFrame());
                  yoForcePoints.get(controlledBody).set(controlledBodyPoints.get(controlledBody));
                  yoForceVectors.get(controlledBody).set(wrench.getLinearPart());
               }

               // apply selection matrix to wrench
               tmpWrench.reshape(taskSize, 1);
               wrench.getMatrix(wrenchMatrix);
               CommonOps.mult(selectionMatrix, wrenchMatrix, tmpWrench);

               // append wrench to the end of the current objective wrench vector
               int previousSize = fullObjectiveWrench.getNumRows();
               int newSize = previousSize + taskSize;
               fullObjectiveWrench.reshape(newSize, 1, true);
               CommonOps.extract(tmpWrench, 0, taskSize, 0, 1, fullObjectiveWrench, previousSize, 0);

               for (int chainID = 0; chainID < numberOfControlChains; chainID++)
               {
                  // get jacobian
                  long jacobianID = geometricJacobianHolder
                        .getOrCreateGeometricJacobian(vmcDataHandler.getJointsForControl(controlledBody, chainID), centerOfMassFrame);

                  // Apply selection matrix to jacobian
                  int numberOfJoints = vmcDataHandler.jointsInChain(controlledBody, chainID);
                  tmpJMatrix.reshape(taskSize, numberOfJoints);
                  tmpJTMatrix.reshape(numberOfJoints, taskSize);
                  CommonOps.mult(selectionMatrix, geometricJacobianHolder.getJacobian(jacobianID).getJacobianMatrix(), tmpJMatrix);
                  CommonOps.transpose(tmpJMatrix, tmpJTMatrix);

                  // insert new jacobian into full objective jacobian
                  matrixToCopy.set(fullJTMatrix);
                  fullJTMatrix.reshape(vmcDataHandler.numberOfControlledJoints, newSize);
                  fullJTMatrix.zero();
                  CommonOps.extract(matrixToCopy, 0, matrixToCopy.getNumRows(), 0, matrixToCopy.getNumCols(), fullJTMatrix, 0, 0);
                  for (int jointID = 0; jointID < numberOfJoints; jointID++)
                  {
                     CommonOps.extract(tmpJTMatrix, jointID, jointID + 1, 0, taskSize, fullJTMatrix,
                           vmcDataHandler.indexOfInTree(controlledBody, chainID, jointID), previousSize);
                  }
               }
            }
            else
            {
               PrintTools.warn(this, "Do not have a wrench or selection matrix for body " + controlledBody.getName() + ", skipping this body.");
            }
         }

         // compute forces
         CommonOps.mult(fullJTMatrix, fullObjectiveWrench, fullEffortMatrix);
      }
      else
      {
         for (RigidBody controlledBody : vmcDataHandler.getControlledBodies())
         {
            if (vmcDataHandler.hasWrench(controlledBody))
            {
               Wrench wrench = vmcDataHandler.getDesiredWrench(controlledBody);

               // check and set frames
               wrench.changeFrame(centerOfMassFrame);
               wrench.changeBodyFrameAttachedToSameBody(centerOfMassFrame);
               if (yoWrenches.get(controlledBody) != null)
                  yoWrenches.get(controlledBody).set(wrench);

               if (VISUALIZE_DESIRED_WRENCHES && (registry != null) && (yoGraphicsListRegistry != null))
               {
                  controlledBodyPoints.get(controlledBody).setToZero(controlledBody.getBodyFixedFrame());
                  controlledBodyPoints.get(controlledBody).changeFrame(ReferenceFrame.getWorldFrame());
                  yoForcePoints.get(controlledBody).set(controlledBodyPoints.get(controlledBody));
                  yoForceVectors.get(controlledBody).set(wrench.getLinearPart());
               }

               // get jacobian and torques
               long jacobianID = geometricJacobianHolder.getOrCreateGeometricJacobian(vmcDataHandler.getJointsForControl(controlledBody, 0), centerOfMassFrame);
               GeometricJacobian jacobian = geometricJacobianHolder.getJacobian(jacobianID);
               yoJacobians.get(controlledBody).set(jacobian.getJacobianMatrix());
               DenseMatrix64F jointTorques = jacobian.computeJointTorques(wrench);

               // put into full thing
               for (int j = 0; j < vmcDataHandler.jointsInChain(controlledBody, 0); j++)
                  CommonOps.extract(jointTorques, j, j+1, 0, 1, fullEffortMatrix, vmcDataHandler.indexOfInTree(controlledBody, 0, j), 0);

               if (DEBUG)
               {
                  for (int j = 0; j < vmcDataHandler.jointsInChain(controlledBody, 0); j++)
                     vmcTorques.get(vmcDataHandler.getJointsForControl(controlledBody, 0)[j]).set(jointTorques.get(j, 0));
               }
            }
         }
      }


      // Write torques to map
      int index = 0;
      for (InverseDynamicsJoint joint : vmcDataHandler.getControlledJoints())
      {
         jointTorques.put(joint, fullEffortMatrix.get(index));
         index++;
      }

      virtualModelControlSolutionToPack.setJointTorques(jointTorques);
   }
}
