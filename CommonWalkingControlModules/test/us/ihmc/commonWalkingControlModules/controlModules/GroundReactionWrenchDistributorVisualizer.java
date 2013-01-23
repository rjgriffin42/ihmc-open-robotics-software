package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicYoFramePolygon;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameConvexPolygon2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class GroundReactionWrenchDistributorVisualizer
{
   private static final double FORCE_VECTOR_SCALE = 1.0/600.0; // 6000N is drawn 1 meter long. 
   private static final double MOMENT_VECTOR_SCALE = 1.0/50.0; // 50 Nm is drawn 1 meter long.
   private static final double COM_VIZ_RADIUS = 0.1;
   private static final double COP_VIZ_RADIUS = 0.05;

   private final YoVariableRegistry registry = new YoVariableRegistry("GroundReactionWrenchDistributorVisuzalizer");
  
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoFramePoint centerOfMassWorld = new YoFramePoint("centerOfMassViz", "", worldFrame, registry);
   private final YoFrameVector desiredForceWorld = new YoFrameVector("desiredForceViz", worldFrame, registry);
   private final YoFrameVector desiredMomentWorld = new YoFrameVector("desiredMomentViz", worldFrame, registry);
   
   private final YoFrameVector achievedForceWorld = new YoFrameVector("achievedForceViz", worldFrame, registry);
   private final YoFrameVector achievedMomentWorld = new YoFrameVector("achievedMomentViz", worldFrame, registry);
   
   private final ArrayList<YoFrameConvexPolygon2d> contactPolygonsWorld = new ArrayList<YoFrameConvexPolygon2d>();
   private final ArrayList<YoFramePoint> contactReferencePoints = new ArrayList<YoFramePoint>();
   private final ArrayList<YoFrameOrientation> contactPlaneOrientations = new ArrayList<YoFrameOrientation>();
   
   private final ArrayList<YoFramePoint> contactCenterOfPressures = new ArrayList<YoFramePoint>();
   private final ArrayList<YoFrameVector> contactForces = new ArrayList<YoFrameVector>();
   private final ArrayList<YoFrameVector> contactMoments = new ArrayList<YoFrameVector>();
   
   public GroundReactionWrenchDistributorVisualizer(int maxNumberOfFeet, int maxNumberOfVertices, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("GroundReactionWrenchDistributorVisuzalizer");

      for (int i=0; i<maxNumberOfFeet; i++)
      {
         YoFrameConvexPolygon2d contactPolygon = new YoFrameConvexPolygon2d("contactViz" + i, "", worldFrame, maxNumberOfVertices, registry);
         contactPolygonsWorld.add(contactPolygon);

         YoFramePoint contactReferencePoint = new YoFramePoint("contactReferenceViz" + i, worldFrame, registry);
         contactReferencePoints.add(contactReferencePoint);

         YoFrameOrientation contactPlaneOrientation = new YoFrameOrientation("contactOrientation" + i, worldFrame, registry);
         contactPlaneOrientations.add(contactPlaneOrientation);

         DynamicGraphicYoFramePolygon dynamicGraphicPolygon = new DynamicGraphicYoFramePolygon("contactPolygon"+i, contactPolygon, contactReferencePoint, contactPlaneOrientation, 1.0, YoAppearance.Green());
         dynamicGraphicObjectsList.add(dynamicGraphicPolygon);
         
         YoFramePoint contactCenterOfPressure = new YoFramePoint("contactCoPViz"+i, worldFrame, registry);
         YoFrameVector contactForce = new YoFrameVector("contactForceViz" + i, worldFrame, registry);
         YoFrameVector contactMoment = new YoFrameVector("contactMomentViz" + i, worldFrame, registry);
         
         contactCenterOfPressures.add(contactCenterOfPressure);
         contactForces.add(contactForce);
         contactMoments.add(contactMoment);
         
         DynamicGraphicPosition contactCoPViz = new DynamicGraphicPosition("contactCoPViz" + i, contactCenterOfPressure, COP_VIZ_RADIUS, YoAppearance.Brown());
         dynamicGraphicObjectsList.add(contactCoPViz);
         
         DynamicGraphicVector contactForceViz = new DynamicGraphicVector("contactForceViz" + i, contactCenterOfPressure, contactForce, FORCE_VECTOR_SCALE, YoAppearance.Pink());
         dynamicGraphicObjectsList.add(contactForceViz);
         
         DynamicGraphicVector contactMomentViz = new DynamicGraphicVector("contactMomentViz" + i, contactCenterOfPressure, contactMoment, MOMENT_VECTOR_SCALE, YoAppearance.Black());
         dynamicGraphicObjectsList.add(contactMomentViz);

      }

      AppearanceDefinition desiredForceAppearance = YoAppearance.Yellow();
      desiredForceAppearance.setTransparancy(0.9);
      
      AppearanceDefinition desiredMomentAppearance = YoAppearance.Purple();
      desiredMomentAppearance.setTransparancy(0.9);
      
      DynamicGraphicPosition centerOfMassWorldViz = new DynamicGraphicPosition("centerOfMassViz", centerOfMassWorld, COM_VIZ_RADIUS, YoAppearance.Purple(), GraphicType.BALL_WITH_CROSS);
      DynamicGraphicVector desiredForceWorldViz = new DynamicGraphicVector("desiredForceViz", centerOfMassWorld, desiredForceWorld, FORCE_VECTOR_SCALE, desiredForceAppearance);
      DynamicGraphicVector desiredMomentWorldViz = new DynamicGraphicVector("desiredMomentViz", centerOfMassWorld, desiredMomentWorld, MOMENT_VECTOR_SCALE, desiredMomentAppearance);
      DynamicGraphicVector achievedForceWorldViz = new DynamicGraphicVector("achievedForceViz", centerOfMassWorld, achievedForceWorld, FORCE_VECTOR_SCALE, YoAppearance.Yellow());
      DynamicGraphicVector achievedMomentWorldViz = new DynamicGraphicVector("achievedMomentViz", centerOfMassWorld, achievedMomentWorld, MOMENT_VECTOR_SCALE, YoAppearance.Purple());

      dynamicGraphicObjectsList.add(centerOfMassWorldViz);
      dynamicGraphicObjectsList.add(desiredForceWorldViz);
      dynamicGraphicObjectsList.add(desiredMomentWorldViz);
      dynamicGraphicObjectsList.add(achievedForceWorldViz);
      dynamicGraphicObjectsList.add(achievedMomentWorldViz);
      
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      
      parentRegistry.addChild(registry);
   }
   
   public void update(SimulationConstructionSet scs, GroundReactionWrenchDistributorInterface distributor, ReferenceFrame centerOfMassFrame, ArrayList<PlaneContactState> contactStates, SpatialForceVector desiredBodyWrench)
   {
      try
      {
         FramePoint centerOfMassPosition = new FramePoint(centerOfMassFrame);
         centerOfMassPosition.changeFrame(worldFrame);

         centerOfMassWorld.set(centerOfMassPosition);

         SpatialForceVector desiredWrenchOnCenterOfMass = new SpatialForceVector(desiredBodyWrench);
         desiredWrenchOnCenterOfMass.changeFrame(centerOfMassFrame);
         FrameVector desiredForceOnCenterOfMass = desiredWrenchOnCenterOfMass.getLinearPartAsFrameVectorCopy();
         FrameVector desiredTorqueOnCenterOfMass = desiredWrenchOnCenterOfMass.getAngularPartAsFrameVectorCopy();
         desiredForceOnCenterOfMass.changeFrame(worldFrame);
         desiredTorqueOnCenterOfMass.changeFrame(worldFrame);

         desiredForceWorld.set(desiredForceOnCenterOfMass);
         desiredMomentWorld.set(desiredTorqueOnCenterOfMass);

         for (int i=0; i<contactStates.size(); i++)
         {
            PlaneContactState contactState = contactStates.get(i);

            List<FramePoint2d> contactPoints = contactState.getContactPoints2d();
            FrameConvexPolygon2d frameConvexPolygon2d = new FrameConvexPolygon2d(contactPoints);

            YoFrameConvexPolygon2d yoFrameConvexPolygon2d = contactPolygonsWorld.get(i);
            yoFrameConvexPolygon2d.setFrameConvexPolygon2d(frameConvexPolygon2d.changeFrameCopy(ReferenceFrame.getWorldFrame()));

            YoFramePoint contactCenterOfPressureForViz = contactCenterOfPressures.get(i);
            YoFrameVector contactForceForViz = contactForces.get(i);
            YoFrameVector contactMomentForViz = contactMoments.get(i);

            FramePoint2d centerOfPressure2d = distributor.getCenterOfPressure(contactState);

            FramePoint centerOfPressure = new FramePoint(centerOfPressure2d.getReferenceFrame(), centerOfPressure2d.getX(), centerOfPressure2d.getY(), 0.0);
            centerOfPressure.changeFrame(worldFrame);
            contactCenterOfPressureForViz.set(centerOfPressure);

            FrameVector contactForce = distributor.getForce(contactState);
            contactForceForViz.set(contactForce.changeFrameCopy(worldFrame));

            double normalTorque = distributor.getNormalTorque(contactState);
            FrameVector normalForce = new FrameVector(centerOfPressure2d.getReferenceFrame(), 0.0, 0.0, normalTorque);
            contactMomentForViz.set(normalForce.changeFrameCopy(worldFrame));
         }

         SpatialForceVector achievedWrenchOnCenterOfMass = GroundReactionWrenchDistributorAchievedWrenchCalculator.computeAchievedWrench(distributor, desiredBodyWrench.getExpressedInFrame(), contactStates);

         achievedForceWorld.set(achievedWrenchOnCenterOfMass.getLinearPartAsFrameVectorCopy().changeFrameCopy(worldFrame));
         achievedMomentWorld.set(achievedWrenchOnCenterOfMass.getAngularPartAsFrameVectorCopy().changeFrameCopy(worldFrame));


         scs.tickAndUpdate();
      }
      catch(RuntimeException exception)
      {
         System.err.println("Exception Thrown in GroundReactionWrenchDistributorVisualizer.update(): " + exception);
      }
   }

   
}
