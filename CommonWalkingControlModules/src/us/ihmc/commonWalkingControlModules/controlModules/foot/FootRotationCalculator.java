package us.ihmc.commonWalkingControlModules.controlModules.foot;

import java.awt.Color;

import us.ihmc.plotting.Artifact;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.plotting.YoArtifactLineSegment2d;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFramePoint2d;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFrameVector2d;
import us.ihmc.yoUtilities.math.filters.FilteredVelocityYoFrameVector2d;
import us.ihmc.yoUtilities.math.filters.FilteredVelocityYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameLineSegment2d;

/**
 * The FootRotationCalculator is a tool to detect if the foot is rotating around a steady line of rotation.
 * It is used in the PartialFootholdControlModule to determine if the current foothold is only partial and if the foot support polygon should be shrunk.
 * (In this class: LoR = Line of Rotation & CoR = Center of Rotation)
 * @author Sylvain
 *
 */
public class FootRotationCalculator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name = getClass().getSimpleName();

   private final String generalDescription = "LoR = Line of Rotation & CoR = Center of Rotation";

   private final YoVariableRegistry registry;

   /** Alpha filter to filter the foot angular velocity. */
   private final DoubleYoVariable yoAngularVelocityAlphaFilter;
   /** Foot filtered angular velocity in the sole frame. The yaw rate is intentionally ignored. */
   private final AlphaFilteredYoFrameVector2d yoFootAngularVelocityFiltered;
   /** Foot angular velocity around the estimated line of rotation. */
   private final DoubleYoVariable yoAngularVelocityAroundLoR;
   /** Point located on the line of rotation. The measured center of pressure is used to compute it. */

   /** Alpha filter to filter the foot angular velocity. */
   private final DoubleYoVariable yoCoRPositionAlphaFilter;
   private final AlphaFilteredYoFramePoint2d yoCoRPositionFiltered;
   /** Alpha filter to filter the linear velocity of the center of rotation. */
   private final DoubleYoVariable yoCoRVelocityAlphaFilter;
   /** Filtered data of the center of rotation linear velocity. */
   private final FilteredVelocityYoFrameVector2d yoCoRVelocityFiltered;
   /** Linear velocity of the center of rotation that is transversal (perpendicular) to the line of rotation. */
   private final DoubleYoVariable yoCoRTransversalVelocity;
   /** Estimated line of rotation of the foot. It is actually here a line segment that remains contained in the foot. */
   private final YoFrameLineSegment2d yoLineOfRotation;
   /** Absolute angle of the line of rotation. */
   private final DoubleYoVariable yoAngleOfLoR;
   /** Alpha filter used to filter the yaw rate of the line of rotation. */
   private final DoubleYoVariable yoLoRAngularVelocityAlphaFilter;
   /** Filtered yaw rate of the line of rotation. */
   private final FilteredVelocityYoVariable yoLoRAngularVelocityFiltered;

   private final DoubleYoVariable yoCoPErrorAlphaFilter;
   private final AlphaFilteredYoFrameVector2d yoCoPErrorFiltered;
   private final DoubleYoVariable yoCoPErrorPerpendicularToRotation;

   /** Threshold on the yaw rate of the line of rotation to determine whether or not the line of rotation is stable. */
   private final DoubleYoVariable yoStableLoRAngularVelocityThreshold;
   private final BooleanYoVariable yoIsLoRStable;

   /** Threshold on the transversal velocity of the CoR w.r.t. the LoR to determine whether or not the CoR is stable. */
   private final DoubleYoVariable yoStableCoRLinearVelocityThreshold;
   private final BooleanYoVariable yoIsCoRStable;

   /** Threshold on the foot angular velocity around the line of rotation. */
   private final DoubleYoVariable yoAngularVelocityAroundLoRThreshold;
   private final BooleanYoVariable yoIsAngularVelocityAroundLoRPastThreshold;

   /** Main output of this class that informs on wether or not the foot is rotating. */
   private final BooleanYoVariable yoIsFootRotating;

   private final BooleanYoVariable hasBeenInitialized;

   private final FrameVector angularVelocity = new FrameVector();
   private final FrameVector2d angularVelocity2d = new FrameVector2d();
   private final FrameVector2d copError2d = new FrameVector2d();

   private final FramePoint2d centerOfRotation = new FramePoint2d();
   private final FrameLine2d lineOfRotationInSoleFrame = new FrameLine2d();
   private final FrameLine2d lineOfRotationInWorldFrame = new FrameLine2d();
   private final FrameLineSegment2d lineSegmentOfRotation = new FrameLineSegment2d();

   private final ContactablePlaneBody rotatingBody;
   private final TwistCalculator twistCalculator;

   private final ReferenceFrame soleFrame;

   private final Twist bodyTwist = new Twist();
   private final FrameConvexPolygon2d footPolygonInSoleFrame = new FrameConvexPolygon2d();
   private final FrameConvexPolygon2d footPolygonInWorldFrame = new FrameConvexPolygon2d();

   public FootRotationCalculator(String namePrefix, double dt, ContactablePlaneBody rotatingFoot, TwistCalculator twistCalculator,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.twistCalculator = twistCalculator;
      this.rotatingBody = rotatingFoot;
      this.soleFrame = rotatingFoot.getSoleFrame();

      footPolygonInSoleFrame.setIncludingFrameAndUpdate(rotatingFoot.getContactPoints2d());

      registry = new YoVariableRegistry(namePrefix + name);
      parentRegistry.addChild(registry);

      yoAngularVelocityAlphaFilter = new DoubleYoVariable(namePrefix + name + "AngularVelocityAlphaFilter", generalDescription, registry);
      yoFootAngularVelocityFiltered = AlphaFilteredYoFrameVector2d.createAlphaFilteredYoFrameVector2d(namePrefix + "AngularVelocityFiltered", "",
            generalDescription, registry, yoAngularVelocityAlphaFilter, soleFrame);

      yoCoRPositionAlphaFilter = new DoubleYoVariable(namePrefix + "CoRPositionAlphaFilter", registry);
      yoCoRPositionFiltered = AlphaFilteredYoFramePoint2d.createAlphaFilteredYoFramePoint2d(namePrefix + "CoRFiltered", "", generalDescription, registry,
            yoCoRPositionAlphaFilter, soleFrame);
      yoCoRVelocityAlphaFilter = new DoubleYoVariable(namePrefix + "CoRVelocityAlphaFilter", generalDescription, registry);
      yoCoRTransversalVelocity = new DoubleYoVariable(namePrefix + "CoRTransversalVelocity", generalDescription, registry);
      yoCoRVelocityFiltered = FilteredVelocityYoFrameVector2d.createFilteredVelocityYoFrameVector2d(namePrefix + "CoRVelocity", "", generalDescription,
            yoCoRVelocityAlphaFilter, dt, registry, yoCoRPositionFiltered);

      yoLineOfRotation = new YoFrameLineSegment2d(namePrefix + "LoRPosition", "", generalDescription, worldFrame, registry);
      yoAngleOfLoR = new DoubleYoVariable(namePrefix + "AngleOfLoR", generalDescription, registry);
      yoLoRAngularVelocityAlphaFilter = new DoubleYoVariable(namePrefix + "LoRAngularVelocityAlphaFilter", generalDescription, registry);
      yoLoRAngularVelocityFiltered = new FilteredVelocityYoVariable(namePrefix + "LoRAngularVelocityFiltered", generalDescription,
            yoLoRAngularVelocityAlphaFilter, yoAngleOfLoR, dt, registry);

      yoAngularVelocityAroundLoR = new DoubleYoVariable(namePrefix + "AngularVelocityAroundLoR", generalDescription, registry);

      yoCoPErrorAlphaFilter = new DoubleYoVariable(namePrefix + "CoPErrorAlphaFilter", registry);
      yoCoPErrorFiltered = AlphaFilteredYoFrameVector2d.createAlphaFilteredYoFrameVector2d(namePrefix + "CoPErrorFilt", "", registry, yoCoPErrorAlphaFilter,
            soleFrame);
      yoCoPErrorPerpendicularToRotation = new DoubleYoVariable(namePrefix + "CoPErrorPerpendicularToRotation", registry);

      yoStableLoRAngularVelocityThreshold = new DoubleYoVariable(namePrefix + "LoRStableAngularVelocityThreshold", generalDescription, registry);
      yoStableLoRAngularVelocityThreshold.set(2.0);
      yoIsLoRStable = new BooleanYoVariable(namePrefix + "IsLoRStable", generalDescription, registry);

      yoStableCoRLinearVelocityThreshold = new DoubleYoVariable(namePrefix + "CoRStableLinearVelocityThreshold", generalDescription, registry);
      yoStableCoRLinearVelocityThreshold.set(0.01);
      yoIsCoRStable = new BooleanYoVariable(namePrefix + "IsCoRStable", generalDescription, registry);

      yoAngularVelocityAroundLoRThreshold = new DoubleYoVariable(namePrefix + "AngularVelocityAroundLoRThreshold", generalDescription, registry);
      yoAngularVelocityAroundLoRThreshold.set(0.5);
      yoIsAngularVelocityAroundLoRPastThreshold = new BooleanYoVariable(namePrefix + "IsAngularVelocityAroundLoRPastThreshold", generalDescription, registry);

      yoIsFootRotating = new BooleanYoVariable(namePrefix + "IsFootRotating", generalDescription, registry);

      hasBeenInitialized = new BooleanYoVariable(namePrefix + "HasBeenInitialized", registry);

      angularVelocity2d.setToZero(soleFrame);
      lineOfRotationInSoleFrame.setIncludingFrame(soleFrame, 0.0, 0.0, 1.0, 0.0);

      if (yoGraphicsListRegistry != null)
      {
         Artifact lineOfRotationArtifact = new YoArtifactLineSegment2d(namePrefix + "LineOfRotation", yoLineOfRotation, Color.ORANGE, 0.005, 0.01);
         yoGraphicsListRegistry.registerArtifact("FootRotation", lineOfRotationArtifact);
      }
   }

   public void compute(FramePoint2d desiredCoP, FramePoint2d cop)
   {
      footPolygonInWorldFrame.setIncludingFrameAndUpdate(footPolygonInSoleFrame);
      footPolygonInWorldFrame.changeFrameAndProjectToXYPlane(worldFrame);

      twistCalculator.packTwistOfBody(bodyTwist, rotatingBody.getRigidBody());
      bodyTwist.packAngularPart(angularVelocity);

      angularVelocity.changeFrame(soleFrame);
      angularVelocity.setZ(0.0);
      angularVelocity2d.setIncludingFrame(soleFrame, angularVelocity.getX(), angularVelocity.getY());

      yoFootAngularVelocityFiltered.update(angularVelocity2d);
      yoFootAngularVelocityFiltered.getFrameTuple2dIncludingFrame(angularVelocity2d);

      yoAngleOfLoR.set(Math.atan2(angularVelocity2d.getY(), angularVelocity2d.getX()));
      yoLoRAngularVelocityFiltered.updateForAngles();

      copError2d.setToZero(soleFrame);
      copError2d.sub(desiredCoP, cop);

      yoCoPErrorFiltered.update(copError2d);
      yoCoPErrorPerpendicularToRotation.set(yoCoPErrorFiltered.cross(yoFootAngularVelocityFiltered));

      yoCoRPositionFiltered.update(cop);
      yoCoRPositionFiltered.getFrameTuple2dIncludingFrame(centerOfRotation);
      yoCoRVelocityFiltered.update();
      yoCoRTransversalVelocity.set(yoCoRVelocityFiltered.cross(yoFootAngularVelocityFiltered));

      if (!hasBeenInitialized.getBooleanValue())
      {
         hasBeenInitialized.set(true);
         return;
      }

      yoIsLoRStable.set(Math.abs(yoLoRAngularVelocityFiltered.getDoubleValue()) < yoStableLoRAngularVelocityThreshold.getDoubleValue());

      yoIsCoRStable.set(Math.abs(yoCoRTransversalVelocity.getDoubleValue()) < yoStableCoRLinearVelocityThreshold.getDoubleValue());

      yoAngularVelocityAroundLoR.set(yoFootAngularVelocityFiltered.length());
      yoIsAngularVelocityAroundLoRPastThreshold.set(yoAngularVelocityAroundLoR.getDoubleValue() > yoAngularVelocityAroundLoRThreshold.getDoubleValue());

      yoIsFootRotating.set(yoIsLoRStable.getBooleanValue() && yoIsCoRStable.getBooleanValue() && yoIsAngularVelocityAroundLoRPastThreshold.getBooleanValue());

      if (yoIsFootRotating.getBooleanValue())
      {
         lineOfRotationInSoleFrame.set(centerOfRotation, angularVelocity2d);
         lineOfRotationInWorldFrame.setIncludingFrame(lineOfRotationInSoleFrame);
         lineOfRotationInWorldFrame.changeFrameAndProjectToXYPlane(worldFrame);
         FramePoint2d[] intersections = footPolygonInWorldFrame.intersectionWith(lineOfRotationInWorldFrame);

         if (intersections == null || intersections.length == 1)
         {
            yoLineOfRotation.setToNaN();
         }
         else
         {
            lineSegmentOfRotation.setIncludingFrame(intersections);
            yoLineOfRotation.setFrameLineSegment2d(lineSegmentOfRotation);
         }
      }
      else
      {
         yoLineOfRotation.setToNaN();
      }
   }

   public boolean isFootRotating()
   {
      return yoIsFootRotating.getBooleanValue();
   }

   public void getLineOfRotation(FrameLine2d lineOfRotationToPack)
   {
      lineOfRotationToPack.setIncludingFrame(lineOfRotationInSoleFrame);
   }

   public void reset()
   {
      yoLineOfRotation.setToNaN();
      yoFootAngularVelocityFiltered.reset();
      yoFootAngularVelocityFiltered.setToNaN();
      yoCoRPositionFiltered.reset();
      yoCoRPositionFiltered.setToNaN();

      yoCoRVelocityFiltered.reset();
      yoCoRVelocityFiltered.setToNaN();

      yoAngleOfLoR.set(0.0);
      yoLoRAngularVelocityFiltered.set(Double.NaN);
      yoLoRAngularVelocityFiltered.reset();

      yoAngularVelocityAroundLoR.set(Double.NaN);
      yoIsLoRStable.set(false);
      yoIsCoRStable.set(false);
      yoIsFootRotating.set(false);

      hasBeenInitialized.set(false);
   }

   public void setAlphaFilter(double alpha)
   {
      yoAngularVelocityAlphaFilter.set(alpha);
   }

   public void setFootAngularVelocityThreshold(double threshold)
   {
      yoAngularVelocityAroundLoRThreshold.set(threshold);
   }

   public void setStableAngularVelocityThreshold(double threshold)
   {
      yoStableLoRAngularVelocityThreshold.set(threshold);
   }
}
