package us.ihmc.imageProcessing;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.xuggler.XugglerSimplified;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.line.LineSegment2D_I32;
import georegression.struct.point.Point2D_I32;
import us.ihmc.imageProcessing.segmentation.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ImageDifferenceTest implements MouseListener {

   SimpleImageSequence<ImageFloat32> sequence;
   BufferedImage output;
   BufferedImage outputChange;

   boolean paused = false;

   public ImageDifferenceTest(SimpleImageSequence<ImageFloat32> sequence) {
      this.sequence = sequence;
   }

   public void process() {

      ImageFloat32 frame = sequence.next();

      ImageFloat32 sum = frame.clone();
      ImageFloat32 background = new ImageFloat32(sum.width,sum.height);
      ImageFloat32 difference = new ImageFloat32(sum.width,sum.height);

      ImageUInt8 change = new ImageUInt8(sum.width,sum.height);

      output = new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB);
      outputChange = new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB);

      ImagePanel gui = ShowImages.showWindow(output,"Difference");
      ImagePanel guiChange = ShowImages.showWindow(outputChange,"Difference");

      int total = 1;

      while( sequence.hasNext() ) {
         frame = sequence.next();

         PixelMath.divide(sum,total,background);
         PixelMath.subtract(frame,background,difference);
         PixelMath.abs(difference,difference);
         if( total > 10 )
            checkChange(difference,change);

         ConvertBufferedImage.convertTo(difference,output);
         gui.repaint();
         VisualizeBinaryData.renderBinary(change,outputChange);
         guiChange.repaint();



         PixelMath.add(frame,sum,sum);
         total++;
      }
   }

   private void checkChange( ImageFloat32 difference , ImageUInt8 change ) {
      for( int y = 0; y < difference.height; y++ ) {
         for( int x = 0; x < difference.width; x++ ) {
            float v = difference.unsafe_get(x,y);
            if( v > 10 ) {
               change.data[ change.getIndex(x,y) ] |= 1;
            }
         }
      }
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      paused = !paused;
   }

   @Override
   public void mousePressed(MouseEvent e) {}

   @Override
   public void mouseReleased(MouseEvent e) {}

   @Override
   public void mouseEntered(MouseEvent e) {}

   @Override
   public void mouseExited(MouseEvent e) {}

   public static void main( String args[] ) {
      String videoFile = "../ImageProcessing/media/videos/leftEye.mp4";

      XugglerSimplified<ImageFloat32> xuggler = new XugglerSimplified<ImageFloat32>(videoFile,ImageFloat32.class);

      ImageDifferenceTest app = new ImageDifferenceTest(xuggler);
      app.process();
   }
}
