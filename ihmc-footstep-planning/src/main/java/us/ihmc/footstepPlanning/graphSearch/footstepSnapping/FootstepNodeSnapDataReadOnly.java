package us.ihmc.footstepPlanning.graphSearch.footstepSnapping;

import us.ihmc.commonWalkingControlModules.polygonWiggling.GradientDescentStepConstraintSolver;
import us.ihmc.commonWalkingControlModules.polygonWiggling.PolygonWiggler;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.polygonSnapping.PlanarRegionsListPolygonSnapper;

public interface FootstepNodeSnapDataReadOnly
{
   /**
    * Snap transform describing the projection of a 2D step onto a planar region.
    * The relationship between the unsnapped planar footstep transform T_planar, snapped step transform T_snapped and this transform S is:
    * <br>
    * T_snapped = S * T_planar
    *
    * <br>
    * {@link FootstepNodeTools#getSnappedNodeTransform}
    * {@link PlanarRegionsListPolygonSnapper#snapPolygonToPlanarRegionsList}
    */
   RigidBodyTransformReadOnly getSnapTransform();

   /**
    * Transform for wiggling a step inside a planar region.
    * The relationship between the unsnapped planar footstep transform T_planar, snapped step transform T_snapped, snap transform S,
    * the planar region's transform from local to world P, the wiggler solver output W_local, and this transform W_world:
    * <br>
    * T_snapped = (P * W_local * P_inv) * S * T_planar = W_world * S * T_planar
    *
    * <br>
    * The wiggle transform is set to NaN by default.
    * {@link PolygonWiggler#findWiggleTransform}
    * {@link GradientDescentStepConstraintSolver#wigglePolygon}
    */
   RigidBodyTransformReadOnly getWiggleTransformInWorld();

   /**
    * Transform of the snapped footstep, T_snapped in the formulas above.
    * This transform includes the wiggle transform if it's available.
    */
   RigidBodyTransformReadOnly getSnappedNodeTransform(FootstepNode footsteNode);

   /**
    * Cropped foothold polygon in sole frame.
    */
   ConvexPolygon2DReadOnly getCroppedFoothold();

   /**
    * Planar region ID that the step is snapped to
    */
   int getPlanarRegionId();

   default void packSnapAndWiggleTransform(RigidBodyTransform transformToPack)
   {
      transformToPack.set(getSnapTransform());
      if (!getWiggleTransformInWorld().containsNaN())
      {
         transformToPack.preMultiply(getWiggleTransformInWorld());
      }
   }
}
