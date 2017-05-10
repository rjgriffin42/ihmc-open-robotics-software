package us.ihmc.robotics.screwTheory;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixDimensionException;

import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

/**
 * The {@code WeightMatrix3D} provides a simple way to define weights in a particular frame, which typically make up the main diagonal of a matrix. 
 * <p>
 * Given the set of weights about particular axis of interest and a reference frame to which these axes refer to, the
 * {@code WeightMatrix3D} is then able compute the corresponding 6-by-6 selection matrix.
 * </p>
 * <p>
 * The principal use-case of this class is to help users define the QP weights of each axis for a given end-effector and what frame they should be 
 * expressed in. 
 * </p>
 * <p>
 * Note that the {@link #selectionFrame} is optional. It is preferable to provide it when possible,
 * but when it is absent, i.e. equal to {@code null}, the weight matrix will then be generated
 * assuming the destination frame is the same as the selection frame.
 * </p>
 */
public class WeightMatrix6D
{
   /**
    * The 3-by-3 selection matrix A<sub>3x3</sub> for the angular part of this weight matrix.
    */
   private final WeightMatrix3D angularWeights = new WeightMatrix3D();
   /**
    * The 3-by-3 selection matrix L<sub>3x3</sub> for the linear part of this weight matrix.
    */
   private final WeightMatrix3D linearWeights = new WeightMatrix3D();

   /**
    * Creates a new weight matrix. This weight matrix is initialized with all the weights set to NAN. Until the weights are changed, this weight matrix is independent from its
    * selection frame.
    */
   public WeightMatrix6D()
   {
   }

   /**
    * Copy constructor.
    * 
    * @param other the selection matrix to copy. Not modified.
    */
   public WeightMatrix6D(WeightMatrix6D other)
   {
      set(other);
   }

   /**
    * Sets the selection frame of both the angular and linear parts to {@code null}.
    * <p>
    * When the selection frame is {@code null}, the conversion into a 6-by-6 weight matrix will
    * be done regardless of the destination frame.
    * </p>
    */
   public void clearSelectionFrame()
   {
      setSelectionFrame(null);
   }

   /**
    * Sets the selection frame of the angular part to {@code null}.
    * <p>
    * When the selection frame is {@code null}, the conversion into a weight matrix will be done
    * regardless of the destination frame.
    * </p>
    * <p>
    * The linear part of this weight matrix remains unchanged.
    * </p>
    */
   public void clearAngularSelectionFrame()
   {
      angularWeights.clearSelectionFrame();
   }

   /**
    * Sets the selection frame of the linear part to {@code null}.
    * <p>
    * When the selection frame is {@code null}, the conversion into a weight matrix will be done
    * regardless of the destination frame.
    * </p>
    * <p>
    * The angular part of this weight matrix remains unchanged.
    * </p>
    */
   public void clearLinearSelectionFrame()
   {
      linearWeights.clearSelectionFrame();
   }

   /**
    * set the frame the linear and angular weights are expressed in 
    * 
    * @param selectionFrame the new frame to which the weights are expressed in 
    */
   public void setSelectionFrame(ReferenceFrame selectionFrame)
   {
      setSelectionFrames(selectionFrame, selectionFrame);
   }

   /**
    * Sets the selection frame for the angular and linear parts separately.
    * 
    * @param angularSelectionFrame the new frame to which the angular weights are referring to.
    * @param linearSelectionFrame the new frame to which the linear  weights are referring to.
    */
   public void setSelectionFrames(ReferenceFrame angularSelectionFrame, ReferenceFrame linearSelectionFrame)
   {
      angularWeights.setSelectionFrame(angularSelectionFrame);
      linearWeights.setSelectionFrame(linearSelectionFrame);
   }

   /**
    * sets the linear and angular weights to NAN and sets the selection frame to null
    * <p>
    * Until the selection is changed, this weight matrix is independent from its selection frame.
    * </p>
    */
   public void clear()
   {
      angularWeights.clear();
      linearWeights.clear();
   }

