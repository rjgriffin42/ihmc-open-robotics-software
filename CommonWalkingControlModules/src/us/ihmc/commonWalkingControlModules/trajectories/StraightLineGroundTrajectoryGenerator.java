package us.ihmc.commonWalkingControlModules.trajectories;

import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;

public class StraightLineGroundTrajectoryGenerator implements GroundTrajectoryGenerator
{

   private final YoVariableRegistry registry;
   
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
   
   
   private final FramePoint startPoint;
   private final FramePoint endPoint;
   public StraightLineGroundTrajectoryGenerator(String name, CommonWalkingReferenceFrames referenceFrames, YoVariableRegistry parentRegistry)
   {
      registry = new YoVariableRegistry(name);      
      ankleZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();
      
      startPoint = new FramePoint(ReferenceFrame.getWorldFrame());
      endPoint = new FramePoint(ReferenceFrame.getWorldFrame());
      new FrameVector(ReferenceFrame.getWorldFrame());
      parentRegistry.addChild(registry);
   }

   public void getViaPoints(YoFramePoint[] viaPointsToPack, RobotSide swingSide, double tStart, FramePoint startPointIn, double tEnd, FramePoint endPointIn, double[] tOfViaPoints, double heightOfViaPoints[])
   {
      
      ReferenceFrame groundPlaneFrame = ankleZUpFrames.get(swingSide.getOppositeSide());
      
      
      startPoint.setAndChangeFrame(startPointIn);
      startPoint.changeFrame(groundPlaneFrame);
      
      endPoint.setAndChangeFrame(endPointIn);
      endPoint.changeFrame(groundPlaneFrame);
      
      double deltaX = endPoint.getX() - startPoint.getX();
      double deltaY = endPoint.getY() - startPoint.getY();
      
      double tScale = 1.0 / (tEnd - tStart);
      
      
      for(int i = 0; i < tOfViaPoints.length; i++)
      {
         FramePoint viaPoint = new FramePoint(groundPlaneFrame);
         
         
         viaPoint.set(startPoint.getX() + tScale*(tOfViaPoints[i]-tStart)*deltaX, 
                      startPoint.getY() + tScale*(tOfViaPoints[i]-tStart)*deltaY, 
                      heightOfViaPoints[i]);
         
         viaPoint.changeFrame(viaPointsToPack[i].getReferenceFrame());
         viaPointsToPack[i].set(viaPoint);
         
         
      }
      
   }

}
