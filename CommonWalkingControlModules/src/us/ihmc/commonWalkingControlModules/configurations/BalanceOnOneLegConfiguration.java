package us.ihmc.commonWalkingControlModules.configurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.ArrayTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class BalanceOnOneLegConfiguration
{
   private final double[] yawPitchRoll;
   private final FramePoint desiredCapturePoint;
   private final FramePoint desiredSwingFootPosition;
   private final double kneeBendSupportLeg;

   public BalanceOnOneLegConfiguration(double[] yawPitchRoll, FramePoint desiredCapturePoint, FramePoint desiredSwingFootPosition, double kneeBendSupportLeg)
   {
      this.yawPitchRoll = ArrayTools.copyArray(yawPitchRoll);
      this.desiredCapturePoint = new FramePoint(desiredCapturePoint);
      this.desiredSwingFootPosition = new FramePoint(desiredSwingFootPosition);
      this.kneeBendSupportLeg = kneeBendSupportLeg;
   }

   public double[] getYawPitchRoll()
   {
      return yawPitchRoll;
   }

   public FramePoint getDesiredCapturePoint()
   {
      return desiredCapturePoint;
   }

   public FramePoint getDesiredSwingFootPosition()
   {
      return desiredSwingFootPosition;
   }

   public double getKneeBendSupportLeg()
   {
      return kneeBendSupportLeg;
   }

   public String toString()
   {
      return Arrays.toString(yawPitchRoll) + ", " + desiredCapturePoint + ", " + desiredSwingFootPosition;
   }

   public static ArrayList<BalanceOnOneLegConfiguration> generateABunch(int desiredNumberOfConfigurations, RobotSide supportSide,
           ReferenceFrame supportFootZUpFrame)    // int xyCapturePositions, int yawPitchRollPositions, int swingPositions)
   {
      ArrayList<BalanceOnOneLegConfiguration> ret = new ArrayList<BalanceOnOneLegConfiguration>();

      double captureMinX = 0.02;
      double captureMaxX = 0.06;

      double captureMinY = -0.01;
      double captureMaxY = 0.01;

      double[] yawPitchRollMin = new double[] {-0.1, -0.1, -0.1};
      double[] yawPitchRollMax = new double[] {0.1, 0.1, 0.1};

      double[] swingMin = new double[] {-0.2, 0.2, 0.10};
      double[] swingMax = new double[] {0.2, 0.4, 0.30};

      double kneeBendSupportLegMin = 0.0 * Math.PI / 180;
      double kneeBendSupportLegMax = 60.0 * Math.PI / 180;

      Random random = new Random(101L);

      for (int i = 1; i <= desiredNumberOfConfigurations; i++)
      {
         double captureX = createRandomDoubleInRange(captureMinX, captureMaxX, random);
         double captureY = createRandomDoubleInRange(captureMinY, captureMaxY, random);

         double yaw = createRandomDoubleInRange(yawPitchRollMin[0], yawPitchRollMax[0], random);
         double pitch = createRandomDoubleInRange(yawPitchRollMin[1], yawPitchRollMax[1], random);
         double roll = createRandomDoubleInRange(yawPitchRollMin[2], yawPitchRollMax[2], random);

         double swingX = createRandomDoubleInRange(swingMin[0], swingMax[0], random);
         double swingY = createRandomDoubleInRange(swingMin[1], swingMax[1], random);
         double swingZ = createRandomDoubleInRange(swingMin[2], swingMax[2], random);

         swingY = supportSide.negateIfLeftSide(swingY);

         double kneeBendSupportLeg = createRandomDoubleInRange(kneeBendSupportLegMin, kneeBendSupportLegMax, random);

         double[] yawPitchRoll = new double[] {yaw, pitch, roll};
         FramePoint desiredCapturePoint = new FramePoint(supportFootZUpFrame, captureX, captureY, 0.0);
         FramePoint desiredSwingFootPosition = new FramePoint(supportFootZUpFrame, swingX, swingY, swingZ);

         BalanceOnOneLegConfiguration configuration = new BalanceOnOneLegConfiguration(yawPitchRoll, desiredCapturePoint, desiredSwingFootPosition,
                                                         kneeBendSupportLeg);

         ret.add(configuration);
      }

      return ret;
   }

   private static double createRandomDoubleInRange(Double start, Double end, Random random)
   {
      if (start > end)
      {
         throw new IllegalArgumentException("Start cannot exceed End.");
      }

      Double range = end - start;

      // compute a fraction of the range, 0 <= fraction < range
      Double fraction = (range * random.nextDouble());

      return (fraction + start);
   }

   public static void main(String[] args)
   {
      RobotSide supportSide = RobotSide.LEFT;
      ReferenceFrame supportFootFrame = ReferenceFrame.getWorldFrame();

      ArrayList<BalanceOnOneLegConfiguration> configurations = generateABunch(100000, supportSide, supportFootFrame);

      for (BalanceOnOneLegConfiguration configuration : configurations)
      {
         System.out.println(configuration);
      }

   }
}
