package us.ihmc.commonWalkingControlModules.trajectories;

import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class StraightUpThenParabolicCartesianTrajectoryGeneratorTest
{
   @Test
   public void testMaxHeight()
   {
      Random random = new Random(176L);

      ReferenceFrame referenceFrame = ReferenceFrame.getWorldFrame();

      FramePoint positionToPack = new FramePoint(referenceFrame);
      FrameVector velocityToPack = new FrameVector(referenceFrame);
      FrameVector accelerationToPack = new FrameVector(referenceFrame);

      int n = 1000;
      double epsilon = 1e-3;
      for (int i = 0; i < n; i++)
      {
         double straightUpVelocity = random.nextDouble();
         double parabolicTime = straightUpVelocity + random.nextDouble();
         double groundClearance = random.nextDouble();
         YoVariableRegistry registry = new YoVariableRegistry("test");
         StraightUpThenParabolicCartesianTrajectoryGenerator trajectoryGenerator = new StraightUpThenParabolicCartesianTrajectoryGenerator("test",
                                                                                      referenceFrame, straightUpVelocity, parabolicTime, groundClearance, registry);

         FramePoint initialPosition = new FramePoint(referenceFrame, RandomTools.getRandomVector(random));
         FrameVector initialVelocity = new FrameVector(referenceFrame, RandomTools.getRandomVector(random));
         FramePoint finalDesiredPosition = new FramePoint(initialPosition);
         finalDesiredPosition.add(new FrameVector(referenceFrame, random.nextDouble(), random.nextDouble(), random.nextDouble()));

         trajectoryGenerator.initialize(initialPosition, initialVelocity, null, finalDesiredPosition, null);

         double zMax = finalDesiredPosition.getZ() + groundClearance;
         double minZDifference = Double.POSITIVE_INFINITY;

         double dt = parabolicTime / 2000.0;
         while (!trajectoryGenerator.isDone())
         {
            trajectoryGenerator.computeNextTick(positionToPack, velocityToPack, accelerationToPack, dt);
            double z = positionToPack.getZ();
            if (z > zMax + epsilon)
               fail("z = " + z + ", zMax = " + zMax);

            double zDifference = Math.abs(z - zMax);
            if (zDifference < minZDifference)
               minZDifference = zDifference;
         }

         if (minZDifference > epsilon)
            fail("minZDifference = " + minZDifference);
      }
   }

}
