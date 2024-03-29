package badania.fuzzy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import badania.TestResult;
import fuzzlib.DefuzMethod;
import fuzzlib.FuzzySet;
import fuzzlib.avers.Average;
import fuzzlib.norms.SNorm;
import fuzzlib.norms.TNorm;
import fuzzlib.reasoning.ReasoningSystem;
import fuzzlib.reasoning.SystemConfig;
import neuralnetwork.data.DataPackage;
import neuralnetwork.data.DataVector;

public class FuzzyTestWorker extends Thread {

	private double a = 10;
	private double b = 2;
	private int sizePackage = 5;
	private boolean verbose = false;
	
	//number of segments
	private int segments = 12; 

	private int success = 0;
	private int failed = 0;

	private long start;
	private long stop;

	private FuzzyTest test;

	DataPackage learnPkg;
	DataPackage testPkg;
	DataPackage qualityPkg;
	DataPackage qualityTestPkg;

	private ReasoningSystem rs;

	/**
	 * Configure test
	 * 
	 * @param a           range divider (the higher a - the narrower the set
	 *                    description.)
	 * @param sizePackage number of learning samples taken from each class
	 * @param verbose     if true - print results on console
	 */
	public FuzzyTestWorker(double a, double b, int sizePackage, int segments, boolean verbose, FuzzyTest test) {
		super();
		this.a = a;
		this.b = b;
		this.sizePackage = sizePackage;
		this.verbose = verbose;
		this.test = test;
		this.segments = segments;
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
		//loadData_oneFromEachClass();
		//loadData_oneFromAll();
		//loadData_random();
		initializeFuzzySystem();
		buildRules();
		testFS();

		stop = System.currentTimeMillis();

		test.addResult(new TestResult(success, failed, stop - start));

	}

	private void loadData_leaveOneOut() {

		learnPkg = new DataPackage();
		DataPackage bottom = new DataPackage();
		
		String segs = "" + segments;
		if (segs.length() < 2) {
			segs = "0" + segs;
		}
		
		learnPkg.loadTextFile("data/features/SEG"+segs+"_T.txt");
		bottom.loadTextFile("data/features/SEG"+segs+"_B.txt");
		DataPackage rest = learnPkg.splitByColumn(learnPkg.getMaxRowSize()-2);
		learnPkg.merge(bottom);

		//get test vector for Leave One Out method
		testPkg = learnPkg.removeVector(sizePackage);
		//learnPkg.add(testPkg); //for 100% verification only
		
		qualityPkg = new DataPackage();
		bottom = new DataPackage();

		qualityPkg.loadTextFile("data/quality/SEG"+segs+"_T.txt");
		bottom.loadTextFile("data/quality/SEG"+segs+"_B.txt");
		rest = qualityPkg.splitByColumn(qualityPkg.getMaxRowSize()-2);
		qualityPkg.merge(bottom);

		qualityTestPkg = qualityPkg.removeVector(sizePackage);
		//qualityPkg.add(qualityTestPkg); //for 100% verification only
		
		if (verbose) {
			System.out.println(learnPkg);
			System.out.println(testPkg);
		}
	}
	
