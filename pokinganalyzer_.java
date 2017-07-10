import com.sun.media.imageioimpl.common.*;
import ij.*;
import ij.IJ;
import ij.ImagePlus.*;
import ij.ImageStack.*;
import ij.gui.*;
import ij.measure.*;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.util.*;
import java.awt.*;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener; 
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.lang.*;
import java.sql.*;
import javax.media.jai.widget.*;
import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.apache.commons.io.FilenameUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class pokinganalyzer_ implements PlugIn {

public float [][] pokingworm(ImagePlus imp, short [] impdim, int [][] PosI){
// input: the image sequence, the dimensions of the image sequence: # of images, width, height, and the x and y position of the neuron
// output: rvalues are: the corrected and normalized intensity trace of the GCaMP channel, the normalized intensity of the other channel, the distance between the actuator and the neuron, and the distance between the actuator and the neuron in y only


//**** defines variables ****//
		short [][][] position;									// positions in the image sequence
		position = new short [impdim[0]+1][5][4];				// [in image #][...][...]
		float [][] rValues;										// returned values
		rValues = new float[4][impdim[0]];						// [the corrected fluorescence of the neuron in the green channel, fluorescence of the neuron in the red channel, distance between neuron and actuator in ..., total distance between neuron and actuator][in image #]									
		float [] BaselineFluorescence;							// Baseline fluorescence of the neuron for baseline correction
		BaselineFluorescence = new float [2];					// [in the green channel, in the red channel]
		float ThreshGreen = 0;									// Threshold for background vs fluorescence in the green channel
		float ThreshRed = 0;									// Threshold for background vs fluorescence in the red channel
		double [] sdf;											// ...
		sdf = new double [2];									// [in the green channel, in the red channel]

//**** read out the image sequence and define a threshold for deleting all background intensities ****// 
		
		for (short j=0;j<=(short)(impdim[0]-1); j++){
			int [][] pixelvalue = getPixelValuej(imp, impdim, j, (short) (0), (short)(impdim[1]-1), (short) (0), (short)(impdim[2]-1));
			for (short x=0; x<=(short)(impdim[1]-1); x++){
				for (short y=0;y<=(short)((impdim[2]-1)/2);y++){
					ThreshGreen += pixelvalue[x][y];
					ThreshRed += pixelvalue[x][(short)(y+(impdim[2]+1)/2)];
				}
			}
			short progress3 = (short) (j*20/impdim[0]);
			progressBar3.setValue(progress3);			
		}
		ThreshGreen=ThreshGreen/(impdim[0]*impdim[1]*impdim[2]/2);
		ThreshRed=ThreshRed/(impdim[0]*impdim[1]*impdim[2]/2);

//**** all the image analysis happens here ****//
					   		
		for (short j=0;j<=(impdim[0]-1); j++){

			final short [][] fimpdim;							
			fimpdim = new short [3][2];
			float maxmean5=0;
			float mean5 [][];
			mean5 = new float [impdim[1]+1][impdim[2]+1];
			short ystart = (short)(PosI[0][1]-10);
			short yend = (short)(PosI[0][1]+10);
			short xstart = (short)(PosI[0][0]-10);
			short xend = (short)(PosI[0][0]+10);	
			boolean [][][] in;
			in = new boolean [2][impdim[1]][impdim[2]];			
			for (short ij=0; ij<=2; ij++){
				fimpdim [ij][0]=0;
				fimpdim [ij][1]=(short)(impdim[ij]-1);
			}
			int [][] pixelvalue = getPixelValuej(imp, impdim, j, (short) (0), (short)(impdim[1]-1), (short) (0), (short)(impdim[2]-1));				//readout image j from the image sequence
			pixelvalue = ThresholdPixelValues(fimpdim, ThreshGreen, ThreshRed, pixelvalue);															//delete background by threshold
			pixelvalue = HasNeighborsPixelValues(fimpdim, pixelvalue);														//delete pixel intensities that are not surrounded by other pixels with intensities bigger than 1		
			if(j>=1){
				ystart = (short)(position [(j-1)][0][3]-10);
				yend = (short)(position [(j-1)][0][3]+10);
				xstart = (short)(position [(j-1)][0][2]-10);
				xend = (short)(position [(j-1)][0][2]+10);
			}
			if (ystart <= 21){
				rValues [1][0] = 65001;
				j = impdim [0];
				progressBar3.setValue(100);
			}
			else if(yend >= impdim[2]-22){
				rValues [1][0] = 65001;
				j = impdim [0];
				progressBar3.setValue(100);
			}
			else if(xstart <= 21){
				rValues [1][0] = 65001;
				j = impdim [0];
				progressBar3.setValue(100);
			}
			else if(xend >= impdim[1]-22){
				rValues [1][0] = 65001;
				j = impdim [0];
				progressBar3.setValue(100);
			}
			else {

//**** find the brightest spot and thereby the neuron ****//
				for (short y=ystart;y<=yend;y++){
					for (short x=xstart; x<=xend; x++){
						mean5[x][y]=0;
						for (short yi=(short)(y-5);yi<=(short)(y+5);yi++){
							for (short xi=(short)(x-5); xi<=(short)(x+5); xi++){
								mean5[x][y]+=pixelvalue [xi][yi];
							}
						}
						mean5[x][y]/=121;
						if (mean5[x][y]>=maxmean5){
							maxmean5 = mean5[x][y];
							position [j][0][2]=x;
							position [j][0][3]=y;
						}
					}
				}
				double [] sd;
				sd = new double [2];
				for (short y=(short)(position [j][0][3]-2);y<=(short)(position [j][0][3]+2);y++){
					for (short x=(short)(position [j][0][2]-2); x<=(short)(position [j][0][2]+2); x++){
						sd[0]+=((mean5[position [j][0][2]][position [j][0][3]]-pixelvalue [x][y])*(mean5[position [j][0][2]][position [j][0][3]]-pixelvalue [x][y]));
					}
				}
				sd[0]=Math.sqrt(sd[0]/25);		
				maxmean5 = 0;
				for (short y=(short)(ystart+(impdim[2]/2-1));y<=(short)(yend+(impdim[2]/2-1));y++){
					for (short x=xstart; x<=xend; x++){
						mean5[x][y]=0;
						for (short yi=(short)(y-5);yi<=(short)(y+5);yi++){
							for (short xi=(short)(x-5); xi<=(short)(x+5); xi++){
								mean5[x][y]+=pixelvalue [xi][yi];
							}
						}
						mean5[x][y]/=121;
						if (mean5[x][y]>=maxmean5){
							maxmean5 = mean5[x][y];
							position [j][0][0]=x;
							position [j][0][1]=y;
						}
					}
				}
				for (short y=(short)(position [j][0][1]-2);y<=(short)(position [j][0][1]+2);y++){
					for (short x=(short)(position [j][0][0]-2); x<=(short)(position [j][0][0]+2); x++){
						sd[1]+=((mean5[position [j][0][0]][position [j][0][1]]-pixelvalue [x][y])*(mean5[position [j][0][0]][position [j][0][1]]-pixelvalue [x][y]));
					}
				}
				sd[1]=Math.sqrt(sd[1]/25);		
				maxmean5 = 0;	
				if (PosI[1][0]>=1){
					rValues[3][j] = (float)((position[j][0][3]-PosI[1][1])*(position[j][0][3]-PosI[1][1]));
					rValues[3][j] = (float)(Math.sqrt(rValues[3][j]));
					rValues[2][j] = (float)(((position[j][0][2]-PosI[1][0])*(position[j][0][2]-PosI[1][0]))+((position[j][0][3]-PosI[1][1])*(position [j][0][3]-PosI[1][1])));		
					rValues[2][j] = (float)(Math.sqrt(rValues[2][j]));
				}

//**** find pixels that belong to the neuron ****//
				
				byte size = 20;
				for (byte i=0; i<=1;i++){
					short sua = 1;
					short [][] a;
					a = new short [((size*size)+1)*((size*size)+1)][2];					
					a [0][0] = position [j][0][2-(2*i)];
					a [0][1] = position [j][0][3-(2*i)];
					short sub = 0;													
					while (sua>sub){
						if((short)(a[sub][0]-1)>=0){
							if (in [i][a[sub][0]][a[sub][1]] == false){
								if (pixelvalue[a[sub][0]-1][a[sub][1]]>=(mean5[position [j][0][2-(2*i)]][position [j][0][3-(2*i)]]-(sd[i]/4))){
									a [sua][0] = (short)(a[sub][0]-1);
									a [sua][1] = a[sub][1];						
									sua++;
								}
							}	
						}
						else{
							rValues [1][0] = 65001;
							j = impdim [0];
							progressBar3.setValue(100);		
						}
						if((short)(a[sub][0]-1)<=(short)(impdim[1]-1)){
							if (in [i][a[sub][0]][a[sub][1]] == false){						
								if (pixelvalue[a[sub][0]+1][a[sub][1]]>=(mean5[position [j][0][2-(2*i)]][position [j][0][3-(2*i)]]-(sd[i]/4))){
									a [sua][0] = (short)(a[sub][0]+1);
									a [sua][1] = a[sub][1];						
									sua++;
								}
							}	
						}
						else{
							rValues [1][0] = 65001;
							j = impdim [0];
							progressBar3.setValue(100);						
						}
						if((short)(a[sub][1]-1)>=0){		
							if (in [i][a[sub][0]][a[sub][1]] == false){
								if (pixelvalue[a[sub][0]][a[sub][1]-1]>=(mean5[position [j][0][2-(2*i)]][position [j][0][3-(2*i)]]-(sd[i]/4))){
									a [sua][0] = a[sub][0];
									a [sua][1] = (short)(a[sub][1]-1);					
									sua++;
								}
							}	
						}
						else{
							rValues [1][0] = 65001;
							j = impdim [0];
							progressBar3.setValue(100);								
						}
						if((short)(a[sub][1]-1)<=(short)(impdim[2]-1)){		
							if (in [i][a[sub][0]][a[sub][1]] == false){
								if (pixelvalue[a[sub][0]][a[sub][1]+1]>=(mean5[position [j][0][2-(2*i)]][position [j][0][3-(2*i)]]-(sd[i]/4))){
									a [sua][0] = a[sub][0];
									a [sua][1] = (short)(a[sub][1]+1);					
									sua++;
								}
							}
						}
						else{
							rValues [1][0] = 65001;											//1==left feild of view
							j = impdim [0];
							progressBar3.setValue(100);						
						}
						in [i][a[sub][0]][a[sub][1]] = true;						
						sub++;									
						if (sub == sua){
							if (sub<=4){
								int maxip = 0;
								for (short y=(short)(-7);y<=(short)(7);y++){
									for (short x=(short)(-7);x<=(short)(7);x++){
										if (pixelvalue[position [j][0][2-(2*i)]+x][position [j][0][3-(2*i)]+y]>=maxip){
											a [0][0] = (short)(position [j][0][2-(2*i)]+x);
											a [0][1] = (short)(position [j][0][3-(2*i)]+y);
											maxip = pixelvalue[position [j][0][2-(2*i)]+x][position [j][0][3-(2*i)]+y];
										}
									}
								}
								if (a[0][0]==position [j][0][2-(2*i)]){
									sub = sua;									
								}
								else if (a[0][1]==position [j][0][3-(2*i)]){
									sub = sua;
								}
								else {
									sub=0;
									sua=1;
									for (byte lk=0;lk<=4;lk++){
										in[i][a[lk][0]][a[lk][1]]=false;
									}
								}	
							}
						}
						if (sua>=5000){
							rValues [1][0] = 65002;														//2==neuron too big
						}
						if (rValues [1][0] >= 65000){
							i = 3;
							sub = (short)(sua + 1); 
							progressBar3.setValue(100);	
						}
					}
				}
			}	

//**** estimate neuron's intensity ****//

			if (rValues [1][0] <= 65000){
				float [] meanbg;
				meanbg = new float [2];
				short [] count;
				count = new short [4];
				for (short y=(short)(-20);y<=(short)(20);y++){
					for (short x=(short)(-20);x<=(short)(20);x++){
						for (byte i=0;i<=1;i++){												
							if(in[i][position[j][0][2-(2*i)]+x][position[j][0][3-(2*i)]+y]==true){
								if (pixelvalue [position[j][0][2-(2*i)]+x][position[j][0][3-(2*i)]+y]<=65000){
									rValues[i][j]+=pixelvalue [position[j][0][2-(2*i)]+x][position[j][0][3-(2*i)]+y];
									count[2*i]++;						
								}
								else{
									rValues [1][0] = 61003;													//3==too bright
								}
							}							
							else {
								meanbg[i]+=pixelvalue [position[j][0][2-(2*i)]+x][position[j][0][3-(2*i)]+y];
								count[(2*i)+1]++;															
							}						
						}
					}
				}			
				rValues[0][j]/=count[0];
				rValues[1][j]/=count[2];
				meanbg[0]/=(count[1]);
				meanbg[1]/=(count[3]);
				rValues[0][j]-=meanbg[0];
				rValues[1][j]-=meanbg[1];
				if (j==49){
					for (short ij=24;ij<=j;ij++){
						for (short ijj=0;ijj<=1;ijj++){
							BaselineFluorescence[ijj]+=rValues[ijj][ij];
						}	
					}
					BaselineFluorescence[0]/=25;
					BaselineFluorescence[1]/=25;
					float [] ma;
					ma = new float [2];
					for (short ij=0;ij<=j;ij++){
						for (short ijj=0;ijj<=1;ijj++){						
							rValues[ijj][ij]/=BaselineFluorescence[ijj];
						}						
						if(ij>=24){
							for (short ijj=0;ijj<=1;ijj++){								
								ma[ijj]+=rValues[ijj][ij];
							}
						}	
					}
					for (short ijj=0;ijj<=1;ijj++){								
						ma[ijj]/=25;
					}					
					for (short ij=24;ij<=j;ij++){
						for (short ijj=0;ijj<=1;ijj++){						
							sdf[ijj]+=((ma[ijj]-rValues[ijj][ij])*(ma[ijj]-rValues[ijj][ij]));
						}	
					}
					for (short ijj=0;ijj<=1;ijj++){						
						sdf[ijj]=Math.sqrt(sdf[ijj]/25);	
					}	
					for (short ij=0;ij<=j;ij++){
						rValues[0][ij]= (float)(rValues[0][ij]+((double)(sdf[0]/sdf[1])*(1-rValues[1][ij])));
					}
				}
				if (j>=50){
					for (short ijj=0;ijj<=1;ijj++){							
						rValues[ijj][j]/=BaselineFluorescence[ijj];
					}
					rValues[0][j]=(float)(rValues[0][j]+((double)(sdf[0]/sdf[1])*(1-rValues[1][j])));	
				}
			}			
			else {
				j = impdim [0];
				progressBar3.setValue(100);
			}
			short progress3 = (short) (20+(j*80/impdim[0]));
			progressBar3.setValue(progress3);
		}
	progressBar3.setValue(100);
	return rValues;
}

public int [][] getPixelValuej(ImagePlus imp, short [] impdim, short j, short xmin, short xmax, short ymin, short ymax){
//input: the image sequence, the dimesion of the image sequence: # of images, the width, the height, the image that should be analyzed, the region that should be analyzed
//output: the pixelvalues in the image j in the defined region
	ImageProcessor ip;
	ip = imp.getProcessor();
	int [][] pixelvalue;
	pixelvalue = new int [impdim [1]][impdim [2]]; 
	imp.setSlice(j+1);
	for (short y=ymin; y<=(short)(ymax); y++) {
      	for (short x=xmin; x<=(short)(xmax); x++) {
			pixelvalue [x][y] = (int)(ip.getPixelValue(x, y));
     	}
	}
	return pixelvalue;
}

int smooth(short x, short y, int [][] pixelvalue){
//input: definition of the pixel (x,y), all pixelvalues
//output: pixelvalue of (x,y) being recaclulated by a matrix
	float pvsmoothed = 0;
	pvsmoothed = -1*pixelvalue[x-2][y-2]+0*pixelvalue[x-1][y-2]+1*pixelvalue[x][y-2]+0*pixelvalue[x+1][y-2]-1*pixelvalue[x+2][y-2]+0*pixelvalue[x-2][y-1]+2*pixelvalue[x-1][y-1]+5*pixelvalue[x][y-1]+2*pixelvalue[x+1][y-1]+0*pixelvalue[x+2][y-1]+1*pixelvalue[x-2][y]+5*pixelvalue[x-1][y]+10*pixelvalue[x][y]+5*pixelvalue[x+1][y]+1*pixelvalue[x+2][y]+0*pixelvalue[x-2][y+1]+2*pixelvalue[x-1][y+1]+5*pixelvalue[x][y+1]+2*pixelvalue[x+1][y+1]+0*pixelvalue[x+2][y+1]-1*pixelvalue[x-2][y+2]+0*pixelvalue[x-1][y+2]+1*pixelvalue[x][y+2]+0*pixelvalue[x+1][y+2]-1*pixelvalue[x+2][y+2];																			// pixelvalue[j][x-2][y-2] +0*pixelvalue[j][x-1][y-2] +1*pixelvalue[j][x][y-2] +0*pixelvalue[j][x+1][y-2] -1*pixelvalue[j][x+2][y-2] +0*pixelvalue[j][x-2][y-1] +2*pixelvalue[j][x-1][y-1] +5*pixelvalue[j][x][y-1] +2*pixelvalue[j][x+1][y-1] +0*pixelvalue[j][x+2][y-1] +1*pixelvalue[j][x-2][y] +5*pixelvalue[j][x-1][y] +25*pixelvalue[j][x][y]  +5*pixelvalue[j][x+1][y]  +1*pixelvalue[j][x+2][y]  +0*pixelvalue[j][x-2][y+1] +2*pixelvalue[j][x-1][y+1]  +5*pixelvalue[j][x][y+1]  +2*pixelvalue[j][x+1][y+1] +0*pixelvalue[j][x+2][y+1] -1*pixelvalue[j][x-2][y+2] +0*pixelvalue[j][x-1][y+2]  +1*pixelvalue[j][x][y+2] +0*pixelvalue[j][x+1][y+2] -1*pixelvalue[j][x+2][y+2];	 
	pvsmoothed /=38;	
	if (pvsmoothed<=1){pvsmoothed=0;}
	return (int)(pvsmoothed);	
}

int [][] smoothj(short [] impdim, int [][] pixelvalue){
//input: dimension of the image sequence: # of images, width, height, all pixelvalues
//output: pixelvalue of all pixels in image sequence being recaclulated by a matrix	
	int [][] pvsmooth;
	pvsmooth = new int [impdim[1]+1][impdim[2]+1];	
	for (short y=0;y<=(impdim[2]-1);y++){
		for (short x=0; x<=(impdim[1]-1); x++){
			if (y<=2){
				if(x<=2){
					pvsmooth [x][y] = smooth((short)(x+2), (short)(y+2), pixelvalue);
				}
				else if (x>=(short)(impdim[1]-3)){
					pvsmooth [x][y] = smooth((short)(impdim[1]-3), (short)(y+2), pixelvalue);
				}
				else {
					pvsmooth [x][y] = smooth(x, (short)(y+2), pixelvalue);
				}
			}
			else if (y>=(short)(impdim[2]-3)){
				if(x<=2){
					pvsmooth [x][y] = smooth((short)(x+2), (short)(impdim[2]-3), pixelvalue);
				}
				else if (x>=(short)(impdim[1]-3)){
					pvsmooth [x][y] = smooth((short)(impdim[1]-3), (short)(impdim[2]-3), pixelvalue);
				}
				else {
					pvsmooth [x][y] = smooth(x, (short)(impdim[2]-3), pixelvalue);
				}				
			}					
			else {if(x<=2){
					pvsmooth [x][y] = smooth((short)(x+2), (y), pixelvalue);
				}
				else if (x>=(short)(impdim[1]-3)){
					pvsmooth [x][y] = smooth((short)(impdim[1]-3), (y), pixelvalue);
				}
				else {
					pvsmooth [x][y] = smooth(x, y, pixelvalue);
				}
			}
		}
	}
	return pvsmooth;
}

int [][] ThresholdPixelValues (short [][] impdim, float ThreshGreen, float ThreshRed, int [][] pixelvalue){
//input: dimension of the image sequence: # of images, width, height, a threchold for the GCaMP region of the image, a threshold for the other region, all pixelvalues
//output: pixelvalue either reduced by the threshold or put to 0
		for (short x=0; x<=(impdim[1][1]); x++){
		for (short y=0;y<=(short)((impdim[2][1]-1)/2);y++){
			if(pixelvalue [x][y]<=ThreshGreen){
				pixelvalue [x][y] = 0;
			}
			else {
				pixelvalue [x][y] = (int)(pixelvalue [x][y]-ThreshGreen);
			}
			if(pixelvalue [x][y+(impdim[2][1]+2)/2]<=ThreshRed){
				pixelvalue [x][y+(impdim[2][1]+2)/2] = 0;
			}
			else {
				pixelvalue [x][y+(impdim[2][1]+2)/2] = (int)(pixelvalue [x][y+(impdim[2][1]+2)/2]-ThreshRed);
			}							
		}
	}
	return pixelvalue;
}

int [][] HasNeighborsPixelValues (short [][] impdim, int [][] pixelvalue){			
//input: dimension of the image sequence: # of images, width, height, all pixelvalues
//output: pixelvalue either the original pixelvalue if the pixel has at least one neighbouring pixel that also is above the Threshold, else pixelvalue is 0
	for (short y=impdim[2][0];y<=(short)(impdim[2][1]-1);y++){
		for (short x=impdim[1][0]; x<=(impdim[1][1]-1); x++){
			if (y>=1){if(x>=1){if(x<=(short)(impdim[1][1]-2)){if(y<=(short)(impdim[2][1]-2)){
				if (hasneighbors (x, y, impdim, (int)(1), pixelvalue)<=0){
					pixelvalue [x][y] = 0;
				}
			}}}}
		}
	}
	return pixelvalue;
}

byte hasneighbors (short x, short y, short [][] impdim, int I, int [][] pixelvalue){
//input: definition of the pixel (x,y), the dimension of the image sequence: # of images, width, height, an intensity threshold, all pixelvalues
//output: the # of neighbouring pixel that also have a pixelvalue above the threshold
	byte neighbors = 0;
	if (x<=(impdim[1][1]-1)){
		if (pixelvalue[x+1][y]>=I){
			neighbors++;
		}
	}
	else{
		neighbors++;
	}
	if (x>=(impdim[1][0]+1)){
		if (pixelvalue[x-1][y]>=I){
			neighbors++;
		}
	}
	else{
		neighbors++;
	}			
	if (y<=(impdim[2][1]-1)){
		if (pixelvalue[x][y+1]>=I){
			neighbors++;
		}
	}
	else{
		neighbors++;
	}		
	if (y>=(impdim[2][0]+1)){
		if (pixelvalue[x][y-1]>=I){
			neighbors++;
		}
	}
	else{
		neighbors++;
	}			
	return neighbors;
}


String [] openVideo (){
//output: the file directory and name
	String [] file;
	file = new String [2];
	Frame fram = new Frame();
	FileDialog fd = new FileDialog(fram,"Open image sequence",FileDialog.LOAD);
	fd.setVisible(true);
	file [0] = fd.getDirectory();				//get the path of the file
	file [1] = fd.getFile();					//get the name of the file
	return file;
}

String [] savetable (){
//output: the file directory and name
	String [] file;
	file = new String [2];
	Frame fram = new Frame();
	FileDialog fd = new FileDialog(fram,"Save table",FileDialog.SAVE);
	fd.setFile("Intensity.txt");
	fd.setVisible(true);
	file [0] = fd.getDirectory();				//get the path of the file
	file [1] = fd.getFile();					//get the name of the file	
	return file;
}

private void definecardana(){         					
//defines the analysis card
	c = new GridBagConstraints();
	c.ipady = 20;
	c.weightx = 0.0;
	c.gridwidth = 6;
	c.gridx = 0;
	c.gridy = 0;								
	cardAnalyze.add(textfieldAT, c);
	c.ipady = 0;
	c.weightx = 0.0;
	c.gridwidth = 3;
	c.gridx = 0;
	c.gridy = 1;									
	cardAnalyze.add(textfieldT, c);
	c.weightx = 0.0;
	c.gridwidth = 2;
	c.gridx = 3;
	c.gridy = 1;		        					
	cardAnalyze.add(fieldT, c);
	if (PC == true){
		c.ipady = 0;
		c.weightx = 0.0;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 2;									
		cardAnalyze.add(textfieldPi, c);
		c.weightx = 0.0;
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 2;		        					
		cardAnalyze.add(fieldPi, c);
		c.ipady = 0;
		c.weightx = 0.0;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 3;									
		cardAnalyze.add(textfieldMag, c);
		c.weightx = 0.0;
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 3;		        					
		cardAnalyze.add(fieldMag, c);
		c.ipady = 0;
		c.weightx = 0.0;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 4;									
		cardAnalyze.add(textfieldBin, c);
		c.weightx = 0.0;
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 4;		        					
		cardAnalyze.add(fieldBin, c);
	}	
	c.ipady = 100;
	c.weighty = 0.1; 
	c.weightx = 1.0;
	c.gridwidth = 6;
	c.gridx = 0;
	c.gridy = 5;
	c.anchor = GridBagConstraints.CENTER;
	c.fill = GridBagConstraints.HORIZONTAL;	        					
	cardAnalyze.add(buttonStartAnalysis, c);
	c.ipady = 1;
	c.weighty = 0.02; 
	c.weightx = 0.1;
	c.gridwidth = 3;
	c.gridx = 0;
	c.gridy = 8;
	cardAnalyze.add(textfieldPA, c);
	c.ipady = 1;
	c.weighty = 0.02; 
	c.weightx = 0.15;
	c.gridwidth = 2;
	c.gridx = 3;
	c.gridy = 8;											
	progressBar3.setValue(0); 
	cardAnalyze.add(progressBar3, c);
	cardAnalyze.revalidate();
	cardAnalyze.repaint();   
	dialog.pack();
}

public void initUI(){
//intilizes the user interface
    progressBar3 = new JProgressBar(0, 100);
    progressBar3.setStringPainted(true);	
	cASE1 = new  JCheckBox();
	PosI = new int [2][2];
    textfieldP = new JLabel("Please define the TRN of interest and (if you see it) the actuator.");
    textfieldPC = new JLabel("Define the actuator?");
    textfieldOp = new JLabel("Please open a video."){
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.height = 20;
            return size;
        }
    };
    textfieldAT = new JLabel("Please enter acquisition rate and press Start Analysis.");
    textfieldT = new JLabel("Please enter acquisition rate (Hz).");
    textfieldPi = new JLabel("Please enter the cameras cell size (\u00B5"+"m).");
    textfieldBin = new JLabel("Please enter the binning factor (if used).");
    textfieldMag = new JLabel("Please enter the magnification of the objective.");      
    textfieldS = new JLabel("Save the results");
    textfieldPA = new JLabel("Analyzing calcium signals ...");
    textfieldH = new JLabel("Help");
 	textfieldR = new JLabel("Requirements:");
 	textfieldRa = new JLabel("Required:");
 	textfieldR2 = new JLabel("- A b/w intensity image sequence (as .tif file) of at leat 50 images, where the");
 	textfieldR2a = new JLabel("   calcium-induced fluoresence intensity changes are displayed in the upper half and");
	textfieldR2b = new JLabel("   the fluoresence intensity for correction is displayed in the lower half of the image.");
 	textfieldRb = new JLabel("Expected, but not necessary:");
 	textfieldR3 = new JLabel("- at least 50 images before the first stimulus.");
 	textfieldHa = new JLabel("How to:"); 	
	textfieldH1 = new JLabel("To perform an analysis of a GCaMP timecourse within a neuron while poking a worm:");
	textfieldH2 = new JLabel("- Press the Open a Video button in the first tab (Open video) and choose a .tif file.");
	textfieldH3 = new JLabel("- In the second tab (Define ROIs); define at least the TRN of interest (Press on the Define"); 
	textfieldH3b = new JLabel("   TRN button and click on the TRN within the displayed image).");
	textfieldH4 = new JLabel("- You may also define the position of the actuator so the program can estimate "); 
	textfieldH4b = new JLabel("   the distance between the TRN and the actuator (check the box behind the Define the");
	textfieldH4c = new JLabel("   actuator? question, press on the Define Actuator button and click on the (dark) border"); 
	textfieldH4d = new JLabel("   of the poking channel within the displayed image).");
	textfieldH5 = new JLabel("- In the third tab (Analysis); enter the acquisition rate and press the Start Analysis"); 
	textfieldH5b = new JLabel("   button.");
	textfieldH6 = new JLabel("- Depending on the size of the video the analysis may take a while.");
	textfieldH7 = new JLabel("- In the fourth tab (Results) the results will be displayed when the program has finished -");
	textfieldH7b = new JLabel("    you may save a result table by pressing the Save the Results Table button and choosing"); 
	textfieldH7c = new JLabel("    the folder to save it to.");
	textfieldH8 = new JLabel("- You may then select another TRN within the same video or a new video and repeat the"); 
	textfieldH8b = new JLabel("   analysis.");    
    fieldT = new JSpinner(new SpinnerNumberModel(10.0, 0.01, 100.0, 0.01));
    fieldPi = new JSpinner(new SpinnerNumberModel(6.5, 0.01, 100.0, 0.1)); 
    fieldBin = new JSpinner(new SpinnerNumberModel(4.0, 1, 100.0, 1));
    fieldMag = new JSpinner(new SpinnerNumberModel(63.0, 0.01, 500.0, 0.01));       
	buttonLoad = new JButton("Open a Video");
	buttonTRN = new JButton("Define TRN");
	buttonPC = new JButton("Define Actuator");	
	buttonStartAnalysis = new JButton("Start Analysis");
    buttonSaveTable = new JButton("Save the Result Table");
	dialog = new JDialog(IJ.getInstance(), "Poking Analyzer");
 	dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	dialog.setResizable(false);
    JTabbedPane tabbedPane = new JTabbedPane();
  	cardOpen = new JPanel(new BorderLayout()) {
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width = 512;
            return size;
        }
        
    };
    cardOpen.add(textfieldOp,BorderLayout.NORTH);
    cardOpen.add(buttonLoad,BorderLayout.WEST);	
	gridbag = new GridBagLayout();
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	cardDefine = new JPanel(gridbag);
  	cardAnalyze = new JPanel(gridbag);
   	cardSave = new JPanel();;	   	
   	cardHelp = new JPanel();
   	cardHelp.setLayout(new BoxLayout(cardHelp, BoxLayout.PAGE_AXIS));
    cardHelp.add(textfieldH);
	Dimension minSize = new Dimension(10, 10);
	Dimension prefSize = new Dimension(10, 10);
	Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
	cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));
    cardHelp.add(textfieldR);
	minSize = new Dimension(5, 5);
	prefSize = new Dimension(5, 5);
	maxSize = new Dimension(Short.MAX_VALUE, 5);
	cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));    
    cardHelp.add(textfieldRa);
    cardHelp.add(textfieldR2);
    cardHelp.add(textfieldR2a);
    cardHelp.add(textfieldR2b);
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize)); 
    cardHelp.add(textfieldRb);
    cardHelp.add(textfieldR3);
	minSize = new Dimension(15, 15);
	prefSize = new Dimension(15, 15);
	maxSize = new Dimension(Short.MAX_VALUE, 15);
	cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldHa);
	minSize = new Dimension(5, 5);
	prefSize = new Dimension(5, 5);
	maxSize = new Dimension(Short.MAX_VALUE, 5);
	cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));      
    cardHelp.add(textfieldH1);
	minSize = new Dimension(2, 2);
	prefSize = new Dimension(2, 2);
	maxSize = new Dimension(Short.MAX_VALUE, 2);
	cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));   
    cardHelp.add(textfieldH2);
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH3);
    cardHelp.add(textfieldH3b);
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH4);
    cardHelp.add(textfieldH4b);
    cardHelp.add(textfieldH4c);
    cardHelp.add(textfieldH4d);        
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH5);
    cardHelp.add(textfieldH5b);
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH6);
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH7);
    cardHelp.add(textfieldH7b);
    cardHelp.add(textfieldH7c);    
    cardHelp.add(new Box.Filler(minSize, prefSize, maxSize));  
    cardHelp.add(textfieldH8);
	cardHelp.add(textfieldH8b);
	tabbedPane.addTab("Open video", cardOpen);
    tabbedPane.addTab("Define ROIs", cardDefine);
	tabbedPane.addTab("Analysis", cardAnalyze);
	tabbedPane.addTab("Results", cardSave);
	tabbedPane.addTab("Help", cardHelp);	
    dialog.add(tabbedPane, BorderLayout.CENTER);
	dialog.setResizable(false);
	dialog.pack();
	dialog.setVisible(true);    			
}

