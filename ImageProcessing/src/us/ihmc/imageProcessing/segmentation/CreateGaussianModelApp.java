package us.ihmc.imageProcessing.segmentation;

import boofcv.misc.BoofMiscOps;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class CreateGaussianModelApp {
   public static void main( String args[] ) throws IOException {
      Gaussian3D_F64 model;
      String roadName = "test_line.txt";
      String lineName = "line.txt";
//      String coneBlack = "cone_black.txt";
//      String coneOrange = "cone_orange.txt";
//
      System.out.println("Loading model");
      model = GaussianColorClassifier.train(new FileInputStream(roadName));
      model.print();

      BoofMiscOps.saveXML(model,"test_line.xml");

//      model = GaussianColorClassifier.train(new FileInputStream(lineName));
//      model.print();
//
//      BoofMiscOps.saveXML(model,"gaussian_line.xml");

//      model = GaussianColorClassifier.train(new FileInputStream(coneBlack));
//      model.print();
//
//      BoofMiscOps.saveXML(model,"gaussian_cone_black.xml");
//
//      model = GaussianColorClassifier.train(new FileInputStream(coneOrange));
//      model.print();
//
//      BoofMiscOps.saveXML(model,"gaussian_cone_orange.xml");
//
//      model = GaussianColorClassifier.train(new FileInputStream("white_start.txt"));
//      model.print();
//
//      BoofMiscOps.saveXML(model,"gaussian_white_start.xml");
//
//      model = GaussianColorClassifier.train(new FileInputStream("road_between_lines.txt"));
//      model.print();
//
//      BoofMiscOps.saveXML(model,"gaussian_road_between_lines.xml");
   }
}
