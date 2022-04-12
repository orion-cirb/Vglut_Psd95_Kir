/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Vglut_Psd95_Kir_Tools;

import mcib3d.geom.Point3D;
import mcib3d.geom.Voxel3D;

/**
 *
 * @author phm
 */
public class Synapse_Vglut_Psd95 {
    
    int index = 0;
    // Synapse Point
    private Point3D synPt;
    // Number of Kir at d synMaxDist
    private int nbKir;
    // Kir area of Kir
    private double areaKir;
    // Kir Intensity
    private double meanIntKir;
    private double sdIntKir;
    // Meandistance to Kir 4.1
    private double meanDistKir;
    private double sdDistKir;
	
	public Synapse_Vglut_Psd95(int index, Point3D synPt, int nbKir, double areaKir, double meanIntKir, double sdIntKir, double meanDistKir, double sdDistKir) {
            this.index = index;
            this.synPt = synPt;
            this.nbKir = nbKir;
            this.areaKir = areaKir;
            this.meanIntKir = meanIntKir;
            this.sdIntKir = sdIntKir;
            this.meanDistKir = meanDistKir;
            this.sdDistKir = sdDistKir;
	}
        
        public void setIndex(int index) {
            this.index = index;
	}
        
        public void setSynPt(Point3D synPt) {
            this.synPt = synPt;
	}
        
        public void setNbKir(int nbKir) {
            this.nbKir = nbKir;
        }
        
        public void setAreaKir(double areaKir) {
            this.areaKir = areaKir;
	}
        
        public void setMeanIntKir(double meanIntKir) {
            this.meanIntKir = meanIntKir;
	}
        public void setSdIntKir(double sdIntKir) {
            this.sdIntKir = sdIntKir;
	}
        public void setMeanDistKir(double meanDistKir) {
            this.meanDistKir = meanDistKir;
	}
        
        public void setSdDistKir(double sdDistKir) {
            this.sdDistKir = sdDistKir;
	}
        
        
        public int getIndex() {
            return index;
        }
        
        public Point3D getSynPt() {
            return synPt;
        }
        
        public int getNbKir() {
            return nbKir;
        }
        
        public double getAreaKir() {
            return areaKir;
        }
        
        public double getMeanIntKir() {
            return meanIntKir;
        }
        
        public double getSdIntKir() {
            return sdIntKir;
        }
        
        public double getMeanDistKir() {
            return meanDistKir;
        }
        
        public double getSdDistKir() {
            return sdDistKir;
        }
}