   /**
    * Sets all the angular weights to NAN and clears the angular selection frame.
    * <p>
    * The linear part remains unmodified.
    * </p>
    */
   public void clearAngularWeights()
   {
      angularWeights.clear();
   }

   /**
    * Sets all the linear weights to NAN and clears the angular selection frame.
    * <p>
    * The angular part remains unmodified.
    * </p>
    */
   public void clearLinearWeights()
   {
      linearWeights.clear();
   }

   /**
    * Sets this weight matrix to {@code other}.
    * 
    * @param other the other weight matrix. Not modified.
    */
   public void set(WeightMatrix6D other)
   {
      angularWeights.set(other.angularWeights);
      linearWeights.set(other.linearWeights);
   }

   /**
    * Sets the angular part of this weight matrix to {@code angularPart}.
    * 
    * @param angularPart the new value for the angular part of this weight matrix. Not modified.
    */
   public void setAngularPart(WeightMatrix3D angularPart)
   {
      this.angularWeights.set(angularPart);
   }

   /**
    * Sets the linear part of this weight matrix to {@code linearPart}.
    * 
    * @param linearPart the new value for the linear part of this weight matrix. Not modified.
    */
   public void setLinearPart(WeightMatrix3D linearPart)
   {
      this.linearWeights.set(linearPart);
   }

   
   /**
    * sets the angular weights
    * 
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param angularXWeight angular weight of the x axis
    * @param angularYWeight angular weight of the y axis
    * @param angularZWeight angular weight of the z axis
    */
  public void setAngularWeights(double angularXWeight, double angularYWeight, double angularZWeight)
  {
     setAngularXAxisWeight(angularXWeight);
     setAngularYAxisWeight(angularYWeight);
     setAngularZAxisWeight(angularZWeight);
  }


   /**
    * sets the angular x axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the x axis weight.
    */
   public void setAngularXAxisWeight(double weight)
   {
      angularWeights.setXAxisWeight(weight);
   }

   /**
    * sets the angular y axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the y axis weight.
    */
   public void setAngularYAxisWeight(double weight)
   {
      angularWeights.setYAxisWeight(weight);
   }

   /**
    * sets the angular z axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the z axis weight.
    */
   public void setAngularZAxisWeight(double weight)
   {
      angularWeights.setZAxisWeight(weight);
   }
   /**
    * sets the linear weights
    * 
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param angularXWeight linear weight of the x axis
    * @param angularYWeight linear weight of the y axis
    * @param angularZWeight linear weight of the z axis
    */
   public void setLinearWeights(double linearXWeight, double linearYWeight, double linearZWeight)
   {
      setLinearXAxisWeight(linearXWeight);
      setLinearYAxisWeight(linearYWeight);
      setLinearZAxisWeight(linearZWeight);
   }

   /**
    * sets the linear x axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the x axis weight.
    */
   public void setLinearXAxisWeight(double weight)
   {
      linearWeights.setXAxisWeight(weight);
   }

   /**
    * sets the linear y axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the y axis weight.
    */
   public void setLinearYAxisWeight(double weight)
   {
      linearWeights.setYAxisWeight(weight);
   }

   /**
    * sets the linear z axis weight
    * <p>
    * Note that it is preferable to also set selection frame to which this selection is referring
    * to.
    * </p>
    * 
    * @param the z axis weight.
    */
   public void setLinearZAxisWeight(double weight)
   {
      linearWeights.setZAxisWeight(weight);
   }

   /**
    * Converts this into an actual 6-by-6 weight matrix that is to be used with data expressed in
    * the {@code destinationFrame}.
    * <p>
    * The given {@code selectionMatrixToPack} is first reshaped to be a 6-by-6 matrix which will be
    * set to represent this.
    * </p>
    * 
    * @param destinationFrame the reference frame in which the weight matrix is to be used.
    * @param weightMatrixToPack the dense-matrix is first reshaped to a 6-by-6 matrix and then
    *           set to represent this. Modified.
    * @throws MatrixDimensionException if the given matrix is too small.
    */
   public void getFullWeightMatrixInFrame(ReferenceFrame destinationFrame, DenseMatrix64F weightMatrixToPack)
   {
      weightMatrixToPack.reshape(6, 6);
      weightMatrixToPack.zero();

      angularWeights.getFullWeightMatrixInFrame(destinationFrame, 0, 0, weightMatrixToPack);
      linearWeights.getFullWeightMatrixInFrame(destinationFrame, 3, 3, weightMatrixToPack);
   }

