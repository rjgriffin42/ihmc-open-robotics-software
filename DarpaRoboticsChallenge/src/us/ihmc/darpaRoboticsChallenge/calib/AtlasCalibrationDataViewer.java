package us.ihmc.darpaRoboticsChallenge.calib;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.zip.ZipFile;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LimbName;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCoordinateSystem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePose;

public class AtlasCalibrationDataViewer extends AtlasKinematicCalibrator
{
   //YoVariables for Display
   private final YoFramePoint ypLeftEE, ypRightEE;
   private final YoFramePose yposeLeftEE, yposeRightEE;
   public AtlasCalibrationDataViewer()
   {
      super();
      ypLeftEE = new YoFramePoint("leftEE", ReferenceFrame.getWorldFrame(), registry);
      ypRightEE = new YoFramePoint("rightEE", ReferenceFrame.getWorldFrame(),registry);
      yposeLeftEE = new YoFramePose("leftPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
      yposeRightEE = new YoFramePose("rightPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
    }

   @Override
   protected void addDynamicGraphicObjects()
   {
      double transparency = 0.5;
      double scale=0.02;
      DynamicGraphicPosition dgpLeftEE = new DynamicGraphicPosition("dgpLeftEE", ypLeftEE, scale, new YoAppearanceRGBColor(Color.BLUE, transparency));
      DynamicGraphicPosition dgpRightEE = new DynamicGraphicPosition("dgpRightEE", ypRightEE, scale, new YoAppearanceRGBColor(Color.RED, transparency));
      
      scs.addDynamicGraphicObject(dgpLeftEE);
      scs.addDynamicGraphicObject(dgpRightEE);

      DynamicGraphicCoordinateSystem dgPoseLeftEE = new DynamicGraphicCoordinateSystem("dgposeLeftEE", yposeLeftEE, 5*scale);
      DynamicGraphicCoordinateSystem dgPoseRightEE = new DynamicGraphicCoordinateSystem("dgposeRightEE", yposeRightEE, 5*scale);
      scs.addDynamicGraphicObject(dgPoseLeftEE);
      scs.addDynamicGraphicObject(dgPoseRightEE);

   }
   
   @Override
   protected void updateDynamicGraphicsObjects()
   {
      FramePoint
      leftEE=new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT, LimbName.ARM)  ,0, 0.13,0),
      rightEE=new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.RIGHT, LimbName.ARM),0,-0.13,0);
      

      ypLeftEE.set(leftEE.changeFrameCopy(CalibUtil.world));
      ypRightEE.set(rightEE.changeFrameCopy(CalibUtil.world));
      
      yposeLeftEE.set(new FramePose(leftEE, new FrameOrientation(leftEE.getReferenceFrame())).changeFrameCopy(CalibUtil.world));
      yposeRightEE.set(new FramePose(rightEE,new FrameOrientation(rightEE.getReferenceFrame())).changeFrameCopy(CalibUtil.world));
   }
   
   
   
   public void loadData(String calib_file)
   {
      
      BufferedReader reader = null;
      try
      {
         if (calib_file.contains("zip"))
         {
            ZipFile zip = new ZipFile(calib_file);            
            reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntries().nextElement())));
         }
         else
            reader = new BufferedReader(new FileReader(calib_file));
      }
      catch (IOException e1)
      {
         System.out.println("Cannot load calibration file " + calib_file);
         e1.printStackTrace();
      }

      String line;
      final int numJoints = 28;
      System.out.println("total joints should be " + numJoints);
      try
      {
         while ((line = reader.readLine()) != null)
         {
            if (line.matches("^entry.*"))
            {
               Map<String, Double> q_ = new HashMap<>();
               Map<String, Double> qout_ = new HashMap<>();

               for (int i = 0; i < numJoints; i++)
               {
                  line = reader.readLine();
                  if (line != null)
                  {
                     String[] items = line.split("\\s");
                     
                     if(items[0].equals("neck_ay"))
                        items[0]=new String("neck_ry");
                     q_.put(items[0], new Double(items[1]));
                     qout_.put(items[0], new Double(items[2]));
                  }
                  else
                  {
                     System.out.println("One ill-formed data entry");
                     break;
                  }

               }

               if (q_.size() == numJoints)
                  q.add((Map)q_);
               if (qout_.size() == numJoints)
                  qout.add((Map)qout_);
            }
         }
      }
      catch (IOException e1)
      {
         System.err.println("File reading error");
         e1.printStackTrace();
      }
      System.out.println("total entry loaded q/qout " + q.size() + "/" + qout.size());
   }

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      AtlasWristLoopKinematicCalibrator calib = new AtlasWristLoopKinematicCalibrator();
      calib.loadData("data/manip_motions/log4.zip");
      
      Map<String,Double> qout0 = (Map)calib.qout.get(0);
      Map<String,DoubleYoVariable> yoQout= new HashMap<>();
      Map<String,DoubleYoVariable> yoQdiff= new HashMap<>();
      for(String jointName: qout0.keySet())
      {
         yoQout.put(jointName, new DoubleYoVariable("qout_"+jointName, calib.registry));
         yoQdiff.put(jointName, new DoubleYoVariable("qdiff_"+jointName, calib.registry));
      }
      
      calib.createDisplay(calib.q.size());
      
      for(int i=0;i<calib.q.size();i++)
      {
         CalibUtil.setRobotModelFromData(calib.fullRobotModel, (Map)calib.q.get(i));
         for(String jointName: qout0.keySet())
         {
            yoQout.get(jointName).set((double)calib.qout.get(i).get(jointName));
            yoQdiff.get(jointName).set((double)calib.q.get(i).get(jointName)-(double)calib.qout.get(i).get(jointName));
         }
         calib.displayUpdate();
      }
      
   }

}
