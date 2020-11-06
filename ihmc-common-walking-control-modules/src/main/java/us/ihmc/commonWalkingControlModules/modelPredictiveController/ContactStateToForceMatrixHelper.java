package us.ihmc.commonWalkingControlModules.modelPredictiveController;

import org.ejml.data.DMatrixRMaj;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoContactPoint;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.FrictionConeRotationCalculator;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.SpatialForce;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;

import java.util.List;

public class ContactStateToForceMatrixHelper
{
   private final int maxNumberOfContactPoints;
   private final int numberOfBasisVectorsPerContactPoint;
   private final int rhoSize;
   private final FrameVector3D[] basisVectors;
   private final FramePoint3D[] basisVectorsOrigin;
   private final double basisVectorAngleIncrement;
   private final PoseReferenceFrame planeFrame;

   private final DMatrixRMaj rhoMaxMatrix;
   private final DMatrixRMaj rhoWeightMatrix;
   private final DMatrixRMaj rhoRateWeightMatrix;

   private final DMatrixRMaj maxContactForces;

   private final DMatrixRMaj wrenchJacobianInWorldFrame;
   private final RotationMatrix normalContactVectorRotationMatrix = new RotationMatrix();
   private final FrictionConeRotationCalculator coneRotationCalculator;

   public ContactStateToForceMatrixHelper(int maxNumberOfContactPoints,
                                          int numberOfBasisVectorsPerContactPoint,
                                          FrictionConeRotationCalculator coneRotationCalculator)
   {
      this.maxNumberOfContactPoints = maxNumberOfContactPoints;
      this.numberOfBasisVectorsPerContactPoint = numberOfBasisVectorsPerContactPoint;
      this.coneRotationCalculator = coneRotationCalculator;

      rhoSize = maxNumberOfContactPoints * numberOfBasisVectorsPerContactPoint;
      basisVectorAngleIncrement = 2.0 * Math.PI / numberOfBasisVectorsPerContactPoint;

      rhoMaxMatrix = new DMatrixRMaj(rhoSize, 1);
      rhoWeightMatrix = new DMatrixRMaj(rhoSize, rhoSize);
      rhoRateWeightMatrix = new DMatrixRMaj(rhoSize, rhoSize);

      maxContactForces = new DMatrixRMaj(maxNumberOfContactPoints, 1);
      for (int i = 0; i < maxNumberOfContactPoints; i++)
         maxContactForces.set(i, 0, Double.POSITIVE_INFINITY);

      basisVectors = new FrameVector3D[rhoSize];
      basisVectorsOrigin = new FramePoint3D[rhoSize];
      planeFrame = new PoseReferenceFrame("ContactFrame", ReferenceFrame.getWorldFrame());

      wrenchJacobianInWorldFrame = new DMatrixRMaj(Wrench.SIZE, rhoSize);

      for (int i = 0; i < rhoSize; i++)
      {
         basisVectors[i] = new FrameVector3D(ReferenceFrame.getWorldFrame());
         basisVectorsOrigin[i] = new FramePoint3D(ReferenceFrame.getWorldFrame());
      }
   }

   public int getRhoSize()
   {
      return rhoSize;
   }

   public FrameVector3DReadOnly getBasisVector(int index)
   {
      return basisVectors[index];
   }

   public FramePoint3DReadOnly getBasisVectorOrigin(int index)
   {
      return basisVectorsOrigin[index];
   }

   Point3D point = new Point3D();

