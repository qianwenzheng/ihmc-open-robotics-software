package us.ihmc.humanoidRobotics.communication.packets.walking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.ros.generators.RosEnumValueDocumentation;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.humanoidRobotics.communication.packets.FrameBasedMessage;
import us.ihmc.humanoidRobotics.communication.packets.PacketValidityChecker;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.TransformTools;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.trajectories.TrajectoryType;

@RosMessagePacket(documentation = "This message specifies the position, orientation and side (left or right) of a desired footstep in world frame.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE)
public class FootstepDataMessage extends Packet<FootstepDataMessage> implements TransformableDataObject<FootstepDataMessage>, FrameBasedMessage
{
   public enum FootstepOrigin
   {
      @RosEnumValueDocumentation(documentation = "The location of the footstep refers to the location of the ankle frame."
            + " The ankle frame is fixed in the foot, centered at the last ankle joint."
            + " The orientation = [qx = 0.0, qy = 0.0, qz = 0.0, qs = 1.0] corresponds to: x-axis pointing forward, y-axis pointing left, z-axis pointing upward."
            + " This option is for backward compatibility only and will be gone in an upcoming release."
            + " This origin is deprecated as it directly depends on the robot structure and is not directly related to the actual foot sole.")
      AT_ANKLE_FRAME,
      @RosEnumValueDocumentation(documentation = "The location of the footstep refers to the location of the sole frame."
            + " The sole frame is fixed in the foot, centered at the center of the sole."
            + " The orientation = [qx = 0.0, qy = 0.0, qz = 0.0, qs = 1.0] corresponds to: x-axis pointing forward, y-axis pointing left, z-axis pointing upward."
            + " This origin is preferred as it directly depends on the actual foot sole and is less dependent on the robot structure.")
      AT_SOLE_FRAME
   }
   @RosExportedField(documentation = "Specifies whether the given location is the location of the ankle or the sole.")
   public FootstepOrigin origin;
   
   public FootstepOrigin getOrigin()
   {
      return origin;
   }
   
   public void setOrigin(FootstepOrigin origin)
   {
      this.origin = origin;
   }
   
   @RosExportedField(documentation = "Specifies which foot will swing to reach the foostep.")
   public RobotSide robotSide;
   @RosExportedField(documentation = "Specifies the position of the footstep.")
   public Point3D location;
   @RosExportedField(documentation = "Specifies the orientation of the footstep.")
   public Quaternion orientation;
   
   @RosExportedField(documentation = "The ID of the reference frame to execute the swing in. This is also the expected frame of all pose data in this message.")
   public long trajectoryReferenceFrameId = ReferenceFrame.getWorldFrame().getNameBasedHashCode();

   @RosExportedField(documentation = "predictedContactPoints specifies the vertices of the expected contact polygon between the foot and\n"
         + "the world. A value of null or an empty list will default to using the entire foot. Contact points are expressed in sole frame. This ordering does not matter.\n"
         + "For example: to tell the controller to use the entire foot, the predicted contact points would be:\n" + "predicted_contact_points:\n"
         + "- {x: 0.5 * foot_length, y: -0.5 * toe_width}\n" + "- {x: 0.5 * foot_length, y: 0.5 * toe_width}\n"
         + "- {x: -0.5 * foot_length, y: -0.5 * heel_width}\n" + "- {x: -0.5 * foot_length, y: 0.5 * heel_width}\n")
   public ArrayList<Point2D> predictedContactPoints;

   @RosExportedField(documentation = "This contains information on what the swing trajectory should be for each step. Recomended is DEFAULT.")
   public TrajectoryType trajectoryType = TrajectoryType.DEFAULT;
   @RosExportedField(documentation = "Contains information on how high the robot should swing its foot. This affects trajectory types DEFAULT and OBSTACLE_CLEARANCE.")
   public double swingHeight = 0.0;
   @RosExportedField(documentation = "In case the trajectory type is set to CUSTOM two swing waypoints can be specified here. The waypoints define sole positions in workd."
   		+ "The controller will compute times and velocities at the waypoints. This is a convinient way to shape the trajectory of the swing. If full control over the swing"
   		+ "trajectory is desired use the trajectory type WAYPOINTS instead.")
   public Point3D[] positionWaypoints = new Point3D[0];

   @RosExportedField(documentation = "The swingDuration is the time a foot is not in ground contact during a step."
         + "\nIf the value of this field is invalid (not positive) it will be replaced by a default swingDuration.")
   public double swingDuration = -1.0;
   @RosExportedField(documentation = "The transferDuration is the time spent with the feet in ground contact before a step."
         + "\nIf the value of this field is invalid (not positive) it will be replaced by a default transferDuration.")
   public double transferDuration = -1.0;

   /**
    * Empty constructor for serialization.
    */
   public FootstepDataMessage()
   {
   }

   public FootstepDataMessage(RobotSide robotSide, Point3DReadOnly location, QuaternionReadOnly orientation)
   {
      this(robotSide, new Point3D(location), new Quaternion(orientation), null);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3D location, Quaternion orientation)
   {
      this(robotSide, location, orientation, null);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3D location, Quaternion orientation, ArrayList<Point2D> predictedContactPoints)
   {
      this(robotSide, location, orientation, predictedContactPoints, TrajectoryType.DEFAULT, 0.0);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3D location, Quaternion orientation, TrajectoryType trajectoryType, double swingHeight)
   {
      this(robotSide, location, orientation, null, trajectoryType, swingHeight);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3D location, Quaternion orientation, ArrayList<Point2D> predictedContactPoints,
         TrajectoryType trajectoryType, double swingHeight)
   {
      this.robotSide = robotSide;
      this.location = location;
      this.orientation = orientation;
      if (predictedContactPoints != null && predictedContactPoints.size() == 0)
         this.predictedContactPoints = null;
      else
         this.predictedContactPoints = predictedContactPoints;
      this.trajectoryType = trajectoryType;
      this.swingHeight = swingHeight;
   }

   public FootstepDataMessage(FootstepDataMessage footstepData)
   {
      this.robotSide = footstepData.robotSide;
      this.location = new Point3D(footstepData.location);
      this.orientation = new Quaternion(footstepData.orientation);
      this.orientation.checkIfUnitary();
      if (footstepData.predictedContactPoints == null || footstepData.predictedContactPoints.isEmpty())
      {
         this.predictedContactPoints = null;
      }
      else
      {
         this.predictedContactPoints = new ArrayList<>();
         for (Point2D contactPoint : footstepData.predictedContactPoints)
         {
            this.predictedContactPoints.add(new Point2D(contactPoint));
         }
      }
      this.trajectoryType = footstepData.trajectoryType;
      this.swingHeight = footstepData.swingHeight;

      if (footstepData.positionWaypoints != null)
      {
         this.positionWaypoints = new Point3D[footstepData.positionWaypoints.length];
         for (int i = 0; i < footstepData.positionWaypoints.length; i++)
            positionWaypoints[i] = new Point3D(footstepData.positionWaypoints[i]);
      }

      this.swingDuration = footstepData.swingDuration;
      this.transferDuration = footstepData.transferDuration;
      
      this.origin = footstepData.origin;
   }

   @Override
   public FootstepDataMessage clone()
   {
      return new FootstepDataMessage(this);
   }

   public FootstepDataMessage(Footstep footstep)
   {
      robotSide = footstep.getRobotSide();

      FramePoint location = new FramePoint();
      FrameOrientation orientation = new FrameOrientation();
      footstep.getPose(location, orientation);
      ReferenceFrame trajectoryFrame = footstep.getFootstepPose().getReferenceFrame();
      setTrajectoryReferenceFrameId(trajectoryFrame);
      this.location = location.getPoint();
      this.orientation = orientation.getQuaternion();

      List<Point2D> footstepContactPoints = footstep.getPredictedContactPoints();
      if (footstepContactPoints != null)
      {
         if (predictedContactPoints == null)
         {
            predictedContactPoints = new ArrayList<>();
         }
         else
         {
            predictedContactPoints.clear();
         }
         for (Point2D contactPoint : footstepContactPoints)
         {
            predictedContactPoints.add(new Point2D(contactPoint));
         }
      }
      else
      {
         predictedContactPoints = null;
      }
      trajectoryType = footstep.getTrajectoryType();
      swingHeight = footstep.getSwingHeight();

      if (footstep.getCustomPositionWaypoints().size() != 0)
      {
         positionWaypoints = new Point3D[footstep.getCustomPositionWaypoints().size()];
         for (int i = 0; i < footstep.getCustomPositionWaypoints().size(); i++)
         {
            FramePoint framePoint = footstep.getCustomPositionWaypoints().get(i);
            framePoint.checkReferenceFrameMatch(trajectoryFrame);
            positionWaypoints[i] = new Point3D(framePoint.getPoint());
         }
      }
   }

   public ArrayList<Point2D> getPredictedContactPoints()
   {
      return predictedContactPoints;
   }

   public Point3D getLocation()
   {
      return location;
   }

   public void getLocation(Point3D locationToPack)
   {
      locationToPack.set(location);
   }

   public Quaternion getOrientation()
   {
      return orientation;
   }

   public void getOrientation(Quaternion orientationToPack)
   {
      orientationToPack.set(this.orientation);
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public double getSwingHeight()
   {
      return swingHeight;
   }

   public void setRobotSide(RobotSide robotSide)
   {
      this.robotSide = robotSide;
   }

   public void setLocation(Point3D location)
   {
      if (this.location == null) this.location = new Point3D();
      this.location.set(location);
   }

   public void setOrientation(Quaternion orientation)
   {
      if (this.orientation == null) this.orientation = new Quaternion();
      this.orientation.set(orientation);
   }

   public void setSwingHeight(double swingHeight)
   {
      this.swingHeight = swingHeight;
   }

   public void setPredictedContactPoints(ArrayList<Point2D> predictedContactPoints)
   {
      this.predictedContactPoints = predictedContactPoints;
   }

   public TrajectoryType getTrajectoryType()
   {
      return trajectoryType;
   }

   public void setTrajectoryType(TrajectoryType trajectoryType)
   {
      this.trajectoryType = trajectoryType;
   }

   public Point3D[] getCustomPositionWaypoints()
   {
      return positionWaypoints;
   }

   public void setCustomPositionWaypoints(Point3D[] trajectoryWaypoints)
   {
      this.positionWaypoints = trajectoryWaypoints;
   }

   public void setTimings(double swingDuration, double transferDuration)
   {
      setSwingDuration(swingDuration);
      setTransferDuration(transferDuration);
   }

   public void setSwingDuration(double swingDuration)
   {
      this.swingDuration = swingDuration;
   }

   public void setTransferDuration(double transferDuration)
   {
      this.transferDuration = transferDuration;
   }

   public double getSwingDuration()
   {
      return swingDuration;
   }

   public double getTransferDuration()
   {
      return transferDuration;
   }

   @Override
   public String toString()
   {
      String ret = "";

      FrameOrientation frameOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame(), this.orientation);
      double[] ypr = frameOrientation.getYawPitchRoll();
      ret = location.toString();
      ret += ", YawPitchRoll = " + Arrays.toString(ypr) + "\n";
      ret += "Predicted Contact Points: ";
      if (predictedContactPoints != null)
      {
         ret += "size = " + predictedContactPoints.size() + "\n";
      }
      else
      {
         ret += "null";
      }

      ret += trajectoryType.name() + "\n";

      if(positionWaypoints != null)
      {
         ret += "waypoints = " + positionWaypoints.length + "\n";
      }
      else
      {
         ret += "no waypoints" + "\n";
      }

      return ret;
   }

   @Override
   public boolean epsilonEquals(FootstepDataMessage footstepData, double epsilon)
   {
      if (trajectoryReferenceFrameId != footstepData.getTrajectoryReferenceFrameId())
      {
         return false;
      }
      
      boolean robotSideEquals = robotSide == footstepData.robotSide;
      boolean locationEquals = location.epsilonEquals(footstepData.location, epsilon);

      boolean orientationEquals = orientation.epsilonEquals(footstepData.orientation, epsilon);
      if (!orientationEquals)
      {
         Quaternion temp = new Quaternion();
         temp.setAndNegate(orientation);
         orientationEquals = temp.epsilonEquals(footstepData.orientation, epsilon);
      }

      boolean contactPointsEqual = true;

      if ((this.predictedContactPoints == null) && (footstepData.predictedContactPoints != null))
         contactPointsEqual = false;
      else if ((this.predictedContactPoints != null) && (footstepData.predictedContactPoints == null))
         contactPointsEqual = false;
      else if (this.predictedContactPoints != null)
      {
         int size = predictedContactPoints.size();
         if (size != footstepData.predictedContactPoints.size())
            contactPointsEqual = false;
         else
         {
            for (int i = 0; i < size; i++)
            {
               Point2D pointOne = predictedContactPoints.get(i);
               Point2D pointTwo = footstepData.predictedContactPoints.get(i);

               if (!(pointOne.distanceSquared(pointTwo) < 1e-7))
                  contactPointsEqual = false;
            }
         }
      }

      boolean trajectoryWaypointsEqual = true;

      if ((this.positionWaypoints == null) && (footstepData.positionWaypoints != null))
         trajectoryWaypointsEqual = false;
      else if ((this.positionWaypoints != null) && (footstepData.positionWaypoints == null))
         trajectoryWaypointsEqual = false;
      else if (this.positionWaypoints != null)
      {
         int size = positionWaypoints.length;
         if (size != footstepData.positionWaypoints.length)
            trajectoryWaypointsEqual = false;
         else
         {
            for (int i = 0; i < size; i++)
            {
               Point3D pointOne = positionWaypoints[i];
               Point3D pointTwo = footstepData.positionWaypoints[i];

               if (!(pointOne.distanceSquared(pointTwo) < 1e-7))
                  trajectoryWaypointsEqual = false;
            }
         }
      }

      boolean sameTimings = MathTools.epsilonEquals(swingDuration, footstepData.swingDuration, epsilon);
      sameTimings = sameTimings && MathTools.epsilonEquals(transferDuration, footstepData.transferDuration, epsilon);

      return robotSideEquals && locationEquals && orientationEquals && contactPointsEqual && trajectoryWaypointsEqual && sameTimings;
   }

   @Override
   public FootstepDataMessage transform(RigidBodyTransform transform)
   {
      FootstepDataMessage ret = this.clone();

      // Point3D location;
      ret.location = TransformTools.getTransformedPoint(this.getLocation(), transform);

      // Quat4d orientation;
      ret.orientation = TransformTools.getTransformedQuat(this.getOrientation(), transform);

      // Waypoints if they exist:
      if (positionWaypoints != null)
      {
         for (int i = 0; i < positionWaypoints.length; i++)
            ret.positionWaypoints[i] = TransformTools.getTransformedPoint(positionWaypoints[i], transform);
      }

      return ret;
   }

   public FootstepDataMessage(Random random)
   {
      TrajectoryType[] trajectoryTypes = TrajectoryType.values();
      int randomOrdinal = random.nextInt(trajectoryTypes.length);

      this.robotSide = random.nextBoolean() ? RobotSide.LEFT : RobotSide.RIGHT;
      this.location = RandomGeometry.nextPoint3DWithEdgeCases(random, 0.05);
      this.orientation = RandomGeometry.nextQuaternion(random);
      int numberOfPredictedContactPoints = random.nextInt(10);
      this.predictedContactPoints = new ArrayList<>();

      for (int i = 0; i < numberOfPredictedContactPoints; i++)
      {
         predictedContactPoints.add(new Point2D(random.nextDouble(), random.nextDouble()));
      }

      this.trajectoryType = trajectoryTypes[randomOrdinal];
      this.swingHeight = RandomNumbers.nextDoubleWithEdgeCases(random, 0.05);

      this.swingDuration = RandomNumbers.nextDouble(random, -1.0, 2.0);
      this.transferDuration = RandomNumbers.nextDouble(random, -1.0, 2.0);

      if (trajectoryType == TrajectoryType.CUSTOM)
      {
         positionWaypoints = new Point3D[2];
         positionWaypoints[0] = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         positionWaypoints[1] = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
      }
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateFootstepDataMessage(this);
   }

   /** {@inheritDoc} */
   @Override
   public long getTrajectoryReferenceFrameId()
   {
      return trajectoryReferenceFrameId;
   }

   /** {@inheritDoc} */
   @Override
   public void setTrajectoryReferenceFrameId(long trajectoryReferenceFrameId)
   {
      this.trajectoryReferenceFrameId = trajectoryReferenceFrameId;
   }

   /** {@inheritDoc} */
   @Override
   public void setTrajectoryReferenceFrameId(ReferenceFrame trajectoryReferenceFrame)
   {
      trajectoryReferenceFrameId = trajectoryReferenceFrame.getNameBasedHashCode();
   }

   /** {@inheritDoc} */
   @Override
   public long getDataReferenceFrameId()
   {
      // the data frame is not supported by footsteps
      return trajectoryReferenceFrameId;
   }

   /** {@inheritDoc} */
   @Override
   public void setDataReferenceFrameId(long dataReferenceFrameId)
   {
      throw new RuntimeException("The data frame is not supported by footsteps");
   }

   /** {@inheritDoc} */
   @Override
   public void setDataReferenceFrameId(ReferenceFrame dataReferenceFrame)
   {
      throw new RuntimeException("The data frame is not supported by footsteps");
   }

   /** {@inheritDoc} */
   @Override
   public Point3D getControlFramePosition()
   {
      // The choice of control frame is not implemented for stepping. Waypoints need to be defined for the sole frame.
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public Quaternion getControlFrameOrientation()
   {
      // The choice of control frame is not implemented for stepping. Waypoints need to be defined for the sole frame.
      return null;
   }

}
