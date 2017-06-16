package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FrameLine2d;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

/**
 * The purpose of this class is to check if it is probable that the foot is rotating, given a
 * line of rotation, the actual cop, and the desired cop.
 *
 * @author Georg
 *
 */
public class RotationVerificator
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry;

   private final ReferenceFrame soleFrame;
   private final YoFrameVector2d yoCopError;

   /**
    * Check if the error between cop and desired cop perpendicular the line of
    * rotation is above a threshold
    */
   private final YoDouble perpendicularCopError;
   private final YoDouble perpendicluarCopErrorThreshold;
   private final YoBoolean perpendicularCopErrorAboveThreshold;

   /**
    * Check if the angle between cop error vector and the perpendicular error
    * is below a threshold
    */
   private final YoDouble angleBetweenCopErrorAndLine;
   private final YoDouble angleThreshold;
   private final YoBoolean angleOkay;

   /**
    * Check if desired cop is in area that will be cut off
    */
   private final YoBoolean desiredCopOnCorrectSide;

   public RotationVerificator(String namePrefix,
         ContactablePlaneBody foot,
         ExplorationParameters explorationParameters,
         YoVariableRegistry parentRegistry)
   {
      soleFrame = foot.getSoleFrame();

      registry = new YoVariableRegistry(namePrefix + name);
      parentRegistry.addChild(registry);

      yoCopError = new YoFrameVector2d(namePrefix + "CopError", "", soleFrame, registry);

      perpendicularCopError = new YoDouble(namePrefix + "PerpendicularCopError", registry);
      perpendicluarCopErrorThreshold = explorationParameters.getPerpendicluarCopErrorThreshold();
      perpendicularCopErrorAboveThreshold = new YoBoolean(namePrefix + "PerpendicularCopErrorAboveThreshold", registry);

      angleBetweenCopErrorAndLine = new YoDouble(namePrefix + "AngleBetweenCopErrorAndLine", registry);
      angleThreshold = explorationParameters.getCopAllowedAreaOpeningAngle();
      angleOkay = new YoBoolean(namePrefix + "AngleOkay", registry);

      desiredCopOnCorrectSide = new YoBoolean(namePrefix + "DesiredCopOnCorrectSide", registry);
   }

   private final FrameVector2d copError2d = new FrameVector2d();

   public boolean isRotating(FramePoint2d cop,
         FramePoint2d desiredCop,
         FrameLine2d lineOfRotation)
   {
      cop.checkReferenceFrameMatch(soleFrame);
      desiredCop.checkReferenceFrameMatch(soleFrame);
      lineOfRotation.checkReferenceFrameMatch(soleFrame);

      if (!lineOfRotation.isPointOnLine(cop)) return false;

      copError2d.setToZero(soleFrame);
      copError2d.sub(desiredCop, cop);
      yoCopError.set(copError2d);

      perpendicularCopError.set(lineOfRotation.distance(desiredCop));
      boolean errorAboveThreshold = perpendicularCopError.getDoubleValue() >= perpendicluarCopErrorThreshold.getDoubleValue();
      perpendicularCopErrorAboveThreshold.set(errorAboveThreshold);

      double acos = perpendicularCopError.getDoubleValue() / copError2d.length();
      angleBetweenCopErrorAndLine.set(Math.acos(Math.abs(acos)));
      boolean angleInBounds = angleBetweenCopErrorAndLine.getDoubleValue() <= angleThreshold.getDoubleValue();
      angleOkay.set(angleInBounds);

      boolean correctSide = lineOfRotation.isPointOnRightSideOfLine(desiredCop);
      desiredCopOnCorrectSide.set(correctSide);

      return errorAboveThreshold && angleInBounds && correctSide;
   }
}