private ImagePlus imp; 
private	String [] file;
private	JDialog dialog;
private JPanel cardOpen;
private JPanel cardDefine;
private JPanel cardAnalyze;
private JPanel cardSave;
private JPanel cardHelp;
private JButton buttonLoad;
private JButton buttonTRN;
private JButton buttonPC;
private JButton buttonStartAnalysis;
private JButton buttonSaveTable;
private JButton buttonSavePlot;
private JLabel textfieldP;
private JLabel textfieldPC;
private JLabel textfieldT;
private JLabel textfieldPi;
private JLabel textfieldBin;
private JLabel textfieldMag;
private JLabel textfieldDis;
private JLabel textfieldDisy;
private JLabel textfieldOp;
private JLabel textfieldS;
private JLabel textfieldPR;
private JLabel textfieldPA;
private JLabel textfieldAT;
private JLabel textfieldR;
private JLabel textfieldRa;
private JLabel textfieldR2;
private JLabel textfieldR2a;
private JLabel textfieldR2b;
private JLabel textfieldRb;
private JLabel textfieldR3;
private JLabel textfieldH;
private JLabel textfieldHa;
private JLabel textfieldH1;
private JLabel textfieldH2;
private JLabel textfieldH3;
private JLabel textfieldH3b;
private JLabel textfieldH4;
private JLabel textfieldH4b;
private JLabel textfieldH4c;
private JLabel textfieldH4d;
private JLabel textfieldH5;
private JLabel textfieldH5b;
private JLabel textfieldH6;
private JLabel textfieldH7;
private JLabel textfieldH7b;
private JLabel textfieldH7c;
private JLabel textfieldH8;
private JLabel textfieldH8b;
private ImageIcon ipng;
private JLabel ipngl;
private boolean in;
private boolean TRN;
private boolean PC;
private boolean noPC;
private boolean times;
private JCheckBox cASE1;
private JProgressBar progressBar3;
private short [] impdim;
private Double ti;
private JSpinner fieldT;
private Double Pi;
private JSpinner fieldPi;
private Double Bin;
private JSpinner fieldBin;
private Double Mag;
private JSpinner fieldMag;
private int [][] PosI;
private float [] hi;
private float [][] poking;
private short pok1;
private short endtime;
private double meandis;
private double meandisy;
private MouseListener ml1;
private MouseListener ml;
private GridBagLayout gridbag;
private GridBagConstraints c;
private File jpgfile; 

