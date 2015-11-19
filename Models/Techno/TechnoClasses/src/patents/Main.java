/**
 * 
 */
package patents;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class Main {
	
	
	public static HashMap<String,LinkedList<Patent>> constructSortedClasses(String file){
		HashMap<String,HashSet<Patent>> classes = Reader.importFromFile(file);
		//System.out.println(classes.keySet().size());
		
		/*
		long bound = 0;
		for(HashSet<Patent> h:classes.values()){
			//System.out.println(h.size());
			bound+=h.size()*h.size();
		}
		//System.out.println(bound);
		*/
		
		return sortClasses(classes);
	
		
		
	}
	
	
	/**
	 * overlap assuming list are sorted.
	 * 
	 * @param l1
	 * @param l2
	 * @return
	 */
	private static LinkedList<Patent> overlap(LinkedList<Patent> l1,LinkedList<Patent> l2){
		LinkedList<Patent> res = new LinkedList<Patent>();
		Iterator<Patent> i1 = l1.iterator();Iterator<Patent> i2 = l2.iterator();
		Patent current1 = i1.next(),current2 = i2.next();
		while(i1.hasNext()&&i2.hasNext()){
			if(current1.equals(current2)){
				res.add(current1); current1 = i1.next();current2 = i2.next();
			}else{
				if(current1.compareTo(current2)<0){
					current1 = i1.next();
				}else{
					current2 = i2.next();
				}
			}
		}
		return res;
	}
	
	private static Patent[] overlap_array(LinkedList<Patent> l1,LinkedList<Patent> l2){
		LinkedList<Patent> res = overlap(l1,l2);
		Patent[] resarray = new Patent[res.size()];
		int i=0;for(Patent p:res){resarray[i]=p;i++;}
		return resarray;
	}
	
	
	/**
	 * 
	 * @param classes
	 * @return
	 */
	private static HashMap<String,LinkedList<Patent>> sortClasses(HashMap<String,HashSet<Patent>> classes){
		HashMap<String,LinkedList<Patent>> sortedClasses = new HashMap<String,LinkedList<Patent>>();
		for(String k:classes.keySet()){
			LinkedList<Patent> l = new LinkedList<Patent>(classes.get(k));
			System.out.println("sorting class : "+l.size());
			Collections.sort(l);
			sortedClasses.put(k, l);
		}
		return sortedClasses;
	}
	
	
	private static void computeOverlap(HashMap<String,LinkedList<Patent>> sortedClasses){
		int n = sortedClasses.keySet().size();
		int[][] overlapMatrix = new int[n][n];
		String[] classNames = new String[n];
		int[] classSizes = new int[n];
		int k = 0;
		// classes names and sizes
		for(String s:sortedClasses.keySet()){classNames[k]=s;classSizes[k]=sortedClasses.get(s).size();k++;}
		
		for(int i=0;i<n-1;i++){
			System.out.println(i);
			overlapMatrix[i][i]=classSizes[i];
			for(int j=i+1;j<n;j++){
				overlapMatrix[i][j]=(overlap_array(sortedClasses.get(classNames[i]),sortedClasses.get(classNames[j]))).length;
			}
		}
		
		overlapMatrix[n-1][n-1]=classSizes[n-1];
		
		for(int i=1;i<n;i++){
			for(int j=0;j<i;j++){
				overlapMatrix[i][j]=overlapMatrix[j][i];
			}
		}
		
		
		Writer.writeCSV(overlapMatrix, "res/overlap.csv");
		Writer.writeCSV(classSizes, "res/sizes.csv");
	}
	
	
	/**
	 * 
	 * 
	 * 
	 * @param sortedClasses
	 */
	private static void computeSecondOrderOverlaps(HashMap<String,LinkedList<Patent>> sortedClasses){
		int n = sortedClasses.keySet().size();
		//int[][] overlaps = new int[n][4];
		// use list, more simple (should be n*(n-1)/2 * (n-2) + n (for classes sizes)
		int[][] overlaps = new int[(n*(n-1)*(n-2)/2) + n][4];
		String[] classNames = new String[n];
		int r = 0;for(String s:sortedClasses.keySet()){classNames[r]=s;r++;}
		
		r = 0;
		for(int i=0;i<n-1;i++){
			System.out.println(i);
			for(int j=i;j<n;j++){
				LinkedList<Patent> overlap2 = overlap(sortedClasses.get(classNames[i]),sortedClasses.get(classNames[j]));
				// then computes snd order overlap
				System.out.println("Sorting overlap : "+overlap2.size());
				Collections.sort(overlap2);
				for(int k=j;k<n;k++){
					if(i==j&&j==k){
						// add class size
						overlaps[r][0]=i;overlaps[r][1]=i;overlaps[r][2]=i;overlaps[r][3]=sortedClasses.get(classNames[i]).size();
					}else{
						// check only if k ≠ i and k ≠ j -> will have some repetitions
						// -> having k > j should do the trick : take k >= j
						LinkedList<Patent> overlap3 = overlap(overlap2,sortedClasses.get(classNames[k]));
						overlaps[r][0]=i;overlaps[r][1]=j;overlaps[r][2]=k;overlaps[r][3]=overlap3.size();
					}
					r++;
				}
			}
		}
		
		Writer.writeCSV(overlaps, "res/overlap_snd_order.csv");
	}
	
	
	
	
	
	/**
	 * Techno distances on overlapping patents
	 * 
	 *   -- DOES NOT WORK WITH 32G Memory --
	 * 
	 * 
	 */
	private static void computeDistanceOnOverlap(HashMap<String,LinkedList<Patent>> sortedClasses){
		System.out.println("Computing distances on overlaps...");
		
		int n = sortedClasses.keySet().size();
		String[] classNames = new String[n];
		int k = 0;
		for(String s:sortedClasses.keySet()){classNames[k]=s;k++;}
		
		HashSet<TechnoDistance> distances = new HashSet<TechnoDistance>();
		for(int i=0;i<n-1;i++){
			System.out.println(i);
			System.out.println(" dists : "+distances.size());
			for(int j=i+1;j<n;j++){
				System.out.println(j);
				Patent[] overlap = overlap_array(sortedClasses.get(classNames[i]),sortedClasses.get(classNames[j]));
				System.out.println("  s : "+overlap.length);
				for(int p1 = 0;p1<overlap.length - 1;p1++){
					for(int p2=p1+1;p2<overlap.length;p2++){
						distances.add(new TechnoDistance(overlap[p1],overlap[p2]));
					}
				}
			}
		}
		
		Writer.writeSet(distances, "res/distances_on_overlaps.csv");
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap<String,LinkedList<Patent>> sortedClasses = constructSortedClasses("../../../Data/raw/classesTechno/class.csv");
		//computeOverlap(sortedClasses);
		//computeDistanceOnOverlap(sortedClasses);
		computeSecondOrderOverlaps(sortedClasses);
	}

}