	private void initializeFuzzySystem() {
		
		//1. create ReasoningSystem
		
		SystemConfig config = new SystemConfig();

		config.setInputWidth(14*segments); // number of inputs
		config.setOutputWidth(100);// number of outputs

		// number of premises (it is dynamically extended if needed so it can be not
		// set)
		config.setNumberOfPremiseSets(10000*segments);
		config.setNumberOfConclusionSets(100);

		config.setIsOperationType(TNorm.TN_PRODUCT);
		config.setAndOperationType(TNorm.TN_PRODUCT);
		config.setOrOperationType(SNorm.SN_PROBABSUM);
		config.setImplicationType(TNorm.TN_PRODUCT);
		config.setConclusionAgregationType(SNorm.SN_PROBABSUM);
//		config.setConclusionAgregationType(Average.SUM);
		config.setTruthCompositionType(TNorm.TN_PRODUCT);
		config.setAutoDefuzzyfication(false);
		config.setDefuzzyfication(DefuzMethod.DF_COG);
		config.setAutoAlpha(true);
		config.setTruthPrecision(0.001, 0.0001);

		rs = new ReasoningSystem(config);

		//2. create conclusions
		
		FuzzySet fuzzyfier = new FuzzySet();
		fuzzyfier.newTriangle(0, 0.5);
		// fuzzyfier.IncreaseYPrecision(0.1, 0.001);

		// fuzzy conclusions (regular fuzzy sets)
		for (int i=0; i<100; i++) {
			FuzzySet conclusion = new FuzzySet(fuzzyfier);
			conclusion.setId(""+(i+1));
			conclusion.fuzzyfy(1);
			rs.addConclusionSet(conclusion);
		}
		
		//3. configure inputs and outputs 
		
		for(int i=0; i<(14*segments); i++) {
			rs.describeInputVar(i, "wej"+(i+1), "");
//			rs.getInputVar(i).fuzz = new FuzzySet().newTriangle(0, 1/b);
//			rs.getInputVar(i).fuzz = new FuzzySet().newGaussian(0, 1/b);
//			rs.getInputVar(i).fuzz = new FuzzySet().newTriangle(0, learnPkg.getColumnRange(i)/b);
			rs.getInputVar(i).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(i) / b);
		}
		for(int i=0; i<100; i++) {
			rs.describeOutputVar(i, "wyj"+(i+1), "");
		}
	}

	private void buildRules() {
		int x = 0;

		// listy nazw przes�anek dla kazdej z klas
		Map<String, List<String>> nazwy = new HashMap<String, List<String>>();
		for (int i = 0; i < 100; i++) {
			nazwy.put("c" + (i + 1), new LinkedList<String>());
		}

		try {
			Iterator<DataVector> quality_it = qualityPkg.getList().iterator();
			for (DataVector vector : learnPkg.getList()) {

				DataVector q_vector = quality_it.next();
				
				// numer klasy to ostatnia pozycja
				int klasa = vector.get(vector.size()-1).intValue();

				// dla pozosta�ych pozycji wiersza
				for (int i = 0; i < (vector.size()-1); i++) {
					double Axi = vector.get(i); // pobierz kolejny parametr
					// wczytaj jego zakres
					double Zx = learnPkg.getColumnRange(i);
					if (Zx < 0.001) Zx = 0.001;
					// zbuduj nazw� zbioru przes�anki
					String nazwa = "fs_a" + (i+1) + "_" + "x" + (x+1);
					FuzzySet temp = new FuzzySet(nazwa, "");
					
					//pobierz jako��
//					double quality = 1.0;
					double quality = q_vector.get(i);
					if (quality < 0.001) quality = 0.001;
					
							
					// zdefiniuj funkcj� przynalezno�ci

					//temp.newTriangle(Axi, Zx / a);
					//temp.newGaussian(Axi, (Zx / a) / quality);
					temp.newGaussian(Axi, (Zx / a) / quality);
					//temp.newTrapezium(Axi, (Zx/a), (Zx/a) / 10.0 );

					// dodaj zbi�r jako przes�ank�
					rs.addPremiseSet(temp);
				}

				// dodaj regu�� w oparciu o wygenerowane przes�anki (dla wszytskich parametr�w)
				rs.addRule(14*segments, 1);

				// 7*segments po��cze� AND parami - ka�dy wynik na stos
				for (int i=1; i<=14*segments; i=i+2) {
					rs.addRuleItem("wej"+i, "fs_a"+i+"_x" + (x+1), "AND", "wej"+(i+1), "fs_a"+(i+1)+"_x" + (x+1));					
				}
				// (7*segments-1) operacji AND dla wynik�w na stosie
				for (int i=0; i<((7*segments)-1); i++) {
					rs.addRuleItem("STACK", "", "AND", "STACK", "");					
				}
				
				rs.addRuleConclusion("wyj" + klasa, "" + klasa);

				// dodaj nazw� przes�anki do zbioru odpowiedniej klasy
				// ( na potrzeby openLeftAndRightDescription() )
				nazwy.get("c" + klasa).add("x" + (x+1));
				
//				if (x % 10 == 0) {
//					System.out.print("");
//				}

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

	private void openLeftAndRightDescription(ReasoningSystem rs, Map<String, List<String>> nazwy) {
		// loop through all parameters
		for (int i = 0; i < 14*segments; i++) {
			String fs_name = "fs_a" + (i + 1);
			try {
				// loop through all classes
				for (int j = 0; j < 100; j++) {
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
			Iterator<DataVector> quality_it = qualityTestPkg.getList().iterator();
			for (DataVector object : testPkg.getList()) {

				DataVector q_vector = quality_it.next();

				int class_obtained = 0;

				int class_real = object.get(object.size()-1).intValue();

				for (int z = 0; z < 14*segments; z++) {
//					double quality = q_vector.get(z);					
//					if (quality < 0.001) quality = 0.001;
//					rs.getInputVar(z).fuzz = new FuzzySet().newGaussian(0, (learnPkg.getColumnRange(z) / b) / quality);
					rs.setInput(z, object.get(z));
				}
				rs.Process();

				double max_result = -1.0;
				for (int i = 0; i < 100; i++) {
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
				 
//				DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
//				otherSymbols.setDecimalSeparator('.');
//				DecimalFormat df = new DecimalFormat("0.00000", otherSymbols);

//				System.out.println();
//				String line = "idx"+sizePackage+"\t o"+class_obtained+"\t r" + class_real + "\t";
//				for (int c=0; c<100; c++) {
//					line += df.format(rs.getOutputVar(c).outset.getMaximumMembership())+"\t";
//				}
//				System.out.println(line);
				//System.out.println("----------------------------------------------------");
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

	private void printParametersOfReasoningSystem(ReasoningSystem rs, Map<String, List<String>> nazwy) {
		for (int i = 0; i < 14*segments; i++) {
			String fs_name = "fs_a" + (i + 1);
			System.out.println("___ parameter: " + fs_name);
			try {
				// loop through all classes
				for (int j = 0; j < 100; j++) {
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
