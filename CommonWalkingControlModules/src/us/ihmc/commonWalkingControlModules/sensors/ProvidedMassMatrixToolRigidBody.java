package us.ihmc.commonWalkingControlModules.sensors;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.vecmath.Matrix3d;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
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
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphic;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;



public class ProvidedMassMatrixToolRigidBody
{
   
   private final YoVariableRegistry registry;
   
   private final PoseReferenceFrame toolFrame; 
   private final RigidBody toolBody;
   
   private final ReferenceFrame handFixedFrame;
   private final ReferenceFrame wristFrame;
   
   private final YoFramePoint objectCenterOfMass;
   private final YoFramePoint objectCenterOfMassInWorld;
   private final YoFrameVector objectForceInWorld;
   
   private final DoubleYoVariable objectMass;
   
   private final InverseDynamicsCalculator inverseDynamicsCalculator;
   private final SixDoFJoint toolJoint;
   private final ReferenceFrame elevatorFrame;
   
   private final FramePoint temporaryPoint = new FramePoint();
   private final FrameVector temporaryVector = new FrameVector();
   private final SpatialAccelerationVector toolAcceleration = new SpatialAccelerationVector();

   
   
   public ProvidedMassMatrixToolRigidBody(String name, final InverseDynamicsJoint wristJoint, final FullRobotModel fullRobotModel, double gravity, 
         double controlDT, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      
      
      this.handFixedFrame = wristJoint.getSuccessor().getBodyFixedFrame();
      this.wristFrame = wristJoint.getFrameAfterJoint();
      
      this.elevatorFrame = fullRobotModel.getElevatorFrame();
      toolFrame = new PoseReferenceFrame(name + "Frame", elevatorFrame);
      
      RigidBodyInertia inertia = new RigidBodyInertia(toolFrame, new Matrix3d(), 0.0);
      
      this.toolJoint = new SixDoFJoint(name+"Joint", fullRobotModel.getElevator(), fullRobotModel.getElevator().getBodyFixedFrame());
      this.toolBody = new RigidBody(name+"Body", inertia, toolJoint);
      
      TwistCalculator twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), toolBody);
      boolean doVelocityTerms = true;
      SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(toolBody, elevatorFrame,
            ScrewTools.createGravitationalSpatialAcceleration(fullRobotModel.getElevator(), gravity), twistCalculator, doVelocityTerms, doVelocityTerms);
      
      ArrayList<InverseDynamicsJoint> jointsToIgnore = new ArrayList<InverseDynamicsJoint>();
      jointsToIgnore.addAll(twistCalculator.getRootBody().getChildrenJoints());
      jointsToIgnore.remove(toolJoint);
      
      inverseDynamicsCalculator = new InverseDynamicsCalculator(ReferenceFrame.getWorldFrame(), new LinkedHashMap<RigidBody, Wrench>(),
            jointsToIgnore, spatialAccelerationCalculator, twistCalculator, doVelocityTerms);
           
      objectCenterOfMass = new YoFramePoint(name + "CoMOffset", wristFrame, registry);
      objectMass = new DoubleYoVariable(name + "ObjectMass", registry);
      objectForceInWorld = new YoFrameVector(name + "Force", ReferenceFrame.getWorldFrame(), registry);
      
      
      this.objectCenterOfMassInWorld = new YoFramePoint(name + "CoMInWorld", ReferenceFrame.getWorldFrame(), registry);
      
      if(yoGraphicsListRegistry != null)
      {
         
         YoGraphicsList yoGraphicsList = new YoGraphicsList(name);
         YoGraphic comViz = new YoGraphicPosition(name + "CenterOfMassViz", objectCenterOfMassInWorld, 0.05, YoAppearance.Red());
         yoGraphicsList.add(comViz);              
                  
         YoGraphic vectorViz = new YoGraphicVector(name + "ForceViz", objectCenterOfMassInWorld, objectForceInWorld, YoAppearance.Yellow());
         yoGraphicsList.add(vectorViz);
         
         yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      }
      parentRegistry.addChild(registry);
   }
   
   
   public void update()
   {
      toolBody.getInertia().setMass(objectMass.getDoubleValue());
      
      
      temporaryPoint.setIncludingFrame(objectCenterOfMass.getFrameTuple());
      temporaryPoint.changeFrame(elevatorFrame);
      toolFrame.setPositionAndUpdate(temporaryPoint);
      
      
      FramePoint toolFramePoint = new FramePoint(toolFrame);
      toolFramePoint.changeFrame(ReferenceFrame.getWorldFrame());
      
      // Visualization stuff
      objectCenterOfMassInWorld.set(toolFramePoint);
   }

   public void control(SpatialAccelerationVector spatialAccelerationVector, Wrench toolWrench)
   {
      
      update();
      
      toolAcceleration.set(spatialAccelerationVector);
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
      
      temporaryVector.setIncludingFrame(handFixedFrame, toolWrench.getLinearPartX(), toolWrench.getLinearPartY(), toolWrench.getLinearPartZ());
      temporaryVector.changeFrame(ReferenceFrame.getWorldFrame());
      temporaryVector.scale(0.01);
      objectForceInWorld.set(temporaryVector);
      
   }

}
