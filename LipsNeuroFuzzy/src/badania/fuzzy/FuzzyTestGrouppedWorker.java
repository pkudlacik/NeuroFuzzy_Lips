package badania.fuzzy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import badania.TestResult;
import fuzzlib.DefuzMethod;
import fuzzlib.FuzzySet;
import fuzzlib.norms.SNMax;
import fuzzlib.norms.SNorm;
import fuzzlib.norms.TNorm;
import fuzzlib.reasoning.ReasoningSystem;
import fuzzlib.reasoning.SystemConfig;
import neuralnetwork.data.DataPackage;
import neuralnetwork.data.DataVector;

public class FuzzyTestGrouppedWorker extends Thread {

	private double a = 10;
	private double b = 2;
	private int sizePackage = 5;
	private boolean verbose = false;

	private int success = 0;
	private int failed = 0;

	private long start;
	private long stop;

	private FuzzyTest test;

	DataPackage learnPkg;
	DataPackage testPkg;

	private ReasoningSystem rs;

	/**
	 * Configure test
	 * 
	 * @param a           range divider (the higher a - the narrower the set
	 *                    description.)
	 * @param sizePackage number of learning samples taken from each class
	 * @param verbose     if true - print results on console
	 */
	public FuzzyTestGrouppedWorker(double a, double b, int sizePackage, boolean verbose, FuzzyTest test) {
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

		loadData();
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

		testPkg.add(test2);
		testPkg.add(test3);

		learnPkg.add(learn2);
		learnPkg.add(learn3);

		if (verbose) {
			System.out.println(learnPkg);
			System.out.println(testPkg);
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

//		rs.getInputVar(0).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(1) / b);
//		rs.getInputVar(1).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(2) / b);
//		rs.getInputVar(2).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(3) / b);
//		rs.getInputVar(3).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(4) / b);
//		rs.getInputVar(4).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(5) / b);
//		rs.getInputVar(5).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(6) / b);
//		rs.getInputVar(6).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(7) / b);
//		rs.getInputVar(7).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(8) / b);
//		rs.getInputVar(8).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(9) / b);
//		rs.getInputVar(9).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(10) / b);
//		rs.getInputVar(10).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(11) / b);
//		rs.getInputVar(11).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(12) / b);
//		rs.getInputVar(12).fuzz = new FuzzySet().newGaussian(0, learnPkg.getColumnRange(13) / b);

		rs.getOutputVar(0).id = "wyj1";
		rs.getOutputVar(1).id = "wyj2";
		rs.getOutputVar(2).id = "wyj3";

		rs.addConclusionSet(class1);
		rs.addConclusionSet(class2);
		rs.addConclusionSet(class3);
	}

	private void buildRules() {

		// lista przes³anek dla kazdej z klas
		FuzzySet[][] premises = new FuzzySet[3][13];

		SNorm snorm = new SNMax();

		FuzzySet temp_container = new FuzzySet();

		for (DataVector vector : learnPkg.getList()) {

			// nazwa klasy to pierwsza pozycja
			int klasa = (int) vector.get(0);

			// dla pozosta³ych pozycji wiersza
			for (int i = 1; i < vector.size(); i++) {

				double Axi = vector.get(i); // pobierz kolejny parametr
				// wczytaj jego zakres
				double Zx = learnPkg.getColumnRange(i);

				FuzzySet temp = new FuzzySet();

				// zdefiniuj funkcjê przynaleznoœci

				// temp.newTriangle(Axi, Zx / a);
				temp.newGaussian(Axi, Zx / a);
				// temp.newTrapezium(Axi, (Zx/a), (Zx/a) / 2.0 );

				// agreguj zbiór do odp. klasy i przes³anki
				if (premises[klasa - 1][i - 1] == null) { // gdy nowy zbiór
					// zbuduj nazwê zbioru przes³anki
					String nazwa = "fs_a" + i + "_c" + klasa;
					temp.setId(nazwa);
					premises[klasa - 1][i - 1] = temp;
				} else {
					FuzzySet.processSetsWithNorm(temp_container, temp, premises[klasa - 1][i - 1], snorm);
					premises[klasa - 1][i - 1].assign(temp_container);
				}
			}
		}

		// 1. dodaj stworzone przes³anki do systemu
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 13; j++) {
				rs.addPremiseSet(premises[i][j]);
			}
		}

