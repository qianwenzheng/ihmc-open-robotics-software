package us.ihmc.avatar.behaviorTests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.euclid.referenceFrame.FramePose2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.behaviorServices.FiducialDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.behaviors.fiducialLocation.FollowFiducialBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridge;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.simulationConstructionSetTools.util.environments.FiducialsFlatGroundEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.commons.thread.ThreadTools;

public abstract class AvatarWalkToFiducialsBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCWalkToLocationBehaviorTest.class + " after class.");
   }

   @Before
   public void setUp()
   {
      drcBehaviorTestHelper = new DRCBehaviorTestHelper(new FiducialsFlatGroundEnvironment(), getSimpleRobotName(), DRCObstacleCourseStartingLocation.DEFAULT,
                                                        simulationTestingParameters, getRobotModel());
   }

   public void testWalkToFiducials() throws SimulationExceededMaximumTimeException
   {
      if (!ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer())
      {
         simulationTestingParameters.setKeepSCSUp(true);
      }

      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      assertTrue(drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0));

      CommunicationBridge communicationBridge = drcBehaviorTestHelper.getBehaviorCommunicationBridge();
      FullHumanoidRobotModel fullRobotModel = drcBehaviorTestHelper.getSDFFullRobotModel();
      HumanoidReferenceFrames referenceFrames = drcBehaviorTestHelper.getReferenceFrames();
      WalkingControllerParameters walkingControllerParams = getRobotModel().getWalkingControllerParameters();
      
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
      drcBehaviorTestHelper.getSimulationConstructionSet().addYoGraphicsListRegistry(yoGraphicsListRegistry);

      FiducialDetectorBehaviorService fiducialDetectorBehaviorService = new FiducialDetectorBehaviorService(communicationBridge, yoGraphicsListRegistry);
      fiducialDetectorBehaviorService.setTargetIDToLocate(50);
      FollowFiducialBehavior followFiducialBehavior = new FollowFiducialBehavior(communicationBridge, fullRobotModel, referenceFrames, fiducialDetectorBehaviorService);
      followFiducialBehavior.initialize();
      
      drcBehaviorTestHelper.getSimulationConstructionSet().getRootRegistry().addChild(fiducialDetectorBehaviorService.getYoVariableRegistry());
      drcBehaviorTestHelper.getSimulationConstructionSet().getRootRegistry().addChild(followFiducialBehavior.getYoVariableRegistry());
      
      assertTrue(drcBehaviorTestHelper.executeBehaviorUntilDone(followFiducialBehavior));

      assertTrue(followFiducialBehavior.isDone());

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   private FramePose2D getCurrentMidFeetPose2dCopy()
   {
      drcBehaviorTestHelper.updateRobotModel();
      ReferenceFrame midFeetFrame = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();
      FramePose3D midFeetPose = new FramePose3D();
      midFeetPose.setToZero(midFeetFrame);
      midFeetPose.changeFrame(ReferenceFrame.getWorldFrame());
      FramePose2D ret = new FramePose2D();
      ret.setIncludingFrame(midFeetPose.getReferenceFrame(), midFeetPose.getX(), midFeetPose.getY(), midFeetPose.getYaw());

      return ret;
   }
}
