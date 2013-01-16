package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameConvexPolygon2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class GroundReactionWrenchDistributorVisuzalizer
{
   private final YoVariableRegistry registry = new YoVariableRegistry("GroundReactionWrenchDistributorVisuzalizer");
  
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoFramePoint centerOfMassWorld = new YoFramePoint("centerOfMass", "", worldFrame, registry);
   private final YoFrameVector totalForceWorld = new YoFrameVector("totalForce", worldFrame, registry);
   private final YoFrameVector totalMomentWorld = new YoFrameVector("totalMoment", worldFrame, registry);
   
   private final ArrayList<YoFrameConvexPolygon2d> contactPolygonsWorld = new ArrayList<YoFrameConvexPolygon2d>();
   
   
   private final SimulationConstructionSet scs;
   
   public GroundReactionWrenchDistributorVisuzalizer(int maxNumberOfFeet, int maxNumberOfVertices)
   {
      for (int i=0; i<maxNumberOfFeet; i++)
      {
         YoFrameConvexPolygon2d contactPolygon = new YoFrameConvexPolygon2d("contact" + i, "", ReferenceFrame.getWorldFrame(), maxNumberOfVertices, registry);
         contactPolygonsWorld.add(contactPolygon);
      }
      
      Robot nullRobot = new Robot("null");
      scs = new SimulationConstructionSet(nullRobot);
      
      
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      
      DynamicGraphicPosition centerOfMassWorldViz = new DynamicGraphicPosition("centerOfMass", "", registry, 0.03, YoAppearance.Red(), GraphicType.BALL_WITH_CROSS);
      DynamicGraphicVector totalForceWorldViz = new DynamicGraphicVector("totalForceViz", centerOfMassWorld, totalForceWorld, 0.03, YoAppearance.Yellow());

      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("GroundReactionWrenchDistributorVisuzalizer");
      dynamicGraphicObjectsList.add(centerOfMassWorldViz);
      dynamicGraphicObjectsList.add(totalForceWorldViz);
      
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      
      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      scs.startOnAThread();
   }
   
   
   public void update(LeeGoswamiGroundReactionWrenchDistributor distributor, ReferenceFrame centerOfMassFrame, ArrayList<PlaneContactState> contactStates, SpatialForceVector totalBodyWrench)
   {
      FramePoint centerOfMassPosition = new FramePoint(centerOfMassFrame);
      centerOfMassPosition.changeFrame(worldFrame);
      
      centerOfMassWorld.set(centerOfMassPosition);
      
      SpatialForceVector totalWrenchOnCenterOfMass = new SpatialForceVector(totalBodyWrench);
      totalWrenchOnCenterOfMass.changeFrame(centerOfMassFrame);
      FrameVector totalForceOnCenterOfMass = totalWrenchOnCenterOfMass.getLinearPartAsFrameVectorCopy();
      FrameVector totalTorqueOnCenterOfMass = totalWrenchOnCenterOfMass.getAngularPartAsFrameVectorCopy();
      totalForceOnCenterOfMass.changeFrame(worldFrame);
      totalTorqueOnCenterOfMass.changeFrame(worldFrame);
      
      totalForceWorld.set(totalForceOnCenterOfMass);
      totalMomentWorld.set(totalTorqueOnCenterOfMass);
      
      
   }
   
   
   public static void main(String[] args)
   {
      
   }
   
}
