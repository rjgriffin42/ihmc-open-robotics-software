package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;


import java.awt.Color;
import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLineSegment2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class SCSDoubleSupportICPTesterController5 implements RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   int numberOfConsideredFootstepLocations = 4;
   int numberOfStepsInStepList = 5;
   private final ArrayList<YoFramePoint> footStepLocationsFramePoints = new ArrayList<YoFramePoint>();


   private final ArrayList<YoFramePoint> consideredFootStepLocationsFramePoints = new ArrayList<YoFramePoint>();

   private final ArrayList<YoFramePoint> equivalentConstantCoPsFramePoints = new ArrayList<YoFramePoint>();
   private final ArrayList<DenseMatrix64F> equivalentConstantCoPsVectors = new ArrayList<DenseMatrix64F>();

   private final ArrayList<YoFramePoint> initialICPsFramePoints = new ArrayList<YoFramePoint>();
   private final ArrayList<DenseMatrix64F> initialICPsVectors = new ArrayList<DenseMatrix64F>();

   private final YoFramePoint initialDoubleSupportICPposFramePoint = new YoFramePoint("initialDoubleSupportICPposFramePoint", "",
                                                                        ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint finalDoubleSupportICPposFramePoint = new YoFramePoint("finalDoubleSupportICPposFramePoint", "", ReferenceFrame.getWorldFrame(),
                                                                      registry);

   private final YoFramePoint desiredDCMposOfTimeFramePoint = new YoFramePoint("desiredDCMposOfTime", "", ReferenceFrame.getWorldFrame(), registry);
   private final YoFramePoint desiredECMPofTimeFramePoint = new YoFramePoint("desiredECMPofTime", "", ReferenceFrame.getWorldFrame(), registry);


   private ArrayList<YoFrameLineSegment2d> listOfICPLineSegments = new ArrayList<YoFrameLineSegment2d>();

   private final YoFrameLineSegment2d icpVelocityLine = new YoFrameLineSegment2d("icpVelocityLine", "", ReferenceFrame.getWorldFrame(), registry);

   private final YoFrameLineSegment2d comVelocityLine = new YoFrameLineSegment2d("comVelocityLine", "", ReferenceFrame.getWorldFrame(), registry);

   private final YoFrameVector desiredDCMvelOfTimeFrameVector = new YoFrameVector("desiredDCMvelOfTime", "", ReferenceFrame.getWorldFrame(), registry);
   private final DoubleYoVariable desiredDCMvelAbsolute = new DoubleYoVariable("desiredDCMvelAbsolute", registry);
   private final DoubleYoVariable desiredCOMvelAbsolute = new DoubleYoVariable("desiredCOMvelAbsolute", registry);

   private PointAndLinePlotter equivalentCoPAndICPPlotter;

   private final YoFramePoint icpVelocityArrowTip = new YoFramePoint("icpVelocityArrowTip", ReferenceFrame.getWorldFrame(), getYoVariableRegistry());
   private final YoFramePoint comVelocityArrowTip = new YoFramePoint("comVelocityArrowTip", ReferenceFrame.getWorldFrame(), getYoVariableRegistry());

   private final YoFramePoint comPositionFramePoint = new YoFramePoint("comPositionFramePoint", "", ReferenceFrame.getWorldFrame(), registry);
   private final DenseMatrix64F comPositionVector = new DenseMatrix64F(3, 1, true, 0, 0, 0.5);

   private final YoFrameVector comVelocityFrameVector = new YoFrameVector("comVelocityFrameVector", "", ReferenceFrame.getWorldFrame(), registry);
   private final DenseMatrix64F comVelocityVector = new DenseMatrix64F(3, 1, true, 0, 0, 0);

   private final BooleanYoVariable isFirstStep = new BooleanYoVariable("isFirstStep", registry);
   private final BooleanYoVariable isSingleSupport = new BooleanYoVariable("isSingleSupport", registry);
   private final SmoothSupportState supportState = new SmoothSupportState();
   private final DoubleYoVariable moveTime = new DoubleYoVariable("moveTime", registry);
   private final DoubleYoVariable currentTime = new DoubleYoVariable("currentTime", registry);

   private final DoubleYoVariable simDT = new DoubleYoVariable("simDT", registry);

   private final double singleSupportTime;
   private final double doubleSupportTime;
   private final double initialTransferSupportTime;

   private DoubleSupportICPComputer dsICPcomputer = new DoubleSupportICPComputer(registry);

   double leftRightFlip = 1;
   double stepLength = 0.3;
   double halfStepWidth = 0.1;

   public SCSDoubleSupportICPTesterController5(PointAndLinePlotter pointAndLinePlotter, DoubleYoVariable yoTime, double simDText, double singleSupportTimeExt,
           double doubleSupportTimeExt, double initialTransferSupportTimeExt)
   {
      this.equivalentCoPAndICPPlotter = pointAndLinePlotter;

      this.simDT.set(simDText);
      this.singleSupportTime = singleSupportTimeExt;
      this.doubleSupportTime = Math.max(simDText, doubleSupportTimeExt);
      this.initialTransferSupportTime = initialTransferSupportTimeExt;

      YoFramePoint tempstepListElement1 = new YoFramePoint("stepListElement" + 100, "", ReferenceFrame.getWorldFrame(), registry);
      tempstepListElement1.set(0, -leftRightFlip * halfStepWidth, 0.5);
      footStepLocationsFramePoints.add(tempstepListElement1);


      for (int i = 0; i < numberOfStepsInStepList; i++)
      {
         YoFramePoint tempstepListElement = new YoFramePoint("stepListElement" + i, "", ReferenceFrame.getWorldFrame(), registry);
         tempstepListElement.set(i * stepLength, leftRightFlip * halfStepWidth, 0.5);

         footStepLocationsFramePoints.add(tempstepListElement);
         leftRightFlip = -leftRightFlip;
      }


      for (int i = 0; i < numberOfConsideredFootstepLocations; i++)
      {
         consideredFootStepLocationsFramePoints.add(new YoFramePoint("tempFramePoint" + i, "", ReferenceFrame.getWorldFrame(), registry));
      }

      for (int i = 0; i < numberOfConsideredFootstepLocations; i++)
      {
         DenseMatrix64F tempMatrix = new DenseMatrix64F(3, 1, true, 0, 0, 0);
         equivalentConstantCoPsVectors.add(tempMatrix);

         YoFramePoint tempFramePoint = new YoFramePoint("equivalentConstantCoPframepoint" + i, "", ReferenceFrame.getWorldFrame(), registry);
         equivalentConstantCoPsFramePoints.add(tempFramePoint);
      }


      for (int i = 0; i < numberOfConsideredFootstepLocations - 1; i++)
      {
         YoFramePoint tempInitialICPFramePoint = new YoFramePoint("tempInitialICPFramePoint" + i, "", ReferenceFrame.getWorldFrame(), registry);
         initialICPsFramePoints.add(tempInitialICPFramePoint);

         YoFrameLineSegment2d tempYoFrameLine2d = new YoFrameLineSegment2d("line" + i, "", ReferenceFrame.getWorldFrame(), registry);
         listOfICPLineSegments.add(tempYoFrameLine2d);


         DenseMatrix64F tempMatrix = new DenseMatrix64F(3, 1, true, 0, 0, 0);
         initialICPsVectors.add(tempMatrix);

      }

      equivalentCoPAndICPPlotter.plotYoFramePoints("currentFoot", consideredFootStepLocationsFramePoints, YoAppearance.Black(), 0.01);
      equivalentCoPAndICPPlotter.plotYoFramePoints("equivalentConstCoP", equivalentConstantCoPsFramePoints, YoAppearance.Red(), 0.005);
      equivalentCoPAndICPPlotter.plotYoFramePoints("initialICP", initialICPsFramePoints, YoAppearance.Green(), 0.01);
      
      equivalentCoPAndICPPlotter.plotYoFramePoint("initialDoubleSupportICPpos", initialDoubleSupportICPposFramePoint, YoAppearance.Cyan(), 0.01);
      equivalentCoPAndICPPlotter.plotYoFramePoint("finalDoubleSupportICPpos", finalDoubleSupportICPposFramePoint, YoAppearance.Cyan(), 0.01);
      equivalentCoPAndICPPlotter.plotYoFramePoint("desiredDCMposOfTime", desiredDCMposOfTimeFramePoint, YoAppearance.OrangeRed(), 0.01);

      equivalentCoPAndICPPlotter.plotYoFramePoint("comPosition", comPositionFramePoint, YoAppearance.Blue(), 0.015);
      equivalentCoPAndICPPlotter.plotYoFramePoint("desiredECMPofTime", desiredECMPofTimeFramePoint, YoAppearance.Magenta(), 0.011);
      equivalentCoPAndICPPlotter.plotLineSegments("icpLines", listOfICPLineSegments, Color.orange);
      equivalentCoPAndICPPlotter.plotLineSegment("icpVelocityLine", icpVelocityLine, Color.gray);
      equivalentCoPAndICPPlotter.plotLineSegment("comVelocityLine", comVelocityLine, Color.blue);
   }


   public PointAndLinePlotter getPointAndLinePlotter()
   {
      return equivalentCoPAndICPPlotter;
   }

   public void doControl()
   {
      supportState.propagateStateAndStateTime(this.simDT.getDoubleValue());
      moveTime.set(supportState.getMoveTime());
      currentTime.set(supportState.getCurrentTime());
      isSingleSupport.set(supportState.getIsSingleSupport());
      isFirstStep.set(supportState.getIsFirstStep());

//    supportState

      double steppingTime = supportState.getSteppingTime();
      double eCMPheight = 1.0;
      double gravConst = 9.81;
      double doubleSupportFirstStepFraction = 0.5;
      double dcmConst = Math.sqrt(eCMPheight / gravConst);

      if (supportState.getStepListUpdateRequestFlag() == true)
      {
         dsICPcomputer.updateSubFootListForSmoothICPTrajectory(equivalentConstantCoPsVectors, footStepLocationsFramePoints, equivalentConstantCoPsFramePoints,
                 consideredFootStepLocationsFramePoints, numberOfConsideredFootstepLocations, equivalentConstantCoPsVectors);

         supportState.setStepListUpdateRequestFlag(false);
      }


      if (supportState.getIsSingleSupport())
      {
         dsICPcomputer.computeDoubleSupportPolynomialParams(equivalentConstantCoPsVectors, dcmConst, steppingTime, doubleSupportFirstStepFraction,
                 initialICPsVectors, supportState.getIsFirstStep(), supportState.getInitialTransferSupportTime(), supportState.getDoubleSupportTime());
      }


      dsICPcomputer.calcDCMandECMPofTime(equivalentConstantCoPsVectors, doubleSupportFirstStepFraction, dcmConst, initialICPsVectors,
                                         supportState.getIsFirstStep(), supportState.getInitialTransferSupportTime(), supportState.getDoubleSupportTime(),
                                         supportState.getIsSingleSupport(), supportState.getCurrentTime(), supportState.getSteppingTime());




      for (int i = 0; i < numberOfConsideredFootstepLocations; i++)
      {
         equivalentConstantCoPsFramePoints.get(i).set(equivalentConstantCoPsVectors.get(i).get(0), equivalentConstantCoPsVectors.get(i).get(1),
                 equivalentConstantCoPsVectors.get(i).get(2));
      }


      for (int i = 0; i < initialICPsVectors.size(); i++)
      {
         initialICPsFramePoints.get(i).set(initialICPsVectors.get(i).get(0), initialICPsVectors.get(i).get(1), initialICPsVectors.get(i).get(2));
      }


      initialDoubleSupportICPposFramePoint.set(dsICPcomputer.getInitialDoubleSupportICPpos().get(0), dsICPcomputer.getInitialDoubleSupportICPpos().get(1),
              dsICPcomputer.getInitialDoubleSupportICPpos().get(2));

      finalDoubleSupportICPposFramePoint.set(dsICPcomputer.getFinalDoubleSupportICPpos().get(0), dsICPcomputer.getFinalDoubleSupportICPpos().get(1),
              dsICPcomputer.getFinalDoubleSupportICPpos().get(2));


      desiredDCMposOfTimeFramePoint.set(dsICPcomputer.getDesiredDCMposOfTime().get(0), dsICPcomputer.getDesiredDCMposOfTime().get(1),
                                        dsICPcomputer.getDesiredDCMposOfTime().get(2));
      desiredDCMvelOfTimeFrameVector.set(dsICPcomputer.getDesiredDCMvelOfTime().get(0), dsICPcomputer.getDesiredDCMvelOfTime().get(1),
                                         dsICPcomputer.getDesiredDCMvelOfTime().get(2));
      desiredECMPofTimeFramePoint.set(dsICPcomputer.getDesiredECMPofTime().get(0), dsICPcomputer.getDesiredECMPofTime().get(1),
                                      dsICPcomputer.getDesiredECMPofTime().get(2));

      desiredDCMvelAbsolute.set(Math.sqrt(Math.pow(desiredDCMvelOfTimeFrameVector.getX(), 2) + Math.pow(desiredDCMvelOfTimeFrameVector.getY(), 2)
              + Math.pow(desiredDCMvelOfTimeFrameVector.getZ(), 2)));



      JojosICPutilities.discreteIntegrateCoMAndGetCoMVelocity(simDT.getDoubleValue(), dcmConst, dsICPcomputer.getDesiredDCMposOfTime(), comPositionVector,
              comVelocityVector);

      comPositionFramePoint.set(comPositionVector.get(0), comPositionVector.get(1), comPositionVector.get(2));
      comVelocityFrameVector.set(comVelocityVector.get(0), comVelocityVector.get(1), comVelocityVector.get(2));

      desiredCOMvelAbsolute.set(Math.sqrt(Math.pow(comVelocityFrameVector.getX(), 2) + Math.pow(comVelocityFrameVector.getY(), 2)
              + Math.pow(comVelocityFrameVector.getZ(), 2)));


      PointAndLinePlotter.setLineSegmentBasedOnStartAndEndFramePoints(listOfICPLineSegments.get(0),
              equivalentConstantCoPsFramePoints.get(0).getFramePoint2dCopy(), initialICPsFramePoints.get(1).getFramePoint2dCopy());


      PointAndLinePlotter.setLineSegmentBasedOnStartAndEndFramePoints(listOfICPLineSegments.get(1),
              equivalentConstantCoPsFramePoints.get(1).getFramePoint2dCopy(), initialICPsFramePoints.get(2).getFramePoint2dCopy());

      PointAndLinePlotter.setLineSegmentBasedOnStartAndEndFramePoints(listOfICPLineSegments.get(2),
              equivalentConstantCoPsFramePoints.get(2).getFramePoint2dCopy(), equivalentConstantCoPsFramePoints.get(3).getFramePoint2dCopy());


      
      PointAndLinePlotter.setEndPointGivenStartAndAdditionalVector(icpVelocityArrowTip, desiredDCMposOfTimeFramePoint, desiredDCMvelOfTimeFrameVector, 0.5);
      PointAndLinePlotter.setLineSegmentBasedOnStartAndEndFramePoints(icpVelocityLine, desiredDCMposOfTimeFramePoint.getFramePoint2dCopy(),
              icpVelocityArrowTip.getFramePoint2dCopy());

      PointAndLinePlotter.setEndPointGivenStartAndAdditionalVector(comVelocityArrowTip, comPositionFramePoint, comVelocityFrameVector, 0.5);
      PointAndLinePlotter.setLineSegmentBasedOnStartAndEndFramePoints(comVelocityLine, comPositionFramePoint.getFramePoint2dCopy(),
              comVelocityArrowTip.getFramePoint2dCopy());
   }

   public void initialize()
   {
      this.isSingleSupport.set(true);

      boolean initialIsSingleSupport = false;
      this.supportState.initializeSupportState(initialIsSingleSupport, singleSupportTime, doubleSupportTime, initialTransferSupportTime);
   }


   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return getClass().getSimpleName();
   }

   public String getDescription()
   {
      return getName();
   }

}
