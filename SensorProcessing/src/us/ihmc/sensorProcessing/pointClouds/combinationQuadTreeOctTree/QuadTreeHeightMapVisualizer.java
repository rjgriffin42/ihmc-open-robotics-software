package us.ihmc.sensorProcessing.pointClouds.combinationQuadTreeOctTree;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.utilities.dataStructures.quadTree.Box;
import us.ihmc.utilities.dataStructures.quadTree.QuadTreeForGroundLeaf;
import us.ihmc.utilities.dataStructures.quadTree.QuadTreeForGroundNode;

public class QuadTreeHeightMapVisualizer
{
   public static Graphics3DNode drawHeightMap(QuadTreeHeightMapInterface heightMap, SimulationConstructionSet scs, double minX, double minY, double maxX, double maxY, double resolution)
   {
      AppearanceDefinition[] rainbow = YoAppearance.getStandardRoyGBivRainbow();

      Graphics3DObject heightMapGraphic = new Graphics3DObject();

      for (double x = minX; x<maxX; x = x + resolution)
      {
         for (double y = minY; y<maxY; y = y + resolution)
         {
            double z = heightMap.getHeightAtPoint(x, y);

            if (!Double.isNaN(z))
            {
               int index = (int) (z / resolution);
               index = index % rainbow.length;
               if (index < 0) index = index + rainbow.length;
               
               AppearanceDefinition appearance = rainbow[index];

               heightMapGraphic.identity();
               heightMapGraphic.translate(x, y, z - resolution/4.0);
               heightMapGraphic.addCube(resolution, resolution, resolution/4.0, appearance);
            }
         }
      }
      return scs.addStaticLinkGraphics(heightMapGraphic);
   }


   public static Graphics3DNode drawAllPointsInQuadTree(QuadTreeHeightMapInterface heightMap, double resolution, SimulationConstructionSet scs, AppearanceDefinition appearance)
   {
      if (heightMap instanceof QuadTreeForGroundHeightMap)
      {
         ArrayList<Point3d> points = new ArrayList<Point3d>();
         ((QuadTreeForGroundHeightMap) heightMap).getAllPoints(points);

         return drawPoints(scs, points, resolution, appearance);
      }
      return null;
   }

   public static Graphics3DNode drawPoints(SimulationConstructionSet scs, ArrayList<Point3d> points, double resolution, AppearanceDefinition appearance)
   {
      Graphics3DObject pointsInQuadTreeGraphic = new Graphics3DObject();

      for (Point3d point : points)
      {
         pointsInQuadTreeGraphic.identity();
         pointsInQuadTreeGraphic.translate(point);
         pointsInQuadTreeGraphic.addCube(resolution, resolution, resolution/4.0, appearance);
      }

      Graphics3DNode graphics3DNodeHandle = scs.addStaticLinkGraphics(pointsInQuadTreeGraphic);
      return graphics3DNodeHandle;
   }


   public static Graphics3DNode drawNodeBoundingBoxes(QuadTreeForGroundHeightMap heightMap, SimulationConstructionSet scs, double heightToDrawAt)
   {
      QuadTreeForGroundNode rootNode = heightMap.getRootNode();
      Graphics3DObject nodeBoundsGraphic = new Graphics3DObject();
      drawNodeBoundingBoxesRecursively(rootNode, nodeBoundsGraphic, 0, heightToDrawAt);

      Graphics3DNode graphics3DNodeHandle = scs.addStaticLinkGraphics(nodeBoundsGraphic);
      return graphics3DNodeHandle;
   }

   private static void drawNodeBoundingBoxesRecursively(QuadTreeForGroundNode node, Graphics3DObject nodeBoundsGraphic, int depth, double nodeZ)
   {
      AppearanceDefinition[] rainbow = YoAppearance.getStandardRoyGBivRainbow();

      Box bounds = node.getBounds();

      nodeBoundsGraphic.identity();

      if (node.hasChildren())
      {
         nodeBoundsGraphic.translate(bounds.centreX, bounds.centreY, nodeZ);
         nodeBoundsGraphic.addCube(0.9 * (bounds.maxX - bounds.minX), 0.9 * (bounds.maxY - bounds.minY), 0.002, rainbow[depth % rainbow.length]);
      }
      else
      {
         QuadTreeForGroundLeaf leaf = node.getLeaf();
         if (leaf != null)
         {
            Point3d averagePoint = leaf.getAveragePoint();
            nodeBoundsGraphic.translate(bounds.centreX, bounds.centreY, averagePoint.getZ());
            nodeBoundsGraphic.addCube(0.9 * (bounds.maxX - bounds.minX), 0.9 * (bounds.maxY - bounds.minY), 0.002, YoAppearance.Black());
         }
      }

      ArrayList<QuadTreeForGroundNode> children = new ArrayList<QuadTreeForGroundNode>();
      node.getChildrenNodes(children);

      for (QuadTreeForGroundNode child : children)
      {
         drawNodeBoundingBoxesRecursively(child, nodeBoundsGraphic, depth + 1, nodeZ + 0.01);
      }
   }
}