   public void computeMatrices(ConvexPolygon2DReadOnly contactPointsInPlaneFrame,
                               FramePose3DReadOnly framePose,
                               double rhoWeight,
                               double rhoRateWeight,
                               double mu)
   {
      int numberOfContactPointsInContact = contactPointsInPlaneFrame.getNumberOfVertices();
      if (numberOfContactPointsInContact > maxNumberOfContactPoints)
         throw new RuntimeException("Unhandled number of contact points: " + numberOfContactPointsInContact);

      planeFrame.setPoseAndUpdate(framePose);

      // Compute the orientation of the normal contact vector and the corresponding transformation matrix
      computeNormalContactVectorRotation(normalContactVectorRotationMatrix);

      int rhoIndex = 0;

      for (int contactPointIndex = 0; contactPointIndex < numberOfContactPointsInContact; contactPointIndex++)
      {
         Point2DReadOnly contactPoint = contactPointsInPlaneFrame.getVertex(contactPointIndex);

         // rotate each friction cone approximation to point one vector towards the center of the foot
         point.set(contactPointsInPlaneFrame.getVertex(contactPointIndex));
         double angleOffset = coneRotationCalculator.computeConeRotation(contactPointsInPlaneFrame, point);

         for (int basisVectorIndex = 0; basisVectorIndex < numberOfBasisVectorsPerContactPoint; basisVectorIndex++)
         {
            FramePoint3D basisVectorOrigin = basisVectorsOrigin[rhoIndex];
            FrameVector3D basisVector = basisVectors[rhoIndex];

            basisVectorOrigin.setIncludingFrame(planeFrame, contactPoint, 0.0);
            computeBasisVector(basisVectorIndex, angleOffset, normalContactVectorRotationMatrix, basisVector, mu);

            rhoWeightMatrix.set(rhoIndex, rhoIndex, rhoWeight * maxNumberOfContactPoints / numberOfContactPointsInContact);
            rhoRateWeightMatrix.set(rhoIndex, rhoIndex, rhoRateWeight);
            rhoMaxMatrix.set(rhoIndex, 0, basisVector.getZ() * maxContactForces.get(contactPointIndex) / numberOfBasisVectorsPerContactPoint);

            rhoIndex++;
         }
      }

      computeWrenchJacobianInFrame(ReferenceFrame.getWorldFrame(), wrenchJacobianInWorldFrame);

      // Should not get there as long as the number of contact points of the contactable body is less or equal to maxNumberOfContactPoints.
      for (; rhoIndex < rhoSize; rhoIndex++)
         clear(rhoIndex);
   }

   private final FrameVector3D contactNormalVector = new FrameVector3D();

   private void computeNormalContactVectorRotation(RotationMatrix normalContactVectorRotationMatrixToPack)
   {
      contactNormalVector.setIncludingFrame(planeFrame, 0.0, 0.0, 1.0);
      EuclidGeometryTools.orientation3DFromZUpToVector3D(contactNormalVector, normalContactVectorRotationMatrixToPack);
   }

   private void clear(int rhoIndex)
   {
      FramePoint3D basisVectorOrigin = basisVectorsOrigin[rhoIndex];
      FrameVector3D basisVector = basisVectors[rhoIndex];

      basisVectorOrigin.setToZero(ReferenceFrame.getWorldFrame());
      basisVector.setToZero(ReferenceFrame.getWorldFrame());

      rhoMaxMatrix.set(rhoIndex, 0, Double.POSITIVE_INFINITY);
      rhoWeightMatrix.set(rhoIndex, rhoIndex, 0.0);
      rhoRateWeightMatrix.set(rhoIndex, rhoIndex, 0.0);
   }

   private void computeBasisVector(int basisVectorIndex,
                                   double rotationOffset,
                                   RotationMatrix normalContactVectorRotationMatrix,
                                   FrameVector3D basisVectorToPack,
                                   double mu)
   {
      double angle = rotationOffset + basisVectorIndex * basisVectorAngleIncrement;

      // Compute the linear part considering a normal contact vector pointing z-up
      basisVectorToPack.setIncludingFrame(planeFrame, Math.cos(angle) * mu, Math.sin(angle) * mu, 1.0);

      // Transforming the result to consider the actual normal contact vector
      normalContactVectorRotationMatrix.transform(basisVectorToPack);
      basisVectorToPack.normalize();
   }

   private final SpatialForce unitSpatialForceVector = new SpatialForce();

   public void computeWrenchJacobianInFrame(ReferenceFrame frame, DMatrixRMaj matrixToPack)
   {
      matrixToPack.reshape(Wrench.SIZE, rhoSize);
      for (int rhoIndex = 0; rhoIndex < rhoSize; rhoIndex++)
      {
         FramePoint3D basisVectorOrigin = basisVectorsOrigin[rhoIndex];
         FrameVector3D basisVector = basisVectors[rhoIndex];
         basisVectorOrigin.changeFrame(frame);
         basisVector.changeFrame(frame);
         unitSpatialForceVector.setIncludingFrame(null, basisVector, basisVectorOrigin);
         unitSpatialForceVector.get(0, rhoIndex, matrixToPack);
      }
   }
}
