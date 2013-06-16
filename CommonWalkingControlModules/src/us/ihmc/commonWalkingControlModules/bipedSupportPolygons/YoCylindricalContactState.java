package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import us.ihmc.commonWalkingControlModules.wrenchDistribution.CylindricalContactState;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicReferenceFrame;

public class YoCylindricalContactState implements CylindricalContactState, ModifiableContactState
{
   private boolean VISUALIZE = false;
   
   private final YoVariableRegistry registry;
   private final ReferenceFrame frameAfterJoint;
   private final ReferenceFrame cylinderFrame;
   private final BooleanYoVariable inContact;
   private final DoubleYoVariable coefficientOfFriction;
   private final DoubleYoVariable tensileGripForce;
   private final DoubleYoVariable cylinderRadius;
   private final DoubleYoVariable halfHandWidth;
   private final DoubleYoVariable gripWeaknessFactor;
   private final DynamicGraphicReferenceFrame cylinderRefererenceFrameGraphic;

   // TODO: Probably get rid of that. Now, it is used for smooth unload/load transitions in the CarIngressEgressController.
   private final DoubleYoVariable wRho, wPhi;

   public YoCylindricalContactState(String namePrefix, ReferenceFrame frameAfterJoint, ReferenceFrame cylinderFrame, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());

      if (dynamicGraphicObjectsListRegistry == null)
      {
         VISUALIZE = false;
      }

      String name = "cyl_" + namePrefix;
      this.inContact = new BooleanYoVariable(name + "InContact", registry);
      this.coefficientOfFriction = new DoubleYoVariable(name + "CoefficientOfFriction", registry);
      this.tensileGripForce = new DoubleYoVariable(name + "TensileGripForce", registry);
      this.cylinderRadius = new DoubleYoVariable(name + "CylinderRadius", registry);
      this.halfHandWidth = new DoubleYoVariable(name + "halfHandWidth", registry);
      this.gripWeaknessFactor = new DoubleYoVariable(name + "gripWeaknessFactor", registry);
      this.frameAfterJoint = frameAfterJoint;
      this.cylinderFrame = cylinderFrame;
      
      this.wRho = new DoubleYoVariable(name + "_wRhoContactRegularization", registry);
      this.wPhi = new DoubleYoVariable(name + "_wPhiContactRegularization", registry);
      resetContactRegularization();

      parentRegistry.addChild(registry);
      
      
      if (VISUALIZE)
      {
         this.cylinderRefererenceFrameGraphic = new DynamicGraphicReferenceFrame(cylinderFrame, registry, 0.2);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("YoCylindricalContactState", cylinderRefererenceFrameGraphic);
      }
      else
      {
         cylinderRefererenceFrameGraphic = null;
      }
   }

   public void set(double coefficientOfFriction, ContactableCylinderBody contactableCylinderBody, boolean inContact)
   {
      set(coefficientOfFriction, contactableCylinderBody.getGripStrength(), contactableCylinderBody.getCylinderRadius(),
            contactableCylinderBody.getHalfHandWidth(), contactableCylinderBody.getGripWeaknessFactor(), inContact);
   }

   public void set(double coefficientOfFriction, double gripStrength, double cylinderRadius, double halfHandWidth, double gripWeaknessFactor, boolean inContact)
   {
      this.inContact.set(inContact);

      if (coefficientOfFriction < 0.0)
         throw new RuntimeException("coefficientOfFriction is negative: " + coefficientOfFriction);
      this.coefficientOfFriction.set(coefficientOfFriction);

      if (gripStrength < 0.0)
         throw new RuntimeException("gripStrength is negative: " + gripStrength);
      this.tensileGripForce.set(gripStrength);

      if (cylinderRadius < 0.0)
         throw new RuntimeException("cylinderRadius is negative: " + cylinderRadius);
      this.cylinderRadius.set(cylinderRadius);

      if (halfHandWidth < 0.0)
         throw new RuntimeException("halfHandWidth is negative: " + halfHandWidth);
      this.halfHandWidth.set(halfHandWidth);

      if (gripWeaknessFactor < 0.0)
         throw new RuntimeException("gripWeaknessFactor is negative: " + gripWeaknessFactor);
      if (gripWeaknessFactor > 1.0)
         throw new RuntimeException("gripWeaknessFactor is over 1, : " + gripWeaknessFactor);
      this.gripWeaknessFactor.set(gripWeaknessFactor);
      
      if (cylinderRefererenceFrameGraphic != null) cylinderRefererenceFrameGraphic.update();
   }

   public boolean isInContact()
   {
      this.cylinderFrame.update();
      //      cylinderRefererenceFrameGraphic.update();
      return inContact.getBooleanValue();
   }

   public double getCylinderRadius()
   {
      return this.cylinderRadius.getDoubleValue();
   }

   public double getHalfHandWidth()
   {
      return this.halfHandWidth.getDoubleValue();
   }

   public double getCoefficientOfFriction()
   {
      return this.coefficientOfFriction.getDoubleValue();
   }

   public double getTensileGripForce()
   {
      return this.tensileGripForce.getDoubleValue();
   }

   public double getGripWeaknessFactor()
   {
      return this.gripWeaknessFactor.getDoubleValue();
   }

   public ReferenceFrame getEndEffectorFrame()
   {
      return frameAfterJoint;
   }

   public void setInContact(boolean inContact)
   {
      this.inContact.set(inContact);
   }

   public ReferenceFrame getCylinderFrame()
   {
      return this.cylinderFrame;
   }

   public void clear()
   {
      inContact.set(false);
   }

   public void setRhoContactRegularization(double wRho)
   {
      this.wRho.set(wRho);
   }

   public double getRhoContactRegularization()
   {
      return wRho.getDoubleValue();
   }

   public void setPhiContactRegularization(double wPhi)
   {
      this.wPhi.set(wPhi);
   }

   public double getPhiContactRegularization()
   {
      return wPhi.getDoubleValue();
   }

   public void resetContactRegularization()
   {
      this.wRho.set(DEFAULT_WRHO);
      this.wPhi.set(DEFAULT_WPHI);
   }
}