public void run(String arg) { 
 	initUI();
    buttonLoad.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
			poking = null;
			PC = false;
			TRN = false;
			cardOpen.removeAll();
			cardDefine.removeAll();
			cardAnalyze.removeAll();
			cardSave.removeAll();
			String [] file;
			file = new String [2];                
            file = openVideo();
			String ext = FilenameUtils.getExtension(""+file[1]);
	   	    if (file[0] == null){
				cardOpen.add(textfieldOp,BorderLayout.NORTH);        		
	    		cardOpen.add(buttonLoad,BorderLayout.WEST);	   	    	
				cardOpen.revalidate();
				cardOpen.repaint();
	   	    }
	   	    else if (new String("tif").equals(ext)){	
				imp = IJ.openVirtual(""+file[0]+""+file[1]);
				impdim = new short [3];  
				impdim [1] = (short)imp.getHeight();			//height (y) of the image
				impdim [2] = (short)imp.getWidth();				//width (x) of the image	 
			    impdim [0] = (short)imp.getStackSize();				
				if(impdim[0]>=49){	
					boolean zoom = false;
					if (impdim [1] >= 800){
						zoom = true;
					}
					else if (impdim [2] >= 800){
						zoom = true;
					}
					if (zoom==true){
						float zoomfactor = 0;
						if 	(impdim [1] >= impdim [2]){
							zoomfactor=((float)(impdim[1])/(float)(800)); 
						}
						else{
							zoomfactor=((float)(impdim[2])/(float)(800)); 
						}
						int binf = 0;
						if (zoomfactor>=4){
							binf = 5;
						}
						else if (zoomfactor>=3){
							binf = 4;
						}
						else if (zoomfactor>=2){
							binf = 3;
						}
						else if (zoomfactor>=1){
							binf = 2;
						}													
						ImageProcessor ipz;
						ipz = imp.getProcessor();
						ipz.setInterpolationMethod(ImageProcessor.BILINEAR);
						ipz = ipz.bin(binf);
						ImagePlus imp2 = new ImagePlus("title", ipz);
						IJ.saveAs(imp2, "JPEG", ""+file[0]+""+file[1]+".jpg");
				     	ipng = new ImageIcon(file[0]+""+file[1]+".jpg");	     	
				     	jpgfile = new File (file[0]+""+file[1]+".jpg"); 
				     	ipngl = new JLabel(ipng);					
						cardOpen.add(textfieldOp,BorderLayout.NORTH);        		
			    		cardOpen.add(buttonLoad,BorderLayout.WEST);
			    		cardOpen.add(new JLabel("opened: "+file[0]+""+file[1]),BorderLayout.CENTER);
						cardOpen.add(new JLabel(ipng),BorderLayout.SOUTH);
						cardOpen.revalidate();
						cardOpen.repaint();
					}
					else{
					     	IJ.saveAs(imp, "JPEG", ""+file[0]+""+file[1]+".jpg");
				         	imp.hide();
					     	ipng = new ImageIcon(file[0]+""+file[1]+".jpg");	     	
					     	jpgfile = new File (file[0]+""+file[1]+".jpg"); 
					     	ipngl = new JLabel(ipng);	
							cardOpen.add(textfieldOp,BorderLayout.NORTH);        		
				    		cardOpen.add(buttonLoad,BorderLayout.WEST);
				    		cardOpen.add(new JLabel("opened: "+file[0]+""+file[1]),BorderLayout.CENTER);
							cardOpen.add(new JLabel(ipng),BorderLayout.SOUTH);
							cardOpen.revalidate();
							cardOpen.repaint();
					}
					c = new GridBagConstraints();
					c.ipady = 20;
					c.weightx = 0.0;
					c.gridwidth = 5;
					c.gridx = 0;
					c.gridy = 0;
					cardDefine.add(textfieldP, c);
					c.ipady = 0;
					c.ipadx = 20;				
					c.weightx = 1;
					c.gridwidth = 1;
					c.gridx = 0;
					c.gridy = 1;					
					cardDefine.add(buttonTRN, c);
					c.ipadx = 0;				
					c.weightx = 0.0;		
					c.gridwidth = 2;
					c.gridx = 1;
					c.gridy = 1;								
					cardDefine.add(textfieldPC, c);
					c.ipadx = 0;
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 3;
					c.gridy = 1;				
					cardDefine.add(cASE1, c);	
					cASE1.setSelected(false);
					noPC = true;
					PC = false;
					endtime = (short)(impdim[0]-1);
					cASE1.addItemListener(new ItemListener (){
						public void itemStateChanged(ItemEvent e) {
							if (e.getStateChange() == ItemEvent.SELECTED) {
								buttonPC.setEnabled(true);
								PC = false;
				        		noPC = false;
				        	}
				        	else {
								buttonPC.setEnabled(false);
								PC = false;
								noPC = true;
								textfieldPi.setVisible(false);	        					
								fieldPi.setVisible(false);								
								textfieldMag.setVisible(false);		        					
								fieldMag.setVisible(false);									
								textfieldBin.setVisible(false);	        					
								fieldBin.setVisible(false);									
								cardDefine.revalidate();
								cardDefine.repaint();									
				        	}
						}
					});
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 4;
					c.gridy = 1;	
		   			cardDefine.add(buttonPC, c);
		   			buttonPC.setEnabled(false);
					c.weightx = 0.0;
					c.gridwidth = 5;
					c.gridx = 0;
					c.gridy = 2;	       			
					cardDefine.add(ipngl,c);
					cardDefine.revalidate();
					cardDefine.repaint();
					dialog.pack();
					times = true;						
		        	TRN = false;  							   	    	
				}
		   	    else {
		   	    	IJ.showMessage("Image sequence must have at least 50 images.");
					cardOpen.add(textfieldOp,BorderLayout.NORTH);        		
		    		cardOpen.add(buttonLoad,BorderLayout.WEST);	  	    	
					cardOpen.revalidate();
					cardOpen.repaint();
		   	    }					
	   	    }
	   	    else {
	   	    	IJ.showMessage("The file must be a .tif file.");
				cardOpen.add(textfieldOp,BorderLayout.NORTH);        		
	    		cardOpen.add(buttonLoad,BorderLayout.WEST);	  	    	
				cardOpen.revalidate();
				cardOpen.repaint();
	   	    }	
        }
    });
    buttonTRN.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
		 cardAnalyze.removeAll();
		 cardSave.removeAll();
 		 in = false;
		 ml = new MouseAdapter(){	 	
            @Override
            public void mouseEntered(MouseEvent e) {
          		in=true;
          		ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));	
            }
			@Override
	 			public void mouseExited(MouseEvent e) {
	           	in=false;
	           	ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
            }                   
            @Override
            public void mousePressed(MouseEvent e) {
				if (in == true){
				    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
				    PosI[0][0]=e.getX();
					PosI[0][1]=e.getY();
					TRN = true;
           			if (noPC==false){
               			if (PC==true){
          					definecardana();
               			}
           			}
           			else{
				        definecardana();               				
           			}
           		} 					
			}
            @Override
            public void mouseReleased(MouseEvent e) {
				ipngl.removeMouseListener(ml);
            	ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
            }
        };
		ipngl.addMouseListener(ml);
        }
    });     	
    buttonPC.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent f) {
		 cardAnalyze.removeAll();
		 cardSave.removeAll();
 		 in = false;
 		 ml1 = new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent f) {
            	ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));	
          		in=true;
            }
			@Override
	 			public void mouseExited(MouseEvent f) {
	 			ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
	           	in=false;
            }                   
            @Override
            public void mousePressed(MouseEvent f) {
				if (in == true){
					PC = true;
				    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
				    PosI[1][0]=f.getX();
					PosI[1][1]=f.getY();
					textfieldPi.setVisible(true);	        					
					fieldPi.setVisible(true);								
					textfieldMag.setVisible(true);		        					
					fieldMag.setVisible(true);									
					textfieldBin.setVisible(true);	        					
					fieldBin.setVisible(true);																	
           			if (TRN==true){           				
				        definecardana();				         
           			}
           		} 					
			}
            @Override
            public void mouseReleased(MouseEvent e) {
            	ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
				ipngl.removeMouseListener(ml1);
            }
        };
        ipngl.addMouseListener(ml1);
        }
    });
    buttonStartAnalysis.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {	
			SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
			   protected Boolean doInBackground() throws Exception {
					progressBar3.setValue(0); 
					if (times==true){
						jpgfile.delete();
						jpgfile = null;
					}				
					cardSave.removeAll();
					progressBar3.setValue(0); 
					ti = (Double)(fieldT.getValue());
					if(PC==true){
						Pi = (Double)(fieldPi.getValue());
						Bin = (Double)(fieldBin.getValue());
						Mag = (Double)(fieldMag.getValue());
					}
					float t = (float) (1/ti);
					pok1=200;
					poking = new float [4][impdim[0]];
					poking = pokingworm(imp, impdim, PosI);
					times=false;
					if (poking[1][0]>=65000){
						progressBar3.setValue(0);
						if (poking[1][0]==65001){
							IJ.showMessage("The program could not track the TRN.\nThe neuron left the field of view.");
						}
						if (poking[1][0]==65002){
							IJ.showMessage("The program could not track the TRN.\nIt seems the S/N is too small.");
						}						
						if (poking[1][0]==65003){
							IJ.showMessage("Some regions are oversatuated.");
						}										
					}
					else {
						float maxpoke = 0;
						for (short j=0; j<=(short)(impdim[0]-1); j++){
							if (PC==true){
								poking[2][j]*=(double)((Bin*Pi)/(double)(Mag));
								poking[3][j]*=(double)((Bin*Pi)/(double)(Mag));						
								for(short i=0; i<=0; i++){					
									if(poking[i][j]>=maxpoke){
										maxpoke = poking[i][j];
									}
								}
							}
							else if(poking[0][j]>=maxpoke){
								maxpoke = poking[0][j];							
							}
						}
						int maxgraph = (int)(maxpoke)+1; 
						progressBar3.setValue(100);
						hi = new float [impdim[0]-(pok1-200)];
						XYSeries series1 = new XYSeries("Intensity (green channel - corrected)");
						XYSeries series2 = new XYSeries("Distance Actuator - Neuron");
						XYSeries series3 = new XYSeries("Distance in y Actuator - Neuron");
						for(short j=(short)(pok1-200); j<=(short)(endtime+(pok1-200-1)); j++){
							hi [j-(pok1-200)] = t*(j-(pok1-200));
							series1.add(hi [j-(pok1-200)], poking[0][j]);
						}
						XYSeriesCollection dataset = new XYSeriesCollection();
						dataset.addSeries(series1);
						JFreeChart chart = ChartFactory.createXYLineChart("Relative Fluorescence Changes", "Time [s]", "Relative Fluorescence Intensity", dataset);
						XYPlot plot = (XYPlot)chart.getPlot();
						chart.setBackgroundPaint(new Color(255, 255, 255));
						plot.setBackgroundPaint(Color.white);
						plot.setRangeGridlinePaint(Color.black);
						plot.getRenderer().setSeriesPaint(0, new Color(50, 205, 50));
						plot.getRenderer().setSeriesStroke(0, new BasicStroke(2));
				        plot.getDomainAxis().setRange(0.00, (endtime*t)+0.1);
				        plot.getDomainAxis().setFixedAutoRange(20);
				        plot.getRangeAxis().setRange(0.0, maxgraph);
				        plot.getRangeAxis().setFixedAutoRange(1);
				        plot.setOutlineVisible(false);
						ChartPanel myChart = new ChartPanel(chart, 512, 350, 512, 350, 512, 512, false, true, true, true, true, true);	
						cardSave.add(textfieldS, BorderLayout.NORTH);
						cardSave.add(myChart, BorderLayout.CENTER);
						cardSave.add(buttonSaveTable, BorderLayout.SOUTH);						
						cardSave.add(textfieldDis, BorderLayout.SOUTH);
						if (PC == true){
							for (short j=0; j<=(short)(impdim[0]-1); j++){
								meandis+= poking[2][j];
								meandisy+=poking[3][j];
							}
							meandis/=(double)(impdim[0]);
							meandisy/=(double)(impdim[0]);
							meandis = Math.round(meandis * 10);
							meandis = meandis/10;
							meandisy = Math.round(meandisy * 10);
							meandisy = meandisy/10;							
   							textfieldDis = new JLabel("The mean distance between actuator and cell body was: "+poking[1][0]+" \u00B5"+"m");  							
							cardSave.add(textfieldDis, BorderLayout.SOUTH);							
   							textfieldDisy = new JLabel("The mean distance between actuator and cell body in y was: "+meandisy+" \u00B5"+"m");  							
							cardSave.add(textfieldDisy, BorderLayout.SOUTH);							
						}
						cardSave.revalidate();
						cardSave.repaint();	
					}
					boolean status = true;
					return status;
				}	
			};
			worker.execute();
       	}
    });
    buttonSaveTable.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
	        ResultsTable rt = new ResultsTable();
			rt.setPrecision(4);
			for (short j=0; j<=(short)(endtime); j++){
				int c=0;
				rt.incrementCounter();
				rt.addValue("Time [s]", hi[j]);
				rt.showRowNumbers(false);
				c++;
				rt.addValue("Intensity (F/F0)", poking[0][j]);
				rt.showRowNumbers(false);
				if (PC == true){
					c++;
					rt.addValue("Distance (TRN - Actuator) [\u00B5"+"m]", poking[2][j]);
					rt.showRowNumbers(false);
					c++;
					rt.addValue("Distance in y (TRN - Actuator) [\u00B5"+"m]", poking[3][j]);
					rt.showRowNumbers(false);	
				}				
			}
			rt.show("Intensity");
			IJ.selectWindow("Intensity");
			String [] file;
			file = new String [2];                
            file = savetable();				
			IJ.saveAs("Results", ""+file[0]+""+file[1]);
			IJ.run("Close",""+file[0]+""+file[1]);
    	}
    });
}
}
