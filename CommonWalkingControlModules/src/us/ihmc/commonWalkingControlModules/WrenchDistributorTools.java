package us.ihmc.commonWalkingControlModules;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Wrench;

public class WrenchDistributorTools
{
   public static void computeWrench(Wrench groundReactionWrenchToPack, FrameVector force, FramePoint2d cop, double normalTorque)
   {
      ReferenceFrame referenceFrame = cop.getReferenceFrame();
      force.changeFrame(referenceFrame);

      // r x f + tauN
      Vector3d torque = new Vector3d();
      torque.setX(cop.getY() * force.getZ());
      torque.setY(-cop.getX() * force.getZ());
      torque.setZ(cop.getX() * force.getY() - cop.getY() * force.getX() + normalTorque);

      groundReactionWrenchToPack.set(referenceFrame, force.getVector(), torque);
   }

   public static FramePoint computePseudoCMP3d(FramePoint centerOfMass, FramePoint2d cmp, double fZ, double totalMass, double omega0)
   {
      double zCMP = centerOfMass.getZ() - fZ / (totalMass * MathTools.square(omega0));
      FramePoint ret = cmp.toFramePoint();
      ret.changeFrame(centerOfMass.getReferenceFrame());
      ret.setZ(zCMP);
      return ret;
   }

   public static FrameVector computeForce(FramePoint centerOfMass, FramePoint cmp, double fZ)
   {
      cmp.changeFrame(centerOfMass.getReferenceFrame());
      FrameVector force = new FrameVector(centerOfMass);
      force.sub(cmp);
      force.scale(fZ / force.getZ());

      return force;
   }

   public static void getSupportVectors(List<FrameVector> normalizedSupportVectorsToPack, double mu, ReferenceFrame contactPlaneFrame)
   {
      int numberOfSupportVectors = normalizedSupportVectorsToPack.size();
      double angleIncrement = 2.0 * Math.PI / ((double) numberOfSupportVectors);

      for (int i = 0; i < numberOfSupportVectors; i++)
      {
         double angle = i * angleIncrement;
         getSupportVector(normalizedSupportVectorsToPack.get(i), angle, mu, contactPlaneFrame);
      }
   }

   public static void getSupportVector(FrameVector normalizedSupportVectorToPack, double angle, double mu, ReferenceFrame contactPlaneFrame)
   {
      double x = mu * Math.cos(angle);
      double y = mu * Math.sin(angle);
      double z = 1.0;

      normalizedSupportVectorToPack.set(contactPlaneFrame, x, y, z);
      normalizedSupportVectorToPack.normalize();
   }

   public static void computeSupportVectorMatrixBlock(DenseMatrix64F supportVectorMatrixBlock, ArrayList<FrameVector> normalizedSupportVectors,
           ReferenceFrame referenceFrame)
   {
      for (int i = 0; i < normalizedSupportVectors.size(); i++)
      {
         FrameVector normalizedSupportVector = normalizedSupportVectors.get(i);
         normalizedSupportVector.changeFrame(referenceFrame);
         MatrixTools.setDenseMatrixFromTuple3d(supportVectorMatrixBlock, normalizedSupportVector.getVector(), 0, i);
      }
   }

   public static double computeFz(double totalMass, double gravityZ, double centerOfMassHeightAcceleration)
   {
      double fZ = totalMass * (gravityZ + centerOfMassHeightAcceleration);
      return fZ;
   }
}
