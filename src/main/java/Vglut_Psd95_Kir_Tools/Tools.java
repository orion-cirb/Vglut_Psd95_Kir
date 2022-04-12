package Vglut_Psd95_Kir_Tools;


import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import mcib3d.geom.Point3D;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.Measure2Distance;
import mcib3d.geom2.measurements.MeasureCentroid;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationDistance;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.image3d.ImageHandler;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author phm
 */
public class Tools {

    public boolean canceled = true;
    public double minDotVol = 0.21;
    public double maxDotVol = Double.MAX_VALUE;
    public String Vglut_thMethod = "Moments";
    public String Pds95_thMethod = "Moments";
    public String Kir_thMethod = "Moments";
    public double sphereDistSynap = 10;
    public Calibration cal;
        
    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));

    public CLIJ2 clij2 = CLIJ2.getInstance();
    
    
     /**
     * return objects population in an ClearBuffer image
     * @param imgCL
     * @return pop objects population
     */

    public Objects3DIntPopulation getPopFromClearBuffer(ClearCLBuffer imgCL) {
        ClearCLBuffer labels = clij2.create(imgCL.getWidth(), imgCL.getHeight(), imgCL.getDepth());
        clij2.connectedComponentsLabelingBox(imgCL, labels);
        // filter size
        ClearCLBuffer labelsSizeFilter = clij2.create(imgCL.getWidth(), imgCL.getHeight(), imgCL.getDepth());
        clij2.release(imgCL);
        clij2.excludeLabelsOutsideSizeRange(labels, labelsSizeFilter, minDotVol, maxDotVol);
        clij2.release(labels);
        ImagePlus img = clij2.pull(labelsSizeFilter);
        clij2.release(labelsSizeFilter);
        ImageHandler imh = ImageHandler.wrap(img);
        flush_close(img);
        Objects3DIntPopulation pop = new Objects3DIntPopulation(imh);
        imh.closeImagePlus();
        return(pop);
    }  
    
    
    
    public String[] dialog(String[] chs) {
        String[] channelNames = {"Vglut", "PSD95", "Kir4.1"};
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 100, 0);
        gd.addImage(icon);
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String chNames : channelNames) {
            gd.addChoice(chNames+" : ", chs, chs[index]);
            index++;
        }
        gd.addMessage("--- Dots filter size ---", Font.getFont(Font.MONOSPACED), Color.blue);
        gd.addNumericField("Min. volume size (µm3) : ", minDotVol, 3);
        gd.addNumericField("Max. volume size (µm3) : ", maxDotVol, 3);
        gd.addMessage("StarDist model", Font.getFont("Monospace"), Color.blue);
        gd.addMessage("--- Threshold methods ---", Font.getFont(Font.MONOSPACED), Color.blue);
        String [] thMethods = new AutoThresholder().getMethods();
        gd.addChoice("Threshold method for Vglut : ", thMethods, Vglut_thMethod); 
        gd.addChoice("Threshold method for PSD95 : ", thMethods, Pds95_thMethod); 
        gd.addChoice("Threshold method for Kir 4.1 : ", thMethods, Kir_thMethod);
        gd.addNumericField("Sphere radius from synapse to Kir 4.1 (µm) : ", sphereDistSynap, 3);
        gd.showDialog();
        if (gd.wasCanceled())
            canceled = true;
        String[] chChoices = new String[channelNames.length];
        for (int n = 0; n < chChoices.length; n++) 
            chChoices[n] = gd.getNextChoice();
        minDotVol = gd.getNextNumber();
        maxDotVol = gd.getNextNumber();
        Vglut_thMethod = gd.getNextChoice();
        Pds95_thMethod = gd.getNextChoice();
        Kir_thMethod = gd.getNextChoice();
        sphereDistSynap = gd.getNextNumber();
        return(chChoices);
    }
     
    // Flush and close images
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }

    /**
     * Threshold 
     * USING CLIJ2
     * @param imgCL
     * @param thMed
     * @param fill 
     */
    public ClearCLBuffer threshold(ClearCLBuffer imgCL, String thMed) {
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        return(imgCLBin);
    }
    
    /* Median filter 
     * Using CLIJ2
     * @param ClearCLBuffer
     * @param sizeXY
     * @param sizeZ
     */ 
    public ClearCLBuffer median_filter(ClearCLBuffer  imgCL, double sizeXY, double sizeZ) {
        ClearCLBuffer imgCLMed = clij2.create(imgCL);
        clij2.mean3DBox(imgCL, imgCLMed, sizeXY, sizeXY, sizeZ);
        clij2.release(imgCL);
        return(imgCLMed);
    }
    
    
