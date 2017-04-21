package us.ihmc.commonWalkingControlModules.controllerCore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.filters.RateLimitedYoFrameVector;
import us.ihmc.robotics.math.filters.RateLimitedYoSpatialVector;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoseUsingQuaternions;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.frames.YoSpatialVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

/**
 * {@code FeedbackControllerToolbox} is meant to be used only in the
 * {@link WholeBodyFeedbackController}.
 * <p>
 * It is used as a factory for creating a unique set of {@code YoVariable}s used by the feedback
 * controllers. For instance, when a {@code YoFramePoint} has already been created by a controller,
 * the same object will be given to be next controller needing it.
 * </p>
 * 
 * @author Sylvain Bertrand
 *
 */
public class FeedbackControllerToolbox implements FeedbackControllerDataReadOnly
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final String centerOfMassName = "centerOfMass";

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final List<Pair<? extends Clearable, List<BooleanYoVariable>>> clearableData = new ArrayList<>();

   private final Map<RigidBody, EnumMap<Type, Pair<YoFramePoint, List<BooleanYoVariable>>>> endEffectorPositions = new HashMap<>();
   private final Map<RigidBody, EnumMap<Type, Pair<YoFrameQuaternion, List<BooleanYoVariable>>>> endEffectorOrientations = new HashMap<>();
   private final Map<RigidBody, EnumMap<Type, EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>>>> endEffectorDataVectors = new HashMap<>();
   private final Map<RigidBody, EnumMap<Space, Pair<RateLimitedYoFrameVector, List<BooleanYoVariable>>>> endEffectorRateLimitedDataVectors = new HashMap<>();

   private final Map<RigidBody, YoOrientationPIDGainsInterface> endEffectorOrientationGains = new HashMap<>();
   private final Map<RigidBody, YoPositionPIDGainsInterface> endEffectorPositionGains = new HashMap<>();

   private final Map<RigidBody, YoSE3OffsetFrame> endEffectorControlFrames = new HashMap<>();

   private final EnumMap<Type, Pair<YoFramePoint, List<BooleanYoVariable>>> centerOfMassPositions = new EnumMap<>(Type.class);
   private final EnumMap<Type, EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>>> centerOfMassDataVectors = new EnumMap<>(Type.class);
   private final EnumMap<Space, Pair<RateLimitedYoFrameVector, List<BooleanYoVariable>>> centerOfMassRateLimitedDataVectors = new EnumMap<>(Space.class);
   private YoPositionPIDGainsInterface centerOfMassPositionGains;

   public FeedbackControllerToolbox(YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
   }

   /**
    * Retrieves and returns the {@code YoFramePoint} for the center of mass associated with the
    * given {@code type}, if it does not exist it is created.
    * 
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoFramePoint} matching the search criterion.
    */
   public YoFramePoint getCenterOfMassPosition(Type type, BooleanYoVariable enabled)
   {
      Pair<YoFramePoint, List<BooleanYoVariable>> yoFramePointEnabledPair = centerOfMassPositions.get(type);

      if (yoFramePointEnabledPair == null)
      {
         String namePrefix = centerOfMassName;
         namePrefix += type.getName();
         namePrefix += Space.POSITION.getName();
         YoFramePoint yoFramePoint = new YoFramePoint(namePrefix, worldFrame, registry);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         yoFramePointEnabledPair = new ImmutablePair<>(yoFramePoint, endabledList);
         centerOfMassPositions.put(type, yoFramePointEnabledPair);
         clearableData.add(yoFramePointEnabledPair);
      }

      yoFramePointEnabledPair.getRight().add(enabled);

      return yoFramePointEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code YoFrameVector} for the center of mass associated with the
    * given {@code type}, and {@code space}, if it does not exist it is created.
    * 
    * @param type the type of the data to retrieve.
    * @param space the space of the data to retrieve.
    * @return the unique {@code YoFrameVector} matching the search criteria.
    */
   public YoFrameVector getCenterOfMassDataVector(Type type, Space space, BooleanYoVariable enabled)
   {
      EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>> dataVectors = centerOfMassDataVectors.get(type);

      if (dataVectors == null)
      {
         dataVectors = new EnumMap<>(Space.class);
         centerOfMassDataVectors.put(type, dataVectors);
      }

      Pair<YoFrameVector, List<BooleanYoVariable>> yoFrameVectorEnabledPair = dataVectors.get(space);

      if (yoFrameVectorEnabledPair == null)
      {
         String namePrefix = centerOfMassName;
         namePrefix += type.getName();
         namePrefix += space.getName();
         YoFrameVector yoFrameVector = new YoFrameVector(namePrefix, worldFrame, registry);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         yoFrameVectorEnabledPair = new ImmutablePair<>(yoFrameVector, endabledList);
         dataVectors.put(space, yoFrameVectorEnabledPair);
         clearableData.add(yoFrameVectorEnabledPair);
      }

      yoFrameVectorEnabledPair.getRight().add(enabled);

      return yoFrameVectorEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code RateLimitedYoFrameVector} for the center of mass associated
    * with the given {@code type}, and {@code space}, if it does not exist it is created.
    * <p>
    * Note: the arguments {@code dt} and {@code maximumRate} are only used if the data does not
    * exist yet.
    * </p>
    * 
    * @param space the space of the data to retrieve.
    * @param rawDataType the type of the raw vector onto which the rate limit is to be applied.
    * @param dt the duration of a control tick.
    * @param maximumRate the maximum rate allowed rate. Not modified.
    * @return the unique {@code RateLimitedYoFrameVector} matching the search criteria.
    */
   public RateLimitedYoFrameVector getCenterOfMassRateLimitedDataVector(Type rawDataType, Space space, double dt, DoubleYoVariable maximumRate,
                                                                        BooleanYoVariable enabled)
   {
      Pair<RateLimitedYoFrameVector, List<BooleanYoVariable>> rateLimitedYoFrameVectorEnabledPair = centerOfMassRateLimitedDataVectors.get(space);

      if (rateLimitedYoFrameVectorEnabledPair == null)
      {
         String namePrefix = centerOfMassName;
         namePrefix += "RateLimited";
         namePrefix += rawDataType.getName();
         namePrefix += space.getName();
         YoFrameVector rawYoFrameVector = getCenterOfMassDataVector(rawDataType, space, enabled);
         RateLimitedYoFrameVector rateLimitedYoFrameVector = new RateLimitedYoFrameVector(namePrefix, "", registry, maximumRate, dt, rawYoFrameVector);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         rateLimitedYoFrameVectorEnabledPair = new ImmutablePair<>(rateLimitedYoFrameVector, endabledList);
         centerOfMassRateLimitedDataVectors.put(space, rateLimitedYoFrameVectorEnabledPair);
         clearableData.add(rateLimitedYoFrameVectorEnabledPair);
      }

      rateLimitedYoFrameVectorEnabledPair.getRight().add(enabled);

      return rateLimitedYoFrameVectorEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the set of gains {@code YoPositionPIDGainsInterface} for the center of
    * mass, if it does not exist it is created.
    * 
    * @return the unique {@code YoPositionPIDGainsInterface} for the center of mass.
    */
   public YoPositionPIDGainsInterface getCenterOfMassGains()
   {
      if (centerOfMassPositionGains == null)
      {
         centerOfMassPositionGains = new YoEuclideanPositionGains(centerOfMassName, registry);
      }
      return centerOfMassPositionGains;
   }

   /**
    * Retrieves and returns the {@code YoFramePoint} associated with the given end-effector and
    * {@code type}, if it does not exist it is created.
    * <p>
    * The name prefix of the created variable is created as follows:<br>
    * {@code namePrefix = endEffector.getName() + type.getName() +}
    * {@link Space#POSITION}{@code .getName()}<br>
    * Such that the desired position for the rigid-body 'rightHand' will have the prefix:
    * "rightHandDesiredPosition".
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoFramePoint} matching the search criteria.
    */
   public YoFramePoint getPosition(RigidBody endEffector, Type type, BooleanYoVariable enabled)
   {
      EnumMap<Type, Pair<YoFramePoint, List<BooleanYoVariable>>> typeDependentPositions = endEffectorPositions.get(endEffector);

      if (typeDependentPositions == null)
      {
         typeDependentPositions = new EnumMap<>(Type.class);
         endEffectorPositions.put(endEffector, typeDependentPositions);
      }

      Pair<YoFramePoint, List<BooleanYoVariable>> yoFramePointEnabledPair = typeDependentPositions.get(type);

      if (yoFramePointEnabledPair == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += Space.POSITION.getName();
         YoFramePoint yoFramePoint = new YoFramePoint(namePrefix, worldFrame, registry);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         yoFramePointEnabledPair = new ImmutablePair<>(yoFramePoint, endabledList);
         typeDependentPositions.put(type, yoFramePointEnabledPair);
         clearableData.add(yoFramePointEnabledPair);
      }

      yoFramePointEnabledPair.getRight().add(enabled);

      return yoFramePointEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code YoFrameQuaternion} associated with the given end-effector and
    * {@code type}, if it does not exist it is created.
    * <p>
    * The name prefix of the created variable is created as follows:<br>
    * {@code namePrefix = endEffector.getName() + type.getName() +}
    * {@link Space#ORIENTATION}{@code .getName()}<br>
    * Such that the current orientation for the rigid-body 'rightHand' will have the prefix:
    * "rightHandCurrentOrientation".
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoFrameQuaternion} matching the search criteria.
    */
   public YoFrameQuaternion getOrientation(RigidBody endEffector, Type type, BooleanYoVariable enabled)
   {
      EnumMap<Type, Pair<YoFrameQuaternion, List<BooleanYoVariable>>> typeDependentOrientations = endEffectorOrientations.get(endEffector);

      if (typeDependentOrientations == null)
      {
         typeDependentOrientations = new EnumMap<>(Type.class);
         endEffectorOrientations.put(endEffector, typeDependentOrientations);
      }

      Pair<YoFrameQuaternion, List<BooleanYoVariable>> yoFrameQuaternionEnabledPair = typeDependentOrientations.get(type);

      if (yoFrameQuaternionEnabledPair == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += Space.ORIENTATION.getName();
         YoFrameQuaternion yoFrameQuaternion = new YoFrameQuaternion(namePrefix, worldFrame, registry);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         yoFrameQuaternionEnabledPair = new ImmutablePair<>(yoFrameQuaternion, endabledList);
         typeDependentOrientations.put(type, yoFrameQuaternionEnabledPair);
         clearableData.add(yoFrameQuaternionEnabledPair);
      }

      yoFrameQuaternionEnabledPair.getRight().add(enabled);

      return yoFrameQuaternionEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code YoFrameVector} associated with the given end-effector,
    * {@code type}, and {@code space}, if it does not exist it is created.
    * <p>
    * The name prefix of the created variable is created as follows:<br>
    * {@code namePrefix = endEffector.getName() + type.getName() + space.getName()}<br>
    * Such that the desired linear velocity for the rigid-body 'rightHand' will have the prefix:
    * "rightHandDesiredLinearVelocity".
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @param space the space of the data to retrieve.
    * @return the unique {@code YoFrameVector} matching the search criteria.
    */
   public YoFrameVector getDataVector(RigidBody endEffector, Type type, Space space, BooleanYoVariable enabled)
   {
      EnumMap<Type, EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>>> dataVectorStep1 = endEffectorDataVectors.get(endEffector);

      if (dataVectorStep1 == null)
      {
         dataVectorStep1 = new EnumMap<>(Type.class);
         endEffectorDataVectors.put(endEffector, dataVectorStep1);
      }

      EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>> dataVectorStep2 = dataVectorStep1.get(type);

      if (dataVectorStep2 == null)
      {
         dataVectorStep2 = new EnumMap<>(Space.class);
         dataVectorStep1.put(type, dataVectorStep2);
      }

      Pair<YoFrameVector, List<BooleanYoVariable>> yoFrameVectorEnabledPair = dataVectorStep2.get(space);

      if (yoFrameVectorEnabledPair == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += type.getName();
         namePrefix += space.getName();
         YoFrameVector yoFrameVector = new YoFrameVector(namePrefix, worldFrame, registry);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         yoFrameVectorEnabledPair = new ImmutablePair<>(yoFrameVector, endabledList);
         dataVectorStep2.put(space, yoFrameVectorEnabledPair);
         clearableData.add(yoFrameVectorEnabledPair);
      }

      yoFrameVectorEnabledPair.getRight().add(enabled);

      return yoFrameVectorEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code RateLimitedYoFrameVector} associated with the given
    * end-effector, {@code type}, and {@code space}, if it does not exist it is created.
    * <p>
    * Note: the arguments {@code dt} and {@code maximumRate} are only used if the data does not
    * exist yet.
    * </p>
    * <p>
    * The name prefix of the created variable is created as follows:<br>
    * {@code namePrefix = endEffector.getName() + "RateLimited" + rawDataType.getName() + space.getName()}<br>
    * Such that the rate-limited vector of the desired linear acceleration for the rigid-body
    * 'rightHand' will have the prefix: "rightHandRateLimitedDesiredLinearAcceleration".
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param space the space of the data to retrieve.
    * @param rawDataType the type of the raw vector onto which the rate limit is to be applied.
    * @param dt the duration of a control tick.
    * @param maximumRate the maximum rate allowed rate. Not modified.
    * @return the unique {@code RateLimitedYoFrameVector} matching the search criteria.
    */
   public RateLimitedYoFrameVector getRateLimitedDataVector(RigidBody endEffector, Type rawDataType, Space space, double dt, DoubleYoVariable maximumRate,
                                                            BooleanYoVariable enabled)
   {
      EnumMap<Space, Pair<RateLimitedYoFrameVector, List<BooleanYoVariable>>> endEffectorDataVectors = endEffectorRateLimitedDataVectors.get(endEffector);

      if (endEffectorDataVectors == null)
      {
         endEffectorDataVectors = new EnumMap<>(Space.class);
         endEffectorRateLimitedDataVectors.put(endEffector, endEffectorDataVectors);
      }

      Pair<RateLimitedYoFrameVector, List<BooleanYoVariable>> rateLimitedYoFrameVectorEnabledPair = endEffectorDataVectors.get(space);

      if (rateLimitedYoFrameVectorEnabledPair == null)
      {
         String namePrefix = endEffector.getName();
         namePrefix += "RateLimited";
         namePrefix += rawDataType.getName();
         namePrefix += space.getName();
         YoFrameVector rawYoFrameVector = getDataVector(endEffector, rawDataType, space, enabled);
         RateLimitedYoFrameVector rateLimitedYoFrameVector = new RateLimitedYoFrameVector(namePrefix, "", registry, maximumRate, dt, rawYoFrameVector);
         List<BooleanYoVariable> endabledList = new ArrayList<>();
         rateLimitedYoFrameVectorEnabledPair = new ImmutablePair<>(rateLimitedYoFrameVector, endabledList);
         endEffectorDataVectors.put(space, rateLimitedYoFrameVectorEnabledPair);
         clearableData.add(rateLimitedYoFrameVectorEnabledPair);
      }

      rateLimitedYoFrameVectorEnabledPair.getRight().add(enabled);

      return rateLimitedYoFrameVectorEnabledPair.getLeft();
   }

   /**
    * Retrieves and returns the {@code YoFramePoseUsingQuaternions} associated with the given
    * end-effector and {@code type}, if it does not exist it is created.
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoFramePoseUsingQuaternions} matching the search criteria.
    */
   public YoFramePoseUsingQuaternions getPose(RigidBody endEffector, Type type, BooleanYoVariable enabled)
   {
      return new YoFramePoseUsingQuaternions(getPosition(endEffector, type, enabled), getOrientation(endEffector, type, enabled));
   }

   /**
    * Retrieves and returns the {@code YoSpatialVector} for holding the angular and linear
    * velocities of the given end-effector for representing a given data {@code type}. If it does
    * not exist it is created.
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoSpatialVector} matching the search criteria.
    */
   public YoSpatialVector getVelocity(RigidBody endEffector, Type type, BooleanYoVariable enabled)
   {
      return new YoSpatialVector(getDataVector(endEffector, type, Space.LINEAR_VELOCITY, enabled),
                                 getDataVector(endEffector, type, Space.ANGULAR_VELOCITY, enabled));
   }

   /**
    * Retrieves and returns the {@code YoSpatialVector} for holding the angular and linear
    * accelerations of the given end-effector for representing a given data {@code type}. If it does
    * not exist it is created.
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param type the type of the data to retrieve.
    * @return the unique {@code YoSpatialVector} matching the search criteria.
    */
   public YoSpatialVector getAcceleration(RigidBody endEffector, Type type, BooleanYoVariable enabled)
   {
      return new YoSpatialVector(getDataVector(endEffector, type, Space.LINEAR_ACCELERATION, enabled),
                                 getDataVector(endEffector, type, Space.ANGULAR_ACCELERATION, enabled));
   }

   /**
    * Retrieves and returns the {@code RateLimitedYoSpatialVector} for the rate-limited angular and
    * linear velocities of the given end-effector. The date type of the vector is defined by
    * {@code type}. If it does not exist it is created.
    * <p>
    * Note: the arguments {@code dt}, {@code maximumLinearRate}, and {@code maximumAngularRate} are
    * only used if the data does not exist yet.
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param rawDataType the type of the raw vector onto which the rate limit is to be applied.
    * @param dt the duration of a control tick.
    * @param maximumRate the maximum rate allowed rate. Not modified.
    * @return the unique {@code RateLimitedYoSpatialVector} matching the search criteria.
    */
   public RateLimitedYoSpatialVector getRateLimitedVelocity(RigidBody endEffector, Type rawDataType, double dt, DoubleYoVariable maximumLinearRate,
                                                            DoubleYoVariable maximumAngularRate, BooleanYoVariable enabled)
   {
      return new RateLimitedYoSpatialVector(getRateLimitedDataVector(endEffector, rawDataType, Space.LINEAR_VELOCITY, dt, maximumLinearRate, enabled),
                                            getRateLimitedDataVector(endEffector, rawDataType, Space.ANGULAR_VELOCITY, dt, maximumAngularRate, enabled));
   }

   /**
    * Retrieves and returns the {@code RateLimitedYoSpatialVector} for the rate-limited angular and
    * linear accelerations of the given end-effector. The date type of the vector is defined by
    * {@code type}. If it does not exist it is created.
    * <p>
    * Note: the arguments {@code dt}, {@code maximumLinearRate}, and {@code maximumAngularRate} are
    * only used if the data does not exist yet.
    * </p>
    * 
    * @param endEffector the end-effector to which the returned data is associated.
    * @param rawDataType the type of the raw vector onto which the rate limit is to be applied.
    * @param dt the duration of a control tick.
    * @param maximumRate the maximum rate allowed rate. Not modified.
    * @return the unique {@code RateLimitedYoSpatialVector} matching the search criteria.
    */
   public RateLimitedYoSpatialVector getRateLimitedAcceleration(RigidBody endEffector, Type rawDataType, double dt, DoubleYoVariable maximumLinearRate,
                                                                DoubleYoVariable maximumAngularRate, BooleanYoVariable enabled)
   {
      return new RateLimitedYoSpatialVector(getRateLimitedDataVector(endEffector, rawDataType, Space.LINEAR_ACCELERATION, dt, maximumLinearRate, enabled),
                                            getRateLimitedDataVector(endEffector, rawDataType, Space.ANGULAR_ACCELERATION, dt, maximumAngularRate, enabled));
   }

   /**
    * Retrieves and returns the set of gains {@code YoOrientationPIDGainsInterface} associated to
    * the given end-effector, if it does not exist it is created.
    * 
    * @param endEffector the end-effector to which the gains are associated.
    * @return the unique {@code YoOrientationPIDGainsInterface} associated with the given
    *         end-effector.
    */
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

   /**
    * Retrieves and returns the set of gains {@code YoPositionPIDGainsInterface} associated to the
    * given end-effector, if it does not exist it is created.
    * 
    * @param endEffector the end-effector to which the gains are associated.
    * @return the unique {@code YoPositionPIDGainsInterface} associated with the given end-effector.
    */
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

   /**
    * Retrieves and returns the set of gains {@code YoSE3PIDGainsInterface} associated to the given
    * end-effector, if it does not exist it is created.
    * 
    * @param endEffector the end-effector to which the gains are associated.
    * @return the unique {@code YoSE3PIDGainsInterface} associated with the given end-effector.
    */
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

   /**
    * Retrieves and returns the control frame {@code YoSE3OffsetFrame} associated to the given
    * end-effector, if it does not exist it is created.
    * 
    * @param endEffector the end-effector to which the control frame is associated.
    * @return the unique {@code YoSE3OffsetFrame} control frame associated with the given
    *         end-effector.
    */
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
   public void clearUnusedData()
   {
      for (int i = 0; i < clearableData.size(); i++)
      {
         Pair<? extends Clearable, List<BooleanYoVariable>> pair = clearableData.get(i);
         if (!hasData(pair.getRight()))
            pair.getLeft().setToNaN();
      }
   }

   @Override
   public boolean getCenterOfMassPositionData(FramePoint positionDataToPack, Type type)
   {
      Pair<YoFramePoint, List<BooleanYoVariable>> positionData = centerOfMassPositions.get(type);

      if (positionData == null || !hasData(positionData.getRight()))
         return false;

      positionData.getLeft().getFrameTupleIncludingFrame(positionDataToPack);
      return true;
   }

   @Override
   public boolean getCenterOfMassVectorData(FrameVector vectorDataToPack, Type type, Space space)
   {
      EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>> endEffectorDataTyped = centerOfMassDataVectors.get(type);

      if (endEffectorDataTyped == null)
         return false;

      Pair<YoFrameVector, List<BooleanYoVariable>> vectorData = endEffectorDataTyped.get(space);

      if (vectorData == null || !hasData(vectorData.getRight()))
         return false;

      vectorData.getLeft().getFrameTupleIncludingFrame(vectorDataToPack);
      return true;
   }

   @Override
   public boolean getPositionData(RigidBody endEffector, FramePoint positionDataToPack, Type type)
   {
      EnumMap<Type, Pair<YoFramePoint, List<BooleanYoVariable>>> endEffectorData = endEffectorPositions.get(endEffector);

      if (endEffectorData == null)
         return false;

      Pair<YoFramePoint, List<BooleanYoVariable>> positionData = endEffectorData.get(type);

      if (positionData == null || !hasData(positionData.getRight()))
         return false;

      positionData.getLeft().getFrameTupleIncludingFrame(positionDataToPack);
      return true;
   }

   @Override
   public boolean getOrientationData(RigidBody endEffector, FrameOrientation orientationDataToPack, Type type)
   {
      EnumMap<Type, Pair<YoFrameQuaternion, List<BooleanYoVariable>>> endEffectorData = endEffectorOrientations.get(endEffector);

      if (endEffectorData == null)
         return false;

      Pair<YoFrameQuaternion, List<BooleanYoVariable>> orientationData = endEffectorData.get(type);

      if (orientationData == null || !hasData(orientationData.getRight()))
         return false;

      orientationData.getLeft().getFrameOrientationIncludingFrame(orientationDataToPack);
      return true;
   }

   @Override
   public boolean getVectorData(RigidBody endEffector, FrameVector vectorDataToPack, Type type, Space space)
   {
      EnumMap<Type, EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>>> endEffectorData = endEffectorDataVectors.get(endEffector);

      if (endEffectorData == null)
         return false;

      EnumMap<Space, Pair<YoFrameVector, List<BooleanYoVariable>>> endEffectorDataTyped = endEffectorData.get(type);

      if (endEffectorDataTyped == null)
         return false;

      Pair<YoFrameVector, List<BooleanYoVariable>> vectorData = endEffectorDataTyped.get(space);

      if (vectorData == null || !hasData(vectorData.getRight()))
         return false;

      vectorData.getLeft().getFrameTupleIncludingFrame(vectorDataToPack);
      return true;
   }

   private static boolean hasData(List<BooleanYoVariable> enabledList)
   {
      for (int i = 0; i < enabledList.size(); i++)
      {
         if (enabledList.get(i).getBooleanValue())
            return true;
      }
      return false;
   }
}
