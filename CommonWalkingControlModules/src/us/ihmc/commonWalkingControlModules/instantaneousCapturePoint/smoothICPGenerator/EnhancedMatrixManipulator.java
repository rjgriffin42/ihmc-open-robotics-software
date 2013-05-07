package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;


import org.ejml.data.DenseMatrix64F;

public class EnhancedMatrixManipulator
{
   public EnhancedMatrixManipulator()
   {
   }

   ;

   public static void setMatrixColumnToVector(int columnIndex, DenseMatrix64F Matrix, DenseMatrix64F vector)
   {
      for (int i = 0; i < Matrix.getNumRows(); i++)
      {
         Matrix.set(i, columnIndex, vector.get(i, columnIndex));
      }
   }

   public static void setMatrixRowToVector(int rowIndex, DenseMatrix64F Matrix, DenseMatrix64F vector)
   {
      for (int i = 0; i < Matrix.getNumCols(); i++)
      {
         Matrix.set(rowIndex, i, vector.get(i));
      }
   }

   public static DenseMatrix64F getMatrixColumn(int columnIndex, DenseMatrix64F Matrix)
   {
      int rowSize = Matrix.getNumRows();

      DenseMatrix64F column = new DenseMatrix64F(rowSize, 1);

      for (int i = 0; i < rowSize; i++)
      {
         column.set(i, Matrix.get(i, columnIndex));
      }

      return column;
   }

   public static DenseMatrix64F getMatrixRow(int rowIndex, DenseMatrix64F Matrix)
   {
      int columnSize = Matrix.getNumCols();

      DenseMatrix64F row = new DenseMatrix64F(1, columnSize);

      for (int i = 0; i < columnSize; i++)
      {
         row.set(i, Matrix.get(rowIndex, i));
      }

      return row;
   }
}

//~ Formatted by Jindent --- http://www.jindent.com
