package us.ihmc.robotics.linearAlgebra;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;

import us.ihmc.robotics.functionApproximation.DampedLeastSquaresSolver;
import us.ihmc.robotics.linearAlgebra.DampedNullspaceCalculator;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.time.ExecutionTimer;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class DampedLeastSquaresNullspaceCalculator implements DampedNullspaceCalculator
{
   private final DampedLeastSquaresSolver pseudoInverseSolver;

   private final DenseMatrix64F pseudoInverseMatrix;
   private final DenseMatrix64F nullspaceProjector;
   private final DenseMatrix64F tempMatrixForProjectionInPlace;

   private final DenseMatrix64F inverseMatrix;
   private final LinearSolver<DenseMatrix64F> linearSolver;

   private final DenseMatrix64F dampedMatrix;

   private double alpha = 0.0;

   public DampedLeastSquaresNullspaceCalculator(int matrixSize, double alpha)
   {
      pseudoInverseSolver = new DampedLeastSquaresSolver(matrixSize, alpha);
      pseudoInverseMatrix = new DenseMatrix64F(matrixSize, matrixSize);
      nullspaceProjector = new DenseMatrix64F(matrixSize, matrixSize);
      tempMatrixForProjectionInPlace = new DenseMatrix64F(matrixSize, matrixSize);

      this.alpha = alpha;

      dampedMatrix = new DenseMatrix64F(matrixSize, matrixSize);
      inverseMatrix = new DenseMatrix64F(matrixSize, matrixSize);
      this.linearSolver = LinearSolverFactory.linear(matrixSize);
   }

   @Override
   public void setPseudoInverseAlpha(double alpha)
   {
      pseudoInverseSolver.setAlpha(alpha);
      this.alpha = alpha;
   }

   /**
    * Compute the nullspace projector of the given matrix as follows:
    * <p>
    * &Nu; = I - M<sup>+</sup>M
    * </p>
    * Where M<sup>+</sup> is the Moore-Penrose pseudo-inverse of M.
    * A damped least-squares solver is used to compute M<sup>+</sup>.
    * @param matrixToComputeNullspaceOf the matrix to compute the nullspace of for the projection. Not Modified.
    * @param nullspaceProjectorToPack matrix to store the resulting nullspace matrix. Modified.
    */
   @Override
   public void computeNullspaceProjector(DenseMatrix64F matrixToComputeNullspaceOf, DenseMatrix64F nullspaceProjectorToPack)
   {
      int nullspaceMatrixSize = matrixToComputeNullspaceOf.getNumCols();

      pseudoInverseMatrix.reshape(nullspaceMatrixSize, matrixToComputeNullspaceOf.getNumRows());
      pseudoInverseSolver.setA(matrixToComputeNullspaceOf);
      pseudoInverseSolver.invert(pseudoInverseMatrix);

      // I - J^* J
      nullspaceProjectorToPack.reshape(nullspaceMatrixSize, nullspaceMatrixSize);
      CommonOps.setIdentity(nullspaceProjectorToPack);
      CommonOps.multAdd(-1.0, pseudoInverseMatrix, matrixToComputeNullspaceOf, nullspaceProjectorToPack);
   }

   /**
    * Perform a projection in place as follows:
    * <p>
    * A = A * (I - B<sup>+</sup>B)
    * </p> 
    * @param matrixToProjectOntoNullspace the matrix to be projected, A in the equation. Modified.
    * @param matrixToComputeNullspaceOf the matrix to compute the nullspace of for the projection, B in the equation. Not Modified.
    */
   @Override
   public void projectOntoNullspace(DenseMatrix64F matrixToProjectOntoNullspace, DenseMatrix64F matrixToComputeNullspaceOf)
   {
      tempMatrixForProjectionInPlace.set(matrixToProjectOntoNullspace);
      projectOntoNullspace(tempMatrixForProjectionInPlace, matrixToComputeNullspaceOf, matrixToProjectOntoNullspace);
   }

   /**
    * Project {@code matrixToProjectOntoNullspace} onto the nullspace of {@code matrixToComputeNullspaceOf} as follows:
    * <p>
    * C = A * (I - B<sup>+</sup>B)
    * </p> 
    * @param matrixToProjectOntoNullspace the matrix to be projected, A in the equation. Not modified.
    * @param matrixToComputeNullspaceOf the matrix to compute the nullspace of for the projection, B in the equation. Not Modified.
    * @param projectedMatrixToPack matrix to store the resulting projection, C in the equation. Modified.
    */
   @Override
   public void projectOntoNullspace(DenseMatrix64F matrixToProjectOntoNullspace, DenseMatrix64F matrixToComputeNullspaceOf, DenseMatrix64F projectedMatrixToPack)
   {
      computeNullspaceProjector(matrixToComputeNullspaceOf, nullspaceProjector);
      projectedMatrixToPack.reshape(matrixToProjectOntoNullspace.getNumRows(), matrixToProjectOntoNullspace.getNumCols());
      CommonOps.mult(matrixToProjectOntoNullspace, nullspaceProjector, projectedMatrixToPack);
   }
}
