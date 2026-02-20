//package com.test;
//import java.util.StringTokenizer;
//public class CommaSeprated {
//
//    public static  void  main(String args[]){
//
//        String s = "banana,mango,apple,orange";
//        StringTokenizer t = new StringTokenizer(s,",");
//        for (; t.hasMoreTokens(); ) {
//            String token = t.nextToken();
//            System.out.println(token);
//        }
//    }
//}


package com.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class CommaSeprated {
    public static void main(String[] args) {
        String filePath = "data.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, ",");

                if (isHeader) {
                    System.out.println("Headers:");
                    isHeader = false;
                } else {
                    System.out.println("Data:");
                }

                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    System.out.print(token + "  ");
                }
                System.out.println("\n-----------------------------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
