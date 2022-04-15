/*
 * Find Vglut in nucleus
 * Vglut in Astro+processes
 * Vglut outside Astro+processes and nucleus
 * Author Philippe Mailly
 */

import Vglut_Psd95_Kir_Tools.Synapse_Vglut_Psd95;
import Vglut_Psd95_Kir_Tools.Tools;
import ij.*;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;



public class Vglut_Psd95_Kir implements PlugIn {
    
    Tools tools = new Tools();
    
    private String imageDir = "";
    public String outDirResults = "";
    private boolean canceled = false;
    public String file_ext = "hgsb";
    public BufferedWriter results_analyze;
   
    
    /**
     * 
     * @param arg
     */
    @Override
    public void run(String arg) {
        try {
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }   
            // Find images with nd extension
            ArrayList<String> imageFile = tools.findImages(imageDir, file_ext);
            if (imageFile == null) {
                IJ.showMessage("Error", "No images found with "+file_ext+" extension");
                return;
            }
            // create output folder
            outDirResults = imageDir + File.separator+ "Results"+ File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Find channel names
            String[] chsName = {"ch00", "ch01", "ch02"};
            
            // Channels dialog
            
            String[] channels = tools.dialog(chsName);
            if ( channels == null || tools.canceled) {
                IJ.showStatus("Plugin cancelled");
                return;
            }
            
            // Write header
            String header= "Image Name\tTotal Vglut\tTotal Psd95\tTotal Kir\t#Synapse\tNb kir at "+tools.sphereDistSynap+" µm\tKir surface\tKir Mean intensity\t"
                    + "Kir SD Intensity\tKir Mean Distance\tKir SD distance\n";
            FileWriter fwNucleusGlobal = new FileWriter(outDirResults + "Synapses_Results.xls", false);
            results_analyze = new BufferedWriter(fwNucleusGlobal);
            results_analyze.write(header);
            results_analyze.flush();
            
            for (String f : imageFile) {
                String rootName = FilenameUtils.getBaseName(f);
                System.out.println(imageDir+rootName+"_"+channels[0]+".tif");
                
                // open Vglut Channel crop border
                System.out.println("--- Opening Vglut channel  ...");
                ImagePlus imgVglut = IJ.openImage(imageDir+rootName+"_"+channels[2]+".tif");
                tools.cal = imgVglut.getCalibration();
                // Crop border
                int cropW = (int)(imgVglut.getWidth() - 2*tools.borderCrop/tools.cal.pixelWidth);
                int cropXY = (int)((imgVglut.getWidth() - cropW)/2);
                Roi roiCrop = new Roi(cropXY, cropXY, cropW, cropW);
                imgVglut.setRoi(roiCrop);
                ImagePlus imgVglutCrop = imgVglut.crop();
                tools.flush_close(imgVglut);
                // Find all Vglut dots
                Objects3DIntPopulation VglutPop = tools.findDots(imgVglutCrop, tools.Vglut_thMethod);
                // Compute parameters
                int VglutDots = VglutPop.getNbObjects();
                System.out.println(VglutDots +" Vglut found");
                                
                // open Psd95 Channel
                System.out.println("--- Opening Psd95 channel  ...");
                ImagePlus imgPsd95 = IJ.openImage(imageDir+rootName+"_"+channels[1]+".tif");
                imgPsd95.setRoi(roiCrop);
                ImagePlus imgPsd95Crop = imgPsd95.crop();
                tools.flush_close(imgPsd95);
                Objects3DIntPopulation Psd95Pop = tools.findDots(imgPsd95Crop, tools.Pds95_thMethod);
                int Psd95Dots = Psd95Pop.getNbObjects();
                System.out.println(Psd95Dots +" Psd95 found");
                tools.flush_close(imgPsd95Crop);
                
                // Find synapses Vglut-Psd95
                Objects3DIntPopulation synapses = tools.findSynapses(VglutPop, Psd95Pop);
                
                // open Kir Channel
                System.out.println("--- Opening Kir channel  ...");
                ImagePlus imgKir = IJ.openImage(imageDir+rootName+"_"+channels[0]+".tif");
                imgKir.setRoi(roiCrop);
                ImagePlus imgKirCrop = imgKir.crop();
                tools.flush_close(imgKir);
                Objects3DIntPopulation KirPop = tools.findDots(imgKirCrop, tools.Kir_thMethod);
                int KirDots = KirPop.getNbObjects();
                System.out.println(KirDots +" Kir found");
                
                
                // Find Kir arround sphereDistSynapse
                ArrayList<Synapse_Vglut_Psd95> kirSynapses = tools.findKirSynapses(KirPop, synapses, imgKirCrop);
                tools.flush_close(imgKirCrop);
                // Save image objects
                tools.saveImgObjects(VglutPop, Psd95Pop, KirPop, kirSynapses, rootName+"_Objects.tif", imgVglutCrop, outDirResults);
                tools.flush_close(imgVglutCrop);
                
                // write data
                tools.writeData(kirSynapses, VglutDots, Psd95Dots, KirDots, rootName, results_analyze);
            }
        } catch (IOException ex) { 
            Logger.getLogger(Vglut_Psd95_Kir.class.getName()).log(Level.SEVERE, null, ex);
        }
        IJ.showStatus("Process done");
    }
}    
