package repastInterSim.environment;

import com.vividsolutions.jts.geom.Coordinate;

import repast.simphony.space.gis.Geography;

public class CrossingAlternative {
	
	// Coordinates at which the ca meets pavement
	private Coordinate c1 = null;
	private Coordinate c2 = null;
	
	// Default type is unmarked
	private String type = "unmarked";
	
	// id of the road link this crossing is located on
	private String roadLinkID;

	public CrossingAlternative(){
				
	}
	
	public String getRoadLinkID() {
		return roadLinkID;
	}

	public void setRoadLinkID(String roadLinkID) {
		this.roadLinkID = roadLinkID;
	}

	public Integer getvFlow(Geography<Road> rG) {
		// Get the number of vehicles on the road link
		Road r = null;
    	for (Road ri: rG.getAllObjects()) {
    		if (ri.getRoadLinkID().contentEquals(this.roadLinkID)){
    			r = ri;
    		}
    	}
    	int vehicleNumber = r.getRoadLinksVehicleCount();
		return vehicleNumber;
	}


	public Coordinate getC1() {
		return c1;
	}

	public void setC1(Coordinate c1) {
		this.c1 = c1;
	}

	public Coordinate getC2() {
		return c2;
	}

	public void setC2(Coordinate c2) {
		this.c2 = c2;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	

}
