package us.ihmc.commonWalkingControlModules.trajectories;

import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.DoubleProvider;
import us.ihmc.utilities.math.trajectories.providers.PositionProvider;
import us.ihmc.utilities.math.trajectories.providers.VectorProvider;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.BagOfBalls;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;
import us.ihmc.yoUtilities.math.trajectories.PositionTrajectoryGenerator;
import us.ihmc.yoUtilities.math.trajectories.YoPolynomial;

/** 
 * 
 *
 * This trajectory unless the robot is pushed, this class will behave exactly like the TwoWaypointTrajectoyrGenerator except this class has 
 * the soft TouchdownTrajectoryGenerator included rather than the two being combined in an ArrayList of position trajectory generators. When 
 * the robot is pushed, the XY portion of the trajectory are replanned so the robot can recover from the push.
 */
public class PushRecoveryTrajectoryGenerator implements PositionTrajectoryGenerator
{
   private static final boolean VISUALIZE = true;

   private final String namePostFix = getClass().getSimpleName();
   private final YoVariableRegistry registry;
   private final BooleanYoVariable visualize;

   private final int numberOfBallsInBag = 30;
   private final BagOfBalls bagOfBalls;
   private double t0ForViz;
   private double tfForViz;
   private double tForViz;

   private final DoubleProvider swingTimeRemainingProvider;
   private final PositionProvider[] positionSources = new PositionProvider[2];
   private final VectorProvider[] velocitySources = new VectorProvider[2];

   private final DoubleYoVariable swingTime;
   private final DoubleYoVariable timeIntoStep;

   private final YoFramePoint desiredPosition;
   private final YoFrameVector desiredVelocity;
   private final YoFrameVector desiredAcceleration;

   private final YoPolynomial xPolynomial, yPolynomial;
   private final PositionTrajectoryGenerator nominalTrajectoryGenerator;
   private final DoubleProvider swingTimeProvider;

   private FramePoint nominalTrajectoryPosition = new FramePoint();
   private FrameVector nominalTrajectoryVelocity = new FrameVector();
   private FrameVector nominalTrajectoryAcceleration = new FrameVector();

   public PushRecoveryTrajectoryGenerator(String namePrefix, ReferenceFrame referenceFrame, DoubleProvider swingTimeProvider,
         DoubleProvider swingTimeRemainingProvider, PositionProvider initialPositionProvider, VectorProvider initialVelocityProvider,
         PositionProvider finalPositionProvider, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry,
         PositionTrajectoryGenerator nominalTrajectoryGenerator)
   {
      registry = new YoVariableRegistry(namePrefix + namePostFix);
      parentRegistry.addChild(registry);

      this.swingTimeRemainingProvider = swingTimeRemainingProvider;
      this.swingTimeProvider = swingTimeProvider;

      positionSources[0] = initialPositionProvider;
      positionSources[1] = finalPositionProvider;

      velocitySources[0] = initialVelocityProvider;

      xPolynomial = new YoPolynomial(namePrefix + "PolynomialX", 5, registry);
      yPolynomial = new YoPolynomial(namePrefix + "PolynomialY", 5, registry);

      swingTime = new DoubleYoVariable(namePrefix + "SwingTime", registry);
      swingTime.set(swingTimeProvider.getValue());

      timeIntoStep = new DoubleYoVariable(namePrefix + "TimeIntoStep", registry);

      desiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", referenceFrame, registry);
      desiredVelocity = new YoFrameVector(namePrefix + "DesiredVelocity", referenceFrame, registry);
      desiredAcceleration = new YoFrameVector(namePrefix + "DesiredAcceleration", referenceFrame, registry);

      this.visualize = new BooleanYoVariable(namePrefix + "Visualize", registry);
      this.visualize.set(VISUALIZE);

      this.nominalTrajectoryGenerator = nominalTrajectoryGenerator;

      this.bagOfBalls = new BagOfBalls(numberOfBallsInBag, 0.01, namePrefix + "SwingTrajectoryBagOfBalls", registry, yoGraphicsListRegistry);
   }

   private final FrameVector tempVector = new FrameVector();
   private final FramePoint tempPosition = new FramePoint();

