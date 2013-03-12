package us.ihmc.graphics3DAdapter.graphics.appearances;

import java.util.ArrayList;

import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.utilities.Pair;

public class HeightBasedTerrainBlend extends YoAppearanceTransparent
{

   private final HeightMap heightMap;
   private final ArrayList<TextureDefinition> textures = new ArrayList<TextureDefinition>();
   private final ArrayList<Pair<Double, Double>> blends = new ArrayList<Pair<Double, Double>>();
   
   public HeightBasedTerrainBlend(HeightMap heightMap)
   {
      this.heightMap = heightMap;
   }
   
   public HeightMap getHeightMap()
   {
      return heightMap;
   }

   public void addTexture(double scale, String diffuse, String normal)
   {
      textures.add(new TextureDefinition(scale, diffuse, normal));
   }
   
   public void addBlend(double min_height, double fade_dist)
   {
      blends.add(new Pair<Double, Double>(min_height, fade_dist));
   }


   public ArrayList<Pair<Double, Double>> getBlends()
   {
      return blends;
   }
   
   public class TextureDefinition
   {
      private final double scale;
      private final String diffuse;
      private final String normal;

      public TextureDefinition(double scale, String diffuse, String normal)
      {
         super();
         this.scale = scale;
         this.diffuse = diffuse;
         this.normal = normal;
      }

      public double getScale()
      {
         return scale;
      }

      public String getDiffuse()
      {
         return diffuse;
      }

      public String getNormal()
      {
         return normal;
      }

   }

   public ArrayList<TextureDefinition> getTextures()
   {
      return textures;
   }


}
