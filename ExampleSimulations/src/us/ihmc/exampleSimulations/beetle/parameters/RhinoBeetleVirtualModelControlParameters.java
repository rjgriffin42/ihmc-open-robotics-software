package us.ihmc.exampleSimulations.beetle.parameters;

import us.ihmc.commonWalkingControlModules.controlModules.foot.YoFootOrientationGains;
import us.ihmc.commonWalkingControlModules.controlModules.foot.YoFootPositionGains;
import us.ihmc.commonWalkingControlModules.controlModules.foot.YoFootSE3Gains;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.controllers.YoPIDSE3Gains;
import us.ihmc.robotics.controllers.YoSymmetricSE3PIDGains;
import us.ihmc.robotics.controllers.pidGains.PIDSE3Gains;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;

public class RhinoBeetleVirtualModelControlParameters implements HexapodControllerParameters
{
   private final String name = "vmcParams_";
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   
   private final YoPIDSE3Gains footGains;
   
   //body spatial feeback controller params
   private final Vector3D linearWeight = new Vector3D(1.0, 1.0, 10.0);
   private final Vector3D angularWeight = new Vector3D(1.0, 1.0, 1.0);
   private final YoSymmetricSE3PIDGains bodySpatialGains;
   private final double bodyProportionalGains = 8000.0;
   private final double bodyDampingRatio = 3.0;
   private final YoFrameVector bodySpatialLinearQPWeight;
   private final YoFrameVector bodySpatialAngularQPWeight;
   private final SelectionMatrix6D bodySpatialSelectionMatrix = new SelectionMatrix6D();
   
   public RhinoBeetleVirtualModelControlParameters(YoVariableRegistry parentRegistry)
   {
      bodySpatialGains = new YoSymmetricSE3PIDGains(name + "bodySpatialGains", registry);
      bodySpatialGains.setProportionalGain(bodyProportionalGains);
      bodySpatialGains.setDampingRatio(bodyDampingRatio);
      bodySpatialGains.createDerivativeGainUpdater(true);
      
      bodySpatialLinearQPWeight = new YoFrameVector(name + "bodySpatial_linear_QPWeight", ReferenceFrame.getWorldFrame(), registry);
      bodySpatialAngularQPWeight = new YoFrameVector(name + "bodySpatial_angular_QPWeight", ReferenceFrame.getWorldFrame(), registry);
      bodySpatialAngularQPWeight.setVector(angularWeight);
      bodySpatialLinearQPWeight.setVector(linearWeight);
      
      
      footGains = new YoFootSE3Gains(name + "FootGains", registry);
      YoFootPositionGains positionGains = new YoFootPositionGains(name + "footPositionGains", registry);
      positionGains.setProportionalGains(getSwingXYProportionalGain(), getSwingZProportionalGain());
      positionGains.setDampingRatio(0.9);
      positionGains.createDerivativeGainUpdater(true);
      footGains.setPositionGains(positionGains);
      YoFootOrientationGains orientationGains = new YoFootOrientationGains(name + "footOrientationGains", registry);
      orientationGains.setProportionalGains(0.0, 0.0, 0.0);
      footGains.setOrientationGains(orientationGains);
      
      parentRegistry.addChild(registry);
   }

   @Override
   public double getSwingTime()
   {
      return 0.5;
   }

   @Override
   public double getSwingXYProportionalGain()
   {
      return 8000.0;
   }

   @Override
   public double getSwingZProportionalGain()
   {
      return 6000.0;
   }

   @Override
   public PIDSE3Gains getBodySpatialGains()
   {
      return bodySpatialGains;
   }

   @Override
   public void getBodySpatialLinearQPWeight(Vector3D linearWeight)
   {
      bodySpatialLinearQPWeight.get(linearWeight);
   }

   @Override
   public void getBodySpatialAngularQPWeight(Vector3D angularWeight)
   {
      bodySpatialAngularQPWeight.get(angularWeight);
   }

   @Override
   public SelectionMatrix6D getBodySpatialSelectionMatrix()
   {
      return bodySpatialSelectionMatrix;
   }

   @Override
   public YoPIDSE3Gains getFootGains()
   {
      return footGains;
   }

   @Override
   public double getTransferTime()
   {
      // TODO Auto-generated method stub
      return 0;
   }
}