   public void initialize()
   {
      swingTime.set(swingTimeProvider.getValue());
      timeIntoStep.set(swingTime.getDoubleValue() - swingTimeRemainingProvider.getValue());

      
      positionSources[0].get(tempPosition);
      tempPosition.changeFrame(desiredPosition.getReferenceFrame());
      double x0 = tempPosition.getX();
      double y0 = tempPosition.getY();
      
      velocitySources[0].get(tempVector);
      tempVector.changeFrame(desiredPosition.getReferenceFrame());
      double xd0 = tempVector.getX();
      double yd0 = tempVector.getY();
      
      positionSources[1].get(tempPosition);
      tempPosition.changeFrame(desiredPosition.getReferenceFrame());
      double xFinal = tempPosition.getX();
      double yFinal = tempPosition.getY();
      
      xPolynomial.setQuarticUsingFinalAcceleration(timeIntoStep.getDoubleValue(), swingTime.getDoubleValue(), x0, xd0, xFinal, 0.0, 0.0);
      yPolynomial.setQuarticUsingFinalAcceleration(timeIntoStep.getDoubleValue(), swingTime.getDoubleValue(), y0, yd0, yFinal, 0.0, 0.0);

      if (VISUALIZE)
      {
         visualizeTrajectory();
      }
   }

   public void compute(double time)
   {
      timeIntoStep.set(time);

      nominalTrajectoryGenerator.compute(time);

      nominalTrajectoryGenerator.packLinearData(nominalTrajectoryPosition, nominalTrajectoryVelocity, nominalTrajectoryAcceleration);

      xPolynomial.compute(time);
      yPolynomial.compute(time);

      desiredPosition.setX(xPolynomial.getPosition());
      desiredPosition.setY(yPolynomial.getPosition());
      desiredPosition.setZ(nominalTrajectoryPosition.getZ());

      desiredVelocity.setX(xPolynomial.getVelocity());
      desiredVelocity.setY(yPolynomial.getVelocity());
      desiredVelocity.setZ(nominalTrajectoryVelocity.getZ());

      desiredAcceleration.setX(xPolynomial.getAcceleration());
      desiredAcceleration.setY(yPolynomial.getAcceleration());
      desiredAcceleration.setZ(nominalTrajectoryAcceleration.getZ());
   }

   private void visualizeTrajectory()
   {
      t0ForViz = timeIntoStep.getDoubleValue();
      tfForViz = swingTime.getDoubleValue();

      for (int i = 0; i < numberOfBallsInBag; i++)
      {
         tForViz = t0ForViz + (double) i / (double) (numberOfBallsInBag) * (tfForViz - t0ForViz);
         computePositionsForVis(tForViz);
         bagOfBalls.setBall(desiredPosition.getFramePointCopy(), i);
      }
   }

   public void computePositionsForVis(double time)
   {
      nominalTrajectoryGenerator.compute(time);

      xPolynomial.compute(time);
      yPolynomial.compute(time);

      nominalTrajectoryGenerator.get(nominalTrajectoryPosition);
      nominalTrajectoryGenerator.packVelocity(nominalTrajectoryVelocity);
      nominalTrajectoryGenerator.packAcceleration(nominalTrajectoryAcceleration);

      desiredPosition.setX(xPolynomial.getPosition());
      desiredPosition.setY(yPolynomial.getPosition());
      desiredPosition.setZ(nominalTrajectoryPosition.getZ());
   }

   public void get(FramePoint positionToPack)
   {
      desiredPosition.getFrameTupleIncludingFrame(positionToPack);
   }

   public void packVelocity(FrameVector velocityToPack)
   {
      desiredVelocity.getFrameTupleIncludingFrame(velocityToPack);
   }

   public void packAcceleration(FrameVector accelerationToPack)
   {
      desiredAcceleration.getFrameTupleIncludingFrame(accelerationToPack);
   }

   @Override
   public boolean isDone()
   {
      return timeIntoStep.getDoubleValue() >= swingTime.getDoubleValue();
   }

   public void packLinearData(FramePoint positionToPack, FrameVector velocityToPack, FrameVector accelerationToPack)
   {
      get(positionToPack);
      packVelocity(velocityToPack);
      packAcceleration(accelerationToPack);
   }

   @Override
   public void showVisualization()
   {
      // TODO Auto-generated method stub
   }

   @Override
   public void hideVisualization()
   {
      // TODO Auto-generated method stub
   }
}