		// 2. dodaj trzy regu³y :

		try {
			for (int i = 0; i < 3; i++) {

				// dodaj regu³ê w oparciu o wygenerowane przes³anki (dla wszytskich parametrów)
				rs.addRule(12, 1);

				rs.addRuleItem("wej0", "fs_a1_c" + (i + 1), "AND", "wej1", "fs_a2_c" + (i + 1));
				rs.addRuleItem("wej2", "fs_a3_c" + (i + 1), "AND", "wej3", "fs_a4_c" + (i + 1));
				rs.addRuleItem("wej4", "fs_a5_c" + (i + 1), "AND", "wej5", "fs_a6_c" + (i + 1));
				rs.addRuleItem("wej6", "fs_a7_c" + (i + 1), "AND", "wej7", "fs_a8_c" + (i + 1));
				rs.addRuleItem("wej8", "fs_a9_c" + (i + 1), "AND", "wej9", "fs_a10_c" + (i + 1));
				rs.addRuleItem("wej10", "fs_a11_c" + (i + 1), "AND", "wej11", "fs_a12_c" + (i + 1));

				rs.addRuleItem("wej12", "fs_a13_c" + (i + 1), "AND", "STACK", "");

				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");
				rs.addRuleItem("STACK", "", "AND", "STACK", "");

				rs.addRuleConclusion("wyj" + (i + 1), "" + (i + 1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// *********** Print parameters for all classes
//		System.out.println();
//		System.out.println("!!! Before opening slopes");
//		printParametersOfReasoningSystem(rs,nazwy);

		openLeftAndRightDescription(rs);

		// *********** Print parameters for all classes
//		System.out.println();
//		System.out.println("!!! After opening slopes");
//		printParametersOfReasoningSystem(rs,nazwy);
//		System.out.println();
	}

	private static void openLeftAndRightDescription(ReasoningSystem rs) {
		// loop through all parameters (1-13)
		for (int i = 0; i < 13; i++) {
			String fs_name = "fs_a" + (i + 1);

			// loop through all classes looking for l-most and r-most

			try {
				// assume that first is leftMost and rightMost
				FuzzySet leftMost = rs.getPremiseSet(fs_name + "_c1");
				FuzzySet rightMost = leftMost;
				// loop through the rest and search
				for (int j = 1; j < 3; j++) {
					FuzzySet fs_temp = rs.getPremiseSet(fs_name + "_c" + (j + 1));
					if (fs_temp.getFirstPoint().x < leftMost.getFirstPoint().x) {
						leftMost = fs_temp;
					}
					if (fs_temp.getLastPoint().x > rightMost.getLastPoint().x) {
						rightMost = fs_temp;
					}
				}

				leftMost.openLeftSlope();
				leftMost.PackFlatSections();
				rightMost.openRightSlope();
				rightMost.PackFlatSections();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void testFS() {

		success = 0;
		failed = 0;

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

	private static void printParametersOfReasoningSystem(ReasoningSystem rs) {
		for (int i = 0; i < 13; i++) {
			String fs_name = "fs_a" + (i + 1);
			System.out.println("___ parameter: " + fs_name);
			try {
				// loop through all classes
				for (int j = 0; j < 3; j++) {
					FuzzySet fs_temp = rs.getPremiseSet(fs_name + "_c" + (j + 1));
					System.out.println(fs_temp.getId() + "[" + fs_temp.getSize() + "]: " + fs_temp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
