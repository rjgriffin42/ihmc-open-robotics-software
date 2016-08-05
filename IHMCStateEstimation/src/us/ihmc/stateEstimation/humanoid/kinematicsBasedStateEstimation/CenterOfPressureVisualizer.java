package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.plotting.YoArtifactPosition;


public class CenterOfPressureVisualizer
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final Map<RigidBody, YoFramePoint> footRawCoPPositionsInWorld = new HashMap();
   private final YoFramePoint overallRawCoPPositionInWorld;
   private final FramePoint2d tempRawCoP2d = new FramePoint2d();
   private final FramePoint tempRawCoP = new FramePoint();
   private final Wrench tempWrench = new Wrench();
   private final Map<RigidBody, FootSwitchInterface> footSwitches;
   private final Collection<RigidBody> footRigidBodies;

   public CenterOfPressureVisualizer(Map<RigidBody, FootSwitchInterface> footSwitches,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.footSwitches = footSwitches;
      footRigidBodies = footSwitches.keySet();

      for (RigidBody rigidBody : footRigidBodies)
      {
         String rigidBodyName = rigidBody.getName();
         rigidBodyName = WordUtils.capitalize(rigidBodyName);

         YoFramePoint rawCoPPositionInWorld = new YoFramePoint("raw" + rigidBodyName + "CoPPositionsInWorld", worldFrame, registry);
         footRawCoPPositionsInWorld.put(rigidBody, rawCoPPositionInWorld);

         YoGraphicPosition copDynamicGraphic = new YoGraphicPosition("Meas " + rigidBodyName + "CoP", rawCoPPositionInWorld, 0.008, YoAppearance.DarkRed(), GraphicType.DIAMOND);
         YoArtifactPosition copArtifact = copDynamicGraphic.createArtifact();
         yoGraphicsListRegistry.registerArtifact("StateEstimator", copArtifact);
      }

      overallRawCoPPositionInWorld = new YoFramePoint("overallRawCoPPositionInWorld", worldFrame, registry);
      YoGraphicPosition overallRawCoPDynamicGraphic = new YoGraphicPosition("Meas CoP", overallRawCoPPositionInWorld, 0.015, YoAppearance.DarkRed(), GraphicType.DIAMOND);
      YoArtifactPosition overallRawCoPArtifact = overallRawCoPDynamicGraphic.createArtifact();
      overallRawCoPArtifact.setVisible(false);
      yoGraphicsListRegistry.registerArtifact("StateEstimator", overallRawCoPArtifact);

      parentRegistry.addChild(registry);
   }

   public void update()
   {
      if (footRawCoPPositionsInWorld != null)
      {
         overallRawCoPPositionInWorld.setToZero();
         double totalFootForce = 0.0;

         for (RigidBody rigidBody : footRigidBodies)
         {
            footSwitches.get(rigidBody).computeAndPackCoP(tempRawCoP2d);
            tempRawCoP.setIncludingFrame(tempRawCoP2d.getReferenceFrame(), tempRawCoP2d.getX(), tempRawCoP2d.getY(), 0.0);
            tempRawCoP.changeFrame(worldFrame);
            footRawCoPPositionsInWorld.get(rigidBody).set(tempRawCoP);

            footSwitches.get(rigidBody).computeAndPackFootWrench(tempWrench);
            double singleFootForce = tempWrench.getLinearPartZ();
            totalFootForce += singleFootForce;
            tempRawCoP.scale(singleFootForce);
            overallRawCoPPositionInWorld.add(tempRawCoP);
         }

         overallRawCoPPositionInWorld.scale(1.0 / totalFootForce);
      }
   }

   public void getOverallRawCoPPositionInWorld(FramePoint2d framePointToPack)
   {
      framePointToPack.set(overallRawCoPPositionInWorld.getX(), overallRawCoPPositionInWorld.getY());
   }

   public void hide()
   {
      for (RigidBody rigidBody : footRigidBodies)
      {
         footRawCoPPositionsInWorld.get(rigidBody).setToNaN();
      }
      overallRawCoPPositionInWorld.setToNaN();
   }
}
