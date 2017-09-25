package us.ihmc.footstepPlanning.polygonSnapping;

import java.util.Random;

import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.jMonkeyEngineToolkit.HeightMapWithNormals;
import us.ihmc.robotics.Axis;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.geometry.PlanarRegionsListGenerator;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.simulationConstructionSetTools.util.environments.PlanarRegionsListDefinedEnvironment;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.ground.TerrainObject3D;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class PlanarRegionsListExamples
{
   public static PlanarRegionsList generateFlatGround(double lengthX, double widthY)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      generator.addCubeReferencedAtBottomMiddle(lengthX, widthY, 0.001);
      PlanarRegionsList flatGround = generator.getPlanarRegionsList();
      return flatGround;
   }

   public static PlanarRegionsList generateStairCase()
   {
      return generateStairCase(new Vector3D(), new Vector3D());
   }

   public static PlanarRegionsList generateStairCase(Vector3D rotationVector)
   {
      return generateStairCase(new Vector3D(), rotationVector);
   }

   public static PlanarRegionsList generateStairCase(Vector3D translationVector, Vector3D rotationVector)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      generator.translate(translationVector);

      int numberOfSteps = 5;

      double length = 0.4;
      double width = 0.8;
      double height = 0.1;

      generator.translate(length * numberOfSteps / 2.0, 0.0, 0.001);
      generator.addRectangle(1.2 * length * numberOfSteps, 1.2 * width);

      generator.identity();
      generator.translate(translationVector);
      generator.translate(length, 0.0, 0.0);
      generator.rotateEuler(rotationVector);
      for (int i = 0; i < numberOfSteps; i++)
      {
         generator.addCubeReferencedAtBottomMiddle(length, width, height);
         generator.translate(length, 0.0, 0.0);
         height = height + 0.1;
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }
   
   public static PlanarRegionsList generateCinderBlockField(double startX, double startY, double cinderBlockSize, double cinderBlockHeight, int courseWidthXInNumberOfBlocks, int courseLengthYInNumberOfBlocks, double heightVariation)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      double courseWidth = courseLengthYInNumberOfBlocks * cinderBlockSize;
      
      generator.translate(startX, startY, 0.001); // avoid graphical issue
      generator.addRectangle(0.6, courseWidth); // standing platform
      generator.translate(0.5, 0.0, 0.0); // forward to first row
      generator.translate(0.0, -0.5 * (courseLengthYInNumberOfBlocks - 1) * cinderBlockSize, 0.0); // over to grid origin
      
      Random random = new Random(1231239L);
      for (int x = 0; x < courseWidthXInNumberOfBlocks; x++)
      {
         for (int y = 0; y < courseLengthYInNumberOfBlocks; y++)
         {
            int angleType = Math.abs(random.nextInt() % 3);
            int axisType = Math.abs(random.nextInt() % 2);
            
            generateSingleCiderBlock(generator, cinderBlockSize, cinderBlockHeight, angleType, axisType);
            
            generator.translate(0.0, cinderBlockSize, 0.0);
         }
         
         if ((x / 2) % 2 == 0)
         {
            generator.translate(0.0, 0.0, heightVariation);
         }
         else
         {
            generator.translate(0.0, 0.0, - heightVariation);
         }
            
         generator.translate(cinderBlockSize, -cinderBlockSize * courseLengthYInNumberOfBlocks, 0.0);
      }
      
      generator.identity();
      generator.translate(0.6 + courseWidthXInNumberOfBlocks * cinderBlockSize, 0.0, 0.001);
      generator.addRectangle(0.6, courseWidth);
      
      return generator.getPlanarRegionsList();
   }

   public static void generateSingleCiderBlock(PlanarRegionsListGenerator generator, double cinderBlockSize, double cinderBlockHeight, int angleType,
                                                int axisType)
   {
      double angle = 0;
      switch (angleType)
      {
      case 0:
         angle = 0.0;
         break;
      case 1:
         angle = Math.toRadians(15);
         break;
      case 2:
         angle = -Math.toRadians(15);
         break;
      }

      Axis axis = null;
      switch (axisType)
      {
      case 0:
         axis = Axis.X;
         break;
      case 1:
         axis = Axis.Y;
         break;
      }

      generator.rotate(angle, axis);
      generator.addCubeReferencedAtBottomMiddle(cinderBlockSize, cinderBlockSize, cinderBlockHeight);
      generator.rotate(-angle, axis);
   }

   public static PlanarRegionsList generateRandomObjects(Random random, int numberOfRandomObjects, double maxX, double maxY, double maxZ)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      double length = RandomNumbers.nextDouble(random, 0.2, 1.0);
      double width = RandomNumbers.nextDouble(random, 0.2, 1.0);
      double height = RandomNumbers.nextDouble(random, 0.2, 1.0);

      for (int i = 0; i < numberOfRandomObjects; i++)
      {
         generator.identity();

         Vector3D translationVector = RandomGeometry.nextVector3D(random, -maxX, -maxY, 0.0, maxX, maxY, maxZ);
         generator.translate(translationVector);

         Quaternion rotation = RandomGeometry.nextQuaternion(random);
         generator.rotate(rotation);

         generator.addCubeReferencedAtBottomMiddle(length, width, height);
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }

   public static PlanarRegionsList generateBumpyGround(Random random, double maxX, double maxY, double maxZ)
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();

      double length = 0.5;
      double width = 0.5;

      generator.translate(maxX/2.0 + length/2.0, maxY/2.0 - width/2.0, 0.0);
      generator.addCubeReferencedAtBottomMiddle(1.5 * maxX, 1.25 * maxY, 0.01);
      generator.identity();

      int sizeX = (int) (maxX/length);
      int sizeY = (int) (maxY/width);

      for (int i=0; i<sizeY; i++)
      {
         generator.identity();
         generator.translate(0.0, i * width, 0.0);
         for (int j=0; j<sizeX; j++)
         {
            generator.translate(length, 0.0, 0.0);
            double height = RandomNumbers.nextDouble(random, 0.01, maxZ);
            generator.addCubeReferencedAtBottomMiddle(length, width, height + random.nextDouble() * 0.1);
         }
      }

      PlanarRegionsList planarRegionsList = generator.getPlanarRegionsList();
      return planarRegionsList;
   }

   public static ConvexPolygon2D createRectanglePolygon(double lengthX, double widthY)
   {
      ConvexPolygon2D convexPolygon = new ConvexPolygon2D();
      convexPolygon.addVertex(lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, widthY / 2.0);
      convexPolygon.addVertex(-lengthX / 2.0, -widthY / 2.0);
      convexPolygon.addVertex(lengthX / 2.0, -widthY / 2.0);
      convexPolygon.update();
      return convexPolygon;
   }

   public static PlanarRegionsList createBodyPathPlannerTestEnvironment()
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      double extrusionDistance = -0.05;

      // starting plane
      generator.translate(1.0, 0.5, 0.0);
      generator.addRectangle(2.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();

      // long plane on the start side
      generator.translate(2.5, -3.5, 0.0);
      generator.addRectangle(1.0 + extrusionDistance, 13.0 + extrusionDistance);
      generator.identity();

      // narrow passage
      double wallSeparation = 0.4;
      double wallWidth = 0.5;
      double wallHeight = 1.0;

      generator.translate(4.5, 2.5, 0.0);
      generator.addRectangle(3.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.rotate(0.5 * Math.PI, Axis.Y);
      generator.translate(-0.5 * wallHeight, 0.5 * (wallSeparation + wallWidth), 0.0);
      generator.addRectangle(wallHeight, wallWidth);
      generator.translate(0.0, -2.0 * 0.5 * (wallSeparation + wallWidth), 0.0);
      generator.addRectangle(wallHeight, wallWidth);
      generator.identity();

      // high-sloped ramp
      generator.translate(3.5, 0.5, 0.5);
      generator.rotate(-0.25 * Math.PI, Axis.Y);
      generator.addRectangle(Math.sqrt(2.0), 1.0);
      generator.identity();
      generator.translate(4.5, 0.5, 1.0);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();
      generator.translate(5.5, 0.5, 0.5);
      generator.rotate(0.25 * Math.PI, Axis.Y);
      generator.addRectangle(Math.sqrt(2.0), 1.0);
      generator.identity();

      // large step down
      double stepDownHeight = 0.4;
      generator.translate(3.5, -1.5, 0.0);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(1.0, 0.0, - stepDownHeight);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(1.0, 0.0, stepDownHeight);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();

      // large step up
      double stepUpHeight = 0.4;
      generator.translate(3.5, -3.5, 0.0);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(1.0, 0.0, stepUpHeight);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(1.0, 0.0, - stepUpHeight);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();

      // barrier
      double barrierHeight = 1.5;
      double barrierWidth = 0.8;

      generator.translate(4.5, -5.5, 0.0);
      generator.addRectangle(3.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(0.0, 0.0, 0.5 * barrierHeight);
      generator.rotate(0.5 * Math.PI, Axis.Y);
      generator.addRectangle(barrierHeight, barrierWidth);
      generator.identity();

      // long gap
      generator.translate(3.5, -7.5, 0.0);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.translate(2.0, 0.0, 0.0);
      generator.addRectangle(1.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();

      // long plane on the goal side
      generator.translate(6.5, -3.5, 0.01);
      generator.addRectangle(1.0 + extrusionDistance, 13.0 + extrusionDistance);
      generator.identity();

      // goal plane
      generator.translate(8.0, -3.5, 0.01);
      generator.addRectangle(2.0 + extrusionDistance, 1.0 + extrusionDistance);
      generator.identity();

      PlanarRegionsList obstacleCourse =  generator.getPlanarRegionsList();

      // overhang, wide barrier, and stepping stones
      generator.translate(4.5, -9.5, 2.5);
      generator.addRectangle(1.5, 0.8);
      generator.identity();

      wallSeparation = 0.9;
      wallWidth = 0.2;
      wallHeight = 1.0;

      generator.translate(3.0, -9.5, 0.0);
      generator.rotate(0.5 * Math.PI, Axis.Y);
      generator.translate(-0.5 * wallHeight, 0.5 * (wallSeparation + wallWidth), 0.0);
      generator.addRectangle(wallHeight, wallWidth);
      generator.translate(0.0, -2.0 * 0.5 * (wallSeparation + wallWidth), 0.0);
      generator.addRectangle(wallHeight, wallWidth);
      generator.identity();

      PlanarRegionsList cinderBlockField = generateCinderBlockField(3.0, -9.5, 0.25, 0.2, 11, 4, 0.0);
      for (int i = 0; i < cinderBlockField.getNumberOfPlanarRegions(); i++)
      {
         obstacleCourse.addPlanarRegion(cinderBlockField.getPlanarRegion(i));
      }

      return obstacleCourse;
   }

   public static void main(String[] args)
   {
      SimulationConstructionSet scs = new SimulationConstructionSet(new Robot("exampleRobot"));
      PlanarRegionsList planarRegionsList = createBodyPathPlannerTestEnvironment();
      PlanarRegionsListDefinedEnvironment environment = new PlanarRegionsListDefinedEnvironment("ExamplePlanarRegionsListEnvironment", planarRegionsList, 1e-5,
                                                                                                false);
      TerrainObject3D terrainObject3D = environment.getTerrainObject3D();
      scs.addStaticLinkGraphics(terrainObject3D.getLinkGraphics());
      scs.setGroundVisible(false);

      Graphics3DObject startAndEndGraphics = new Graphics3DObject();
      startAndEndGraphics.translate(0.5, 0.5, 0.5);
      startAndEndGraphics.addSphere(0.2, YoAppearance.Green());
      startAndEndGraphics.identity();
      startAndEndGraphics.translate(8.5, -3.5, 0.5);
      startAndEndGraphics.addSphere(0.2, YoAppearance.Red());
      scs.addStaticLinkGraphics(startAndEndGraphics);

      scs.startOnAThread();
   }
}