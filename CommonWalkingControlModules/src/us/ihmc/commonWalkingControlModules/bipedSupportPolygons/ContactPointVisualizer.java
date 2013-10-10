package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author twan
 *         Date: 5/28/13
 */
public class ContactPointVisualizer
{
   private final static ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final List<YoFramePoint> contactPointsWorld = new ArrayList<YoFramePoint>();
   private final List<DynamicGraphicPosition> dynamicGraphicPositions = new ArrayList<DynamicGraphicPosition>();
   private final List<DynamicGraphicVector> dynamicGraphicVectors = new ArrayList<DynamicGraphicVector>();
   private final List<YoFrameVector> normalVectors = new ArrayList<YoFrameVector>();
   private final double normalVectorScale = 0.1;
   private final int maxNumberOfDynamicGraphicPositions;
   private final Collection<? extends PlaneContactState> contactStates;

   public ContactPointVisualizer(Collection<? extends PlaneContactState> contactStates, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
         YoVariableRegistry parentRegistry)
   {
      this.contactStates = contactStates;
      int totalNumberOfContactPoints = 0;
      for (PlaneContactState contactState : contactStates)
         totalNumberOfContactPoints += contactState.getTotalNumberOfContactPoints();

      maxNumberOfDynamicGraphicPositions = totalNumberOfContactPoints;

      for (int i = 0; i < maxNumberOfDynamicGraphicPositions; i++)
      {
         YoFramePoint contactPointWorld = new YoFramePoint("contactPoint" + i, worldFrame, this.registry);
         contactPointsWorld.add(contactPointWorld);
         DynamicGraphicPosition dynamicGraphicPosition = contactPointWorld.createDynamicGraphicPosition("contactViz" + i, 0.01, YoAppearance.Crimson());
         dynamicGraphicPositions.add(dynamicGraphicPosition);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("contactPoints", dynamicGraphicPosition);

         YoFrameVector normalVector = new YoFrameVector("contactNormal" + i, worldFrame, registry);
         normalVectors.add(normalVector);
         DynamicGraphicVector dynamicGraphicVector = new DynamicGraphicVector("contactNormalViz" + i, contactPointWorld, normalVector, YoAppearance.Crimson());
         dynamicGraphicVectors.add(dynamicGraphicVector);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("contactPoints", dynamicGraphicVector);
      }
      parentRegistry.addChild(registry);
   }

   private final FramePoint tempFramePoint = new FramePoint(worldFrame);
   private final FrameVector tempFrameVector = new FrameVector(worldFrame);

   public void update()
   {
      int i = 0;
      for (PlaneContactState contactState : contactStates)
      {
         contactState.getContactNormalFrameVector(tempFrameVector);
         tempFrameVector.changeFrame(worldFrame);
         tempFrameVector.scale(normalVectorScale);

         for (ContactPoint contactPoint : contactState.getContactPoints())
         {
            updateContactPointDynamicGraphicObjects(i++, contactPoint);
         }
      }
   }

   private void updateContactPointDynamicGraphicObjects(int i, ContactPoint contactPoint)
   {
      if (contactPoint.isInContact())
      {
         tempFramePoint.setAndChangeFrame(contactPoint.getPosition());
         tempFramePoint.changeFrame(worldFrame);
         contactPointsWorld.get(i).set(tempFramePoint);
         normalVectors.get(i).set(tempFrameVector);

         dynamicGraphicPositions.get(i).showGraphicObject();
         dynamicGraphicVectors.get(i).showGraphicObject();
      }
      else
      {
         contactPointsWorld.get(i).setToNaN();
         normalVectors.get(i).set(Double.NaN, Double.NaN, Double.NaN);
         dynamicGraphicPositions.get(i).hideGraphicObject();
         dynamicGraphicVectors.get(i).hideGraphicObject();
      }

      dynamicGraphicPositions.get(i).update();
      dynamicGraphicVectors.get(i).update();
   }
}
