/**
 * @Author: Sagar Joshi
 *  Problem Statement:
 * 1. There are i interviewers and c candidates
 * 2. There are only 2 interviewers allowed for each interview
 * 3. All interviewers must interview the candidate only once
 * 4. The same interview pair should not repeat immediately for the next candidate
 * 5. The interview pair can can repeat after all other options are exhausted
 * Provide an interview schedule for all the c candidates
 * */

import java.util.*;


public class InterviewScheduler {
    private final PriorityQueue<int[]> pq;
    public  InterviewScheduler () {
        pq = new PriorityQueue<>(Comparator.comparingInt(o -> o[2]));
    }
    public HashMap<Integer, List<List<Integer>>> findCombinations(int interviewers, int interview, int candidates) {
        //1. find all the combinations
        List<Integer> interviewerList = new ArrayList<>();
        for (int i = 0; i < interviewers; i++) {
            interviewerList.add(i);
        }
        List<int[]> combinations = new ArrayList<>();
        generateCombinations(interviewerList, 0, interview, new ArrayList<>(), combinations);

        //2. Put them in a map key = index of the combination and value will  be freq used.
        for (int[] c : combinations) {
            pq.offer(new int[]{c[0], c[1], 0});
        }

        //3. Iterate through these candidates.
        return allocateInterviewers(interviewers, candidates);
    }

    private HashMap<Integer, List<List<Integer>>> allocateInterviewers(int interviewers, int candidates) {
        HashMap<Integer, HashSet<Integer>> candidateMap = new HashMap<>();
        HashMap<Integer, List<List<Integer>>> answer = new HashMap<>();
        for (int i = 0; i < candidates; i++) {
            while (!pq.isEmpty()) {
                int[] comb = pq.poll();
                if (!candidateMap.containsKey(i)) {
                    HashSet<Integer> set = new HashSet<>();
                    set.add(comb[0]);
                    set.add(comb[1]);
                    candidateMap.put(i, set);
                    List<List<Integer>> list = answer.getOrDefault(i, new ArrayList<>());
                    list.add(List.of(comb[0], comb[1]));
                    answer.put(i, list);
                    pq.offer(new int[]{comb[0], comb[1], comb[2] + 1});
                } else {
                    HashSet<Integer> set = candidateMap.get(i);
                    if (!set.contains(comb[0]) && !set.contains(comb[1])) {
                        set.add(comb[0]);
                        set.add(comb[1]);
                        candidateMap.put(i, set);
                        List<List<Integer>> list = answer.getOrDefault(i, new ArrayList<>());
                        list.add(List.of(comb[0], comb[1]));
                        answer.put(i, list);
                        pq.offer(new int[]{comb[0], comb[1], comb[2] + 1});
                    } else {
                        pq.offer(new int[]{comb[0], comb[1], comb[2] + 1});
                    }
                }
                if (candidateMap.get(i).size() == interviewers) {
                    break;
                }
            }
        }
        return answer;
    }
    private void generateCombinations(List<Integer> interviewers, int index, int n, List<Integer> currentList, List<int[]> combinations) {
        //Generates combinations
        if (currentList.size() == n) {
            int[] combination = new int[n];
            for (int i = 0; i < n; i++) {
                combination[i] = currentList.get(i);
            }
            combinations.add(combination);
        }

        for (int i = index; i < interviewers.size(); i++) {
            Integer pivot = i;
            currentList.add(pivot);
            generateCombinations(interviewers, i + 1, n, currentList, combinations);
            currentList.remove(pivot);
        }
    }

}
