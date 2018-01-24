package com.silviuned.service.data;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.silviuned.model.UserRating;

public class DataLoader {


    public static final String TRAINING_DATA_PATH = "E:\\Development\\Facultate\\Disertatie\\Implementation\\training\\";
    public static final String TEST_DATA_PATH = "E:\\Development\\Facultate\\Disertatie\\Implementation\\test\\";

    // Loads the data from the provided file.
    public static Map<Integer, List<UserRating>> loadData(String folderPath) {

        Map<Integer, List<UserRating>> result = new HashMap<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                BufferedReader br = null;
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    br = new BufferedReader(fr);

                    String line = br.readLine();
                    int movieId = Integer.valueOf(line.split(":")[0]);

                    while ((line = br.readLine()) != null) {
                        String[] split = line.split(",");

                        int userId = Integer.valueOf(split[0]);
                        UserRating userRating = new UserRating(userId, Short.valueOf(split[1]), split[2]);

                        if (result.containsKey(movieId)) {
                            result.get(movieId).add(userRating);
                        } else {
                            List<UserRating> list = new ArrayList<>();
                            list.add(userRating);
                            result.put(movieId,  list);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (br != null)
                            br.close();
                        if (fr != null)
                            fr.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            throw new IllegalStateException();
        }

        return result;
    }
}
