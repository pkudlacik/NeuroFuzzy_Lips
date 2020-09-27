package badania.fuzzy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import badania.TestResult;
import fuzzlib.DefuzMethod;
import fuzzlib.FuzzySet;
import fuzzlib.norms.SNorm;
import fuzzlib.norms.TNorm;
import fuzzlib.reasoning.ReasoningSystem;
import fuzzlib.reasoning.SystemConfig;
import neuralnetwork.data.DataPackage;
import neuralnetwork.data.DataVector;
import shg.clustering.Cluster;
import shg.clustering.ClusterData;
import shg.clustering.Clusterer;

public class FuzzyTestClusteredWorker extends Thread {

	private double a = 10;
	private double b = 2;
	private int sizePackage = 5;

	int clusters = 10; //3 - 83%
	double distance = 1000.0;	
	
	private boolean verbose = false;

	private int success = 0;
	private int failed = 0;

	private long start;
	private long stop;

	private FuzzyTest test;

	DataPackage learnPkg;
	DataPackage testPkg;
	ClusterData cluster_data_c1 = new ClusterData();
	ClusterData cluster_data_c2 = new ClusterData();
	ClusterData cluster_data_c3 = new ClusterData();

	private ReasoningSystem rs;
	private Clusterer cs1 = new Clusterer(); 
	private Clusterer cs2 = new Clusterer(); 
	private Clusterer cs3 = new Clusterer(); 

	/**
	 * Configure test
	 * 
	 * @param a           range divider (the higher a - the narrower the set
	 *                    description.)
	 * @param sizePackage number of learning samples taken from each class
	 * @param verbose     if true - print results on console
	 */
	public FuzzyTestClusteredWorker(double a, double b, int sizePackage, boolean verbose, FuzzyTest test) {
		super();
		this.a = a;
		this.b = b;
		this.sizePackage = sizePackage;
		this.verbose = verbose;
		this.test = test;
	}

	public int getSuccess() {
		return success;
	}

	public int getFailed() {
		return failed;
	}

	public long getStart() {
		return start;
	}

	public long getStop() {
		return stop;
	}

	/**
	 * Run test
	 */
	public void run() {
		start = System.currentTimeMillis();

		loadData_leaveOneOut();

		//loadData();
		clusterData();
		initializeFuzzySystem();
		buildRules();
		testFS();

		stop = System.currentTimeMillis();

		test.addResult(new TestResult(success, failed, stop - start));

	}

	private void loadData() {
		testPkg = new DataPackage();
		DataPackage test2 = new DataPackage();
		DataPackage test3 = new DataPackage();

		testPkg.loadTextFile("klasa0.txt");
		test2.loadTextFile("klasa1.txt");
		test3.loadTextFile("klasa2.txt");

		learnPkg = testPkg.removeRandomVectors(sizePackage);
		DataPackage learn2 = test2.removeRandomVectors(sizePackage);
		DataPackage learn3 = test3.removeRandomVectors(sizePackage);
		
		//copy data for clustering
		for (DataVector vector:learnPkg.getList()) {
			cluster_data_c1.addVector(vector.getData());
		}
		for (DataVector vector:learn2.getList()) {
			cluster_data_c2.addVector(vector.getData());
		}
		for (DataVector vector:learn3.getList()) {
			cluster_data_c3.addVector(vector.getData());
		}

		//add up test sets 
		testPkg.add(test2);
		testPkg.add(test3);
		
		//add up learning sets 		
		learnPkg.add(learn2);
		learnPkg.add(learn3);

		if (verbose) {
			System.out.println(learnPkg);
			System.out.println(testPkg);
		}
				
	}

	private void loadData_leaveOneOut() {
		learnPkg = new DataPackage();
		DataPackage learn2 = new DataPackage();
		DataPackage learn3 = new DataPackage();

		learnPkg.loadTextFile("klasa0.txt");
		learn2.loadTextFile("klasa1.txt");
		learn3.loadTextFile("klasa2.txt");
		
		int pos = sizePackage;
		
		//take out one vector
		if (pos < learnPkg.size()) {
			testPkg = learnPkg.removeVector(pos);
		} else {
			pos -= learnPkg.size();
			if (pos < learn2.size()) {
				testPkg = learn2.removeVector(pos);
			} else {
				pos -= learn2.size();
				testPkg = learn3.removeVector(pos);
			}
		}
		
		//copy data for clustering
		for (DataVector vector:learnPkg.getList()) {
			cluster_data_c1.addVector(vector.getData());
		}
		for (DataVector vector:learn2.getList()) {
			cluster_data_c2.addVector(vector.getData());
		}
		for (DataVector vector:learn3.getList()) {
			cluster_data_c3.addVector(vector.getData());
		}
		
		learnPkg.add(learn2);
		learnPkg.add(learn3);
		
		if (verbose) {
			System.out.println(learnPkg);
			System.out.println(testPkg);
		}
	}

