/*
 * Copyright (c) 2013-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Project BUBO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.ihmc.sensorProcessing.bubo.clouds.detect.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.RansacMulti;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;

import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Box3D_F64;
import us.ihmc.sensorProcessing.bubo.clouds.detect.CloudShapeTypes;
import us.ihmc.sensorProcessing.bubo.clouds.detect.PointCloudShapeFinder;
import us.ihmc.sensorProcessing.bubo.clouds.detect.alg.ApproximateSurfaceNormals;
import us.ihmc.sensorProcessing.bubo.clouds.detect.alg.PointVectorNN;

/**
 * Uses RANSAC to find a best fit shape using the entire point cloud. Approximate normals are first
 * computed.
 *
 * @author Peter Abeles
 */
public class Ransac_to_PointCloudShapeFinder implements PointCloudShapeFinder
{

   RansacMulti<PointVectorNN> ransac;
   ApproximateSurfaceNormals surfaceNormals;
   List<CloudShapeTypes> shapeList;

   FastArray<PointVectorNN> pointNormList = new FastArray<PointVectorNN>(PointVectorNN.class);
   // reference to cloud data
   List<Point3D_F64> cloud;
   // mark which points are inliers and which are not
   GrowQueue_B marks = new GrowQueue_B();
   // storage for the matched shape
   FastQueue<Shape> output = new FastQueue<Shape>(Shape::new);
   // optimizes the fit parameters to the inlier set
   List<ModelFitter<Object, PointVectorNN>> fitters;
   // storage for optimized parameters
   List<Object> models = new ArrayList<Object>();
   // the minimum number of points a shape needs for it to be accepted
   private int minimumPoints;

   /**
    * Specifies internal algorithms.
    *
    * @param surfaceNormals Algorithm used to compute surface normals
    * @param ransac         RANSAC configured with the models its matching
    * @param minimumPoints  The minimum number of points it will need to match
    * @param shapeList      List of shapes matching the RANSAC configuration
    */
   public Ransac_to_PointCloudShapeFinder(ApproximateSurfaceNormals surfaceNormals, RansacMulti<PointVectorNN> ransac, List<ModelManager> modelManagers,
                                          List<ModelFitter<Object, PointVectorNN>> fitters, int minimumPoints, List<CloudShapeTypes> shapeList)
   {
      this.surfaceNormals = surfaceNormals;
      this.ransac = ransac;
      this.fitters = fitters;
      this.minimumPoints = minimumPoints;
      this.shapeList = shapeList;

      for (int i = 0; i < modelManagers.size(); i++)
      {
         models.add(modelManagers.get(i).createModelInstance());
      }
   }

   @Override
   public void process(List<Point3D_F64> cloud, Box3D_F64 boundingBox)
   {
      this.cloud = cloud;
      output.reset();
      pointNormList.reset();
      surfaceNormals.process(cloud, pointNormList);

      // run ransac and if it failed just give up
      if (!ransac.process(pointNormList.toList()))
         return;

      List<PointVectorNN> inliers = ransac.getMatchSet();
      if (inliers.size() < minimumPoints)
         return;

      ModelFitter<Object, PointVectorNN> fitter = fitters.get(ransac.getModelIndex());
      Object shapeParam = models.get(ransac.getModelIndex());

      fitter.fitModel(inliers, ransac.getModelParameters(), shapeParam);

      // convert the results into output format
      Shape os = output.grow();
      os.parameters = shapeParam;
      os.type = shapeList.get(ransac.getModelIndex());
      os.points.clear();
      os.indexes.reset();

      // add the points to it
      for (int j = 0; j < inliers.size(); j++)
      {
         PointVectorNN pv = inliers.get(j);
         os.points.add(pv.p);
         os.indexes.add(pv.index);
      }
   }

   @Override
   public List<Shape> getFound()
   {
      return output.toList();
   }

   @Override
   public void getUnmatched(List<Point3D_F64> unmatched)
   {
      marks.resize(cloud.size());
      for (int i = 0; i < cloud.size(); i++)
      {
         marks.data[i] = false;
      }

      List<PointVectorNN> inliers = ransac.getMatchSet();
      for (int j = 0; j < inliers.size(); j++)
      {
         PointVectorNN pv = inliers.get(j);
         marks.data[pv.index] = true;
      }

      for (int i = 0; i < cloud.size(); i++)
      {
         if (!marks.data[i])
         {
            unmatched.add(cloud.get(i));
         }
      }
   }

   @Override
   public List<CloudShapeTypes> getShapesList()
   {
      return shapeList;
   }

   @Override
   public boolean isSupportMultipleObjects()
   {
      return false;
   }
}
