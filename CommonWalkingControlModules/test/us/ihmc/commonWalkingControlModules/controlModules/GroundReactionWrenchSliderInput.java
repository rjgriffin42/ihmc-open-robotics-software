package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.CylindricalContactState;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributor;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributorInputData;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionWrenchDistributorOutputData;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.TranslationReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.inputdevices.MidiSliderBoard;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class GroundReactionWrenchSliderInput
{
   public static void main(String[] args)
   {
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      Robot nullRobot = new Robot("null");
      SimulationConstructionSet scs = new SimulationConstructionSet(nullRobot);

      int numberOfContacts = 2;
      int maxNumberOfVertices = 10;
      GroundReactionWrenchDistributorVisualizer visualizer = new GroundReactionWrenchDistributorVisualizer(numberOfContacts, maxNumberOfVertices, 0,
                                                                scs.getRootRegistry(), dynamicGraphicObjectsListRegistry);

      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

      TranslationReferenceFrame centerOfMassFrame = new TranslationReferenceFrame("centerOfMassFrame", worldFrame);

      YoVariableRegistry registry = new YoVariableRegistry("Wrench");
      YoFramePoint centerOfMassPosition = new YoFramePoint("centerOfMass", "", worldFrame, registry);
      YoFrameVector desiredForceOnCenterOfMass = new YoFrameVector("forceDesired", "", worldFrame, registry);
      YoFrameVector desiredTorqueOnCenterOfMass = new YoFrameVector("torqueDesired", "", worldFrame, registry);
      centerOfMassPosition.set(0.1, 0.1, 1.0);
      desiredForceOnCenterOfMass.set(10.0, 20.0, 1000.0);
      desiredTorqueOnCenterOfMass.set(1.0, 2.0, 3.0);

      ArrayList<YoFrameVector> contactTranslations = new ArrayList<YoFrameVector>();

//    GroundReactionWrenchDistributorInterface distributor = new GeometricFlatGroundReactionWrenchDistributor(registry, dynamicGraphicObjectsListRegistry);
//    GroundReactionWrenchDistributorInterface distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, nSupportVectors, parentRegistry);

      ContactPointWrenchDistributorSliderInput contactPointWrenchDistributorSliderInput = new ContactPointWrenchDistributorSliderInput(scs, registry,
                                                                                             centerOfMassFrame);
      GroundReactionWrenchDistributor distributor = contactPointWrenchDistributorSliderInput.getDistributor();
      GroundReactionWrenchDistributorInputData inputData = new GroundReactionWrenchDistributorInputData();

      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);


      ArrayList<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();
      double coefficientOfFriction = 0.7;

      double footLength = 0.3;
      double footWidth = 0.15;

      for (int i = 0; i < numberOfContacts; i++)
      {
         YoFrameVector contactTranslation = new YoFrameVector("contactTranslation" + i, worldFrame, registry);

         if (i == 0)
            contactTranslation.set(0.1, 0.5, 0.05);
         if (i == 1)
            contactTranslation.set(0.0, -0.5, 0.02);

         contactTranslations.add(contactTranslation);

         TranslationReferenceFrame planeFrame = new TranslationReferenceFrame("contact" + i, worldFrame);
         planeFrame.updateTranslation(contactTranslation.getFrameVectorCopy());
         planeFrame.update();

         YoPlaneContactState yoPlaneContactState = new YoPlaneContactState("contact" + i, planeFrame, planeFrame, registry);

         List<FramePoint2d> contactPoints = new ArrayList<FramePoint2d>();
         contactPoints.add(new FramePoint2d(planeFrame, footLength / 2.0, footWidth / 2.0));
         contactPoints.add(new FramePoint2d(planeFrame, footLength / 2.0, -footWidth / 2.0));
         contactPoints.add(new FramePoint2d(planeFrame, -footLength / 2.0, -footWidth / 2.0));
         contactPoints.add(new FramePoint2d(planeFrame, -footLength / 2.0, footWidth / 2.0));

         yoPlaneContactState.set(contactPoints, coefficientOfFriction);

         contactStates.add(yoPlaneContactState);
         inputData.addPlaneContact(yoPlaneContactState);
      }

      scs.addYoVariableRegistry(registry);

      MidiSliderBoard sliderBoard = new MidiSliderBoard(scs);
      sliderBoard.setSlider(0, "forceDesiredX", scs, -100.0, 100.0);
      sliderBoard.setSlider(1, "forceDesiredY", scs, -100.0, 100.0);
      sliderBoard.setSlider(2, "forceDesiredZ", scs, 100.0, 2000.0);

      sliderBoard.setSlider(3, "torqueDesiredX", scs, -20.0, 20.0);
      sliderBoard.setSlider(4, "torqueDesiredY", scs, -20.0, 20.0);
      sliderBoard.setSlider(5, "torqueDesiredZ", scs, -20.0, 20.0);

      sliderBoard.setSlider(6, "contactTranslation0X", scs, -2.0, 2.0);
      sliderBoard.setSlider(7, "contactTranslation0Y", scs, -2.5, -0.3);
      sliderBoard.setSlider(8, "contactTranslation0Z", scs, -0.05, 0.3);

      sliderBoard.setSlider(9, "contactTranslation1X", scs, -2.0, 2.0);
      sliderBoard.setSlider(10, "contactTranslation1Y", scs, 0.3, 2.5);
      sliderBoard.setSlider(11, "contactTranslation1Z", scs, -0.05, 0.3);

      MidiSliderBoard sliderBoard2 = new MidiSliderBoard(scs);
      sliderBoard2.setSlider(0, "centerOfMassX", scs, -2.0, 2.0);
      sliderBoard2.setSlider(1, "centerOfMassY", scs, -2.0, 2.0);
      sliderBoard2.setSlider(2, "centerOfMassZ", scs, 0.0, 4.0);

      scs.startOnAThread();

      while (true)
      {
         contactPointWrenchDistributorSliderInput.updateWeights();

         centerOfMassFrame.updateTranslation(centerOfMassPosition.getFrameVectorCopy());
         centerOfMassFrame.update();

         for (int i = 0; i < numberOfContacts; i++)
         {
            PlaneContactState contactState = contactStates.get(i);
            TranslationReferenceFrame planeFrame = (TranslationReferenceFrame) contactState.getPlaneFrame();
            FrameVector contactTranslation = contactTranslations.get(i).getFrameVectorCopy();
            planeFrame.updateTranslation(contactTranslation);
            planeFrame.update();
         }

         SpatialForceVector desiredNetSpatialForceVector = new SpatialForceVector(centerOfMassFrame);
         desiredNetSpatialForceVector.setAngularPart(desiredTorqueOnCenterOfMass.getFrameVectorCopy().getVector());
         desiredNetSpatialForceVector.setLinearPart(desiredForceOnCenterOfMass.getFrameVectorCopy().getVector());

         inputData.setSpatialForceVectorAndUpcomingSupportSide(desiredNetSpatialForceVector, null);

         GroundReactionWrenchDistributorOutputData distributedWrench = new GroundReactionWrenchDistributorOutputData();
         distributor.solve(distributedWrench, inputData);

         visualizer.update(scs, distributedWrench, centerOfMassFrame, contactStates, desiredNetSpatialForceVector, new ArrayList<CylindricalContactState>());
         scs.tickAndUpdate();
         ThreadTools.sleep(100L);
      }
   }

}
