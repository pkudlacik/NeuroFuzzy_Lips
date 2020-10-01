package badania.main;

import badania.fuzzy.FuzzyTest;

public class MainFuzzyTest {

	public static void main(String[] args) {
		
		FuzzyTest test = new FuzzyTest();
		
		test.setSimpleResults(true);
		test.setShowOnlyBestResults(false);
//		test.setBestLevel(90.0);
		test.setBestRange(2.0);
		test.setThreads(4);
		
		// Clusteryzowane: 25 - 93.5%(A:1.5), 20 - 93%(A:1.1), 15 - 92% (A:1.0), 10 - 91% (A:0.7), 5 - 

		//dla clusteryzowanych
//		test.doTest(0.8, 0.8, 0.1, // A - start, stop, step
//					23, 23, 0.1, // B - start, stop, step
//					10, 10, // size - start, stop
//					100 ); // runs in one set

//		test.doTestLeaveOneOut(4.5, 23, 178); //92.7% - 10 clusters, a-4.5

		// Grupowane:
		
		//dla grupowanych
//		test.doTest(1.0, 20.0, 1.0, // A - start, stop, step
//					24, 24, 1.0, // B - start, stop, step
//					10, 30, // size - start, stop
//					10 ); // runs in one set

		// Pojedyncze (dane znormalizowane): 20 i wyzej - 93.7%(A:3.5), 15 - 93.7%(A:3.5), 10 - 92.5%(A:2.6), 5 - 90.5%(A:2.3) 
		// Pojedyncze: 20 i wyzej - 93.8-94,5%(A:2.8-5.0), 15 - 94,0%(A:3.3), 10 - 93.5%(A:2.5), 5 - 91.7%(A:2.3) 
		
		// dla pojedynczych
//		test.doTest(3.5, 3.5, 0.1, // A - start, stop, step
//				23, 23, 0.5, // B - start, stop, step
//				15, 15, // size - start, stop
//				100 ); // runs in one set

		//for (double a = 2.0; a < 10.0; a += 0.5)
			test.doTestLeaveOneOut(3.0, 20, 1);
			
		//20 segmentów	
			
		//3	20	354	246	59	41	0	0	3701.422
		//The best: 59%, A: 3.0, B: 20 - without quality
			
		//2	20	381	219	63.5	36.5	0	0	3841.205
		//The best: 83.5%, A: 3.0, B: 20 - quality,
			
		//40 segmentów
		
		// 98.167%, A: 3.0, B: 20 - quality,
			
			
	}

}
