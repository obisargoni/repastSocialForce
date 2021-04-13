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

	
	public HashMap<String, double[]> getAnglesDistancesToObstaclesFromCoordBreaing(Coordinate c, double b) {
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
        List<Geometry> obstGeomsPoints = pedMinDist.getObstacleGeometries(fieldOfVisionApprox, SpaceBuilder.pedObstructionPointsGeography);		
        
    	// Then sample field of vision and initialise arrays of distances that correspond to each angle
        double[] d2s = new double[fovAngles.size()-1];
    	double[] displacementDistances = new double[fovAngles.size()-1];
    	
    	// Initialise values as -1 so can identify default values
    	for (int i=0; i<d2s.length; i++) {
    		d2s[i] = -1;
    		displacementDistances[i] = -1;
    	}
    	
    	// First find the minimum displacement distance and associated angle for obstruction geometries
    	double start = System.currentTimeMillis();
    	pedMinDist.dispalcementDistancesToGeometries(obstGeomsPoints, fovAngles, d2s, displacementDistances);
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
		
		HashMap<String, double[]> output = getAnglesDistancesToObstaclesFromCoordBreaing(c, b);
		
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
	 * Test ped next to wall identifies obstacles points at expected angles and distances.
	 */
	@Test
	void testDistanceToObject2() {
		Coordinate c = new Coordinate(530509.6389832983, 180908.11179611267);
		double b = 3.9209401504483683;
		
		HashMap<String, double[]> output = getAnglesDistancesToObstaclesFromCoordBreaing(c, b);
		
		double[] expectedAngles = {2.611943211452621, 3.6429872589076853, 4.143583854519489, 4.520772370555282, 4.765214246949521, 5.229937089444116};
		double[] expectedDistances = {-1.0, 0.5471421433667476, 0.2539724955402363, 0.20371170329094668, 0.1932528727861567};
		
		for (int i=0; i< Math.max(expectedAngles.length, output.get("angles").length); i++) {
			assert Double.compare(output.get("angles")[i], expectedAngles[i]) == 0;
		}
		
		for (int i=0; i< Math.max(expectedDistances.length, output.get("distances").length); i++) {
			assert Double.compare(output.get("distances")[i], expectedDistances[i]) == 0;
		}
	}

}
