package us.ihmc.avatar;

import java.util.List;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.conversion.VisualsConversionTools;
import us.ihmc.scs2.SimulationConstructionSet2;
import us.ihmc.scs2.definition.visual.VisualDefinition;
import us.ihmc.simulationConstructionSetTools.util.ground.MeshTerrainObject;
import us.ihmc.simulationConstructionSetTools.util.ground.MeshTerranObjectParameters;
import us.ihmc.simulationConstructionSetTools.util.ground.MeshTerranObjectParameters.ConvexDecomposition;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoInteger;

public class MeshTerrainObjectViewer
{
   private final YoInteger maxNoOfHulls;
   private final YoInteger maxNoOfVertices;
   private final YoInteger maxVoxelResolution;

   private final YoDouble maxVolumePercentError;
   private final YoDouble maxConcavity;

   private final YoBoolean showOriginalMeshGraphics;
   private final YoBoolean showDecomposedMeshGraphics;
   private final YoEnum<ConvexDecomposition> decompositionType; 
   
   private final YoBoolean updateVisuals;

   private MeshTerranObjectParameters parameters;
   private MeshTerrainObject meshTerrainObject;
   
   private static SimulationConstructionSet2 scs = null;
   private final YoRegistry registry = new YoRegistry("DecomopsitionParameters");

   private List<VisualDefinition> visuals = null;

   private String relativeFilePath;

   public MeshTerrainObjectViewer()
   {

      MeshTerranObjectParameters parameters = new MeshTerranObjectParameters();

      maxNoOfHulls = new YoInteger("maxNoOfHulls", registry);
      maxNoOfVertices = new YoInteger("maxNoOfVertices", registry);
      maxVoxelResolution = new YoInteger("maxVoxelResolution", registry);

      maxVolumePercentError = new YoDouble("MaxVolumePercentError", registry);
      maxConcavity = new YoDouble("MaxConcavity", registry);

      showOriginalMeshGraphics = new YoBoolean("ShowOriginalMeshGraphics", registry);
      showDecomposedMeshGraphics = new YoBoolean("ShowDecomposedMeshGraphics", registry);
      decompositionType = new YoEnum<>("DecompositionType", registry, ConvexDecomposition.class);
      
      updateVisuals = new YoBoolean("UpdateVisuals", registry);

      maxNoOfHulls.set(parameters.getMaxNoOfHulls());
      maxNoOfVertices.set(parameters.getMaxNoOfVertices());
      maxVoxelResolution.set(parameters.getVoxelResolution());

      maxVolumePercentError.set(parameters.getMaxVolumePercentError());
      maxConcavity.set(parameters.getMaxConvacity());

      showOriginalMeshGraphics.set(parameters.isShowUndecomposedMeshGraphics());
      showDecomposedMeshGraphics.set(parameters.isShowDecomposedMeshGraphics());
      decompositionType.set(parameters.getDecompositionType());
      
      scs = new SimulationConstructionSet2("MeshTerrainObjectViewer");
      scs.addRegistry(registry);

      this.relativeFilePath = "models/Stool/Stool.obj";
      this.parameters = new MeshTerranObjectParameters();

      updateParameters();
      makeMeshTerrainObject();
      updateGraphics();

      scs.startSimulationThread();
   }

   private void makeMeshTerrainObject()
   {
      RigidBodyTransform configuration = new RigidBodyTransform();
      configuration.setRotationEulerAndZeroTranslation(new Vector3D(0.0, 0.0, -Math.PI / 2.0));
      meshTerrainObject = new MeshTerrainObject(relativeFilePath, parameters);
   }

   private void updateGraphics()
   {
      if (visuals != null)
         scs.removeStaticVisuals(visuals);
      visuals = VisualsConversionTools.toVisualDefinitions(meshTerrainObject.getLinkGraphics());
      scs.addStaticVisuals(visuals);
   }

   private void updateParameters()
   {
      // TODO Auto-generated method stub
      this.parameters.setMaxNoOfHulls(maxNoOfHulls.getValue());
      this.parameters.setMaxNoOfVertices(maxNoOfVertices.getValue());
      this.parameters.setVoxelResolution(maxVoxelResolution.getValue());

      this.parameters.setMaxVolumePercentError(maxVolumePercentError.getValue());
      this.parameters.setMaxConvacity(maxConcavity.getValue());

      this.parameters.setShowDecomposedMeshGraphics(showDecomposedMeshGraphics.getValue());
      this.parameters.setShowUndecomposedMeshGraphics(showOriginalMeshGraphics.getValue());
      
      this.parameters.setDecompositionType(decompositionType.getValue());

   }

   public static void main(String[] args)
   {
      MeshTerrainObjectViewer viewer = new MeshTerrainObjectViewer();
      scs.addAfterPhysicsCallback(time ->
      {
         if (viewer.updateVisuals.getValue())
         {
            viewer.updateVisuals.set(false);
            viewer.updateParameters();
            viewer.makeMeshTerrainObject();
            viewer.updateGraphics();
         }
      });

      scs.initializeBufferSize(1000);
      scs.setRealTimeRateSimulation(true);
      scs.start(true, false, false);
      scs.simulate();
   }

}
