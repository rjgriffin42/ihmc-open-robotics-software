package us.ihmc.commonWalkingControlModules.controllerCore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.commonWalkingControlModules.controlModules.YoSE3OffsetFrame;
import us.ihmc.euclid.interfaces.Clearable;
import us.ihmc.robotics.controllers.OrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.PositionPIDGainsInterface;
import us.ihmc.robotics.controllers.SE3PIDGainsInterface;
import us.ihmc.robotics.controllers.YoAxisAngleOrientationGains;
import us.ihmc.robotics.controllers.YoEuclideanPositionGains;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.filters.RateLimitedYoFrameVector;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class FeedbackControllerToolbox implements FeedbackControllerDataReadOnly
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final List<Clearable> clearableData = new ArrayList<>();

   private final Map<RigidBody, EnumMap<Type, YoFramePoint>> endEffectorPositions = new HashMap<>();
   private final Map<RigidBody, EnumMap<Type, YoFrameQuaternion>> endEffectorOrientations = new HashMap<>();
   private final Map<RigidBody, EnumMap<Type, EnumMap<Space, YoFrameVector>>> endEffectorDataVectors = new HashMap<>();
   private final Map<RigidBody, EnumMap<Space, RateLimitedYoFrameVector>> endEffectorRateLimitedDataVectors = new HashMap<>();

   private final Map<RigidBody, YoOrientationPIDGainsInterface> endEffectorOrientationGains = new HashMap<>();
   private final Map<RigidBody, YoPositionPIDGainsInterface> endEffectorPositionGains = new HashMap<>();

   private final Map<RigidBody, YoSE3OffsetFrame> endEffectorControlFrames = new HashMap<>();

   public FeedbackControllerToolbox(YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
   }

   public YoFramePoint getOrCreatePosition(RigidBody endEffector, Type type)
   {
      EnumMap<Type, YoFramePoint> typeDependentPositions = endEffectorPositions.get(endEffector);

      if (typeDependentPositions == null)
      {
         typeDependentPositions = new EnumMap<>(Type.class);
         endEffectorPositions.put(endEffector, typeDependentPositions);
      }

      YoFramePoint yoFramePoint = typeDependentPositions.get(type);

      if (yoFramePoint == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += Space.POSITION.getName();
         yoFramePoint = new YoFramePoint(namePrefix, worldFrame, registry);
         typeDependentPositions.put(type, yoFramePoint);
         clearableData.add(yoFramePoint);
      }

      return yoFramePoint;
   }

   public YoFrameQuaternion getOrCreateOrientation(RigidBody endEffector, Type type)
   {
      EnumMap<Type, YoFrameQuaternion> typeDependentOrientations = endEffectorOrientations.get(endEffector);

      if (typeDependentOrientations == null)
      {
         typeDependentOrientations = new EnumMap<>(Type.class);
         endEffectorOrientations.put(endEffector, typeDependentOrientations);
      }

      YoFrameQuaternion yoFrameQuaternion = typeDependentOrientations.get(type);

      if (yoFrameQuaternion == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += Space.ORIENTATION.getName();
         yoFrameQuaternion = new YoFrameQuaternion(namePrefix, worldFrame, registry);
         typeDependentOrientations.put(type, yoFrameQuaternion);
         clearableData.add(yoFrameQuaternion);
      }

      return yoFrameQuaternion;
   }

   public YoFrameVector getOrCreateDataVector(RigidBody endEffector, Type type, Space space)
   {
      EnumMap<Type, EnumMap<Space, YoFrameVector>> dataVectorStep1 = endEffectorDataVectors.get(endEffector);

      if (dataVectorStep1 == null)
      {
         dataVectorStep1 = new EnumMap<>(Type.class);
         endEffectorDataVectors.put(endEffector, dataVectorStep1);
      }

      EnumMap<Space, YoFrameVector> dataVectorStep2 = dataVectorStep1.get(type);

      if (dataVectorStep2 == null)
      {
         dataVectorStep2 = new EnumMap<>(Space.class);
         dataVectorStep1.put(type, dataVectorStep2);
      }

      YoFrameVector yoFrameVector = dataVectorStep2.get(space);

      if (yoFrameVector == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += space.getName();
         yoFrameVector = new YoFrameVector(namePrefix, worldFrame, registry);
         dataVectorStep2.put(space, yoFrameVector);
         clearableData.add(yoFrameVector);
      }

      return yoFrameVector;
   }

   public RateLimitedYoFrameVector getRateLimitedDataVector(RigidBody endEffector, Type rawDataType, Space space, double dt, DoubleYoVariable maximumRate)
   {
      EnumMap<Space, RateLimitedYoFrameVector> endEffectorDataVectors = endEffectorRateLimitedDataVectors.get(endEffector);

      if (endEffectorDataVectors == null)
      {
         endEffectorDataVectors = new EnumMap<>(Space.class);
         endEffectorRateLimitedDataVectors.put(endEffector, endEffectorDataVectors);
      }

      RateLimitedYoFrameVector rateLimitedYoFrameVector = endEffectorDataVectors.get(space);
      YoFrameVector rawYoFrameVector = getOrCreateDataVector(endEffector, rawDataType, space);

      if (rateLimitedYoFrameVector == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += "RateLimited";
         namePrefix += rawDataType.getName();
         namePrefix += space.getName();
         rateLimitedYoFrameVector = RateLimitedYoFrameVector.createRateLimitedYoFrameVector(namePrefix, "", registry, maximumRate, dt, rawYoFrameVector);
         endEffectorDataVectors.put(space, rateLimitedYoFrameVector);
         clearableData.add(rateLimitedYoFrameVector);
      }

      return rateLimitedYoFrameVector;
   }

   public YoOrientationPIDGainsInterface getOrientationGains(RigidBody endEffector)
   {
      YoOrientationPIDGainsInterface gains = endEffectorOrientationGains.get(endEffector);

      if (gains == null)
      {
         gains = new YoAxisAngleOrientationGains(endEffector.getName(), registry);
         endEffectorOrientationGains.put(endEffector, gains);
      }
      return gains;
   }

   public YoPositionPIDGainsInterface getPositionGains(RigidBody endEffector)
   {
      YoPositionPIDGainsInterface gains = endEffectorPositionGains.get(endEffector);

      if (gains == null)
      {
         gains = new YoEuclideanPositionGains(endEffector.getName(), registry);
         endEffectorPositionGains.put(endEffector, gains);
      }
      return gains;
   }

   public YoSE3PIDGainsInterface getSE3PIDGains(RigidBody endEffector)
   {
      YoPositionPIDGainsInterface positionGains = getPositionGains(endEffector);
      YoOrientationPIDGainsInterface orientationGains = getOrientationGains(endEffector);

      return new YoSE3PIDGainsInterface()
      {
         @Override
         public void set(PositionPIDGainsInterface positionGains)
         {
            positionGains.set(positionGains);
         }

         @Override
         public void set(OrientationPIDGainsInterface orientationGains)
         {
            orientationGains.set(orientationGains);
         }

         @Override
         public void set(SE3PIDGainsInterface gains)
         {
            positionGains.set(gains.getPositionGains());
            orientationGains.set(gains.getOrientationGains());
         }

         @Override
         public YoPositionPIDGainsInterface getPositionGains()
         {
            return positionGains;
         }

         @Override
         public YoOrientationPIDGainsInterface getOrientationGains()
         {
            return orientationGains;
         }
      };
   }

   public YoSE3OffsetFrame getControlFrame(RigidBody endEffector)
   {
      YoSE3OffsetFrame controlFrame = endEffectorControlFrames.get(endEffector);

      if (controlFrame == null)
      {
         controlFrame = new YoSE3OffsetFrame(endEffector.getName() + "BodyFixedControlFrame", endEffector.getBodyFixedFrame(), registry);
         endEffectorControlFrames.put(endEffector, controlFrame);
      }

      return controlFrame;
   }

   /**
    * Calls {@link Clearable#setToNaN()} to all the register objects used by the feedback
    * controllers.
    * <p>
    * The method should be called at the beginning of the controller core tick such that the unused
    * part of the data will be {@link Double#NaN} making it clear what it is used and what is not.
    * </p>
    */
   public void clearData()
   {
      for (int i = 0; i < clearableData.size(); i++)
         clearableData.get(i).setToNaN();
   }

   @Override
   public boolean getPositionData(RigidBody endEffector, FramePoint positionDataToPack, Type type)
   {
      EnumMap<Type, YoFramePoint> endEffectorData = endEffectorPositions.get(endEffector);

      if (endEffectorData == null)
         return false;

      YoFramePoint positionData = endEffectorData.get(type);

      if (positionData == null)
         return false;

      positionData.getFrameTupleIncludingFrame(positionDataToPack);
      return true;
   }

   @Override
   public boolean getOrientationData(RigidBody endEffector, FrameOrientation orientationDataToPack, Type type)
   {
      EnumMap<Type, YoFrameQuaternion> endEffectorData = endEffectorOrientations.get(endEffector);

      if (endEffectorData == null)
         return false;

      YoFrameQuaternion orientationData = endEffectorData.get(type);

      if (orientationData == null)
         return false;

      orientationData.getFrameOrientationIncludingFrame(orientationDataToPack);
      return true;
   }

   @Override
   public boolean getVectorData(RigidBody endEffector, FrameVector vectorDataToPack, Type type, Space space)
   {
      EnumMap<Type, EnumMap<Space, YoFrameVector>> endEffectorData = endEffectorDataVectors.get(endEffector);

      if (endEffectorData == null)
         return false;

      EnumMap<Space, YoFrameVector> endEffectorDataTyped = endEffectorData.get(type);

      if (endEffectorDataTyped == null)
         return false;

      YoFrameVector vectorData = endEffectorDataTyped.get(space);

      if (vectorData == null)
         return false;

      return false;
   }
}
