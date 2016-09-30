package us.ihmc.simulationconstructionset.physics.visualize;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.ExternalTorque;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.physics.CollisionShape;
import us.ihmc.simulationconstructionset.physics.collision.CollisionHandlerListener;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicVector;

public class DefaultCollisionVisualize implements CollisionHandlerListener
{
   private SimulationConstructionSet scs;

   private List<YoGraphicVector> activeRedYoGraphicVectors = new ArrayList<YoGraphicVector>();
   private List<YoGraphicVector> activeBlueYoGraphicVectors = new ArrayList<YoGraphicVector>();

   private int total;

   private YoVariableRegistry registry;

   private double impulseScale = 0.1;
   private double forceScale = 0.005;

   public DefaultCollisionVisualize(double forceScale, double impulseScale, SimulationConstructionSet scs, int numberOfVectorsToCreate)
   {
      this.forceScale = forceScale;
      this.impulseScale = impulseScale;
      init(scs, numberOfVectorsToCreate);
   }

   private void init(SimulationConstructionSet scs, int numberOfVectorsToCreate)
   {
      this.scs = scs;

      YoVariableRegistry registryRoot = scs.getRootRegistry();
      registry = new YoVariableRegistry("DefaultCollisionVisualize");
      registryRoot.addChild(registry);

      for (int i = 0; i < numberOfVectorsToCreate; i++)
      {
         addImpulseYoGraphicVector(i * 2, true, impulseScale);
         addImpulseYoGraphicVector(i * 2 + 1, false, impulseScale);
      }
   }

   public void callBeforeCollisionDetection()
   {
//      System.out.println("CallBeforeCollision");
//      for (int i = 0; i < total; i++)
//      {
//         setToInfinity(activeRedYoGraphicVectors.get(i));
//         setToInfinity(activeBlueYoGraphicVectors.get(i));
//      }

      total = 0;
   }

   public void collision(CollisionShape shapeA, CollisionShape shapeB, ExternalForcePoint forceA, ExternalForcePoint forceB, ExternalTorque torqueA,
         ExternalTorque torqueB)
   {
      if (total >= activeRedYoGraphicVectors.size())
         return;

      YoGraphicVector yoGraphicVectorA = activeRedYoGraphicVectors.get(total);
      YoGraphicVector yoGraphicVectorB = activeBlueYoGraphicVectors.get(total);

//      System.out.println(forceA);

//      yoGraphicVectorA.set(forceA.getYoPosition(), forceA.getYoForce());
//      yoGraphicVectorB.set(forceB.getYoPosition(), forceB.getYoForce());

//      System.out.println("Visualizing Collision. forceA = " + forceA);
//      System.out.println("Visualizing Collision. forceB = " + forceB);
//
//      System.out.println("Visualizing Collision. forceA.getYoPosition() = " + forceA.getYoPosition());
//      System.out.println("Visualizing Collision. forceA.getYoImpulse() = " + forceA.getYoImpulse());
//      System.out.println("Visualizing Collision. forceB.getYoPosition() = " + forceB.getYoPosition());
//      System.out.println("Visualizing Collision. forceB.getYoImpulse() = " + forceB.getYoImpulse());

      yoGraphicVectorA.set(forceA.getYoPosition(), forceA.getYoImpulse());
      yoGraphicVectorB.set(forceB.getYoPosition(), forceB.getYoImpulse());
      total++;
   }

   public void addImpulseYoGraphicVector(int num, boolean isRed, double scale)
   {
      List<YoGraphicVector> active = isRed ? activeRedYoGraphicVectors : activeBlueYoGraphicVectors;

      DoubleYoVariable yo0 = new DoubleYoVariable("locX_" + num, registry);
      DoubleYoVariable yo1 = new DoubleYoVariable("locY_" + num, registry);
      DoubleYoVariable yo2 = new DoubleYoVariable("locZ_" + num, registry);
      DoubleYoVariable yo3 = new DoubleYoVariable("vecX_" + num, registry);
      DoubleYoVariable yo4 = new DoubleYoVariable("vecY_" + num, registry);
      DoubleYoVariable yo5 = new DoubleYoVariable("vecZ_" + num, registry);

      String name = "ContactVisualized" + num;
      AppearanceDefinition appearance = isRed ? YoAppearance.Red() : YoAppearance.Blue();
      YoGraphicVector yoGraphicVector = new YoGraphicVector(name, yo0, yo1, yo2, yo3, yo4, yo5, scale, appearance);
      scs.addYoGraphic(yoGraphicVector, true);
      active.add(yoGraphicVector);
   }

   public void setToInfinity(YoGraphicVector yoGraphicVector)
   {
      double largeNumber = 1000000;
      yoGraphicVector.set(largeNumber, largeNumber, largeNumber, 0, 0, 0);
   }

   public void setForceScale(double forceScale)
   {
      this.forceScale = forceScale;
   }
}