/**
     * Find images in folder
     * @param imagesFolder
     * @param imageExt
     * @return 
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No Image found in "+imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt))
                images.add(imagesFolder + File.separator + f);
        }
        return(images);
    }
    

    /**
     * Difference of Gaussians 
     * Using CLIJ2
     * @param imgCL
     * @param size1
     * @param size2
     * @return imgGauss
     */ 
    private ClearCLBuffer DOG(ClearCLBuffer imgCL, double size1, double size2) {
        ClearCLBuffer imgCLDOG = clij2.create(imgCL);
        clij2.differenceOfGaussian3D(imgCL, imgCLDOG, size1, size1, size1, size2, size2, size2);
        clij2.release(imgCL);
        return(imgCLDOG);
    }
    
    
     /**
     * Find volume of objects  
     * @param dotsPop
     * @return vol
     */
    
    public double findDotsVolume (Objects3DIntPopulation dotsPop) {
        IJ.showStatus("Findind object's volume");
        List<Double[]> results = dotsPop.getMeasurementsList(new MeasureVolume().getNamesMeasurement());
        double sum = results.stream().map(arr -> arr[1]).reduce(0.0, Double::sum);
        System.out.println("NB "+dotsPop.getNbObjects()+" sum of volume "+sum);
        return(sum);
    }
    
    
     /**
     * Find sum intensity of objects  
     * @param dotsPop
     * @param img
     * @return intensity
     */
    
    public double findDotsIntensity (Objects3DIntPopulation dotsPop, ImagePlus img) {
        IJ.showStatus("Findind object's intensity");
        ImageHandler imh = ImageHandler.wrap(img);
        List<Double[]> results = dotsPop.getMeasurementsIntensityList(new MeasureIntensity().getNamesMeasurement(), imh);
        double sum = results.stream().map(arr -> arr[1]).reduce(0.0, Double::sum);
        System.out.println("NB "+dotsPop.getNbObjects()+" sum of intensity "+sum);
        return(sum);
    }

     /**
     * Find dots population
     * @param imgDot
     * @return 
     */
     public Objects3DIntPopulation findDots(ImagePlus imgDot, String th) {
        IJ.showStatus("Finding dots");
        ClearCLBuffer imgCL = clij2.push(imgDot);
        ClearCLBuffer imgDOG = DOG(imgCL, 1, 2);
        clij2.release(imgCL);
        ClearCLBuffer imgCLBin = threshold(imgDOG, th);
        clij2.release(imgDOG);
        ImagePlus imgBin = clij2.pull(imgCLBin);
        clij2.release(imgCLBin);
        imgBin.setCalibration(cal);
        ClearCLBuffer maskCL = clij2.push(imgBin);
        Objects3DIntPopulation dotsPop = getPopFromClearBuffer(maskCL);
        clij2.release(maskCL);
        dotsPop.setVoxelSizeXY(cal.pixelWidth);
        dotsPop.setVoxelSizeZ(cal.pixelDepth);
        flush_close(imgBin);
        return(dotsPop);
     }
     

     /**
      * 
     * Find synapses point and distance between vglut and homer
     * @param pre
     * @param post
     * @return Synapses
     */
    
    public Objects3DIntPopulation findSynapses (Objects3DIntPopulation prePop, Objects3DIntPopulation postPop) {
        Objects3DIntPopulation synapList = new Objects3DIntPopulation();
        // find synapses 
        IJ.showStatus("Finding synapses ...");
        MeasurePopulationColocalisation colocalisation = new MeasurePopulationColocalisation(prePop, postPop);
        AtomicInteger counter = new AtomicInteger(0);
        prePop.getObjects3DInt().stream().forEach(obj1 -> {
            double[] coloc = colocalisation.getValuesObject1Sorted(obj1.getLabel(), true);
            // Take only the max coloc
            if (coloc.length != 0) {
                Object3DInt obj2 = postPop.getObjectByLabel((float) coloc[1]);
                float xc = (float)(new MeasureCentroid(obj1).getCentroidAsPoint().x + new MeasureCentroid(obj2).getCentroidAsPoint().x)/2;
                float yc = (float)(new MeasureCentroid(obj1).getCentroidAsPoint().y + new MeasureCentroid(obj2).getCentroidAsPoint().y)/2;
                Object3DInt synapse = new Object3DInt(new VoxelInt(Math.round(xc), Math.round(yc), 1, 255));
                synapse.setLabel(counter.floatValue());
                synapse.setVoxelSizeXY(cal.pixelWidth);
                synapse.setVoxelSizeZ(cal.pixelDepth);
                synapList.addObject(synapse);
                counter.addAndGet(1);
            }
        });
        System.out.println(synapList.getNbObjects() +" synpases found");
        return(synapList);
    }
    
    /**
     * Find kir dots at sphereDistSynap
     * @param kirPop
     * @param synapPop
     * @param imgKir
     * @return 
     */
    public ArrayList<Synapse_Vglut_Psd95> findKirSynapses2 (Objects3DIntPopulation kirPop, Objects3DIntPopulation synapPop, ImagePlus imgKir) {
        ArrayList<Synapse_Vglut_Psd95> synapList = new ArrayList<Synapse_Vglut_Psd95>();
        IJ.showStatus("Finding kir dots at "+sphereDistSynap+ " of synapses ...");
        //Objects3DIntPopulation kirSynapPop = new Objects3DIntPopulation();
        ImageHandler imh = ImageHandler.wrap(imgKir);
        MeasurePopulationDistance distances = new MeasurePopulationDistance(synapPop, kirPop, sphereDistSynap);
        synapPop.getObjects3DInt().stream().forEach(obj1 -> {
            int nbKir = 0;
            DescriptiveStatistics kirDistStats = new DescriptiveStatistics();
            DescriptiveStatistics kirIntStats = new DescriptiveStatistics();
            DescriptiveStatistics kirAreaStats = new DescriptiveStatistics();
            double[] dist = distances.getValuesObject1(obj1.getLabel());
            
            for (double d : dist)
                System.out.println("distance = "+d);
            new WaitForUserDialog(Kir_thMethod).show();
            nbKir++;
            kirDistStats.addValue(dist[2]);
            kirIntStats.addValue(new MeasureIntensity(kirPop.getObjectByLabel((float)dist[1]), imh).getValueMeasurement(MeasureIntensity.INTENSITY_SUM));
            kirAreaStats.addValue(new MeasureVolume​(kirPop.getObjectByLabel((float)dist[1])).getValueMeasurement(MeasureVolume.VOLUME_UNIT));
            Synapse_Vglut_Psd95 synapse = new Synapse_Vglut_Psd95((int)obj1.getLabel(), new MeasureCentroid(obj1).getCentroidAsPoint(),nbKir,0,0,0,0,0);
            if (nbKir != 0) {
                synapse.setAreaKir(kirAreaStats.getSum());
                synapse.setMeanIntKir(kirIntStats.getMean());
                synapse.setSdIntKir(kirIntStats.getStandardDeviation());
                synapse.setMeanDistKir(kirDistStats.getMean());
                synapse.setSdDistKir(kirDistStats.getStandardDeviation());
            }
            synapList.add(synapse);
        });
        return(synapList);
        
    }
    
    /**
     * Find kir dots at sphereDistSynap
     * @param kirPop
     * @param synapPop
     * @param imgKir
     * @return 
     */
    public ArrayList<Synapse_Vglut_Psd95> findKirSynapses (Objects3DIntPopulation kirPop, Objects3DIntPopulation synapPop, ImagePlus imgKir) {
        ArrayList<Synapse_Vglut_Psd95> synapList = new ArrayList<Synapse_Vglut_Psd95>();
        IJ.showStatus("Finding kir dots at "+sphereDistSynap+ " of synapses ...");
        ImageHandler imh = ImageHandler.wrap(imgKir);
        synapPop.getObjects3DInt().stream().forEach(obj1 -> {
            DescriptiveStatistics kirDistStats = new DescriptiveStatistics();
            DescriptiveStatistics kirIntStats = new DescriptiveStatistics();
            DescriptiveStatistics kirAreaStats = new DescriptiveStatistics();
            kirPop.getObjects3DInt().stream().forEach(obj2 -> {
                Measure2Distance dist = new Measure2Distance(obj1, obj2);
                double d = dist.getValue(Measure2Distance.DIST_CC_UNIT);
                if (d <= sphereDistSynap) {
                    kirDistStats.addValue(d);
                    kirIntStats.addValue(new MeasureIntensity(obj2, imh).getValueMeasurement(MeasureIntensity.INTENSITY_SUM));
                    kirAreaStats.addValue(new MeasureVolume(obj2).getValueMeasurement(MeasureVolume.VOLUME_UNIT));
                }
            });
            Synapse_Vglut_Psd95 synapse = new Synapse_Vglut_Psd95((int)obj1.getLabel(), new MeasureCentroid(obj1).getCentroidAsPoint(),0,0,0,0,0,0);
            if( kirDistStats.getN() != 0) {
                synapse.setNbKir((int)kirDistStats.getN());
                synapse.setAreaKir(kirAreaStats.getSum());
                synapse.setMeanDistKir(kirDistStats.getMean());
                synapse.setSdDistKir(kirDistStats.getStandardDeviation());
                synapse.setMeanIntKir(kirIntStats.getMean());
                synapse.setSdIntKir(kirIntStats.getStandardDeviation());
            }
            synapList.add(synapse);
        });
        return(synapList);    
    }
    
    /**
     * Draw dots
     */
    private void drawDots(Point3D dot, ImagePlus img){
        int r = 6;
        int x = dot.getRoundX() - r/2;
        int y = dot.getRoundY() - r/2;
        int z = dot.getRoundZ() + 1;
        img.setSlice(z);
        ImageProcessor ip = img.getProcessor();
        ip.setColor(255);
        ip.drawOval(x, y, r, r);
        img.updateAndDraw();
    }
    
    
    /**
     * Save dots Population in image
     * @param pop1
     * @param pop2
     * @param pop3
     * @param imageName
     * @param img 
     * @param outDir 
     */
    public void saveImgObjects(Objects3DIntPopulation pop1, Objects3DIntPopulation pop2, Objects3DIntPopulation pop3, ArrayList<Synapse_Vglut_Psd95> synapses,
            String imageName, ImagePlus img, String outDir) {
        //create image objects population
        ImageHandler imgObj1 = pop1.drawImage();
        ImageHandler imgObj2 = pop2.drawImage();
        ImageHandler imgObj3 = pop3.drawImage();
        ImagePlus imgObj4 = IJ.createImage("Synapse",img.getWidth(), img.getHeight(),img.getNSlices(), 8);
        if (!synapses.isEmpty()) {
            synapses.forEach(synapse -> drawDots(synapse.getSynPt(), imgObj4));
        }
        // save image for objects population
        ImagePlus[] imgColors = {imgObj1.getImagePlus(), imgObj2.getImagePlus(), imgObj3.getImagePlus(), null, null, null, imgObj4};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(img.getCalibration());
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDir + imageName + "_Objects.tif"); 
        imgObj1.closeImagePlus();
        imgObj2.closeImagePlus();
        imgObj3.closeImagePlus();
        flush_close(imgObj4);
        flush_close(imgObjects);
    }
    
    /**
     * Write data
     *  
    */
    public void writeData(ArrayList<Synapse_Vglut_Psd95> synapses, int vglut, int psd95, int kir, String Name, BufferedWriter results) throws IOException {
        synapses.forEach(synapse -> {
            try {
                results.write(Name+"\t"+vglut+"\t"+psd95+"\t"+kir+"\t"+synapse.getIndex()+"\t"+synapse.getNbKir()+"\t"+synapse.getAreaKir()+"\t"+synapse.getMeanIntKir()+
                        "\t"+synapse.getSdIntKir()+"\t"+synapse.getMeanDistKir()+"\t"+synapse.getSdDistKir()+"\n");
            } catch (IOException ex) {
                Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        });
        results.flush();
    }

}
