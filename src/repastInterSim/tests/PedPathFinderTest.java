package repastInterSim.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repastInterSim.environment.GISFunctions;
import repastInterSim.environment.OD;
import repastInterSim.environment.PedObstruction;
import repastInterSim.environment.Road;
import repastInterSim.environment.RoadLink;
import repastInterSim.environment.SpatialIndexManager;
import repastInterSim.environment.contexts.PedObstructionContext;
import repastInterSim.environment.contexts.PedestrianDestinationContext;
import repastInterSim.environment.contexts.RoadContext;
import repastInterSim.environment.contexts.RoadLinkContext;
import repastInterSim.main.GlobalVars;
import repastInterSim.pathfinding.PedPathFinder;
import repastInterSim.pathfinding.RoadNetworkRoute;

class PedPathFinderTest {
	
	Geography<Road> roadGeography = null;
	Geography<RoadLink> roadLinkGeography = null;
	Geography<OD> odGeography = null;
	Geography<PedObstruction> pedObstructGeography = null;
	
	String testGISDir = ".//data//test_gis_data//";
	String pedestrianRoadsPath = null;
	String vehicleRoadsPath = null;
	String roadLinkPath = null;
	String serialisedLookupPath = null;

	void setUpRoads() throws Exception {
		pedestrianRoadsPath = testGISDir + "topographicAreaVehicle.shp";
		vehicleRoadsPath = testGISDir + "topographicAreaPedestrian.shp";
		serialisedLookupPath = testGISDir + "road_link_rodas_cache.serialised";
		
		// Get road geography
		Context<Road> testRoadContext = new RoadContext();
		GeographyParameters<Road> GeoParamsRoad = new GeographyParameters<Road>();
		roadGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("testRoadGeography", testRoadContext, GeoParamsRoad);
		roadGeography.setCRS(GlobalVars.geographyCRSString);		
		
		// Load vehicle origins and destinations
		try {
			GISFunctions.readShapefile(Road.class, vehicleRoadsPath, roadGeography, testRoadContext);
			GISFunctions.readShapefile(Road.class, pedestrianRoadsPath, roadGeography, testRoadContext);
		} catch (MalformedURLException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void setUpRoadLinks() throws Exception {
		
		roadLinkPath = testGISDir + "mastermap-itn RoadLink Intersect Within with orientation.shp";
		
		// Initialise test road link geography and context
		Context<RoadLink> roadLinkContext = new RoadLinkContext();
		GeographyParameters<RoadLink> GeoParams = new GeographyParameters<RoadLink>();
		roadLinkGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("roadLinkGeography", roadLinkContext, GeoParams);
		roadLinkGeography.setCRS(GlobalVars.geographyCRSString);
				
		GISFunctions.readShapefile(RoadLink.class, roadLinkPath, roadLinkGeography, roadLinkContext);
		SpatialIndexManager.createIndex(roadLinkGeography, RoadLink.class);
		
	}
	
	void setUpODs() throws MalformedURLException, FileNotFoundException {
		
		// Initialise OD context and geography
		Context<OD> ODContext = new PedestrianDestinationContext();
		GeographyParameters<OD> GeoParamsOD = new GeographyParameters<OD>();
		odGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("testODGeography", ODContext, GeoParamsOD);
		odGeography.setCRS(GlobalVars.geographyCRSString);
		
		// Load vehicle origins and destinations
		String testODFile = testGISDir + "test_ped_OD1.shp";
		GISFunctions.readShapefile(OD.class, testODFile, odGeography, ODContext);
		SpatialIndexManager.createIndex(odGeography, OD.class);
	}
	
	void setUpPedObstructions() throws MalformedURLException, FileNotFoundException {
		// Ped Obstruction context stores GIS linestrings representing barriers to pedestrian movement
		Context<PedObstruction> pedObstructContext = new PedObstructionContext();
		GeographyParameters<PedObstruction> GeoParams = new GeographyParameters<PedObstruction>();
		pedObstructGeography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("pedObstructGeography", pedObstructContext, GeoParams);
		pedObstructGeography.setCRS(GlobalVars.geographyCRSString);
		
		
		// Load ped obstructions data
		String testPedObstructFile = testGISDir + "boundaryPedestrianVehicleArea.shp";
		GISFunctions.readShapefile(PedObstruction.class, testPedObstructFile, pedObstructGeography, pedObstructContext);
		SpatialIndexManager.createIndex(pedObstructGeography, PedObstruction.class);
	}
	
	@Test
	void testPedestrianRoadDestinationCoordinate() throws Exception {
		
		setUpRoads();
		setUpODs();
		setUpPedObstructions();
		
		File vehcileRoadsFile = new File(vehicleRoadsPath);
		File pedestrianRoadsFile = new File(pedestrianRoadsPath);
		File serialisedLoc = new File(serialisedLookupPath);
		
		// Select origin coordinate and pedestrian road geometry
		List<OD> ods = new ArrayList<OD>();
		odGeography.getAllObjects().iterator().forEachRemaining(ods::add);
		Coordinate o = ods.get(0).getGeom().getCoordinate();
		
		String roadLinkID = "osgb4000000030238946";
		
		List<Road> currentPedRoads = RoadNetworkRoute.getRoadLinkPedestrianRoads(roadGeography, vehcileRoadsFile, pedestrianRoadsFile, serialisedLoc, roadLinkID);
		
		// Expected coords
		Coordinate c1 = new Coordinate(530507.95, 180893.35);
		Coordinate c3 = new Coordinate(530468.0087569832, 180871.8784368495);
		Coordinate c4 = new Coordinate(530482.8182132206, 180870.19519803385);
		Coordinate[] expectedCoords = {c1,null,c3,c4};
		Coordinate destCoord = null;
		for (int i=0;i<currentPedRoads.size(); i++) {
			Road r = currentPedRoads.get(i);
			destCoord = PedPathFinder.pedestrianRoadDestinationCoordinate(o, r.getGeom(), pedObstructGeography);
			if(i==1) {
				assert destCoord==expectedCoords[i];
				continue;
			}
			assert destCoord.equals2D(expectedCoords[i]);
		}
	}

	@Test
	void testGetTacticalDestinationCoodinateOptions() throws Exception {
		setUpRoads();
		setUpRoadLinks();
		setUpODs();
		setUpPedObstructions();
		
		File vehcileRoadsFile = new File(vehicleRoadsPath);
		File pedestrianRoadsFile = new File(pedestrianRoadsPath);
		File serialisedLoc = new File(serialisedLookupPath);
		
		// Select pedestrian origins and destinations to test
		List<OD> ods = new ArrayList<OD>();
		odGeography.getAllObjects().iterator().forEachRemaining(ods::add);
		Coordinate o = ods.get(0).getGeom().getCoordinate();
		
		// Select section of road link network we are testing
		String roadLinkID = "osgb4000000030238946";
		List<RoadLink> rls = new ArrayList<RoadLink>();
		for (RoadLink rl: roadLinkGeography.getAllObjects()) {
			if (rl.getFID().contentEquals(roadLinkID)) {
				rls.add(rl);
			}
		}
		
		// Select pedestrian roads used in testing
		List<Road> currentPedRoads = RoadNetworkRoute.getRoadLinkPedestrianRoads(roadGeography, vehcileRoadsFile, pedestrianRoadsFile, serialisedLoc, roadLinkID);
		
		// Get the tactical desinations for the origin coord and this road link
		HashMap<String, List<Coordinate>> destOptions = PedPathFinder.getTacticalDestinationCoodinateOptions(o, currentPedRoads, rls, pedObstructGeography);
		
		// Check that two keys and two coordinates returned
		Set<String> expectedKeys = new HashSet<String>();
		expectedKeys.add("cross");
		expectedKeys.add("nocross");
		assert destOptions.keySet().containsAll(expectedKeys);
		
		// Check the coorrdinates are as expected
		assert destOptions.get("cross").size() == 1;
		assert destOptions.get("nocross").size() == 2;
	}

}