   /**
    * Converts this into an actual 6-by-6 weight matrix that is to be used with data expressed in
    * the {@code destinationFrame}.
    * <p>
    * In addition to what {@link #getFullWeightMatrixInFrame(ReferenceFrame, DenseMatrix64F)}
    * does, this method also removes the zero-rows of the given weight matrix. This will help to
    * improve performance especially when the resulting weight matrix is to be multiplied to a
    * large matrix.
    * </p>
    * <p>
    * The given {@code selectionMatrixToPack} is first reshaped to be a 6-by-6 matrix which will be
    * set to represent this.
    * </p>
    * 
    * @param destinationFrame the reference frame in which the selection matrix is to be used.
    * @param weightMatrixToPack the dense-matrix is first reshaped to a 6-by-6 matrix and then
    *           set to represent this. The zero-rows of the resulting matrix are removed. Modified.
    * @throws MatrixDimensionException if the given matrix is too small.
    */
   public void getCompactSelectionMatrixInFrame(ReferenceFrame destinationFrame, DenseMatrix64F weightMatrixToPack)
   {
      weightMatrixToPack.reshape(6, 6);
      weightMatrixToPack.zero();

      // Need to do the linear part first as rows might be removed.
      linearWeights.getCompactSelectionMatrixInFrame(destinationFrame, 3, 3, weightMatrixToPack);
      angularWeights.getCompactSelectionMatrixInFrame(destinationFrame, 0, 0, weightMatrixToPack);
   }

   /**
    * The reference frame to which the angular weights are referring.
    * <p>
    * This selection frame can be {@code null}.
    * </p>
    * 
    * @return the current selection frame for the angular part of this weight matrix.
    */
   public ReferenceFrame getAngularSelectionFrame()
   {
      return angularWeights.getSelectionFrame();
   }

   /**
    * The reference frame to which the linear weights are referring.
    * <p>
    * This selection frame can be {@code null}.
    * </p>
    * 
    * @return the current selection frame for the linear part of this weight matrix.
    */
   public ReferenceFrame getLinearSelectionFrame()
   {
      return linearWeights.getSelectionFrame();
   }

   /**
    * Returns the internal reference to the angular part of this weight matrix.
    * 
    * @return the angular part.
    */
   public WeightMatrix3D getAngularPart()
   {
      return angularWeights;
   }

   /**
    * Returns the internal reference to the linear part of this weight matrix.
    * 
    * @return the linear part.
    */
   public WeightMatrix3D getLinearPart()
   {
      return linearWeights;
   }
   
   @Override
   public String toString()
   {
      return "Angular: " + angularWeights + ", linear: " + linearWeights;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((angularWeights == null) ? 0 : angularWeights.hashCode());
      result = prime * result + ((linearWeights == null) ? 0 : linearWeights.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      WeightMatrix6D other = (WeightMatrix6D) obj;
      if (angularWeights == null)
      {
         if (other.angularWeights != null)
            return false;
      }
      else if (!angularWeights.equals(other.angularWeights))
         return false;
      if (linearWeights == null)
      {
         if (other.linearWeights != null)
            return false;
      }
      else if (!linearWeights.equals(other.linearWeights))
         return false;
      return true;
   }

   public void setLinearWeights(Vector3D linearWeight)
   {
      linearWeights.setWeights(linearWeight.getX(), linearWeight.getY(), linearWeight.getZ());
   }

   public void setAngularWeights(Vector3D angularWeight)
   {
      angularWeights.setWeights(angularWeight.getX(), angularWeight.getY(), angularWeight.getZ());
   }
}