	private void clusterData() {

		//normalize
//		cluster_data_c1.normalize();
//		cluster_data_c2.normalize();
//		cluster_data_c3.normalize();
		
		//init clusterers
		cs1.setNumberOfClusters(clusters);
		cs2.setNumberOfClusters(clusters);
		cs3.setNumberOfClusters(clusters);
		cs1.setDistanceThreshold(distance);
		cs2.setDistanceThreshold(distance);
		cs3.setDistanceThreshold(distance);
		cs1.setData(cluster_data_c1);
		cs2.setData(cluster_data_c2);
		cs3.setData(cluster_data_c3);
		
		//cluster my master
		cs1.clusterData();
		cs2.clusterData();
		cs3.clusterData();
		
		//cs1.
		
		//cs1.clusterContinue();
		
//		cs1.clusterDataWithoutDistanceBuffer();
//		cs2.clusterDataWithoutDistanceBuffer();
//		cs3.clusterDataWithoutDistanceBuffer();
		
		if (verbose) {
			System.out.println(cs1.clustersToString());
			System.out.println(cs2.clustersToString());
			System.out.println(cs3.clustersToString());
	
			cs1.printDistances();
			cs2.printDistances();
			cs3.printDistances();
		}
		
	} 
	
	private void initializeFuzzySystem() {
		FuzzySet fuzzyfier = new FuzzySet();
		fuzzyfier.newTriangle(0, 0.5);
		// fuzzyfier.IncreaseYPrecision(0.1, 0.001);

		// fuzzy conclusions (regular fuzzy sets) -

		FuzzySet class1 = new FuzzySet(fuzzyfier);
		FuzzySet class2 = new FuzzySet(fuzzyfier);
		FuzzySet class3 = new FuzzySet(fuzzyfier);

		class1.setId("1");
		class2.setId("2");
		class3.setId("3");

		class1.fuzzyfy(1);
		class2.fuzzyfy(1);
		class3.fuzzyfy(1);

		SystemConfig config = new SystemConfig();

		config.setInputWidth(13); // number of inputs
		config.setOutputWidth(3);// number of outputs

		// number of premises (it is dynamically extended if needed so it can be not
		// set)
		config.setNumberOfPremiseSets(100);
		config.setNumberOfConclusionSets(3);

		config.setIsOperationType(TNorm.TN_PRODUCT);
		config.setAndOperationType(TNorm.TN_MINIMUM);
		config.setOrOperationType(SNorm.SN_PROBABSUM);
		config.setImplicationType(TNorm.TN_MINIMUM);
		config.setConclusionAgregationType(SNorm.SN_PROBABSUM);
		config.setTruthCompositionType(TNorm.TN_MINIMUM);
		config.setAutoDefuzzyfication(false);
		config.setDefuzzyfication(DefuzMethod.DF_COG);
		config.setAutoAlpha(true);
		config.setTruthPrecision(0.001, 0.0001);

		rs = new ReasoningSystem(config);

		// set identifiers for inputs and outputs
		rs.getInputVar(0).id = "wej0";
		rs.getInputVar(1).id = "wej1";
		rs.getInputVar(2).id = "wej2";
		rs.getInputVar(3).id = "wej3";
		rs.getInputVar(4).id = "wej4";
		rs.getInputVar(5).id = "wej5";
		rs.getInputVar(6).id = "wej6";
		rs.getInputVar(7).id = "wej7";
		rs.getInputVar(8).id = "wej8";
		rs.getInputVar(9).id = "wej9";
		rs.getInputVar(10).id = "wej10";
		rs.getInputVar(11).id = "wej11";
		rs.getInputVar(12).id = "wej12";

//		rs.getInputVar(0).fuzz = new FuzzySet().newGaussian(0, 1/b);
//		rs.getInputVar(1).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(2).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(3).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(4).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(5).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(6).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(7).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(8).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(9).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(10).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(11).fuzz = new FuzzySet().newTriangle(0, 1/b);
//		rs.getInputVar(12).fuzz = new FuzzySet().newTriangle(0, 1/b);

//		rs.getInputVar(0).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(1)/b);
//		rs.getInputVar(1).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(2)/b);
//		rs.getInputVar(2).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(3)/b);
//		rs.getInputVar(3).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(4)/b);
//		rs.getInputVar(4).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(5)/b);
//		rs.getInputVar(5).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(6)/b);
//		rs.getInputVar(6).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(7)/b);
//		rs.getInputVar(7).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(8)/b);
//		rs.getInputVar(8).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(9)/b);
//		rs.getInputVar(9).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(10)/b);
//		rs.getInputVar(10).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(11)/b);
//		rs.getInputVar(11).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(12)/b);
//		rs.getInputVar(12).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(13)/b);

		rs.getInputVar(0).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(1) / b);
		rs.getInputVar(1).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(2) / b);
		rs.getInputVar(2).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(3) / b);
		rs.getInputVar(3).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(4) / b);
		rs.getInputVar(4).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(5) / b);
		rs.getInputVar(5).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(6) / b);
		rs.getInputVar(6).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(7) / b);
		rs.getInputVar(7).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(8) / b);
		rs.getInputVar(8).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(9) / b);
		rs.getInputVar(9).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(10) / b);
		rs.getInputVar(10).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(11) / b);
		rs.getInputVar(11).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(12) / b);
		rs.getInputVar(12).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(13) / b);

		rs.getOutputVar(0).id = "wyj1";
		rs.getOutputVar(1).id = "wyj2";
		rs.getOutputVar(2).id = "wyj3";

		rs.addConclusionSet(class1);
		rs.addConclusionSet(class2);
		rs.addConclusionSet(class3);
	}

	private void buildRules() {
		int x = 0;

		// listy nazw przes³anek dla kazdej z klas
		Map<String, List<String>> nazwy = new HashMap<String, List<String>>();
		for (int i = 0; i < 3; i++) {
			nazwy.put("c" + (i + 1), new LinkedList<String>());
		}
		
		//add up all clusters
		List<Cluster> list = new LinkedList<Cluster>();
		
		list.addAll(cs1.getClusters());
		list.addAll(cs2.getClusters());
		list.addAll(cs3.getClusters());

		try {
			for (Cluster cl : list) {

				// nazwa klasy - pierwsza pozycja
				int klasa = cl.getAverage().get(0).intValue();

				// dla wszystkich pozycji klastra
				for (int i = 1; i < cl.getAverage().size(); i++) {
					double Axi = cl.getAverage().get(i); // pobierz œredni¹ dla parametru
					//zakres parametru (w klastrze)
					//double Zx = cl.getMaxValues().get(i) - cl.getMinValues().get(i);
					//zakres parametru (w zbiorze ucz¹cym)
					double Zx = learnPkg.getColumnRange(i);
					if (Zx < 0.001) Zx = 0.001;
					
					
					// zbuduj nazwê zbioru przes³anki
					String nazwa = "fs_a" + i + "_" + "x" + x;
					FuzzySet temp = new FuzzySet(nazwa, "");

					// zdefiniuj funkcjê przynaleznoœci

					// temp.newTriangle(Axi, Zx / a);
					temp.newGaussian(Axi, Zx / a);
					//temp.newTrapezium(Axi, (Zx/a), (Zx/a) / 2.0 );
//					temp.addPoint(cl.getMinValues().get(i)-(Zx/a)/2, 0.0);
//					temp.addPoint(cl.getMinValues().get(i), 1.0);
//					temp.addPoint(cl.getMaxValues().get(i), 1.0);
//					temp.addPoint(cl.getMaxValues().get(i)+(Zx/a)/2, 0.0);

					// dodaj zbiór jako przes³ankê
					rs.addPremiseSet(temp);
				}

				// dodaj regu³ê w oparciu o wygenerowane przes³anki (dla wszytskich parametrów)
				rs.addRule(12, 1);

				rs.addRuleItem("wej0", "fs_a1_x" + x, "AND", "wej1", "fs_a2_x" + x);
				rs.addRuleItem("wej2", "fs_a3_x" + x, "AND", "wej3", "fs_a4_x" + x);
				rs.addRuleItem("wej4", "fs_a5_x" + x, "AND", "wej5", "fs_a6_x" + x);
				rs.addRuleItem("wej6", "fs_a7_x" + x, "AND", "wej7", "fs_a8_x" + x);
				rs.addRuleItem("wej8", "fs_a9_x" + x, "AND", "wej9", "fs_a10_x" + x);
				rs.addRuleItem("wej10", "fs_a11_x" + x, "AND", "wej11", "fs_a12_x" + x);

				rs.addRuleItem("wej12", "fs_a13_x" + x, "AND", "STACK", "");

				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");

				rs.addRuleConclusion("wyj" + klasa, "" + klasa);

				// dodaj nazwê przes³anki do zbioru odpowiedniej klasy
				// ( na potrzeby openLeftAndRightDescription() )
				nazwy.get("c" + klasa).add("x" + x);

				x++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// *********** Print parameters for all classes
//		System.out.println();
//		System.out.println("!!! Before opening slopes");
//		printParametersOfReasoningSystem(rs,nazwy);

		//openLeftAndRightDescription(rs, nazwy);

		// *********** Print parameters for all classes
//		System.out.println();
//		System.out.println("!!! After opening slopes");
//		printParametersOfReasoningSystem(rs,nazwy);
//		System.out.println();
	}

	private static void openLeftAndRightDescription(ReasoningSystem rs, Map<String, List<String>> nazwy) {
		// loop through all parameters (1-13)
		for (int i = 0; i < 13; i++) {
			String fs_name = "fs_a" + (i + 1);
			try {
				// loop through all classes
				for (int j = 0; j < 3; j++) {
					String class_name = "c" + (j + 1);

					// loop through all rules in a class

					boolean first = true;
					FuzzySet leftMost = null;
					FuzzySet rightMost = null;
					for (String nazwa : nazwy.get(class_name)) {
						if (first) {
							first = false;
							// assume that first is leftMost and rightMost
							leftMost = rs.getPremiseSet(fs_name + "_" + nazwa);
							rightMost = leftMost;
						} else {
							FuzzySet fs_temp = rs.getPremiseSet(fs_name + "_" + nazwa);
							if (fs_temp.getFirstPoint().x < leftMost.getFirstPoint().x) {
								leftMost = fs_temp;
							}
							if (fs_temp.getLastPoint().x > rightMost.getLastPoint().x) {
								rightMost = fs_temp;
							}
						}
					}
					leftMost.openLeftSlope();
					leftMost.PackFlatSections();
					rightMost.openRightSlope();
					rightMost.PackFlatSections();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void testFS() {

		success = 0;
		failed = 0;
		
		//testPkg = learnPkg;

		try {
			for (DataVector object : testPkg.getList()) {

				int class_real;
				int class_obtained = 0;

				class_real = (int) object.get(0);

				for (int z = 1; z < 14; z++) {
					rs.setInput(z - 1, object.get(z));
				}
				rs.Process();

				double max_result = 0.0;
				for (int i = 0; i < 3; i++) {
					double temp_result = rs.getOutputVar(i).outset.getMaximumMembership();
					if (temp_result > max_result) {
						max_result = temp_result;
						class_obtained = i + 1;
					}
				}

				// sprawdzanie:

				if (class_real == class_obtained) {
					success++;
				} else {
					failed++;
				}

//				 System.out.println("object: " + object);
//				 System.out.println("Result (test) 1: " +
//				 rs.getOutputVar(0).outset.getMaximumMembership());
//				 System.out.println("Result (test) 2: " +
//				 rs.getOutputVar(1).outset.getMaximumMembership());
//				 System.out.println("Result (test) 3: " +
//				 rs.getOutputVar(2).outset.getMaximumMembership());
//				 System.out.println("----------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (verbose) {
			DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
			otherSymbols.setDecimalSeparator('.');
			DecimalFormat df = new DecimalFormat("#.#", otherSymbols);

			double procSucc = (success * 1.0 / (success + failed)) * 100;
			double procFail = (failed * 1.0 / (success + failed)) * 100;
			System.out.println("***WORKER**********************************************");
			System.out.println("A: " + a + ", B: " + b + ", Package: " + sizePackage);
			System.out.println("Success: " + df.format(procSucc) + "%, (" + success + ")");
			System.out.println("Failed: " + df.format(procFail) + "%, (" + failed + ")");
			long stop = System.currentTimeMillis();
			System.out.println("Czas wykonania (sek.): " + (stop - start) / 1000);
			System.out.println("*******************************************************");
		}

	}

	private static void printParametersOfReasoningSystem(ReasoningSystem rs, Map<String, List<String>> nazwy) {
		for (int i = 0; i < 13; i++) {
			String fs_name = "fs_a" + (i + 1);
			System.out.println("___ parameter: " + fs_name);
			try {
				// loop through all classes
				for (int j = 0; j < 3; j++) {
					String class_name = "c" + (j + 1);
					// loop through all rules in a class
					for (String nazwa : nazwy.get(class_name)) {
						FuzzySet fs_temp = rs.getPremiseSet(fs_name + "_" + nazwa);
						System.out.println("klasa " + class_name + ": " + fs_temp.getId() + "[" + fs_temp.getSize()
								+ "]: " + fs_temp);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
