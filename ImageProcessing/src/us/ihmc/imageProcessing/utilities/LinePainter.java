package us.ihmc.imageProcessing.utilities;

import us.ihmc.utilities.math.geometry.Line2d;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.util.ArrayList;

/**
 * User: Matt
 * Date: 3/5/13
 */
public class LinePainter implements PostProcessor
{
   private ArrayList<Line2d> lines = new ArrayList<Line2d>();
   private float lineThickness = 1.0f;
   private int imageHeight = 480;

   public LinePainter(float lineThickness)
   {
      this.lineThickness = lineThickness;
   }

   public void setImageHeight(int imageHeight)
   {
      this.imageHeight = imageHeight;
   }

   public void setLines(ArrayList<Line2d> lines)
   {
      this.lines = lines;
   }

   public void clearLines()
   {
      lines.clear();
   }

   public void paint(Graphics graphics)
   {
      Color originalGraphicsColor = graphics.getColor();
      graphics.setColor(Color.red);
      Graphics2D g2d = (Graphics2D) graphics;
      Stroke originalStroke = g2d.getStroke();
      g2d.setStroke(new BasicStroke(lineThickness));
      Line2d xMin = new Line2d(new Point2d(0, 0), new Vector2d(1.0, 0.0));
      Line2d xMax = new Line2d(new Point2d(0, imageHeight), new Vector2d(1.0, 0.0));
      for (Line2d line : lines)
      {
         Point2d p1 = line.intersectionWith(xMin);
         Point2d p2 = line.intersectionWith(xMax);
         graphics.drawLine((int) p1.getX(), (int)p1.getY(), (int)p2.getX(), (int) p2.getY());
      }

      graphics.setColor(originalGraphicsColor);
      g2d.setStroke(originalStroke);

   }
}
