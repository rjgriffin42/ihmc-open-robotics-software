package us.ihmc.javaFXToolkit.graphics;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.list.array.TIntArrayList;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import us.ihmc.graphics3DAdapter.graphics.MeshDataHolder;

public class JavaFXMeshDataInterpreter
{
   public static TriangleMesh interpretMeshData(MeshDataHolder meshData)
   {
      return interpretMeshData(meshData, true);
   }

   public static TriangleMesh interpretMeshData(MeshDataHolder meshData, boolean filterDuplicates)
   {
      if (meshData == null || meshData.getTriangleIndices().length == 0)
         return null;

      Point3f[] vertices = meshData.getVertices();
      TexCoord2f[] texturePoints = meshData.getTexturePoints();
      int[] triangleIndices = meshData.getTriangleIndices();
      Vector3f[] normals = meshData.getVertexNormals();
      TIntArrayList facesIndices = new TIntArrayList();

      if (filterDuplicates)
      {
         Pair<int[], Point3f[]> filterDuplicateVertices = filterDuplicates(triangleIndices, vertices);
         Pair<int[], Vector3f[]> filterDuplicateNormals = filterDuplicates(triangleIndices, normals);
         Pair<int[], TexCoord2f[]> filterDuplicateTex = filterDuplicates(triangleIndices, texturePoints);
         vertices = filterDuplicateVertices.getRight();
         normals = filterDuplicateNormals.getRight();
         texturePoints = filterDuplicateTex.getRight();
         
         for (int pos = 0; pos < triangleIndices.length; pos++)
         {
            facesIndices.add(filterDuplicateVertices.getLeft()[pos]); // vertex index
            facesIndices.add(filterDuplicateNormals.getLeft()[pos]); // normal index
            facesIndices.add(filterDuplicateTex.getLeft()[pos]); // texture index
         }
      }
      else
      {
         for (int pos = 0; pos < triangleIndices.length; pos++)
         {
            facesIndices.add(triangleIndices[pos]); // vertex index
            facesIndices.add(triangleIndices[pos]); // normal index
            facesIndices.add(triangleIndices[pos]); // texture index
         }
      }

      int[] indices = facesIndices.toArray();

      TriangleMesh triangleMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
      triangleMesh.getPoints().addAll(convertToFloatArray(vertices));
      triangleMesh.getTexCoords().addAll(convertToFloatArray(texturePoints));
      triangleMesh.getFaces().addAll(indices);
      triangleMesh.getFaceSmoothingGroups().addAll(new int[indices.length / triangleMesh.getFaceElementSize()]);
      triangleMesh.getNormals().addAll(convertToFloatArray(normals));

      return triangleMesh;
   }

   private static <T> Pair<int[], T[]> filterDuplicates(int[] originalIndices, T[] valuesWithDuplicates)
   {
      Map<T, Integer> uniqueValueIndices = new HashMap<>();

      for (int valueIndex = valuesWithDuplicates.length - 1; valueIndex >= 0; valueIndex--)
         uniqueValueIndices.put(valuesWithDuplicates[valueIndex], valueIndex);

      @SuppressWarnings("unchecked")
      T[] filteredValue = (T[]) Array.newInstance(valuesWithDuplicates[0].getClass(), uniqueValueIndices.size());
      int pos = 0;
      
      for (T value : uniqueValueIndices.keySet())
      {
         uniqueValueIndices.put(value, pos);
         filteredValue[pos] = value;
         pos++;
      }

      int[] filteredIndices = new int[originalIndices.length];
      pos = 0;

      for (int triangleIndex : originalIndices)
         filteredIndices[pos++] = uniqueValueIndices.get(valuesWithDuplicates[triangleIndex]);

      return Pair.of(filteredIndices, filteredValue);
   }

   private static float[] convertToFloatArray(Tuple3f[] tuple3fs)
   {
      float[] array = new float[3 * tuple3fs.length];
      int index = 0;
      for (Tuple3f tuple : tuple3fs)
      {
         array[index++] = tuple.getX();
         array[index++] = tuple.getY();
         array[index++] = tuple.getZ();
      }
      return array;
   }

   private static float[] convertToFloatArray(Tuple2f[] tuple2fs)
   {
      float[] array = new float[2 * tuple2fs.length];
      int index = 0;
      for (Tuple2f tuple : tuple2fs)
      {
         array[index++] = tuple.getX();
         array[index++] = tuple.getY();
      }
      return array;
   }
}
