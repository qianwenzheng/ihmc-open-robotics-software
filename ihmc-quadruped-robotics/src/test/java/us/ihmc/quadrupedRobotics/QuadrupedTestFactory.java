package us.ihmc.quadrupedRobotics;

import java.io.IOException;

import us.ihmc.jMonkeyEngineToolkit.GroundProfile3D;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.input.managers.QuadrupedTeleopManager;
import us.ihmc.quadrupedRobotics.model.QuadrupedInitialPositionParameters;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.simulationConstructionSetTools.util.simulationrunner.GoalOrientedTestConductor;

public interface QuadrupedTestFactory
{
   public GoalOrientedTestConductor createTestConductor() throws IOException;

   public QuadrupedTeleopManager getStepTeleopManager();

   public void setControlMode(QuadrupedControlMode controlMode);

   public void setGroundContactModelType(QuadrupedGroundContactModelType groundContactModelType);
   
   public void setUseStateEstimator(boolean useStateEstimator);
   
   public void setGroundProfile3D(GroundProfile3D groundProfile3D);

   public void setUsePushRobotController(boolean usePushRobotController);
import us.ihmc.quadrupedRobotics.model.QuadrupedSimulationInitialPositionParameters;

   public void setInitialPosition(QuadrupedInitialPositionParameters initialPosition);

   public void setUseNetworking(boolean useNetworking);
}
