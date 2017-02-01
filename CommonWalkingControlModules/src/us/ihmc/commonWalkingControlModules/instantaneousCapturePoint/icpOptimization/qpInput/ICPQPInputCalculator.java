package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput;

import org.ejml.data.DenseMatrix64F;
import org.ejml.data.Matrix;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.robotics.linearAlgebra.MatrixTools;

public class ICPQPInputCalculator
{
   public ICPQPInputCalculator(ICPOptimizationParameters icpOptimizationParameters, int maximumNumberOfCMPVertices)
   {
   }

   public void computeFeedbackTask(ICPQPInput icpQPInput,  DenseMatrix64F feedbackWeight)
   {
      MatrixTools.addMatrixBlock(icpQPInput.quadraticTerm, 0, 0, feedbackWeight, 0, 0, 2, 2, 1.0);
   }

   private final DenseMatrix64F tmpObjective = new DenseMatrix64F(2, 1);
   public void computeFeedbackRegularizationTask(ICPQPInput icpQPInput,  DenseMatrix64F regularizationWeight, DenseMatrix64F objective)
   {
      MatrixTools.addMatrixBlock(icpQPInput.quadraticTerm, 0, 0, regularizationWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(regularizationWeight, tmpObjective, tmpObjective);
      CommonOps.multTransA(objective, tmpObjective, icpQPInput.residualCost);

      MatrixTools.addMatrixBlock(icpQPInput.linearTerm, 0, 0, tmpObjective, 0, 0, 2, 1, 1.0);
   }

   public void computeDynamicRelaxationTask(ICPQPInput icpQPInput, DenseMatrix64F dynamicRelaxationWeight)
   {
      MatrixTools.addMatrixBlock(icpQPInput.quadraticTerm, 0, 0, dynamicRelaxationWeight, 0, 0, 2, 2, 1.0);
   }
}
