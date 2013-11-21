package us.ihmc.darpaRoboticsChallenge.calib;

import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class AtlasArmKinematicsCalibrationTest
{

   @Test
   public void estimateCameraPose() {
      BufferedImage image = UtilImageIO.loadImage("../DarpaRoboticsChallenge/data/atlas_chessboard.jpg");

      if( image == null )
         fail("can't find the test image.");

      AtlasArmKinematicsCalibration alg = new AtlasArmKinematicsCalibration(null);

      // if param has not been set it should fail
      assertFalse(alg.estimateCameraPose(image));

      // just make up some parameters
      int w = image.getWidth();
      int h = image.getHeight();
      IntrinsicParameters param = new IntrinsicParameters(500,500,0,w/2,h/2,w,h,false,null);
      alg.setIntrinsic(param);

      assertTrue(alg.estimateCameraPose(image));

      Se3_F64 pose = alg.getTargetToOrigin();

      assertTrue(pose.T.x != 0 );
      assertTrue(pose.T.y != 0 );
      assertTrue(pose.T.z > 0.2);
   }
}
