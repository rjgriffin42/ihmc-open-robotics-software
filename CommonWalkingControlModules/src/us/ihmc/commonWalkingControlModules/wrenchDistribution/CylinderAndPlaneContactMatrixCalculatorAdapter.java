package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.screwTheory.Wrench;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;

/**
 * @author twan
 *         Date: 5/23/13
 */
public class CylinderAndPlaneContactMatrixCalculatorAdapter
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final CylinderAndPlaneContactMatrixCalculator cylinderAndPlaneContactMatrixCalculator;
   private final CylinderAndPlaneContactSpatialForceVectorCalculator cylinderAndPlaneContactSpatialForceVectorCalculator;
   private final ReferenceFrame centerOfMassFrame;
   private final Map<RigidBody, Wrench> wrenches = new LinkedHashMap<RigidBody, Wrench>();
   private final DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry;
   private final IntegerYoVariable planeContactIndex = new IntegerYoVariable("planeContactIndex", registry);
   
   private final DoubleYoVariable wRhoCylinderContacts = new DoubleYoVariable("wRhoCylinderContacts", registry);
   private final DoubleYoVariable wPhiCylinderContacts = new DoubleYoVariable("wPhiCylinderContacts", registry);
   private final DoubleYoVariable wRhoPlaneContacts = new DoubleYoVariable("wRhoPlaneContacts", registry);
   private final DoubleYoVariable wRhoSmoother = new DoubleYoVariable("wRhoSmoother", registry);
   private final DoubleYoVariable rhoMinScalar = new DoubleYoVariable("rhoMinScalarInAdapter", registry);
   
   private final List<? extends PlaneContactState> planeContactStates;
   private final List<? extends CylindricalContactState> cylindricalContactStates;
   private final List<EndEffector> planeEndEffector;
   private final List<EndEffector> cylindricalEndEffector;
   private final List<EndEffector> allEndEffectors;
   
   private final List<DynamicGraphicObject> endEffectorResultGraphics = new ArrayList<DynamicGraphicObject>();

   public CylinderAndPlaneContactMatrixCalculatorAdapter(ReferenceFrame centerOfMassFrame, int rhoSize, int phiSize, double wRhoCylinderContacts,
         double wPhiCylinderContacts, double wRhoPlaneContacts, double wRhoSmoother, Collection<? extends PlaneContactState> planeContactStates,
         Collection<? extends CylindricalContactState> cylindricalContactStates, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
         YoVariableRegistry parentRegistry)
   {
      this.centerOfMassFrame = centerOfMassFrame;
      
      this.wRhoCylinderContacts.set(wRhoCylinderContacts);
      this.wPhiCylinderContacts.set(wPhiCylinderContacts);
      this.wRhoPlaneContacts.set(wRhoPlaneContacts);
      this.wRhoSmoother.set(wRhoSmoother);
      
      allEndEffectors = new ArrayList<EndEffector>();

      if (planeContactStates != null)
      {
         this.planeContactStates = new ArrayList<PlaneContactState>(planeContactStates);
         planeEndEffector = new ArrayList<EndEffector>();
         populatePlaneEndEffectorMap();
      }
      else
      {
         this.planeContactStates = null;
         planeEndEffector = null;
      }

      if (cylindricalContactStates != null)
      {
         this.cylindricalContactStates = new ArrayList<CylindricalContactState>(cylindricalContactStates);
         cylindricalEndEffector = new ArrayList<EndEffector>();
         populateCylindricalEndEffectorMap();
      }
      else
      {
         this.cylindricalContactStates = null;
         cylindricalEndEffector = null;
      }
      
      
      cylinderAndPlaneContactMatrixCalculator = new CylinderAndPlaneContactMatrixCalculator(centerOfMassFrame, registry, dynamicGraphicObjectsListRegistry,
              rhoSize, phiSize);
      cylinderAndPlaneContactSpatialForceVectorCalculator = new CylinderAndPlaneContactSpatialForceVectorCalculator(centerOfMassFrame, registry,
              dynamicGraphicObjectsListRegistry, rhoSize, phiSize);
      
      
      this.dynamicGraphicObjectsListRegistry = dynamicGraphicObjectsListRegistry;
      parentRegistry.addChild(registry);
   }

   private void populatePlaneEndEffectorMap()
   {
      for (PlaneContactState contactState : this.planeContactStates)
      {
         EndEffector endEffector = EndEffector.fromPlane("" + planeContactIndex.getIntegerValue(), this.centerOfMassFrame, contactState,
               this.wRhoPlaneContacts.getDoubleValue(), rhoMinScalar.getDoubleValue(), registry);
         planeEndEffector.add(endEffector);
         allEndEffectors.add(endEffector);
         registerEndEffectorGraphic(endEffector);
         planeContactIndex.increment();
      }
   }

   private void populateCylindricalEndEffectorMap()
   {
      for (CylindricalContactState contactState : this.cylindricalContactStates)
      {
         EndEffector endEffector = EndEffector.fromCylinder(this.centerOfMassFrame, contactState, this.wRhoCylinderContacts.getDoubleValue(),
               this.wPhiCylinderContacts.getDoubleValue(), registry);
         cylindricalEndEffector.add(endEffector);
         allEndEffectors.add(endEffector);
         registerEndEffectorGraphic(endEffector);
      }
   }

   public void computeMatrices()
   {
      updateEndEffectors();

      cylinderAndPlaneContactMatrixCalculator.computeMatrices(allEndEffectors);
   }

   public Map<RigidBody, Wrench> computeWrenches(DenseMatrix64F rho, DenseMatrix64F phi)
   {
      cylinderAndPlaneContactSpatialForceVectorCalculator.setQRho(cylinderAndPlaneContactMatrixCalculator.getQRho());
      cylinderAndPlaneContactSpatialForceVectorCalculator.setQPhi(cylinderAndPlaneContactMatrixCalculator.getQPhi());
      cylinderAndPlaneContactSpatialForceVectorCalculator.computeWrenches(allEndEffectors, rho, phi);

      // TODO: garbage:
      wrenches.clear();

      for (int i = 0; i < planeEndEffector.size(); i++)
      {
         PlaneContactState planeContactState = planeContactStates.get(i);
         EndEffector endEffector = planeEndEffector.get(i);

         RigidBody rigidBody = planeContactState.getRigidBody();
         SpatialForceVector spatialForceVector = cylinderAndPlaneContactSpatialForceVectorCalculator.getSpatialForceVector(endEffector);
         Wrench newWrench = new Wrench(rigidBody.getBodyFixedFrame(), spatialForceVector.getExpressedInFrame(), spatialForceVector.getLinearPartCopy(),
               spatialForceVector.getAngularPartCopy());
         newWrench.changeFrame(rigidBody.getBodyFixedFrame());

         Wrench wrenchInMap = wrenches.get(rigidBody);
         if (wrenchInMap == null)
         {
            wrenches.put(rigidBody, newWrench);
         }
         else
         {
            wrenchInMap.add(newWrench);
         }
      }

      return wrenches;
   }
   
   private void updateEndEffectors()
   {
      if (planeContactStates != null)
      {
         for (int i = 0; i < planeEndEffector.size(); i++)
         {
            PlaneContactState planeContactState = planeContactStates.get(i);
            EndEffector endEffector = planeEndEffector.get(i);
            endEffector.updateFromPlane(planeContactState, wRhoPlaneContacts.getDoubleValue(), rhoMinScalar.getDoubleValue());                  
         }
      }

      if (cylindricalContactStates != null)
      {
         for (int i = 0; i < cylindricalContactStates.size(); i++)
         {
            CylindricalContactState cylindricalContactState = cylindricalContactStates.get(i);
            EndEffector endEffector = cylindricalEndEffector.get(i);
            endEffector.updateFromCylinder(cylindricalContactState, wRhoCylinderContacts.getDoubleValue(), wPhiCylinderContacts.getDoubleValue());
         }
      }
   }

   public void registerEndEffectorGraphic(EndEffector endEffector)
   {
      DynamicGraphicVector wrenchAngularVectorGraphic = endEffector.getOutput().getWrenchAngularVectorGraphic();
      DynamicGraphicVector wrenchLinearVectorGraphic = endEffector.getOutput().getWrenchLinearVectorGraphic();
      endEffectorResultGraphics.add(wrenchAngularVectorGraphic);
      endEffectorResultGraphics.add(wrenchLinearVectorGraphic);
      String listName = this.getClass().getSimpleName() + "EndEffectorResultGraphics";
      if (dynamicGraphicObjectsListRegistry != null)
      {
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject(listName, wrenchAngularVectorGraphic);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject(listName, wrenchLinearVectorGraphic);
      }
   }

   public DenseMatrix64F getWRho()
   {
      return cylinderAndPlaneContactMatrixCalculator.getWRho();
   }

   public DenseMatrix64F getWPhi()
   {
      return cylinderAndPlaneContactMatrixCalculator.getWPhi();
   }
   
   public void packWRhoSmoother(DenseMatrix64F wRhoSmootherToPack)
   {
      CommonOps.setIdentity(wRhoSmootherToPack);
      CommonOps.scale(wRhoSmoother.getDoubleValue(), wRhoSmootherToPack);
   }

   public void setRhoMinScalar(double rhoMinScalar)
   {
      this.rhoMinScalar.set(rhoMinScalar);
   }

   public DenseMatrix64F getRhoMin()
   {
      return cylinderAndPlaneContactMatrixCalculator.getRhoMin();
   }

   public DenseMatrix64F getQRho()
   {
      return cylinderAndPlaneContactMatrixCalculator.getQRho();
   }

   public DenseMatrix64F getPhiMin()
   {
      return cylinderAndPlaneContactMatrixCalculator.getPhiMin();
   }

   public DenseMatrix64F getPhiMax()
   {
      return cylinderAndPlaneContactMatrixCalculator.getPhiMax();
   }

   public DenseMatrix64F getQPhi()
   {
      return cylinderAndPlaneContactMatrixCalculator.getQPhi();
   }
}
