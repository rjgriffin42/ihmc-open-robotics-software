package us.ihmc.darpaRoboticsChallenge.odometry;

import georegression.struct.point.Point3D_F64;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CarLocalizerFileWritingUtils
{
   public static File getCarLocalizerClassDataDirectory() throws URISyntaxException
   {
      URL url = CarLocalizerFileWritingUtils.class.getResource("data");
      if (url == null)
      {
         throw new RuntimeException("couldn't find data directory");
      }

      return new File(url.toURI());
   }

   public static File getCarLocalizerSourceDataDirectory() throws URISyntaxException
   {
      URL url = CarLocalizerFileWritingUtils.class.getResource("data");
      if (url == null)
      {
         throw new RuntimeException("couldn't find data directory");
      }

      String path = (new File(url.toURI())).toString();
      path = path.replaceFirst("classes", "src");

      return new File(path);
   }

   public static void main(String[] args) throws URISyntaxException, IOException
   {
      System.out.println(getCarLocalizerSourceDataDirectory());

      List<Point3D_F64> data = readFileInDataDirectory("scan.dat");
      writeToFileInSrcDataDirectory("scanCopyTest.dat", data);

   }

   public static void write(String fileName, List<Point3D_F64> points) throws IOException
   {
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
      write(points, writer);
   }

   public static void writeToFileInSrcDataDirectory(String fileName, List<Point3D_F64> points) throws IOException, URISyntaxException
   {
      String filePath = getCarLocalizerSourceDataDirectory().toString() + File.separator + fileName;
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));
      write(points, writer);
   }

   private static void write(List<Point3D_F64> points, BufferedWriter writer) throws IOException
   {
      StringBuilder builder = new StringBuilder("\n\n");
      for (int i = 0; i < points.size(); i++)
      {
         builder.append(points.get(i).x + " " + points.get(i).y + " " + points.get(i).z + "\n");
      }

      writer.write(builder.toString());
      writer.close();
   }

   public static List<Point3D_F64> readFileInDataDirectory(String localFileName) throws IOException, URISyntaxException
   {
      String filePath = getCarLocalizerClassDataDirectory().toString() + File.separator + localFileName;

      return read(filePath);
   }

   public static List<Point3D_F64> read(String filePath) throws IOException
   {
      BufferedReader reader = new BufferedReader(new FileReader((filePath)));

      return read(reader);
   }

   private static List<Point3D_F64> read(BufferedReader reader) throws IOException
   {
      reader.readLine();
      reader.readLine();

      List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

      while (true)
      {
         String s = reader.readLine();
         if (s == null)
            break;
         String w[] = s.split(" ");
         if (w.length != 3)
            break;

         Point3D_F64 p = new Point3D_F64();
         p.setX(Double.parseDouble(w[0]));
         p.setY(Double.parseDouble(w[1]));
         p.setZ(Double.parseDouble(w[2]));
         ret.add(p);
      }

      reader.close();

      return ret;
   }
}
