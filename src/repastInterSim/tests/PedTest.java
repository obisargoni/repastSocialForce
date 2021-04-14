package repastInterSim.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repastInterSim.agent.Ped;
import repastInterSim.environment.GISFunctions;
import repastInterSim.main.IO;
import repastInterSim.main.SpaceBuilder;

class PedTest {
	
	Context<Object> context = new DefaultContext<Object>();
	
	String testGISDir = ".//data//test_gis_data//";
	String pedestrianRoadsPath = null;
	String vehicleRoadsPath = null;
	String roadLinkPath = null;
	String pavementLinkPath = null;
	String pedJPath = null;
	String serialisedLookupPath = null;
	
	void setUpProperties() throws IOException {
		IO.readProperties();
	}
	
	Ped createPedAtLocation(boolean minimisesDistance, Coordinate c, double b) {
		// Create pedestrian and point it towards it's destination (which in this case is just across the road)
		Ped ped = EnvironmentSetup.createPedestrian(3,4,minimisesDistance);
		
		// Move ped to position and bearing that has caused an error in the simulation
        Point pt = GISFunctions.pointGeometryFromCoordinate(c);
		Geometry pGeomNew = pt.buffer(ped.getRad());
        GISFunctions.moveAgentToGeometry(SpaceBuilder.geography, pGeomNew, ped);
		ped.setLoc();
		ped.setBearing(b);
		return ped;
	}

	
	public HashMap<String, double[]> wrapperDispalcementDistancesToGeometries(Coordinate c, double b, boolean intersects) {
		// Setup the environment
		try {
			IO.readProperties();
			SpaceBuilder.fac = new GeometryFactory();
			
			EnvironmentSetup.setUpObjectGeography();
			EnvironmentSetup.setUpRoads();
			EnvironmentSetup.setUpPedObstructions();
			EnvironmentSetup.setUpPedObstructionPoints();

			EnvironmentSetup.setUpORRoadLinks();
			EnvironmentSetup.setUpORRoadNetwork(false);
			
			EnvironmentSetup.setUpITNRoadLinks();
			EnvironmentSetup.setUpITNRoadNetwork(true);
			
			EnvironmentSetup.setUpPedJunctions();
			EnvironmentSetup.setUpPavementLinks("pedNetworkLinks.shp");
			EnvironmentSetup.setUpPavementNetwork();
						
			EnvironmentSetup.setUpPedODs();
			EnvironmentSetup.setUpVehicleODs("mastermap-itn RoadNode Intersect Within.shp");
			
			EnvironmentSetup.setUpCrossingAlternatives("crossing_lines.shp");
			
			EnvironmentSetup.assocaiteRoadsWithRoadLinks();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create pedestrian and point it towards it's destination (which in this case is just across the road)
		Ped pedMinDist = EnvironmentSetup.createPedestrian(3,4,false);
		
		// Move ped to position and bearing that has caused an error in the simulation
        Point pt = GISFunctions.pointGeometryFromCoordinate(c);
		Geometry pGeomNew = pt.buffer(pedMinDist.getRad());
        GISFunctions.moveAgentToGeometry(SpaceBuilder.geography, pGeomNew, pedMinDist);
		pedMinDist.setLoc();
		pedMinDist.setBearing(b);	
		
		// Get distance to nearest object and compare
		List<Double> fovAngles = pedMinDist.sampleFoV();
		
		// Get filters groups of objects
        Polygon fieldOfVisionApprox = pedMinDist.getPedestrianFieldOfVisionPolygon(b);
        List<Geometry> obstGeomsPoints = null;		
        
    	// Then sample field of vision and initialise arrays of distances that correspond to each angle
        double[] d2s = null;
    	double[] displacementDistances = null;
    	
    	// First find the minimum displacement distance and associated angle for obstruction geometries
    	double start = System.currentTimeMillis();
    	if(intersects) {
    		obstGeomsPoints = pedMinDist.getObstacleGeometries(fieldOfVisionApprox, SpaceBuilder.pedObstructGeography);
            d2s = new double[fovAngles.size()];
        	displacementDistances = new double[fovAngles.size()];
    		pedMinDist.displacementDistancesToGeometriesIntersect(obstGeomsPoints, fovAngles, d2s, displacementDistances);
    	}
    	else {
    		obstGeomsPoints = pedMinDist.getObstacleGeometries(fieldOfVisionApprox, SpaceBuilder.pedObstructionPointsGeography);
            d2s = new double[fovAngles.size()-1];
        	displacementDistances = new double[fovAngles.size()-1];
        	
        	// Initialise values as -1 so can identify default values
        	for (int i=0; i<d2s.length; i++) {
        		d2s[i] = -1;
        		displacementDistances[i] = -1;
        	}
    		pedMinDist.dispalcementDistancesToGeometries(obstGeomsPoints, fovAngles, d2s, displacementDistances);
    	}
    	
    	double durObstGeog = System.currentTimeMillis() - start;
        System.out.print("Duration get ped obst by geography:" + durObstGeog + "\n");
        
        HashMap<String, double[]> output = new HashMap<String, double[]>();
        double[] angles = new double[fovAngles.size()];
        for (int i=0; i<fovAngles.size(); i++) {
        	angles[i] = fovAngles.get(i);
        }
        output.put("angles", angles);
        output.put("distances", d2s);
        output.put("dispDistances", displacementDistances);
        
        return output;
	}
	
	void validateOutput(HashMap<String, double[]> output, double[] expectedAngles, double[] expectedDistances) {
		for (int i=0; i< Math.max(expectedAngles.length, output.get("angles").length); i++) {
			assert Double.compare(output.get("angles")[i], expectedAngles[i]) == 0;
		}
		
		for (int i=0; i< Math.max(expectedDistances.length, output.get("distances").length); i++) {
			assert Double.compare(output.get("distances")[i], expectedDistances[i]) == 0;
		}
	}
	
	/*
	 * Test that ped far from any walls doesn't identify obstacle geometries
	 */
	@Test
	void testDistanceToObject1() {
		Coordinate c = new Coordinate(530522.0, 180918);
		double b = 3.9209401504483683;
		
		HashMap<String, double[]> output = wrapperDispalcementDistancesToGeometries(c, b, false);
		
		// Set up expected angles
		List<Double> eA = new ArrayList<Double>();
		double theta = (2*Math.PI*75) / 360; // Copied from ped init function
		double angRes = (2*Math.PI) / (36 / 3); // Also copied from init function
		double a = b - theta;
		while (a <= b+theta) {
			eA.add(a);
			a+=angRes;
		}
		
		double[] expectedAngles = new double[eA.size()];
		for(int i=0; i<expectedAngles.length; i++) {
			expectedAngles[i] = eA.get(i);
		}
		
		double[] expectedDistances = new double[eA.size()-1];
		for(int i=0; i<expectedDistances.length; i++) {
			expectedDistances[i] = -1.0;
		}
		
		validateOutput(output, expectedAngles, expectedDistances);
	}
	
	/*
	 * Test that ped far from any walls doesn't identify obstacle geometries.
	 * 
	 * Test the intersect method.
	 */
	@Test
	void testDistanceToObject1b() {
		Coordinate c = new Coordinate(530522.0, 180918);
		double b = 3.9209401504483683;
		
		HashMap<String, double[]> output = wrapperDispalcementDistancesToGeometries(c, b, true);
		
		// Set up expected angles
		List<Double> eA = new ArrayList<Double>();
		double theta = (2*Math.PI*75) / 360; // Copied from ped init function
		double angRes = (2*Math.PI) / (36 / 3); // Also copied from init function
		double a = b - theta;
		while (a <= b+theta) {
			eA.add(a);
			a+=angRes;
		}
		
		double[] expectedAngles = new double[eA.size()];
		for(int i=0; i<expectedAngles.length; i++) {
			expectedAngles[i] = eA.get(i);
		}
		
		double[] expectedDistances = new double[eA.size()];
		for(int i=0; i<expectedDistances.length; i++) {
			expectedDistances[i] = 10.0;
		}
		
		validateOutput(output, expectedAngles, expectedDistances);
	}
	
	/*
	 * Test ped next to wall identifies obstacles points at expected angles and distances.
	 */
	@Test
	void testDistanceToObject2() {
		Coordinate c = new Coordinate(530509.6389832983, 180908.11179611267);
		double b = 3.9209401504483683;
		
		HashMap<String, double[]> output = wrapperDispalcementDistancesToGeometries(c, b, false);
		
		double[] expectedAngles = {2.611943211452621, 3.6429872589076853, 4.143583854519489, 4.520772370555282, 4.765214246949521, 5.229937089444116};
		double[] expectedDistances = {-1.0, 0.5471421433667476, 0.2539724955402363, 0.20371170329094668, 0.1932528727861567};
		
		for (int i=0; i< Math.max(expectedAngles.length, output.get("angles").length); i++) {
			assert Double.compare(output.get("angles")[i], expectedAngles[i]) == 0;
		}
		
		for (int i=0; i< Math.max(expectedDistances.length, output.get("distances").length); i++) {
			assert Double.compare(output.get("distances")[i], expectedDistances[i]) == 0;
		}
	}
	
	/*
	 * Test ped next to wall identifies obstacles points at expected angles and distances.
	 * 
	 * Use the intersect method.
	 */
	@Test
	void testDistanceToObject2b() {
		Coordinate c = new Coordinate(530509.6389832983, 180908.11179611267);
		double b = 3.9209401504483683;
		
		HashMap<String, double[]> output = wrapperDispalcementDistancesToGeometries(c, b, true);
		
		// Set up expected angles
		List<Double> eA = new ArrayList<Double>();
		double theta = (2*Math.PI*75) / 360; // Copied from ped init function
		double angRes = (2*Math.PI) / (36 / 3); // Also copied from init function
		double a = b - theta;
		while (a <= b+theta) {
			eA.add(a);
			a+=angRes;
		}
		
		double[] expectedAngles = new double[eA.size()];
		for(int i=0; i<expectedAngles.length; i++) {
			expectedAngles[i] = eA.get(i);
		}
		
		double[] expectedDistances = {10.0, 10.0, 0.5246611367487147, 0.2458762406829704, 0.1946127776812781, 0.20691517646066288};
		
		validateOutput(output, expectedAngles, expectedDistances);
	}
	
	@Test
	void testDistanceOpDetectsContactWithPed() {
		
		// Create three pedestrians,two that intersect and one that doesnt.
		Coordinate c1 = new Coordinate(530509.6389832983, 180908.11179611267);
		Point p1 = GISFunctions.pointGeometryFromCoordinate(c1);
		double r1 = 0.2;
		Geometry g1 = p1.buffer(r1);
		
		Coordinate c2 = new Coordinate(530509.846, 180908.255);
		Point p2 = GISFunctions.pointGeometryFromCoordinate(c2);
		double r2 = 0.5;
		Geometry g2 = p2.buffer(r2);

		
		Coordinate c3 = new Coordinate(530512, 180907);
		Point p3 = GISFunctions.pointGeometryFromCoordinate(c3);
		double r3 = 0.5;
		Geometry g3 = p3.buffer(r3);
		
		Coordinate c4 = new Coordinate(530513, 180907);
		Point p4 = GISFunctions.pointGeometryFromCoordinate(c4);
		double r4 = 0.5;
		Geometry g4 = p4.buffer(r4);
		
		// Now check distOp and intersects
		DistanceOp dist12 = new DistanceOp(g1, g2);
		assert (dist12.distance()==0) & (g1.intersects(g2));
		
		DistanceOp dist13 = new DistanceOp(g1, g3);
		assert (dist13.distance()>0) & (g1.intersects(g3)==false);
		
		// g3 and g4 should just touch, does identify as touching
		DistanceOp dist34 = new DistanceOp(g3, g4);
		assert (dist34.distance()==0) & (g3.intersects(g4)==true);
		
	}

}
