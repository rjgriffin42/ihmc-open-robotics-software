package com.yobotics.simulationconstructionset.util.graphics;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameConvexPolygon2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class DynamicGraphicObjectEvaluation
{
   public static void main(String[] args)
   {
      debugPolygonStuff();
      
//      Graphics3DAdapter jmeGraphics3dAdapter = new JMEGraphics3dAdapter();

//    Graphics3DAdapter java3DGraphicsAdapter = new Java3DGraphicsAdapter();

//      evaluate(jmeGraphics3dAdapter);

//    evaluate(java3DGraphicsAdapter);
   }
   
   public static void debugPolygonStuff()
   {
      SimulationConstructionSet scs = new SimulationConstructionSet(new Robot("Debug"));
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      int maxNumberOfVertices = 6;
      YoVariableRegistry registry = new YoVariableRegistry("Debug");

      YoFrameConvexPolygon2d transferToPolygon = new YoFrameConvexPolygon2d("transferToPolygon", "", worldFrame, maxNumberOfVertices , registry);
      
      double[][] pointList = new double[][]{{0.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}, {1.0, 0.0}};
      ConvexPolygon2d convexPolygon = new ConvexPolygon2d(pointList);
      transferToPolygon.setConvexPolygon2d(convexPolygon);
      
      double polygonVizScale = 1.0;
      DynamicGraphicYoFramePolygon transferToPolygonViz = new DynamicGraphicYoFramePolygon("transferToPolygon", transferToPolygon, "transferToPolygon", "", registry, polygonVizScale, YoAppearance.Bisque());
      transferToPolygonViz.setPosition(0.0, 0.0, 0.1);
      
      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("FinalDesiredICPCalculator");

      dynamicGraphicObjectsList.add(transferToPolygonViz);

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      
      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      
      scs.startOnAThread();

   }

   public static void evaluate(Graphics3DAdapter graphicsAdapter)
   {
      Robot robot = new Robot("null");

      final SimulationConstructionSet scs = new SimulationConstructionSet(robot, graphicsAdapter, 100);
      scs.setDT(0.1, 1);

      YoVariableRegistry registry = new YoVariableRegistry("Polygon");
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      // Polygon:
      final double[][] pointList = new double[][]
      {
         {0.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}, {1.0, 0.0}, {0.5, 1.2}, {0.5, -0.2}
      };

      ConvexPolygon2d polygon = new ConvexPolygon2d(pointList);

      AppearanceDefinition appearance = YoAppearance.Red();
      appearance.setTransparency(0.8);

      final DynamicGraphicPolygon dynamicGraphicPolygon = new DynamicGraphicPolygon("Polygon", polygon, "polygon", "", registry, 3.0, appearance);
      dynamicGraphicPolygon.setPosition(0.1, 0.2, 1.0);
      dynamicGraphicPolygon.setYawPitchRoll(-0.1, -0.4, -0.3);

      // 3D Text:
      final DynamicGraphicText3D dynamicGraphicText = new DynamicGraphicText3D("Text", "Hello", "text", "", registry, 0.2, YoAppearance.Blue());
      dynamicGraphicText.setPosition(1.0, 0.0, 0.2);
      dynamicGraphicText.setYawPitchRoll(0.3, 0.0, 0.0);

      // Vector:
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      YoFramePoint yoFramePoint = new YoFramePoint("position", "", worldFrame, registry);
      YoFrameVector yoFrameVector = new YoFrameVector("vector", "", worldFrame, registry);

      yoFramePoint.set(0.3, 0.4, 0.2);
      yoFrameVector.set(1.0, 2.0, 3.0);

      final DynamicGraphicVector dynamicGraphicVector = new DynamicGraphicVector("Vector", yoFramePoint, yoFrameVector, 1.0, YoAppearance.Yellow());

      // YoFrameConvexPolygon2d:
      final YoFrameConvexPolygon2d yoFramePolygon = new YoFrameConvexPolygon2d("yoPolygon", "", worldFrame, 10, registry);
      YoFramePoint yoFramePolygonPosition = new YoFramePoint("yoPolygonPosition", "", worldFrame, registry);
      yoFramePolygonPosition.set(2.0, 1.0, 0.3);
      final YoFrameOrientation yoFramePolygonOrientation = new YoFrameOrientation("yoPolygonOrientation", "", worldFrame, registry);
      yoFramePolygonOrientation.setYawPitchRoll(1.2, 0.1, 0.4);
      final DynamicGraphicYoFramePolygon dynamicGraphicYoFramePolygon = new DynamicGraphicYoFramePolygon("YoFramePolygon", yoFramePolygon,
                                                                           yoFramePolygonPosition, yoFramePolygonOrientation, 1.0, YoAppearance.DarkBlue());

      // Box Ghost:
      Graphics3DObject boxGhostGraphics = new Graphics3DObject();
      AppearanceDefinition transparantBlue = YoAppearance.Blue();
      YoAppearance.makeTransparent(transparantBlue, 0.5);
      boxGhostGraphics.translate(0.0, 0.0, -0.5);
      boxGhostGraphics.addCube(1.0, 1.0, 1.0, transparantBlue);

      YoFramePoint boxPosition = new YoFramePoint("boxPosition", "", worldFrame, registry);
      double boxSize = 0.3;
      boxPosition.set(boxSize / 2.0, boxSize / 2.0, boxSize / 2.0);
      YoFrameOrientation boxOrientation = new YoFrameOrientation("boxOrientation", worldFrame, registry);
      DynamicGraphicShape dynamicGraphicBoxGhost = new DynamicGraphicShape("boxGhost", boxGhostGraphics, boxPosition, boxOrientation, boxSize);

      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("Polygon");
      dynamicGraphicObjectsList.add(dynamicGraphicPolygon);
      dynamicGraphicObjectsList.add(dynamicGraphicText);
      dynamicGraphicObjectsList.add(dynamicGraphicVector);
      dynamicGraphicObjectsList.add(dynamicGraphicYoFramePolygon);
      dynamicGraphicObjectsList.add(dynamicGraphicBoxGhost);

      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);

      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      scs.addYoVariableRegistry(registry);

      Graphics3DObject coordinateSystem = new Graphics3DObject();
      coordinateSystem.addCoordinateSystem(1.0);
      scs.addStaticLinkGraphics(coordinateSystem);

      scs.startOnAThread();

      final double[][] secondPointList = new double[][]
      {
         {0.0, 0.0}, {2.0, 0.0}, {0.0, 2.0}
      };


      Runnable runnable = new Runnable()
      {
         public void run()
         {
            int i = 0;

            while (i++ < 20)
            {
               quickPause();

               ConvexPolygon2d newPolygon = new ConvexPolygon2d(secondPointList);
               dynamicGraphicPolygon.updateConvexPolygon2d(newPolygon);

               ConvexPolygon2d newYoPolygon = new ConvexPolygon2d(pointList);
               yoFramePolygon.setConvexPolygon2d(newYoPolygon);
               Vector3d eulerAngles = new Vector3d();
               yoFramePolygonOrientation.getEulerAngles(eulerAngles);
               eulerAngles.setY(eulerAngles.getY() + 0.1);
               yoFramePolygonOrientation.setEulerAngles(eulerAngles);
               
               dynamicGraphicText.setText("Hello");

               scs.tickAndUpdate();

               quickPause();
               newPolygon = new ConvexPolygon2d(pointList);
               dynamicGraphicPolygon.updateConvexPolygon2d(newPolygon);

               newYoPolygon = new ConvexPolygon2d(secondPointList);
               yoFramePolygon.setConvexPolygon2d(newYoPolygon);

               dynamicGraphicText.setText("GoodBye");

               scs.tickAndUpdate();

            }
         }

      };

      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      thread.start();
   }

   private static void quickPause()
   {
      try
      {
         Thread.sleep(200L);
      }
      catch (InterruptedException e)
      {
      }
   }
}
