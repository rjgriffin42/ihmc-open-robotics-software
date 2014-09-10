package us.ihmc.commonWalkingControlModules.captureRegion;

import java.awt.Color;

import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.plotting.YoArtifactPolygon;
import us.ihmc.yoUtilities.math.frames.YoFrameConvexPolygon2d;


public class CaptureRegionVisualizer
{
   private static final String caption = "CaptureRegion";
   private static final Color color = Color.GREEN;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   
   private YoFrameConvexPolygon2d yoCaptureRegionPolygon;
   private FrameConvexPolygon2d captureRegionPolygon;
   private OneStepCaptureRegionCalculator captureRegionCalculator;
   
   public CaptureRegionVisualizer(OneStepCaptureRegionCalculator captureRegionCalculator,
                                  YoGraphicsListRegistry yoGraphicsListRegistry,
                                  YoVariableRegistry parentRegistry)
   {
      this.captureRegionCalculator = captureRegionCalculator;
      
      yoCaptureRegionPolygon = new YoFrameConvexPolygon2d(caption, "", worldFrame, 30, registry);
      captureRegionPolygon = new FrameConvexPolygon2d(worldFrame);
      
      YoArtifactPolygon dynamicGraphicYoPolygonArtifact = 
            new YoArtifactPolygon(caption, yoCaptureRegionPolygon, color, false);
      yoGraphicsListRegistry.registerArtifact(caption, dynamicGraphicYoPolygonArtifact);
      
      parentRegistry.addChild(registry);
   }
   
   public void hide()
   {
      yoCaptureRegionPolygon.hide();
   }
   
   public void update()
   {
      captureRegionPolygon = captureRegionCalculator.getCaptureRegion().changeFrameAndProjectToXYPlaneCopy(worldFrame);
      if (yoCaptureRegionPolygon != null)
      {
         try
         {
            yoCaptureRegionPolygon.setFrameConvexPolygon2d(captureRegionPolygon);
         }
         catch (Exception e)
         {
            System.out.println(e);
         }
      }
   }
}
