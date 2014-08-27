package us.ihmc.commonWalkingControlModules.sensors;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.SingularMatrixException;
import org.ejml.ops.CommonOps;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.kalman.YoKalmanFilter;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CenterOfMassCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.VariableChangedListener;
import us.ihmc.yoUtilities.YoVariable;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;


public class MassMatrixEstimatingToolRigidBody
{
   
   private final YoVariableRegistry registry;
   
   private final double gravity;
   
   private final PoseReferenceFrame toolFrame; 
   private final RigidBody toolBody;
   
   private final ReferenceFrame handFixedFrame;
   private final ReferenceFrame wristFrame;
   private final CenterOfMassCalculator comCalculator;
   
   private final Wrench calculatedObjectWrench;
   
   private final YoFramePoint objectCenterOfMass;
   
   private final AlphaFilteredYoVariable objectMass;
   private final YoKalmanFilter objectCoMFilter;
   private final DenseMatrix64F F = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F G = new DenseMatrix64F(3, 0);
   private final DenseMatrix64F H = new DenseMatrix64F(3, 3);
   
   private final DenseMatrix64F u = new DenseMatrix64F(0, 0);
   
   private final DenseMatrix64F R = new DenseMatrix64F(3, 3);
   private final DenseMatrix64F Q = new DenseMatrix64F(3, 3);
   
   private final DenseMatrix64F y = new DenseMatrix64F(3, 1);
   
   private final DenseMatrix64F x = new DenseMatrix64F(3, 1);
   private final DenseMatrix64F P = new DenseMatrix64F(3, 3);
   
   private final InverseDynamicsCalculator inverseDynamicsCalculator;
   private final SixDoFJoint toolJoint;
   private final ReferenceFrame elevatorFrame;
   
   // Visualization lines
//   private final int numberOfLines = 200;
//   @SuppressWarnings("unchecked")
//   private final Pair<YoFramePoint, YoFramePoint>[] yoLinePoints = new Pair[numberOfLines];
//   private final FrameLine[] centerOfMassLines = new FrameLine[numberOfLines];
//   
//   private int currentIndex = 0;
   
   private final DoubleYoVariable RDiagonal;
   private final DoubleYoVariable QDiagonal;
   
   
   
   public MassMatrixEstimatingToolRigidBody(String name, final InverseDynamicsJoint wristJoint, final FullRobotModel fullRobotModel, double gravity, 
         double controlDT, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.gravity = gravity;
      
      
      this.handFixedFrame = wristJoint.getSuccessor().getBodyFixedFrame();
      this.wristFrame = wristJoint.getFrameAfterJoint();
      
      this.elevatorFrame = fullRobotModel.getElevatorFrame();
      toolFrame = new PoseReferenceFrame(name + "Frame", elevatorFrame);
      
      RigidBodyInertia inertia = new RigidBodyInertia(toolFrame, new Matrix3d(), 0.0);
      
      this.toolJoint = new SixDoFJoint("toolJoint", fullRobotModel.getElevator(), fullRobotModel.getElevator().getBodyFixedFrame());
      this.toolBody = new RigidBody("toolBody", inertia, toolJoint);
      
      TwistCalculator twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), toolBody);
      boolean doVelocityTerms = true;
      SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(toolBody, elevatorFrame,
            ScrewTools.createGravitationalSpatialAcceleration(fullRobotModel.getElevator(), gravity), twistCalculator, doVelocityTerms, doVelocityTerms);
      
      ArrayList<InverseDynamicsJoint> jointsToIgnore = new ArrayList<InverseDynamicsJoint>();
      jointsToIgnore.addAll(twistCalculator.getRootBody().getChildrenJoints());
      jointsToIgnore.remove(toolJoint);
      
      inverseDynamicsCalculator = new InverseDynamicsCalculator(ReferenceFrame.getWorldFrame(), new LinkedHashMap<RigidBody, Wrench>(),
            jointsToIgnore, spatialAccelerationCalculator, twistCalculator, doVelocityTerms);

      RigidBody[] rigidBodies = ScrewTools.computeSubtreeSuccessors(wristJoint);
     
      this.comCalculator = new CenterOfMassCalculator(rigidBodies, wristFrame);
      calculatedObjectWrench = new Wrench(wristFrame, wristFrame);
      
      objectMass = new AlphaFilteredYoVariable("objectMass", registry, AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(1.0, controlDT));
      objectCoMFilter = new YoKalmanFilter(name + "Filter", registry);
      CommonOps.setIdentity(F);
      
      CommonOps.setIdentity(R);
      CommonOps.scale(2.0, R);
      
      CommonOps.setIdentity(Q);
      CommonOps.scale(1e-6, Q);
      
      CommonOps.setIdentity(P);
      CommonOps.scale(100.0, P);

      objectCoMFilter.configure(F, G, H);
      reset();
      
      RDiagonal = new DoubleYoVariable("RDiagonal", registry);
      
      RDiagonal.addVariableChangedListener(new VariableChangedListener()
      {
         
         public void variableChanged(YoVariable v)
         {
            CommonOps.setIdentity(R);
            CommonOps.scale(RDiagonal.getDoubleValue(), R);
         }
      });
      
      QDiagonal = new DoubleYoVariable("QDiagonal", registry);
      QDiagonal.addVariableChangedListener(new VariableChangedListener()
      {
         
         public void variableChanged(YoVariable v)
         {
            CommonOps.setIdentity(Q);
            CommonOps.scale(QDiagonal.getDoubleValue(), Q);
         }
      });
      
      RDiagonal.set(R.get(0,0));
      QDiagonal.set(Q.get(0,0));
      
      
      this.objectCenterOfMass = new YoFramePoint(name + "CenterOfMass", ReferenceFrame.getWorldFrame(), registry);
      
