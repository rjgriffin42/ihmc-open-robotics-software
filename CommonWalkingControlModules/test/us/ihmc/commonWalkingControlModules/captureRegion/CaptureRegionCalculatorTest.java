package us.ihmc.commonWalkingControlModules.captureRegion;


import static org.junit.Assert.*;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.SimulationOverheadPlotter;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class CaptureRegionCalculatorTest
{
   private final boolean SHOW_GUI = false;
   
   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }
   
   @Test
   public void testOne()
   {
      ReferenceFrame leftAnkleZUpFrame = new SimpleAnkleZUpReferenceFrame("leftAnkleZUp");
      ReferenceFrame rightAnkleZUpFrame = new SimpleAnkleZUpReferenceFrame("rightAnkleZUp");
      
      SideDependentList<ReferenceFrame> ankleZUpFrames = new SideDependentList<ReferenceFrame>(leftAnkleZUpFrame, rightAnkleZUpFrame);
      
      double midFootAnkleXOffset = 0.15;
      double footWidth = 0.2;
      double kinematicRangeFromContactReferencePoint = 0.6;

      CapturePointCalculatorInterface capturePointCalculator = new SimpleCapturePointCalculator();
      YoVariableRegistry yoVariableRegistry = new YoVariableRegistry("CaptureRegionCalculatorTest");

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      
      RobotSide supportSide = RobotSide.LEFT;

      CaptureRegionCalculator calculator = new CaptureRegionCalculator(ankleZUpFrames, midFootAnkleXOffset, footWidth, kinematicRangeFromContactReferencePoint, capturePointCalculator, yoVariableRegistry, dynamicGraphicObjectsListRegistry);
      
      double swingTimeRemaining = 0.1;
      
      double[][] pointList = new double[][]{{-0.1, 0.1},{0.2, 0.1}, {0.2, -0.1}, {-0.1, -0.1}};
      FrameConvexPolygon2d supportFoot = new FrameConvexPolygon2d(ankleZUpFrames.get(supportSide), pointList);
      
      if (SHOW_GUI)
      {
         Robot robot = new Robot("Test");
         robot.addYoVariableRegistry(yoVariableRegistry);
         
         SimulationConstructionSet scs = new SimulationConstructionSet(robot);
         dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
                 
         SimulationOverheadPlotter plotter = new SimulationOverheadPlotter();
         plotter.setDrawHistory(false);
         plotter.setXVariableToTrack(null);
         plotter.setYVariableToTrack(null);

         scs.attachPlaybackListener(plotter);
         JPanel r2Sim02PlotterJPanel = plotter.getJPanel();
         scs.addExtraJpanel(r2Sim02PlotterJPanel, "Plotter");
         JPanel r2Sim02PlotterKeyJPanel = plotter.getJPanelKey();

         JScrollPane scrollPane = new JScrollPane(r2Sim02PlotterKeyJPanel);

         scs.addExtraJpanel(scrollPane, "Plotter Legend");

         dynamicGraphicObjectsListRegistry.addArtifactListsToPlotter(plotter.getPlotter());

         scs.addExtraJpanel(plotter.getJPanel(), "Plotter");
         
         Thread thread = new Thread(scs);
         thread.start();
      }
      
      FrameConvexPolygon2d captureRegion = calculator.calculateCaptureRegion(supportSide, supportFoot, swingTimeRemaining);
      captureRegion = captureRegion.changeFrameCopy(ReferenceFrame.getWorldFrame());
      
//      System.out.println("captureRegion = " + captureRegion);
      
      double[][] expectedPointList = new double[][]{
            {0.275, -0.175},
            {0.425, -0.175},
            {0.6149269713632459, -0.2021324244804637},
            {0.5853971601432783, -0.270831831420372},
            {0.5475514140914703, -0.3353246044291683},
            {0.5019775636657666, -0.39460902415271487},
            {0.4985268586524354, -0.3985268586524353},
            {0.4493834743506884, -0.44776426880175807},
            {0.39058605189741147, -0.49396471660730357},
            {0.32649855392065846, -0.5324927696032922},
            {0.275, -0.275}};

      FrameConvexPolygon2d expectedCaptureRegion = new FrameConvexPolygon2d(ReferenceFrame.getWorldFrame(), expectedPointList);

      ArrayList<FramePoint2d> expectedVertices = expectedCaptureRegion.getClockwiseOrderedListOfFramePoints();
      ArrayList<FramePoint2d> actualVertices = captureRegion.getClockwiseOrderedListOfFramePoints();

      assertEquals(expectedVertices.size(), actualVertices.size());
      
      for (int i=0; i<expectedVertices.size(); i++)
      {
         FramePoint2d expectedVertex = expectedVertices.get(i);
         
         FramePoint2d actualVertex = captureRegion.getClosestVertexCopy(expectedVertex);
                  
//         System.out.println("expected = " + expectedVertex);
//         System.out.println("actual = " + actualVertex);
         
         double distance = expectedVertex.distance(actualVertex);
         assertEquals(0.0, distance, 1e-7);
      }
      
      if (SHOW_GUI)
      {
         while(true)
         {
            try
            {
               Thread.sleep(10000);
            } catch (InterruptedException e)
            {
            }

         }
      }
   }

   private class  SimpleAnkleZUpReferenceFrame extends ReferenceFrame
   {
      private static final long serialVersionUID = -2855876641425187923L;
      private final Vector3d offset = new Vector3d();
      
      public SimpleAnkleZUpReferenceFrame(String name)
      {
         super(name, ReferenceFrame.getWorldFrame());
      }

      public void updateTransformToParent(Transform3D transformToParent)
      {
         transformToParent.setIdentity();
         transformToParent.setTranslation(offset);
      }
   }
   
   
   private class SimpleCapturePointCalculator implements CapturePointCalculatorInterface
   {
      FramePoint capturePoint = new FramePoint(ReferenceFrame.getWorldFrame(), 0.25, -0.15, 0.0);
      
      public void computeCapturePoint(RobotSide supportSide)
      {
         return;
      }

      public FramePoint computePredictedCapturePoint(RobotSide supportLeg, double captureTime, FramePoint centerOfPressure)
      {
         FrameVector coPToCapture = new FrameVector(capturePoint);
         coPToCapture.changeFrame(centerOfPressure.getReferenceFrame());
         coPToCapture.sub(centerOfPressure);
         coPToCapture.scale(0.5); // For the tests, just go 10% further than the capture point for the predicted capture point.
         
         FramePoint ret = new FramePoint(capturePoint);
         ret.changeFrame(centerOfPressure.getReferenceFrame());
         
         ret.add(coPToCapture);
         
         return ret;
      }

      public FramePoint getCapturePointInFrame(ReferenceFrame referenceFrame)
      {
          FramePoint ret = new FramePoint(capturePoint);
          ret.changeFrame(referenceFrame);
          
          return ret;
      }

      public FramePoint2d getCapturePoint2dInFrame(ReferenceFrame referenceFrame)
      {
         FramePoint2d ret = capturePoint.toFramePoint2d();
         
         ret.changeFrame(referenceFrame);
         
         return ret;
      }
      
   }
   
}
