// last update: 11/14/2015
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.sql.*;
import javax.swing.*;
import org.apache.commons.io.FilenameUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ASE_Analysis implements PlugIn {
//******************** defines all global variables 
	private ImagePlus imp; 
	private	String [] file;
	private	JDialog dialog = new JDialog(IJ.getInstance(), "Perfusion Analyzer");		
	private JPanel cardOpen;
	private JPanel cardDefine;
	private JPanel cardAnalyze;
	private JPanel cardSave;
	private JButton buttonLoad = new JButton("Open a video");
	private JButton buttonASE1 = new JButton("ASE 1 cell body");	
	private JButton buttonASE2  = new JButton("ASE 2 cell body");
	private JButton buttonASE1t = new JButton("ASE 1 dendrite");
	private JButton buttonASE2t = new JButton("ASE 2 dendrite");			
	private JButton buttonStartAnalysis = new JButton("Start analysis");
	private JButton buttonSaveTable = new JButton("Save the Result Table");
	private JLabel textfieldP = new JLabel("Please define the ASE neurons.");
	private JLabel textfieldT = new JLabel("Please enter acquisition rate (Hz).");
	private JLabel textfieldOp = new JLabel("Please open a video.");
	private JLabel textfieldASE1 = new JLabel("ASE1 neuron and dendrite.");
	private JLabel textfieldASE2 = new JLabel("ASE2 neuron and dendrite.");
	private JLabel textfieldAT = new JLabel("Please enter acquisition rate and press Start Analysis.");
	private JLabel textfieldPR = new JLabel("Reading out the image sequence ...");
	private JLabel textfieldPF = new JLabel("Analyzing the neuron fluorescence ...");	   
	private JLabel textfieldS = new JLabel("Save the results");	
	private ImageIcon ipng;
	private JLabel ipngl;
	private boolean in;
	private boolean times;
	private boolean ASE2j;	
	private boolean NRj;
	private boolean ASE1tj;
	private JCheckBox cASE2 = new  JCheckBox();
	private JCheckBox cNR = new JCheckBox();	
	private JCheckBox cASE1t = new  JCheckBox();
	private JProgressBar progressBar1 = new JProgressBar(0, 100);
	private JProgressBar progressBar4 = new JProgressBar(0, 100);
	private short [] impdim;				//image dimensions [0]=stacksize, [1]=widht, [2]=height	
	private float [][][] pixelvalue;
	private int [][] PosI = new int [7][2];
	private boolean [] ci = new boolean [7];
	private Double ti;
	private float [] hi;
	private byte amo;
	private float [][] analyzed;
	private JSpinner fieldT = new JSpinner(new SpinnerNumberModel(10.0, 0.01, 100.0, 0.01));  
	private MouseListener ml;
	private MouseListener ml1;
	private	ImageProcessor ip;
	private GridBagLayout gridbag;
	private GridBagConstraints c;
	private File jpgfile; 
			
	public void run(String arg) {
		initUI();		//defines and initiates the GUI
		runUI();		//runs the GUI
	}
	
	public void runUI() {
//*********below are the definitions of the buttons/functional part of the program
		theLoadButton();
		theASE1Button();
		theASE1tButton();
		theASE2Button();
		theASE2tButton();
//		theNRButton();		
		theStartAnalysisButton();
		theSaveTableButton();
	}		
	
	protected void initUI() {
//*************allitems here
		progressBar1.setStringPainted(true);
		progressBar4.setStringPainted(true);			
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//*************define the cards	
//card open		
		cardOpen = new JPanel(new BorderLayout()) {
		    public Dimension getPreferredSize() {
		        Dimension size = super.getPreferredSize();
		        size.height = 582;
		        size.width = 512;
		        return size;
		    }
		};
		cardOpen.add(textfieldOp,BorderLayout.NORTH);
		cardOpen.add(buttonLoad,BorderLayout.WEST);
		cardOpen.add(new JTextField("No video loaded, yet."));       
//other cards
		gridbag = new GridBagLayout();
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		cardDefine = new JPanel(gridbag);
  		cardAnalyze = new JPanel(gridbag);
		cardSave = new JPanel();
//the tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();		
		tabbedPane.addTab("Open video", cardOpen);
		tabbedPane.addTab("Define ROIs", cardDefine);
		tabbedPane.addTab("Analysis", cardAnalyze);
		tabbedPane.addTab("Results", cardSave);
//the dialog
		dialog.add(tabbedPane, BorderLayout.CENTER);
		dialog.setResizable(false);
		dialog.pack();
		dialog.setVisible(true);
	}

	private void definecardana(){
		progressBar1.setValue(0);
		progressBar4.setValue(0);         					
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
		c.ipady = 100;
		c.weighty = 0.1; 
		c.weightx = 1.0;
		c.gridwidth = 6;
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;	        					
		cardAnalyze.add(buttonStartAnalysis, c);
		c.ipady = 1;
		c.weighty = 0.02; 
		c.weightx = 0.1;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 4;
		cardAnalyze.add(textfieldPR, c);
		c.ipady = 1; 
		c.weighty = 0.02; 
		c.weightx = 0.15;
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 4;											
		cardAnalyze.add(progressBar1, c);
		c.ipady = 1;
		c.weighty = 0.02; 
		c.weightx = 0.1;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 7;
		cardAnalyze.add(textfieldPF, c);
		c.ipady = 1;
		c.weighty = 0.02; 
		c.weightx = 0.15;
		c.gridwidth = 2;
		c.gridx = 3;
		c.gridy = 7;											
		cardAnalyze.add(progressBar4, c);		
		cardAnalyze.revalidate();
		cardAnalyze.repaint();   
		dialog.pack(); 
	}

	public void theLoadButton(){
		buttonLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cardOpen.removeAll();
				cardDefine.removeAll();
				for (byte l=0;l<=6;l++){
					for(byte k=0;k<=1;k++){
						PosI[l][k]=0;
					}
					ci [l]=false;
				}  
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
			     	IJ.saveAs(imp, "JPEG", ""+file[0]+""+file[1]+".jpg");
			     	ipng = new ImageIcon(file[0]+""+file[1]+".jpg");
			     	jpgfile = new File (file[0]+""+file[1]+".jpg"); 
			     	ipngl = new JLabel(ipng);
			     	impdim = new short [3];
					impdim [0] = (short)imp.getStackSize();			//stsi = stacksize
					impdim [1] = (short)imp.getHeight();			//height (y) of the image
					impdim [2] = (short)imp.getWidth();				//width (x) of the image	
					float [][][] pixelvalue;
					pixelvalue = new float [impdim[0]][impdim[1]][impdim[2]];
					cardOpen.add(textfieldOp,BorderLayout.NORTH);
					cardOpen.add(buttonLoad,BorderLayout.WEST);
					cardOpen.add(new JLabel(file[0]+""+file[1]),BorderLayout.CENTER);
					cardOpen.add(new JLabel(ipng),BorderLayout.SOUTH);
					cardOpen.revalidate();
					cardOpen.repaint();
					c = new GridBagConstraints();
					c.weightx = 0.0;
					c.gridwidth = 5;
					c.gridx = 0;
					c.gridy = 0;								
					cardDefine.add(textfieldP, c);
					c.weightx = 0.0;
					c.gridwidth = 2;
					c.gridx = 0;
					c.gridy = 1;				
					cardDefine.add(textfieldASE1, c);					
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 2;
					c.gridy = 1;				
					cardDefine.add(buttonASE1, c);
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 3;
					c.gridy = 1;
					cardDefine.add(cASE1t, c);	
	    			cASE1t.setSelected(false);
	    			cASE1t.addItemListener(new ItemListener (){
						public void itemStateChanged(ItemEvent e) {
							if (e.getStateChange() == ItemEvent.SELECTED) {
								buttonASE1t.setEnabled(true);
								ASE1tj = true;
				        	}
				        	else {
								buttonASE1t.setEnabled(false);
								ASE1tj = false;
				        	}
						}
					});
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 4;
					c.gridy = 1;					
					cardDefine.add(buttonASE1t, c);
					buttonASE1t.setEnabled(false);
					c.weightx = 0.0;
					c.gridwidth = 2;
					c.gridx = 0;
					c.gridy = 2;				
					cardDefine.add(textfieldASE2, c);	
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 2;
					c.gridy = 2;			
					cardDefine.add(cASE2, c);
					cASE2.setSelected(false);
					cASE2.addItemListener(new ItemListener (){
						public void itemStateChanged(ItemEvent e) {
								if (e.getStateChange() == ItemEvent.SELECTED) {
									buttonASE2.setEnabled(true);
									buttonASE2t.setEnabled(true);
					        		ASE2j = true;
					        	}
					        	else {
									buttonASE2.setEnabled(false);
									buttonASE2t.setEnabled(false);
					        		ASE2j = false;
					        	}
						}					
					});			
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 3;
					c.gridy = 2;				
					cardDefine.add(buttonASE2, c);
					buttonASE2.setEnabled(false);
					c.weightx = 0.0;
					c.gridwidth = 1;
					c.gridx = 4;
					c.gridy = 2;					
					cardDefine.add(buttonASE2t, c);
					buttonASE2t.setEnabled(false);
					c.weightx = 0.0;
					c.gridwidth = 5;
					c.gridx = 0;
					c.gridy = 5;													
					cardDefine.add(ipngl, c);
					cardDefine.revalidate();
					cardDefine.repaint();
					times = true;
					dialog.pack();
        	
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
	}
	public void theASE1Button(){
		buttonASE1.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent f) {
			 cardAnalyze.removeAll();
			 cardSave.removeAll();
			 in = false;
			 ml = new MouseAdapter(){
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
					    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
					    PosI[0][0]=f.getX();
						PosI[0][1]=f.getY();
						ci[0] = true;							
								definecardana();	
		       		} 					
				}
		        @Override
		        public void mouseReleased(MouseEvent f) {
					ipngl.removeMouseListener(ml);
		        }
		    };
			ipngl.addMouseListener(ml);
		    }
		}); 
	}
	public void theASE1tButton(){
		buttonASE1t.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent f) {
			 cardAnalyze.removeAll();
			 in = false;
			 ml = new MouseAdapter(){
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
					    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
					    PosI[1][0]=f.getX();
						PosI[1][1]=f.getY();
						ci [1] = true;
		       			if (ci[0]==true){
								definecardana();
		       			}
		       		} 					
				}
		        @Override
		        public void mouseReleased(MouseEvent f) {
					ipngl.removeMouseListener(ml);
		        }
		    };
			ipngl.addMouseListener(ml);
		     }
		}); 
	}	
	public void theASE2Button(){	
		buttonASE2.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent f) {
			 cardAnalyze.removeAll();
			 in = false;
			 ml = new MouseAdapter(){
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
					    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
					    PosI[3][0]=f.getX();
						PosI[3][1]=f.getY();
						ci[3] = true;
		       			definecardana();
		       		}
				}
		        @Override
		        public void mouseReleased(MouseEvent f) {
					ipngl.removeMouseListener(ml);
		        }
		    };
			ipngl.addMouseListener(ml);
		     }
		});
	}
	public void theASE2tButton(){
		buttonASE2t.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent f) {
			 cardAnalyze.removeAll();
			 in = false;
			 ml = new MouseAdapter(){
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
					    ipngl.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
					    PosI[4][0]=f.getX();
						PosI[4][1]=f.getY();
		       			ci [4] = true;
		       			definecardana();
		       		} 					
				}
		        @Override
		        public void mouseReleased(MouseEvent f) {
					ipngl.removeMouseListener(ml);
		        }
		    };
			ipngl.addMouseListener(ml);
		     }
		});
	}
	public void theStartAnalysisButton(){
		buttonStartAnalysis.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent e) {
				SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
					protected Boolean doInBackground() throws Exception {
						jpgfile.delete();
						cardSave.removeAll();
						progressBar1.setValue(0);
						progressBar4.setValue(0);					
						ti = (Double)(fieldT.getValue());
						float t = (float) (1/ti);
						if (times==true){
							pixelvalue = getPixelValue(imp, impdim);
							imp.close();
							times=false;
						}
						progressBar1.setValue(100);
						amo = 0;
						for (byte k = 0; k<=6; k++){
							if (ci[k] == true){
								amo++;
							}
						}
						analyzed = new float [amo][impdim[0]];
						analyzed = analysis(pixelvalue, file, PosI, ci, impdim);
						float maxanalyzed = 0;
						for (short j=0; j<=(short)(impdim[0]-1); j++){
							for(byte i=0; i<=(byte)(amo-1); i++){
								if(analyzed[i][j]>=maxanalyzed){
									maxanalyzed = analyzed[i][j];
								}
							}	
						}
						int maxgraph = (int)(maxanalyzed)+1;
						hi = new float [impdim[0]];
						XYSeriesCollection dataset = new XYSeriesCollection();
						for (byte kl=0;kl<=(byte)(amo-1);kl++){
							XYSeries series = new XYSeries("ASE"+kl);
							for(short j=(short)(0); j<=(short)(impdim[0]-1); j++){
								hi [j] = t*(j);
								series.add(hi [j], analyzed[kl][j]);						
							}
							dataset.addSeries(series);
						}	
						JFreeChart chart = ChartFactory.createXYLineChart("Relative Fluorescence Changes", "Time [s]", "Relative Fluorescence Intensity", dataset);
						XYPlot plot = (XYPlot)chart.getPlot();
						chart.setBackgroundPaint(new Color(255, 255, 255));
						plot.setBackgroundPaint(Color.white);
						plot.setRangeGridlinePaint(Color.black);
				        plot.getDomainAxis().setRange(0.00, (impdim[0]*t)+0.1);
				        plot.getDomainAxis().setFixedAutoRange(20);
				        plot.getRangeAxis().setRange(0.0, maxgraph);
				        plot.getRangeAxis().setFixedAutoRange(1);
				        plot.setOutlineVisible(false);
						for (byte kl = 0; kl<=(byte)(amo-1); kl++){			
							if (kl==0){
								plot.getRenderer().setSeriesPaint(kl, new Color(178, 34, 34));
							}
							if (kl==1){
								plot.getRenderer().setSeriesPaint(kl, new Color(255, 152, 51));
							}
							if (kl==2){
								plot.getRenderer().setSeriesPaint(kl, new Color(0, 0, 204));  
							}
							if (kl==3){
								plot.getRenderer().setSeriesPaint(kl, new Color(102, 255, 255));
							}							
							plot.getRenderer().setSeriesStroke(kl, new BasicStroke(2));
						}
						ChartPanel myChart = new ChartPanel(chart, 512, 350, 512, 350, 512, 350, false, true, true, true, true, true);								
						cardSave.add(textfieldS, BorderLayout.NORTH);
						cardSave.add(myChart, BorderLayout.CENTER);
						cardSave.add(buttonSaveTable, BorderLayout.SOUTH);
						cardSave.revalidate();
						cardSave.repaint();
			       		boolean status = true;
						return status;
		     		}
				};
				worker.execute();	
		     }
		});
	}
	public void theSaveTableButton(){		
	    buttonSaveTable.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
		        ResultsTable rt = new ResultsTable();
				rt.setPrecision(2);
				for (short j=0; j<=(short)(impdim[0]-1); j++){
					int c=0;
					rt.incrementCounter();
					rt.addValue("Time [s]", hi[j]);
					rt.showRowNumbers(false);
					for (byte kl = 0; kl<=(amo-1); kl++){
						rt.addValue("Intensity (green channel)"+kl, analyzed[kl][j]);
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

	public float [][][] getPixelValue(ImagePlus imp, short [] impdim){
		ip = imp.getProcessor();
		float [][][] pixelvalue;
		pixelvalue = new float [impdim [0]][impdim [1]][impdim [2]]; 
		for (short j=0; j<=(short)(impdim [0]-1);j++){
			imp.setSlice(j+1);
			for (short y=0; y<=(short)(impdim [2]-1); y++) {
	          	for (short x=0; x<=(short)(impdim [1]-1); x++) {
					pixelvalue [j][x][y] = ip.getPixelValue(x, y);
	         	}
			}
			short progress1 = (short) (j*100/impdim[0]);
			progressBar1.setValue(progress1);		
		}
		progressBar1.setValue(100);
		return pixelvalue;
	}
	
	public float [][] analysis (float [][][] pv, String [] file, int [][] PosI, boolean [] ci, short [] impdim){
		byte amo = 0;
		for (byte k = 0; k<=6; k++){
			if (ci[k] == true){
				amo++;
			}
		}
		amo++;
		float [][] result;		//[fASE1,fASE1t,fASE1n,fASE2,fASE2t,fASE2n,NR][j]
		result = new float [amo][impdim[0]];
		float [][][] FI;		//[fASE1,fASE1t,fASE1n,fASE2,fASE2t,fASE2n,NR][mean,max,min,threshold][j]
		FI = new float [7][4][impdim[0]];
		float [] FIa;			//[fASE1,fASE1t,fASE1n,fASE2,fASE2t,fASE2n,NR]
		FIa = new float [7];
		int [][][] miP; 		//[fASE1,fASE1t,fASE1n,fASE2,fASE2t,fASE2n,NR1,NR2][j][x,y]
		miP = new int [8][impdim[0]][2];
		miP [0][0][0]=PosI[0][0];miP [0][0][1]=PosI[0][1];
		if(ci[1]==true){
			miP [1][0][0]=PosI[1][0];miP [1][0][1]=PosI[1][1];
			miP [2][0][0]=(int)((PosI[0][0]+PosI[1][0])/2);miP [2][0][1]=(int)((PosI[0][1]+PosI[1][1])/2);
		}
		if(ci[3]==true){
			miP [3][0][0]=PosI[3][0];miP [3][0][1]=PosI[3][1];
			if(ci[4]==true){
				miP [4][0][0]=PosI[4][0];miP [4][0][1]=PosI[4][1];
				miP [5][0][0]=(int)((PosI[3][0]+PosI[4][0])/2);miP [5][0][1]=(int)((PosI[3][1]+PosI[4][1])/2);
			}
		}
		if(ci[5]==true){
			miP [6][0][0]=(int)(PosI[5][0]);miP [6][0][1]=(int)(PosI[5][1]);
			miP [7][0][0]=(int)(PosI[6][0]);miP [7][0][1]=(int)(PosI[6][1]);
		}
		float maxvalueforgraph = 0;
		for (short j=1;j<=(short)(impdim[0]-1);j++){
			short [] co;
			co = new short [7];
			for (short l=0;l<=6;l++){
				FI[l][1][j]=0;
				FI[l][2][j]=65535;
			}
			byte bor1 = 15;
			byte bor2 = 5;
			for(short x=0;x<=bor1;x++){
				for(short y=0;y<=bor1;y++){
					if(pv[j][miP[0][(j-1)][0]+x-(short)((bor1-1)/2)][miP[0][(j-1)][1]+y-(short)((bor1-1)/2)]<=FI[0][2][j]){
						FI[0][2][j]=pv[j][miP[0][(j-1)][0]+x-(short)((bor1-1)/2)][miP[0][(j-1)][1]+y-(short)((bor1-1)/2)];
					}
					else if((miP[0][(j-1)][0]+x-(short)((bor1-1)/2))>=(miP[0][(j-1)][0]-3)){
							if((miP[0][(j-1)][0]+x-(short)((bor1-1)/2))<=(miP[0][(j-1)][0]+3)){
								if((miP[0][(j-1)][1]+y-(short)((bor1-1)/2))>=(miP[0][(j-1)][1]-3)){
									if((miP[0][(j-1)][1]+y-(short)((bor1-1)/2))<=(miP[0][(j-1)][1]+3)){ 
										if(pv[j][miP[0][(j-1)][0]+x-(short)((bor1-1)/2)][miP[0][(j-1)][1]+y-(short)((bor1-1)/2)]>=FI[0][1][j]){
												FI[0][1][j]=pv[j][miP[0][(j-1)][0]+x-(short)((bor1-1)/2)][miP[0][(j-1)][1]+y-(short)((bor1-1)/2)];
												miP[0][j][0]=miP[0][(j-1)][0]+x-(short)((bor1-1)/2); miP[0][j][1]=miP[0][(j-1)][1]+y-(short)((bor1-1)/2);
										}		
					}}}}
					if(ci[3]==true){
						if(pv[j][miP [3][(j-1)][0]+x-(short)((bor1-1)/2)][miP[3][(j-1)][1]+y-(short)((bor1-1)/2)]<=FI[3][2][j]){
							FI[3][2][j]=pv[j][miP[3][(j-1)][0]+x-(short)((bor1-1)/2)][miP[3][(j-1)][1]+y-(short)((bor1-1)/2)];
						}
						else if((miP[3][(j-1)][0]+x-(short)((bor1-1)/2))>=(miP[3][(j-1)][0]-3)){
								if((miP[3][(j-1)][0]+x-(short)((bor1-1)/2))<=(miP[3][(j-1)][0]+3)){
									if((miP[3][(j-1)][1]+y-(short)((bor1-1)/2))>=(miP[3][(j-1)][1]-3)){
										if((miP[3][(j-1)][1]+y-(short)((bor1-1)/2))<=(miP[3][(j-1)][1]+3)){	 
											if(pv[j][miP[3][(j-1)][0]+x-(short)((bor1-1)/2)][miP[3][(j-1)][1]+y-(short)((bor1-1)/2)]>=FI[3][1][j]){
												FI[3][1][j]=pv[j][miP[3][(j-1)][0]+x-(short)((bor1-1)/2)][miP[3][(j-1)][1]+y-(short)((bor1-1)/2)];
												miP[3][j][0]=miP[3][(j-1)][0]+x-(short)((bor1-1)/2); miP[3][j][1]=miP[3][(j-1)][1]+y-(short)((bor1-1)/2);
											}
						}}}}						
					}
				}
			}
			if(ci[1]==true){
				for(short x=0;x<=bor2;x++){
					if(miP[1][(j-1)][0]+x-(short)((bor2-1)/2)>=0){
						if(miP[1][(j-1)][0]+x+(short)((bor2-1)/2)<=impdim[1]-1){
							for(short y=0;y<=bor2;y++){
								if(miP[1][(j-1)][1]+y-(short)((bor2-1)/2)>=0){
									if(miP[1][(j-1)][1]+y+(short)((bor2-1)/2)<=impdim[2]-1){								
										if(pv[j][miP[1][(j-1)][0]+x-(short)((bor2-1)/2)][miP[1][(j-1)][1]+y-(short)((bor2-1)/2)]<=FI[1][2][j]){
											FI[1][2][j]=pv[j][miP[1][(j-1)][0]+x-(short)((bor2-1)/2)][miP[1][(j-1)][1]+y-(short)((bor2-1)/2)];
										}
										else if(pv[j][miP[1][(j-1)][0]+x-(short)((bor2-1)/2)][miP[1][(j-1)][1]+y-(short)((bor2-1)/2)]>=FI[1][1][j]){
											FI[1][1][j]=pv[j][miP[1][(j-1)][0]+x-(short)((bor2-1)/2)][miP[1][(j-1)][1]+y-(short)((bor2-1)/2)];				
											miP[1][j][0]=miP[1][(j-1)][0]+x-(short)((bor2-1)/2); miP[1][j][1]=miP[1][(j-1)][1]+y-(short)((bor2-1)/2);
										}
										if(ci[4]==true){
											if(pv[j][miP[4][(j-1)][0]+x-(short)((bor2-1)/2)][miP[4][(j-1)][1]+y-(short)((bor2-1)/2)]<=FI[4][2][j]){
												FI[4][2][j]=pv[j][miP[4][(j-1)][0]+x-(short)((bor2-1)/2)][miP[4][(j-1)][1]+y-(short)((bor2-1)/2)];
											}
											else if(pv[j][miP[4][(j-1)][0]+x-(short)((bor2-1)/2)][miP[4][(j-1)][1]+y-(short)((bor2-1)/2)]>=FI[4][1][j]){
												FI[4][1][j]=pv[j][miP[4][(j-1)][0]+x-(short)((bor2-1)/2)][miP[4][(j-1)][1]+y-(short)((bor2-1)/2)];
												miP[4][j][0]=miP[4][(j-1)][0]+x-(short)((bor2-1)/2); miP[4][j][1]=miP[4][(j-1)][1]+y-(short)((bor2-1)/2);
											}						
										}
									}
								}	
							}
						}
					}
				}	
			}
			FI[0][3][j]=FI[0][2][j]+((FI[0][1][j]-FI[0][2][j])/4);
			if(ci[1]==true){
				FI[1][3][j]=FI[1][2][j]+((FI[1][1][j]-FI[1][2][j])/4);
				FI[2][3][j]=FI[1][2][j]+((FI[1][1][j]-FI[1][2][j])/4);
			}	
			if(ci[3]==true){
				FI[3][3][j]=FI[3][2][j]+((FI[3][1][j]-FI[3][2][j])/4);
			}
			if(ci[4]==true){
				FI[4][3][j]=FI[4][2][j]+((FI[4][1][j]-FI[4][2][j])/4);
				FI[5][3][j]=FI[4][2][j]+((FI[4][1][j]-FI[4][2][j])/4);
			}	
			for(short x=0;x<=bor1;x++){
				for(short y=0;y<=bor1;y++){
					if(pv[j][miP[0][j][0]+x-(short)((bor1-1)/2)][miP[0][j][1]+y-(short)((bor1-1)/2)]>=FI[0][3][j]){
						if (j==1){FI[0][0][0]+=pv[0][miP[0][j][0]+x-(short)((bor1-1)/2)][miP[0][j][1]+y-(short)((bor1-1)/2)];}
						FI[0][0][j]+=pv[j][miP[0][j][0]+x-(short)((bor1-1)/2)][miP[0][j][1]+y-(short)((bor1-1)/2)];
						co [0]++;
					}
					if(ci[3]==true){
						if(pv[j][miP[3][j][0]+x-(short)((bor1-1)/2)][miP[3][j][1]+y-(short)((bor1-1)/2)]>=FI[3][3][j]){
							if (j==1){FI[3][0][0]+=pv[j][miP[3][j][0]+x-(short)((bor1-1)/2)][miP[3][j][1]+y-(short)((bor1-1)/2)];}
							FI[3][0][j]+=pv[j][miP[3][j][0]+x-(short)((bor1-1)/2)][miP[3][j][1]+y-(short)((bor1-1)/2)];
							co [3]++;
						}
					}
				}
			}
			if(j==1){FI[0][0][0]/=co[0];FI[0][0][0]-=FI[0][2][j];}
			FI[0][0][j]/=co[0];
			FI[0][0][j]-=FI[0][2][j];
			if(ci[3]==true){
				FI[3][0][j]/=co[3];
				FI[3][0][j]-=FI[3][2][j];
				if(j==1){FI[3][0][0]/=co[3];FI[3][0][0]-=FI[3][2][j];}				
			}
			if(ci[1]==true){
				for(short x=0;x<=bor2;x++){
					if(miP[1][(j-1)][0]+x-(short)((bor2-1)/2)>=0){
						if(miP[1][(j-1)][0]+x+(short)((bor2-1)/2)<=impdim[1]-1){
							for(short y=0;y<=bor2;y++){
								if(miP[1][(j-1)][1]+y-(short)((bor2-1)/2)>=0){
									if(miP[1][(j-1)][1]+y+(short)((bor2-1)/2)<=impdim[2]-1){
										if(pv[j][miP [1][j][0]+x-(short)((bor2-1)/2)][miP[1][j][1]+y-(short)((bor2-1)/2)]>=FI[1][3][j]){
											if(j==1){FI[1][0][0]+=pv[0][miP[1][j][0]+x-(short)((bor2-1)/2)][miP[1][j][1]+y-(short)((bor2-1)/2)];}
											FI[1][0][j]+=pv[j][miP[1][j][0]+x-(short)((bor2-1)/2)][miP[1][j][1]+y-(short)((bor2-1)/2)];
											co[1]++;
										}
										if(ci[4]==true){
											if(pv[j][miP [4][j][0]+x-(short)((bor2-1)/2)][miP[4][j][1]+y-(short)((bor2-1)/2)]>=FI[4][3][j]){
												if(j==1){FI[4][0][0]+=pv[0][miP[4][j][0]+x-(short)((bor2-1)/2)][miP[4][j][1]+y-(short)((bor2-1)/2)];}
												FI[4][0][j]+=pv[j][miP[4][j][0]+x-(short)((bor2-1)/2)][miP[4][j][1]+y-(short)((bor2-1)/2)];
												co[4]++;
											}
										}
									}
								}	
							}
						}
					}
				}
				if(j==1){FI[1][0][0]/=co[1];FI[1][0][0]-=FI[1][2][j];}
				FI[1][0][j]/=co[1];
				FI[1][0][j]-=FI[1][2][j];
				if(ci[4]==true){				
					if(j==1){FI[4][0][0]/=co[4];FI[4][0][0]-=FI[4][2][j];}
					FI[4][0][j]/=co[4];
					FI[4][0][j]-=FI[4][2][j];
				}	
			}
			if (j==74){
				for (short ij=0;ij<=j;ij++){				//[fASE1,fASE1t,fASE1n,fASE2,fASE2t,fASE2n,NR]
					for (short l=0;l<=6;l++){
						if (l==2){
							l++;
						}
						if(ci[l]==true){
							FIa[l]+=FI[l][0][ij];
						}
					}
				}
				for (short l=0;l<=6;l++){
					if (l==2){
						l++;
					}					
					if(ci[l]==true){
						FIa[l]/=74;
					}
				}
				for (short ij=0;ij<=j;ij++){
					byte la = 0;
					for (short l=0;l<=6;l++){
						if (l==2){
							l++;
						}						
						if(ci[l]==true){
							FI[l][0][ij]/=FIa[l];
							result [la][ij] = FI [l][0][ij];
							la++;
						}
					}								
				}
			}
			if (j>74){
				byte la = 0;				
				for (short l=0;l<=6;l++){				
					if (l==2){
						l++;
					}				
					if(ci[l]==true){
						FI[l][0][j]/=FIa[l];
						result [la][j] = FI [l][0][j];
						la++;
					}
				}
			}
			for (short l=0;l<=6;l++){
				if (l==2){
					l++;
				}				
				if (ci[l]==true){
					if (FI[l][0][j]>=maxvalueforgraph){
						maxvalueforgraph = FI[l][0][j];
					}
				}
			}
			short progress4 = (short) (j*100/impdim[0]);
			progressBar4.setValue(progress4);		
		}
		progressBar4.setValue(100);
		return result;
	}
	
	String [] savetable (){
		String [] file;
		file = new String [2];
		Frame fram = new Frame();
		FileDialog fd = new FileDialog(fram,"saveFile",FileDialog.SAVE);
		fd.setFile("Intensity.txt");
		fd.setVisible(true);
		file [0] = fd.getDirectory();				//get the path of the file
		file [1] = fd.getFile();					//get the name of the file	
		return file;
	}
	
	String [] openVideo (){
		String [] file;
		file = new String [2];
		Frame fram = new Frame();
		FileDialog fd = new FileDialog(fram,"openFile",FileDialog.LOAD);
		fd.setVisible(true);
		file [0] = fd.getDirectory();					//get the path of the file
		file [1] = fd.getFile();					//get the name of the file	
		return file;
	}
}