//      for (int i = 0; i < numberOfLines; i++)
//      {
//         Pair<YoFramePoint, YoFramePoint> linePair = new Pair<YoFramePoint, YoFramePoint>(new YoFramePoint(name + "CoMStartPoint" + i,
//               ReferenceFrame.getWorldFrame(), registry), new YoFramePoint(name + "CoMEndPoint" + i, ReferenceFrame.getWorldFrame(), registry));
//         yoLinePoints[i] = linePair;
//      }
      
      if(dynamicGraphicObjectsListRegistry != null)
      {
         
         DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList(name);
         DynamicGraphicObject comViz = objectCenterOfMass.createDynamicGraphicPosition(name + "CenterOfMassViz", 0.05, YoAppearance.Red());
         dynamicGraphicObjectsList.add(comViz);
         
//         for(Pair<YoFramePoint, YoFramePoint> linePair : yoLinePoints)
//         {
//            DynamicGraphicObject comLineViz = new DynamicGraphicLineSegment(name + "CoMApplicationLineViz", linePair.first(), linePair.second(), 1.0, YoAppearance.Red(), false, 0.0002);
//            dynamicGraphicObjectsList.add(comLineViz);
//         }                  
         
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      }
      parentRegistry.addChild(registry);
   }
   
   
   public void update(Wrench measuredWristWrench)
   {
      comCalculator.compute();
      FrameVector gravityVector = new FrameVector(ReferenceFrame.getWorldFrame(), 0.0, 0.0, -gravity);
      gravityVector.changeFrame(wristFrame);
      gravityVector.scale(comCalculator.getTotalMass());
      
      FramePoint com = comCalculator.getCenterOfMass();
      
      FrameVector torque = new FrameVector(wristFrame);
      torque.cross(com, gravityVector);
      
      calculatedObjectWrench.set(wristFrame, gravityVector.getVector(), torque.getVector());
      
      if(measuredWristWrench.getBodyFrame() == null || measuredWristWrench.getExpressedInFrame() == null)
      {
         return;
      }
      
      calculatedObjectWrench.changeBodyFrameAttachedToSameBody(wristFrame);
      calculatedObjectWrench.changeFrame(wristFrame);
      
      measuredWristWrench.sub(calculatedObjectWrench);
      
      // Use calculated wrench for testing, switch to measured to make it work!
//      measuredWristWrench = calculatedObjectWrench;
      
      
      measuredWristWrench.changeFrame(wristFrame);
      measuredWristWrench.changeBodyFrameAttachedToSameBody(wristFrame);
      
      FrameVector torqueDueObject = measuredWristWrench.getAngularPartAsFrameVectorCopy();
      FrameVector forceDueObject = measuredWristWrench.getLinearPartAsFrameVectorCopy();
      
      if(torqueDueObject.getVector().epsilonEquals(new Vector3d(), 1e-4) || forceDueObject.getVector().epsilonEquals(new Vector3d(), 1e-4))
      {
         return;
      }
      
      
      MatrixTools.insertTuple3dIntoEJMLVector(torqueDueObject.getVector(), y, 0);
      MatrixTools.vectorToSkewSymmetricMatrix(H, forceDueObject.getVector());
      CommonOps.scale(-1.0, H);
      
      forceDueObject.changeFrame(ReferenceFrame.getWorldFrame());
      objectMass.update(-forceDueObject.getZ()/gravity);
      toolBody.getInertia().setMass(objectMass.getDoubleValue());
      
      try
      {
         objectCoMFilter.configure(F, G, H);
         
         objectCoMFilter.setProcessNoiseCovariance(Q);
         objectCoMFilter.setMeasurementNoiseCovariance(R);
         
         
         objectCoMFilter.predict(u);
         objectCoMFilter.update(y);
      }
      catch(SingularMatrixException e)
      {
         System.err.println("Matrix is singular, resetting");
         reset();
         return;
      }

      FramePoint objectCoM = new FramePoint(wristFrame);
      MatrixTools.denseMatrixToVector3d(objectCoMFilter.getState(), objectCoM.getPoint(), 0, 0);
      
      
      objectCoM.changeFrame(elevatorFrame);
      toolFrame.setPositionAndUpdate(objectCoM);
      
      
      FramePoint toolFramePoint = new FramePoint(toolFrame);
      toolFramePoint.changeFrame(ReferenceFrame.getWorldFrame());
      
      // Visualization stuff
      objectCenterOfMass.set(toolFramePoint);
      
//      FrameVector radius = new FrameVector(measuredWristWrench.getExpressedInFrame());
//      radius.cross(torqueDueObject, forceDueObject);
//      radius.scale(-1.0/forceDueObject.dot(forceDueObject));
//      
//      
//      FrameLine line = new FrameLine(radius.getReferenceFrame(), radius.getVector(), forceDueObject.getVector());
//      centerOfMassLines[currentIndex] = line;
//      
//      currentIndex++;
//      if(currentIndex >= numberOfLines)
//      {
//         currentIndex = 0;
//      }
//      
//      updateVisuals();
   }

   public void control(SpatialAccelerationVector spatialAccelerationVector, Wrench toolWrench)
   {
      
      SpatialAccelerationVector toolAcceleration = new SpatialAccelerationVector(spatialAccelerationVector);
      toolAcceleration.changeFrameNoRelativeMotion(toolJoint.getFrameAfterJoint());
      
      // TODO: Take relative acceleration between uTorsoCoM and elevator in account
      toolAcceleration.changeBaseFrameNoRelativeAcceleration(elevatorFrame);
      
      
      toolAcceleration.changeBodyFrameNoRelativeAcceleration(toolJoint.getFrameAfterJoint());
      
      toolJoint.setDesiredAcceleration(toolAcceleration);
      inverseDynamicsCalculator.compute();
      inverseDynamicsCalculator.getJointWrench(toolJoint, toolWrench);
      
      
      toolWrench.negate();
      
      toolWrench.changeFrame(handFixedFrame);
      toolWrench.changeBodyFrameAttachedToSameBody(handFixedFrame);
      
   }

   public void reset()
   {
    //Initialize
      objectMass.reset();
      objectCoMFilter.setState(x, P);
   }
   
//   private void updateVisuals()
//   {
//      for(int i = 0; i < numberOfLines; i++)
//      {
//         FrameLine line = centerOfMassLines[i];
//         if(line != null)
//         {
//            FramePoint origin = line.getOriginInFrame(ReferenceFrame.getWorldFrame());
//            FrameVector direction = line.getDirectionInFrame(ReferenceFrame.getWorldFrame());
//            
//            
//            FramePoint end = new FramePoint(origin);
//            end.add(direction);
//            
//            origin.sub(direction);
//            
//            
//            yoLinePoints[i].first().set(origin);
//            yoLinePoints[i].second().set(end);
//            
//            
//         }
//      }
//   }
}
