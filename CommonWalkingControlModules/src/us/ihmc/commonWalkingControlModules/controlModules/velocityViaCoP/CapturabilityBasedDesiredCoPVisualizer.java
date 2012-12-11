package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP;

import java.awt.Color;

import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.YoFrameLineSegment2dArtifact;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameLineSegment2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;

public class CapturabilityBasedDesiredCoPVisualizer
{
   private final YoVariableRegistry registry = new YoVariableRegistry("CapturabilityBasedDesiredCoPVisualizer");
   private final ReferenceFrame world = ReferenceFrame.getWorldFrame();

   private final YoFramePoint desiredCoP = new YoFramePoint("desiredCoP", "", world, registry);
   private final YoFramePoint desiredCapturePoint = new YoFramePoint("desiredCapturePoint", "", world, registry);
   private final YoFrameLineSegment2d guideLine = new YoFrameLineSegment2d("guideLine", "", world, registry);
   private final YoFramePoint desiredCMP = new YoFramePoint("desiredCMP", "", world, registry);

   public CapturabilityBasedDesiredCoPVisualizer(YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      DynamicGraphicObjectsList dynamicGraphicObjectList = new DynamicGraphicObjectsList("CapturabilityBasedDesiredCoPVisualizer");
      ArtifactList artifactList = new ArtifactList("CapturabilityBasedDesiredCoPVisualizer");

      if (dynamicGraphicObjectsListRegistry != null)
      {
         addDesiredCoPViz(dynamicGraphicObjectList, artifactList);
         addDesiredCapturePointViz(dynamicGraphicObjectList, artifactList);
         addDesiredCMPViz(dynamicGraphicObjectList, artifactList);
         addGuideLineViz(artifactList);

         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectList);
         dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      }
      desiredCMP.setToNaN();

      parentRegistry.addChild(registry);
   }

   private void addDesiredCoPViz(DynamicGraphicObjectsList dynamicGraphicObjectList, ArtifactList artifactList)
   {
      DynamicGraphicPosition desiredCoPViz = desiredCoP.createDynamicGraphicPosition("Desired Center of Pressure", 0.012, YoAppearance.Gray(),
                                                DynamicGraphicPosition.GraphicType.CROSS);
      dynamicGraphicObjectList.add(desiredCoPViz);
      artifactList.add(desiredCoPViz.createArtifact());
   }

   private void addDesiredCMPViz(DynamicGraphicObjectsList dynamicGraphicObjectList, ArtifactList artifactList)
   {
      DynamicGraphicPosition desiredCMPViz = desiredCMP.createDynamicGraphicPosition("Desired CMP", 0.012, YoAppearance.Red(),
                                                DynamicGraphicPosition.GraphicType.CROSS);
      dynamicGraphicObjectList.add(desiredCMPViz);
      artifactList.add(desiredCMPViz.createArtifact());
   }

   private void addDesiredCapturePointViz(DynamicGraphicObjectsList dynamicGraphicObjectList, ArtifactList artifactList)
   {
      DynamicGraphicPosition desiredCapturePointViz = desiredCapturePoint.createDynamicGraphicPosition("Desired Capture Point", 0.01, YoAppearance.Yellow(),
                                                         GraphicType.ROTATED_CROSS);
      dynamicGraphicObjectList.add(desiredCapturePointViz);
      artifactList.add(desiredCapturePointViz.createArtifact());
   }

   private void addGuideLineViz(ArtifactList artifactList)
   {
      YoFrameLineSegment2dArtifact guideLineArtifact = new YoFrameLineSegment2dArtifact("Guide Line", guideLine, Color.RED);
      artifactList.add(guideLineArtifact);
   }

   public void setDesiredCoP(FramePoint2d desiredCoP2d)
   {
      FramePoint desiredCoP = desiredCoP2d.toFramePoint();
      desiredCoP.changeFrame(world);
      this.desiredCoP.set(desiredCoP);
   }

   public void setDesiredCapturePoint(FramePoint2d desiredCapturePoint2d)
   {
      FramePoint desiredCapturePoint = desiredCapturePoint2d.toFramePoint();
      desiredCapturePoint.changeFrame(world);
      this.desiredCapturePoint.set(desiredCapturePoint);
      hideGuideLine();
   }

   public void setGuideLine(FrameLineSegment2d guideLine)
   {
      this.guideLine.setFrameLineSegment2d(guideLine.changeFrameCopy(world));
      hideDesiredCapturePoint();
   }

   private void hideDesiredCapturePoint()
   {
      desiredCapturePoint.setToNaN();
   }

   private void hideGuideLine()
   {
      guideLine.setFrameLineSegment2d(null);
   }

   public void setDesiredCMP(FramePoint2d desiredCMP)
   {
      desiredCMP.changeFrame(this.desiredCMP.getReferenceFrame());
      this.desiredCMP.set(desiredCMP.getX(), desiredCMP.getY(), 0.0);
   }
}
