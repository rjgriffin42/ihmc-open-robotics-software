package us.ihmc.robotics.geometry;


/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class ConvexPolygon2dAndConnectingEdges
{
   private final ConvexPolygon2D polygon;
   private final LineSegment2D connectingEdge1, connectingEdge2;

   public ConvexPolygon2dAndConnectingEdges(ConvexPolygon2D polygon, LineSegment2D connectingEdge1, LineSegment2D connectingEdge2)
   {
      this.polygon = polygon;
      this.connectingEdge1 = connectingEdge1;
      this.connectingEdge2 = connectingEdge2;
   }

   public ConvexPolygon2D getConvexPolygon2d()
   {
      return polygon;
   }

   public LineSegment2D getConnectingEdge1()
   {
      return connectingEdge1;
   }

   public LineSegment2D getConnectingEdge2()
   {
      return connectingEdge2;
   }

}
