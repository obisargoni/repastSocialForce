package repastInterSim.environment;

import org.apache.commons.math3.util.FastMath;

// Class for sorting functions that do standard vector operations
public class Vector {
	
	/*
	 * Compute the magnitude of a vector
	 */
	public static double mag(double[] V) {
		double magV = 0;
		for (int i = 0; i<V.length;i++) {
			magV += FastMath.pow(V[i], 2);
		}
		magV = FastMath.sqrt(magV);
		return magV;		
	}
	
	/*
	 * Compute the unit vector for an input vector
	 */
	public static double[] unitV (double[] V) {
		double magV = mag(V);
		
		double[] uV = new double[V.length];
		for (int i = 0; i<V.length;i++) {
			uV[i] = V[i] / magV;
		}
		
		return uV;
	}
	
	/*
	 * Calculate the dot product between two vectors
	 */
	public static double dotProd(double[] U, double[] V) {
		double dP = 0;
		for (int i = 0;i<FastMath.min(U.length, V.length);i++) {
			dP += U[i] * V[i];
		}
		return dP;
	}
	
    /*
     * Generic function to sum two vectors that are the same size
     * 
     * @param a vector of doubles
     * @param b vector of doubles
     * 
     * @return vector of doubles that is the sum of the two input vectors
     */
    public static double[] sumV(double[] a, double[] b) {
    	
    	if (a.length != b.length) {
    		return null;
    	}
    	else {
    		
    	}
        double[] c = new double[a.length];
            for (int i = 0; i < a.length; i++) {
            c[i] = a[i] + b[i];}
        return c;
    }
    
    /*
     * Rotate vector by the given angle
     * 
     * @param double[] u. 2D vector to rotate.
     * @param double theta. The angle in radians to rotate by 
     */
    public static double[] rotate2D(double[] u, double theta) {
    	double[] v = {Math.cos(theta)*u[0] - Math.sin(theta)*u[1],Math.sin(theta)*u[0] + Math.cos(theta)*u[1]};
    	return v;    	
    }
    
    public static double angleBetweenNorthAndUnitVector(double[] u) {
    	double[] n = {0,1};
    	
    	double signX = Math.signum(u[0]);
        double signY = Math.signum(u[1]);
        
        double dp = Vector.dotProd(n, u);
        double a = Math.acos(dp);
        
        // Dot product gives angle between vectors. We need angle clockwise from north
        if (signX == 1) {
        	return a;
        }
        else if (signX == -1) {
        	return 2*Math.PI - a;
        }
        else {
        	return a;
        }
    }
    
    public static double[] difference(double[] u, double[] v) {
    	
    	// Initialise the difference vector
    	double[] diff = new double[Math.min(u.length, v.length)];
    	
    	for (int i = 0 ; i < Math.min(u.length, v.length); i++) {
    		diff[i] = u[i] - v[i];
    	}
    	
    	return diff;
    }
    
    public static double distanceBetweenVectors(double[] u, double[] v) {
    	double[] diff = difference(u,v);
    	
    	double dist = mag(diff);
    	
    	return dist;
    }

	public static double angleBetweenTwoVectors(double[] v1, double[] v2) {
		// Convert to unit vectors
		double cos_ = dotProd(v1,v2)/ mag(v1) / mag(v2);
		double a = Math.acos(cos_);
		
		return a;
	}
	
	public static double angleBetweenTwoVectorsDegree(double[] v1, double[] v2) {
		
		double a = angleBetweenTwoVectors(v1, v2);
		a = Math.toDegrees(a);
		return a;
	}
	
	public static double acuteRangeBetweenAngles(double a1, double a2) {
		// Map to range 0-2pi
		a1 = a1%(2*Math.PI);
		a2 = a2%(2*Math.PI);
		
		double r = Math.abs(a1-a2);
	    if (r>Math.PI) {
	        r = 2*Math.PI - r;
	    }
	    return r;
	}
    
}
